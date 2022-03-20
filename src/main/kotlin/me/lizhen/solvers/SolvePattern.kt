package me.lizhen.solvers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import me.lizhen.schema.Pattern
import me.lizhen.schema.PatternConstraint
import me.lizhen.schema.PatternNode
import toIndexedPair
import toInvertedMap
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource.Monotonic.markNow
import kotlin.time.measureTime

@OptIn(ExperimentalCoroutinesApi::class)
fun IdeographContext.getBestEntry(
    nodeConstraintPairs: List<Pair<PatternNode, List<PatternConstraint>>>
): Pair<Int, Long> = runBlocking {
    withContext(Dispatchers.IO) {
        val channels = nodeConstraintPairs.toIndexedPair().map {
            produce {
                println("Started!!! ${it.first}")
                val count = countConstrainedNodes(it.second.first, *it.second.second.toTypedArray())
                println("\n\n\n\n coroutine returned: $count\n\n\n")
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

//: Flow<Pair<Int, Long>> = nodeConstraintPairs.toIndexedPair().asFlow()
//    .map { pair ->
//        val count = countConstrainedNodes(pair.second.first, *pair.second.second.toTypedArray())
//        println("\n\n\n\n---------$count\n\n\n")
//        Pair(pair.first, count)
//    }//.flowOn(Dispatchers.IO)


//    flow {
//    nodeConstraintPairs.forEachIndexed { index, (node, constraints) ->
////        CoroutineScope(Dispatchers.IO).launch {
//        val count = countConstrainedNodes(node, *constraints.toTypedArray())
//        emit(Pair(index, count))
////        }
//    }
//}


@OptIn(ExperimentalTime::class)
fun IdeographContext.solvePatternBatched(pattern: Pattern): List<PatternSolution> {
    val patternNodeDict = pattern.nodes.associateBy { it.patternId }
    val patternNodeIndexDict = pattern.nodes.toInvertedMap { it.patternId }

    val patternEdgeDict = pattern.edges.orEmpty().associateBy { it.patternId }
//    val patternEdgeIndexDict = pattern.edges?.toInvertedMap { it.patternId }

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


    val mark = markNow()

//    val patternNodeCandidateCounts = nodeConstraintPair.mapValues {
//        patternNodeDict[it.key]?.let { n ->
//            countConstrainedNodes(n, *it.value.toTypedArray())
//        } ?: Long.MAX_VALUE
//    }


    val entryNodePair = runBlocking {
        getBestEntry(nodeConstraintPairs)
    }

    val elapse = mark.elapsedNow()

    println("\n\n$elapse\n\n\n")

//    val patternNodeQueues = patternNodeCandidateCounts.entries.sortedBy { it.value }
//
//    if (patternNodeQueues[0].value >= IdeographContext.MONGO_NODE_LIMIT) {
//        throw Error("Pattern node candidates limit exceeded: $patternNodeQueues")
//    }


    /**
     * evaluate 1 pattern node
     * 1 query
     */
//    fun evaluatePatternNode(
//        solutionToSolve: PatternSolutionUnderEvaluation,
//        patternNodeIndex: Int
//    ): List<PatternSolutionUnderEvaluation> {
//        val (patternId, evaluated) = solutionToSolve.nodes[patternNodeIndex]
//        assert(evaluated == null) { "The pattern node already evaluated." }
//        nodeConstraintPair[patternId]?.let { it ->
//            val nCandidates = queryNodeWithConstraints(
//                patternNodeDict[patternId]!!,
//                *it.toTypedArray()
//            )
//            val filledSolutions = nCandidates.map { wn ->
//                val newNodes = solutionToSolve.nodes.toMutableList()
//                val index = newNodes.indexOfFirst { n -> n.first == patternId }
//                newNodes[index] = patternId to wn
//                solutionToSolve.copy(nodes = newNodes)
//            }
//            return filledSolutions
//        }
//        throw Error("The node has no constraint. Evaluation Aborted.")
//    }


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
            queryNodeWithConstraints(it, *nodeConstraintPair[patternId].orEmpty().toTypedArray())
        }
        if (nodeCandidate.isNullOrEmpty()) return emptyList()
        return solutionsToSolve.flatMap {
            nodeCandidate.map { wn ->
                val newNodes = it.nodes.toMutableList()
                val index = newNodes.indexOfFirst { n -> n.first == patternId }
                newNodes[index] = patternId to wn
                it.copy(nodes = newNodes)
            }
        }
    }


//    fun evaluatePatternEdge(
//        solutionToSolve: PatternSolutionUnderEvaluation,
//        patternEdgeIndex: Int,
//    ): List<PatternSolutionUnderEvaluation>? {
//        val (patternId, evaluated) = solutionToSolve.edges.getOrNull(patternEdgeIndex) ?: return null
//        assert(evaluated == null) { "The pattern edge already evaluated." }
//        val patternEdge = patternEdgeDict[patternId]!!
//        val (fromIndex, toIndex) = solutionToSolve.getNodeIndices(patternEdge)
//        val fromSol = solutionToSolve.nodes[fromIndex]
//        val toSol = solutionToSolve.nodes[toIndex]
//        when {
//            fromSol.second != null && toSol.second != null -> {
//                // 1 query
//                val eCandidates = queryEdges(
//                    patternEdge.type,
//                    nodeFrom = listOf(fromSol.second!!),
//                    nodeTo = listOf(toSol.second!!)
//                )
//                return if (eCandidates.isEmpty()) emptyList() // dead
//                else eCandidates.map {
//                    solutionToSolve.copy(edges = solutionToSolve.getUpdatedEdges(patternEdge, it))
//                }
//            }
//            fromSol.second != null -> {
//                // 2 query
//                val eCandidates = queryEdges(patternEdge.type, nodeFrom = listOf(fromSol.second!!))
//                if (eCandidates.isEmpty()) return emptyList()
//                else {
//                    val toPattern = patternNodeDict[patternEdge.toPatternId]!!
//                    val toTypeName = toPattern.type
//                    val toCandidates = queryNodeWithConstraints(
//                        toTypeName,
//                        eCandidates.map { e -> e.toId }
//                    )
//                    val filteredToCandidates = toCandidates.filter {
//                        nodeConstraintPair[toPattern.patternId].validate(it)
//                    }
//                    return filteredToCandidates.map {
//                        val correspondingEdge = eCandidates.first { e -> e.toId == it.nodeId }
//                        return@map solutionToSolve.copy(
//                            nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
//                            edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                        )
//                    }
//                }
//            }
//            toSol.second != null -> {
//                // 2 query
//                val eCandidates = queryEdges(patternEdge.type, nodeTo = listOf(toSol.second!!))
//                if (eCandidates.isEmpty()) return emptyList()
//                else {
//                    val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
//                    val fromTypeName = fromPattern.type
//                    val fromCandidates = queryNodeWithConstraints(
//                        fromTypeName,
//                        eCandidates.map { e -> e.fromId }
//                    )
//                    val filteredFromCandidates = fromCandidates.filter {
//                        nodeConstraintPair[fromPattern.patternId].validate(it)
//                    }
//                    return filteredFromCandidates.map {
//                        val correspondingEdge = eCandidates.first { e -> e.fromId == it.nodeId }
//                        return@map solutionToSolve.copy(
//                            nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
//                            edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                        )
//                    }
//                }
//            }
//            else -> {
//                // 2-3 query
//                val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
//                val toPattern = patternNodeDict[patternEdge.toPatternId]!!
//                val workspaceEdges = queryEdges(patternEdge.type)
//                val fromNodeIds = workspaceEdges.map { it.fromId }
//                val toNodeIds = workspaceEdges.map { it.toId }
////                val fromNodes = queryNodeWithConstraints(fromPattern.type, fromNodeIds).associateBy { it.nodeId }
////                val toNodes = queryNodeWithConstraints(toPattern.type, toNodeIds).associateBy { it.nodeId }
//                val nodePool = if (fromPattern.type == toPattern.type) {
//                    queryNodeWithConstraints(fromPattern.type, fromNodeIds + toNodeIds)
//                } else {
//                    (queryNodeWithConstraints(fromPattern.type, fromNodeIds) + queryNodeWithConstraints(
//                        toPattern.type,
//                        toNodeIds
//                    ))
//                }.associateBy { it.nodeId }
//                return workspaceEdges.map {
//                    solutionToSolve.copy(
//                        nodes = solutionToSolve.getUpdatedNodes(
//                            listOf(
//                                fromPattern.patternId to nodePool[it.fromId],
//                                toPattern.patternId to nodePool[it.toId]
//                            ),
//                        ),
//                        edges = solutionToSolve.getUpdatedEdges(patternEdge, it)
//                    )
//                }
//            }
//        }
//    }

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

        val (fromPatternIds, _fromSolutions) = solutionsToSolve.map { it.nodes[fromIndex] }.unzip()
        val (toPatternIds, _toSolutions) = solutionsToSolve.map { it.nodes[toIndex] }.unzip()

        val fromSolutions = _fromSolutions.distinctBy { it?.nodeId }
        val toSolutions = _toSolutions.distinctBy { it?.nodeId }

//        assert(fromSolutions.all { it != null } || fromSolutions.all { it == null }) { "Batch error" }
//        assert(toSolutions.all { it != null } || toSolutions.all { it == null }) { "Batch error" }

        val fromNodesFilled = fromSolutions[0] != null
        val toNodesFilled = toSolutions[0] != null

        when {
            fromNodesFilled && toNodesFilled -> {
                // 1 query, TODO: Ranges can be narrowed
//                val maps = fromSolutions.map { it!!.nodeId }
//                    .zip(toSolutions.map { it!!.nodeId })
//                    .groupBy { it.first }
//                    .mapValues { it.value.map { v -> v.second } }

                val eCandidates = queryEdges(
                    patternEdge.type,
                    nodeFrom = fromSolutions.requireNoNulls(),
                    nodeTo = toSolutions.requireNoNulls()
                )

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
                val eCandidates = queryEdges(patternEdge.type, nodeFrom = fromSolutions.requireNoNulls())
                if (eCandidates.isEmpty()) return emptyList()
                else {
                    val toPattern = patternNodeDict[patternEdge.toPatternId]!!

//                    val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!

                    val toTypeName = toPattern.type
                    val toCandidates = queryNodeWithConstraints(
                        toTypeName,
                        eCandidates.map { e -> e.toId }
                    )
                    val filteredToCandidates = toCandidates.filter {
                        nodeConstraintPair[toPattern.patternId].validate(it)
                    }

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


//                    solutionsToSolve.flatMap { solutionToSolve ->
//                        filteredToCandidates.map {
//                            val correspondingEdge = eCandidateDict[it.nodeId]!!
//                            return@map solutionToSolve.copy(
//                                nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
//                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                            )
//                        }
//                    }
                }
            }
            toNodesFilled -> {
                // 2 query
                val eCandidates = queryEdges(patternEdge.type, nodeTo = toSolutions.requireNoNulls())
                if (eCandidates.isEmpty()) return emptyList()
                else {
                    val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
                    val fromTypeName = fromPattern.type
                    val fromCandidates = queryNodeWithConstraints(
                        fromTypeName,
                        eCandidates.map { e -> e.fromId }
                    )
                    val filteredFromCandidates = fromCandidates.filter {
                        nodeConstraintPair[fromPattern.patternId].validate(it)
                    }
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

//                    return solutionsToSolve.flatMap { solutionToSolve ->
//                        filteredFromCandidates.map {
//                            val correspondingEdge = eCandidateDict[it.nodeId]!!
//                            return@map solutionToSolve.copy(
//                                nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
//                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                            )
//                        }
//                    }
                }
            }
            else -> {
                // 2-3 query
                val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
                val toPattern = patternNodeDict[patternEdge.toPatternId]!!
                val workspaceEdges = queryEdges(patternEdge.type)
                val fromNodeIds = workspaceEdges.map { it.fromId }
                val toNodeIds = workspaceEdges.map { it.toId }
//                val fromNodes = queryNodeWithConstraints(fromPattern.type, fromNodeIds).associateBy { it.nodeId }
//                val toNodes = queryNodeWithConstraints(toPattern.type, toNodeIds).associateBy { it.nodeId }
                val nodePool = if (fromPattern.type == toPattern.type) {
                    queryNodeWithConstraints(fromPattern.type, fromNodeIds + toNodeIds)
                } else {
                    (queryNodeWithConstraints(fromPattern.type, fromNodeIds) + queryNodeWithConstraints(
                        toPattern.type,
                        toNodeIds
                    ))
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


    val edgePointIndices = pattern.edges.orEmpty().map { it -> emptySolution.getNodeIndices(it) }
    val nodeAsFromIndices = pattern.nodes.map { emptyList<Int>().toMutableList() }
    val nodeAsToIndices = pattern.nodes.map { emptyList<Int>().toMutableList() }
    pattern.edges?.forEachIndexed { index, e ->
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

        val edgeToEvaluate = evaluatedNodeIndices.flatMap { nodeAsFromIndices[it] }.firstOrNull { edgeIndex ->
            !edgeEvaluatedFlags[edgeIndex]
        } ?: evaluatedNodeIndices.flatMap { nodeAsFromIndices[it] }.firstOrNull { edgeIndex ->
            !edgeEvaluatedFlags[edgeIndex]
        } ?: edgeEvaluatedFlags.indexOfFirst { !it }

        solutionPool = runBlocking {
            evaluatePatternEdge(
                solutionPool,
                edgeToEvaluate
            ) {
                edgeEvaluatedFlags[edgeToEvaluate] = true
                it.forEach { nid -> nodeEvaluatedFlags[nid] = true }
                evaluatedNodeIndices = it
            }
        }
    }

//    while (true) {
//        if (solutionPool.isEmpty()) break
//        val solutionToSolve = solutionPool.removeAt(0)
//        if (solutionToSolve.isValid) // all solutions are evaluated
//            break
//
//        // find target to evaluate
//
//        val patternNodeToEvaluate = solutionToSolve.nodes.firstOrNull {
//            it.second == null && patternNodeCandidateCounts[it.first]!! < IdeographContext.EVALUABLE_CANDIDATE_LIMIT
//        }
//
//        if (patternNodeToEvaluate != null) {
//            nodeConstraintPair[patternNodeToEvaluate.first]?.let {
//                val nCandidates = queryNodeWithConstraints(
//                    patternNodeDict[patternNodeToEvaluate.first]!!,
//                    *it.toTypedArray()
//                )
//                val filledSolutions = nCandidates.map { wn ->
//                    val newNodes = solutionToSolve.nodes.toMutableList()
//                    val index = newNodes.indexOfFirst { n -> n.first == patternNodeToEvaluate.first }
//                    newNodes[index] = patternNodeToEvaluate.first to wn
//                    solutionToSolve.copy(nodes = newNodes)
//                }
//                solutionPool += filledSolutions
//            }
//        } else {
//            solutionToSolve.edges.firstOrNull { (patternId, candidate) ->
//                val patternEdge = patternEdgeDict[patternId]!!
//                if (candidate != null) return@firstOrNull false
//                val (fromIndex, toIndex) = solutionToSolve.getNodeIndices(patternEdge)
//                val fromSol = solutionToSolve.nodes[fromIndex]
//                val toSol = solutionToSolve.nodes[toIndex]
//
//                if ((fromSol.second != null) && (toSol.second == null)) {
//                    val eCandidates = queryEdges(patternEdge.type, nodeFrom = listOf(fromSol.second!!))
//                    if (eCandidates.isEmpty()) return@firstOrNull false
//                    else {
//                        val toPattern = patternNodeDict[patternEdge.toPatternId]!!
//                        val toTypeName = toPattern.type
//                        val toCandidates = queryNodeWithConstraints(
//                            toTypeName,
//                            eCandidates.map { e -> e.toId }
//                        )
//                        val filteredToCandidates = toCandidates.filter {
//                            nodeConstraintPair[toPattern.patternId].validate(it)
//                        }
//                        val filledSolutions = filteredToCandidates.map {
//                            val correspondingEdge = eCandidates.firstOrNull { e -> e.toId == it.nodeId }!!
//                            return@map solutionToSolve.copy(
//                                nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
//                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                            )
//                        }
//                        solutionPool += filledSolutions
//                        return@firstOrNull true
//                    }
//                } else if ((fromSol.second == null) && (toSol.second != null)) {
//                    val eCandidates = queryEdges(patternEdge.type, nodeTo = listOf(toSol.second!!))
//                    if (eCandidates.isEmpty()) return@firstOrNull false
//                    else {
//                        val fromPattern = patternNodeDict[patternEdge.fromPatternId]!!
//                        val fromTypeName = fromPattern.type
//                        val fromCandidates = queryNodeWithConstraints(
//                            fromTypeName,
//                            eCandidates.map { e -> e.fromId }
//                        )
//                        val filteredFromCandidates = fromCandidates.filter {
//                            nodeConstraintPair[fromPattern.patternId].validate(it)
//                        }
//                        val filledSolutions = filteredFromCandidates.map {
//                            val correspondingEdge = eCandidates.firstOrNull { e -> e.fromId == it.nodeId }!!
//                            return@map solutionToSolve.copy(
//                                nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
//                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
//                            )
//                        }
//                        solutionPool += filledSolutions
//                        return@firstOrNull true
//                    }
//                } else if (fromSol.second != null) {
//                    val eCandidates = queryEdges(
//                        patternEdge.type,
//                        nodeFrom = listOf(fromSol.second!!),
//                        nodeTo = listOf(toSol.second!!)
//                    )
//                    if (eCandidates.isEmpty()) return@firstOrNull false
//                    else {
//                        val filledSolutions = eCandidates.map { ec ->
//                            return@map solutionToSolve.copy(
//                                edges = solutionToSolve.getUpdatedEdges(patternEdge, ec)
//                            )
//                        }
//                        solutionPool += filledSolutions
//                        return@firstOrNull true
//                    }
//                } else return@firstOrNull false
//            }
//        }
//    }

    return solutionPool.mapNotNull { it.completed() }
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


