package me.lizhen.solvers

import com.mongodb.reactivestreams.client.ClientSession
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import me.lizhen.schema.*
import toIndexedPair
import toInvertedMap
import kotlin.coroutines.coroutineContext

suspend fun IdeographContext.getOptimizedEntry(
    clientSession: ClientSession,
    nodeConstraintPairs: List<Pair<PatternNode, List<PatternConstraint>>>
): Pair<Int, Long> {
    val collectedPairs: MutableMap<Int, Long> = mutableMapOf()

    val channel = Channel<Pair<Int, Long>>()
    nodeConstraintPairs.toIndexedPair().forEach {
        CoroutineScope(Dispatchers.IO).launch {
            val count = countConstrainedNodes(clientSession, it.second.first, *it.second.second.toTypedArray())
            channel.send(Pair(it.first, count))
        }
    }

    repeat(nodeConstraintPairs.size) {
        val pair = channel.receive()
        val (index, count) = pair
        collectedPairs[index] = count

        val thisEntryHasConstraints = nodeConstraintPairs[index].second.isNotEmpty()
        if (thisEntryHasConstraints || count<100) {
            coroutineContext.cancelChildren()
            return pair
        }
    }
    coroutineContext.cancelChildren()
    return collectedPairs.minByOrNull { it.value }!!.toPair()
}


@OptIn(ExperimentalCoroutinesApi::class)
fun IdeographContext.getBestEntry(
    clientSession: ClientSession,
    nodeConstraintPairs: List<Pair<PatternNode, List<PatternConstraint>>>,
    requiresAllElementDistinct: Boolean = true
): Pair<Int, Long> = runBlocking {
    withContext(Dispatchers.IO) {
        val channels = nodeConstraintPairs.toIndexedPair().map {
            produce {
                val count = countConstrainedNodes(clientSession, it.second.first, *it.second.second.toTypedArray())
                send(Pair(it.first, count))
            }
        }
        val bestEntry = select<Pair<Int, Long>> {
            channels.map { it.onReceive { res -> res } }
        }
        coroutineContext.cancelChildren()
        bestEntry
    }
}


suspend fun IdeographContext.solvePatternBatched(pattern: Pattern): List<PatternSolution> =
    mongoService.startSession().use { session ->
        println(pattern)
        val patternNodeDict = pattern.nodes.associateBy { it.patternId }
        val patternNodeIndexDict = pattern.nodes.toInvertedMap { it.patternId }

        val patternEdgeDict = pattern.edges.orEmpty().associateBy { it.patternId }
        val nodeConstraintPairs = pattern.nodes.map {
            Pair(
                it, pattern.constraints?.filter { pc -> pc.targetPatternId == it.patternId }.orEmpty()
            )
        }
        val nodeConstraintPair = pattern.nodes.associate {
            Pair(
                it.patternId, pattern.constraints?.filter { pc -> pc.targetPatternId == it.patternId }.orEmpty()
            )
        }

        val entryNodePair = runBlocking {
            getOptimizedEntry(session, nodeConstraintPairs)
        }

        println(entryNodePair)

        /**
         * evaluate 1 pattern node batched
         * 1 query
         * @param solutionsToSolve batched
         */
        suspend fun evaluatePatternNode(
            /**
             * Same evaluation status
             */
            solutionsToSolve: List<PatternSolutionUnderEvaluation>,
            patternNodeIndex: Int
        ): List<PatternSolutionUnderEvaluation> {
            val batchedNodes = solutionsToSolve.map { it.nodes[patternNodeIndex] }
            val patternId = batchedNodes[0].first
            assert(batchedNodes.all { it.second == null }) { "The pattern node already evaluated." }

            val nodeCandidate = patternNodeDict[patternId]?.let {
                println(it)
                println(nodeConstraintPair[patternId])
                queryNodeWithConstraints(session, it, *nodeConstraintPair[patternId].orEmpty().toTypedArray()).toList()
            }
//        if (nodeCandidate.isNullOrEmpty()) return emptyList()
            return solutionsToSolve.flatMap {
                nodeCandidate.orEmpty().map { wn ->
                    val newNodes = it.nodes.toMutableList()
                    val index = newNodes.indexOfFirst { n -> n.first == patternId }
                    newNodes[index] = patternId to wn
                    it.copy(nodes = newNodes)
                }
            }
        }

        suspend fun evaluatePatternEdge(
            solutionsToSolve: List<PatternSolutionUnderEvaluation>,
            patternEdgeIndex: Int,
            onComplete: (newlyEvaluatedNodeIndices: List<Int>) -> Unit
        ): List<PatternSolutionUnderEvaluation> {
            val batchedEdges = solutionsToSolve.map { it.edges[patternEdgeIndex] }
            assert(batchedEdges.all { it.second == null }) { "The pattern edge already evaluated." }
            val patternId = batchedEdges[0].first
            val patternEdge = patternEdgeDict[patternId]!!

            val (fromIndex, toIndex) = solutionsToSolve[0].getNodeIndices(patternEdge)

            val (_, _fromSolutions) = solutionsToSolve.map { it.nodes[fromIndex] }.unzip()
            val (_, _toSolutions) = solutionsToSolve.map { it.nodes[toIndex] }.unzip()

            val fromSolutions = _fromSolutions.distinctBy { it?.nodeId }
            val toSolutions = _toSolutions.distinctBy { it?.nodeId }

            val fromNodesFilled = fromSolutions[0] != null
            val toNodesFilled = toSolutions[0] != null

            when {
                fromNodesFilled && toNodesFilled -> {
                    // 1 query, TODO: Ranges can be narrowed
                    val eCandidates = queryEdges(
                        session,
                        patternEdge.type,
                        nodeFrom = fromSolutions.requireNoNulls(),
                        nodeTo = toSolutions.requireNoNulls()
                    ).toList()
                    return (
                            if (eCandidates.isEmpty()) emptyList() // dead
                            else solutionsToSolve.flatMap { solutionToSolve ->
                                eCandidates.map {
                                    solutionToSolve.copy(edges = solutionToSolve.getUpdatedEdges(patternEdge, it))
                                }
                            })
                        .also { onComplete(emptyList()) }
                }
                fromNodesFilled -> {
                    // 2 query
                    val eCandidates = queryEdges(
                        session,
                        patternEdge.type, nodeFrom = fromSolutions.requireNoNulls()
                    ).toList()
                    if (eCandidates.isEmpty()) return emptyList()
                    else {
                        val toPattern = patternNodeDict[patternEdge.toPatternId]!!


                        val toTypeName = toPattern.type
                        val filteredToCandidates = queryNodeWithConstraints(
                            session,
                            toTypeName,
                            eCandidates.map { e -> e.toId }.distinct(),
                            *nodeConstraintPair[toPattern.patternId].orEmpty().toTypedArray()
                        ).toList()

//                        val filteredToCandidates = toCandidates
/*                            toCandidates.filter {
                            nodeConstraintPair[toPattern.patternId].validate(it)
                        }*/

                        val fromNodeIndex = patternNodeIndexDict[patternEdge.fromPatternId]!!

                        val eCandidateDictByTo = eCandidates.associateBy { it.toId }

                        val solutionDict = solutionsToSolve.groupBy {
                            it.nodes[fromNodeIndex].second!!.nodeId
                        }


                        // match candidate to solution needed!!
                        return filteredToCandidates.flatMap { wn ->
                            val correspondingEdge =
                                eCandidateDictByTo[wn.nodeId]!!//eCandidates.firstOrNull { e -> e.toId == wn.nodeId }!!
                            val solutionsToFill = solutionDict[correspondingEdge.fromId]
                            return@flatMap solutionsToFill?.map {
                                PatternSolutionUnderEvaluation(
                                    nodes = it.getUpdatedNodes(toPattern, wn),
                                    edges = it.getUpdatedEdges(patternEdge, correspondingEdge)
                                )
                            }.orEmpty()
                        }.also { onComplete(listOf(toIndex)) }

                    }
                }
                toNodesFilled -> {
                    // 2 query
                    val eCandidates =
                        queryEdges(session, patternEdge.type, nodeTo = toSolutions.requireNoNulls()).toList()
                    if (eCandidates.isEmpty()) return emptyList()
                    else {
                        val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
                        val fromTypeName = fromPattern.type
                        val filteredFromCandidates = queryNodeWithConstraints(
                            session,
                            fromTypeName,
                            eCandidates.map { e -> e.fromId }.distinct(),
                            *nodeConstraintPair[fromPattern.patternId].orEmpty().toTypedArray()
                        ).toList()
//                        val filteredFromCandidates = fromCandidates.filter {
//                            nodeConstraintPair[fromPattern.patternId].validate(it)
//                        }
                        val toNodeIndex = patternNodeIndexDict[patternEdge.toPatternId]!!
                        val solutionDict = solutionsToSolve.groupBy {
                            it.nodes[toNodeIndex].second!!.nodeId
                        }
                        val eCandidateDictByFrom = eCandidates.associateBy { it.fromId }

                        return filteredFromCandidates.flatMap { wn ->
                            val correspondingEdge =
                                eCandidateDictByFrom[wn.nodeId]!!//eCandidates.firstOrNull { e -> e.toId == wn.nodeId }!!
                            val solutionsToFill = solutionDict[correspondingEdge.toId]
                            return@flatMap solutionsToFill?.map {
                                PatternSolutionUnderEvaluation(
                                    nodes = it.getUpdatedNodes(fromPattern, wn),
                                    edges = it.getUpdatedEdges(patternEdge, correspondingEdge)
                                )
                            }.orEmpty()
                        }.also { onComplete(listOf(fromIndex)) }
                    }
                }
                else -> {
                    // 2-3 query
                    val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
                    val toPattern = patternNodeDict[patternEdge.toPatternId]!!
                    val workspaceEdges = queryEdges(session, patternEdge.type).toList()
                    val fromNodeIds = workspaceEdges.map { it.fromId }
                    val toNodeIds = workspaceEdges.map { it.toId }
//                val fromNodes = queryNodeWithConstraints(fromPattern.type, fromNodeIds).associateBy { it.nodeId }
//                val toNodes = queryNodeWithConstraints(toPattern.type, toNodeIds).associateBy { it.nodeId }
                    val nodePool = if (fromPattern.type == toPattern.type) {
                        queryNodeWithConstraints(session, fromPattern.type, fromNodeIds + toNodeIds).toList()
                    } else {
                        // TODO: Parallel?
                        queryNodeWithConstraints(session, fromPattern.type, fromNodeIds).toList() +
                                queryNodeWithConstraints(session, toPattern.type, toNodeIds).toList()
                    }.associateBy { it.nodeId }
                    return solutionsToSolve.flatMap { solutionToSolve ->
                        workspaceEdges.map {
                            solutionToSolve.copy(
                                nodes = solutionToSolve.getUpdatedNodes(
                                    listOf(
                                        fromPattern.patternId to nodePool[it.fromId],
                                        toPattern.patternId to nodePool[it.toId]
                                    ),
                                ),
                                edges = solutionToSolve.getUpdatedEdges(patternEdge, it)
                            )
                        }
                    }.also { onComplete(listOf(fromIndex, toIndex)) }
                }
            }
        }

        val emptySolution = PatternSolutionUnderEvaluation(
            nodes = pattern.nodes.map { it.patternId to null },
            edges = pattern.edges.orEmpty().map { it.patternId to null }
        )

        var solutionPool = listOf(emptySolution)
        val firstPatternNodeId = nodeConstraintPairs[entryNodePair.first].first.patternId


        val edgePointIndices = pattern.edges.orEmpty().map { emptySolution.getNodeIndices(it) }
        val nodeAsFromIndices = pattern.nodes.map { emptyList<Int>().toMutableList() }
        val nodeAsToIndices = pattern.nodes.map { emptyList<Int>().toMutableList() }
        pattern.edges?.indices?.forEach { index ->
            nodeAsFromIndices[edgePointIndices[index].first] += index
            nodeAsToIndices[edgePointIndices[index].second] += index
        }
        val edgeEvaluatedFlags = pattern.edges.orEmpty().map { false }.toMutableList()
        val nodeEvaluatedFlags = pattern.nodes.map { false }.toMutableList()

        var evaluatedNodeIndices = listOf(patternNodeIndexDict[firstPatternNodeId]!!)
        nodeEvaluatedFlags[evaluatedNodeIndices[0]] = true

        solutionPool = runBlocking {
            evaluatePatternNode(
                solutionPool,
                evaluatedNodeIndices[0]
            )
        }

        while (true) {
            if (solutionPool.isEmpty() || solutionPool[0].isValid) break // all solutions are evaluated

            val edgeToEvaluate = evaluatedNodeIndices
                    .flatMap { nodeAsFromIndices[it] }
                    .firstOrNull { edgeIndex ->
                        !edgeEvaluatedFlags[edgeIndex]
                    }

                    ?: evaluatedNodeIndices
                        .flatMap { nodeAsFromIndices[it] }
                        .firstOrNull { edgeIndex ->
                            !edgeEvaluatedFlags[edgeIndex]
                        }

                    ?: edgeEvaluatedFlags
                        .indexOfFirst { !it }

            solutionPool = runBlocking {
                evaluatePatternEdge(
                    solutionPool,
                    edgeToEvaluate
                ) {
                    edgeEvaluatedFlags[edgeToEvaluate] = true
                    it.forEach { nid -> nodeEvaluatedFlags[nid] = true }
                    evaluatedNodeIndices = it
                }
            }.filter { it.isAllEvaluatedDistinct }
        }



        return solutionPool
            .distinctBy { it.uniqKey() }
            .mapNotNull { it.completed() }
//            .also {
//                it[0].edges.toList().forEachIndexed { i, e ->
//                    println("$i   ${e.second}\n   -   ${it[1].edges.toList()[i].second}\n\n\n")
//                }
//            }
            .also { session.close() }
    }

//        patternNodeQueues.forEach { entry ->
//            val patternNode = patternNodeDict[entry.key]
//            val targetingConstraints = nodeConstraintPair[entry.key]
//
//            if (entry.value <= EVALUABLE_CANDIDATE_LIMIT) {
//
//            }
//        }
//
//
//        val nodeCandidateLists = nodeConstraintPair.mapValues {
//            patternNodeDict[it.key]?.let { n ->
//                queryNodeWithConstraints(n, *it.value.toTypedArray())
//            }
//        }
//
//        nodeCandidateLists.forEach {
//            println(it.value?.size)
//        }
//
//        val nodesPool = nodeCandidateLists.flatMap { it.value.orEmpty() }.associateBy { it.nodeId }
//
//        /**
//         * assuming only one
//         */
//        val edgeCandidateLists = pattern.edges?.map { pe ->
//            val fromCandidates = nodeCandidateLists[pe.fromPatternId]
//            val toCandidates = nodeCandidateLists[pe.toPatternId]
//            if (fromCandidates.isNullOrEmpty() || toCandidates.isNullOrEmpty())
//                return@map emptyList()
//
//            val edgeTypeCandidates = patternNodeDict[pe.fromPatternId]?.let { from ->
//                patternNodeDict[pe.toPatternId]?.let { to ->
//                    getEdgeTypeCandidates(from, to)
//                }
//            }
//
//            println(edgeTypeCandidates?.map { it.name })
//
//            edgeTypeCandidates?.flatMap {
//                if (toCandidates.size >= MONGO_NODE_LIMIT) {
//                    return@flatMap queryEdges(it.name, fromCandidates)
//                } else {
//
//                    return@flatMap queryEdges(it.name, fromCandidates, toCandidates)
//                }
//            }.orEmpty()
//        }
//
//        if (edgeCandidateLists?.size != 1) {
//            return emptyList()
//        }
//
//
//        val solutions = edgeCandidateLists[0].mapNotNull { edge ->
//            val fromNode = nodesPool[edge.fromId]
//            val toNode = nodesPool[edge.toId]
//            if (fromNode == null || toNode == null) return@mapNotNull null
//            PatternSolution(
//                nodes = listOf(fromNode, toNode),
//                edges = listOf(edge)
//            )
//        }
//
//        return solutions


