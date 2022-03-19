package me.lizhen.solvers

import me.lizhen.schema.Pattern
import me.lizhen.schema.WorkspaceNode


fun IdeographContext.solvePatternBatched(pattern: Pattern): List<PatternSolution> {
    val patternNodeDict = pattern.nodes.associateBy { it.patternId }
    val patternEdgeDict = pattern.edges.orEmpty().associateBy { it.patternId }
    val nodeConstraintPair = pattern.nodes.associate {
        Pair(
            it.patternId, pattern.constraints?.filter { pc -> pc.targetPatternId == it.patternId }.orEmpty()
        )
    }

    val patternNodeCandidateCounts = nodeConstraintPair.mapValues {
        patternNodeDict[it.key]?.let { n ->
            countConstrainedNodes(n, *it.value.toTypedArray())
        } ?: Long.MAX_VALUE
    }

    val patternNodeQueues = patternNodeCandidateCounts.entries.sortedBy { it.value }

    if (patternNodeQueues[0].value >= IdeographContext.MONGO_NODE_LIMIT) {
        throw Error("Pattern node candidates limit exceeded: $patternNodeQueues")
    }




    /**
     * evaluate 1 pattern node
     * 1 query
     */
    fun evaluatePatternNode(
        solutionToSolve: PatternSolutionUnderEvaluation,
        patternNodeIndex: Int
    ): List<PatternSolutionUnderEvaluation>? {
        val (patternId, evaluated) = solutionToSolve.nodes.getOrNull(patternNodeIndex) ?: return null
        assert(evaluated == null) { "The pattern node already evaluated." }
        nodeConstraintPair[patternId]?.let { it ->
            val nCandidates = queryNodeWithConstraints(
                patternNodeDict[patternId]!!,
                *it.toTypedArray()
            )
            val filledSolutions = nCandidates.map { wn ->
                val newNodes = solutionToSolve.nodes.toMutableList()
                val index = newNodes.indexOfFirst { n -> n.first == patternId }
                newNodes[index] = patternId to wn
                solutionToSolve.copy(nodes = newNodes)
            }
            return filledSolutions
        }
        throw Error("The node has no constraint. Evaluation Aborted.")
    }

    fun evaluatePatternEdge(
        solutionToSolve: PatternSolutionUnderEvaluation,
        patternEdgeIndex: Int,
    ): List<PatternSolutionUnderEvaluation>? {
        val (patternId, evaluated) = solutionToSolve.edges.getOrNull(patternEdgeIndex) ?: return null
        assert(evaluated == null) { "The pattern edge already evaluated." }
        val patternEdge = patternEdgeDict[patternId]!!
        val (fromIndex, toIndex) = solutionToSolve.getNodeIndices(patternEdge)
        val fromSol = solutionToSolve.nodes[fromIndex]
        val toSol = solutionToSolve.nodes[toIndex]
        when {
            fromSol.second != null && toSol.second != null -> {
                // 1 query
                val eCandidates = queryEdges(
                    patternEdge.type,
                    nodeFrom = listOf(fromSol.second!!),
                    nodeTo = listOf(toSol.second!!)
                )
                return if (eCandidates.isEmpty()) emptyList()
                else eCandidates.map {
                    solutionToSolve.copy(edges = solutionToSolve.getUpdatedEdges(patternEdge, it))
                }
            }
            fromSol.second != null -> {
                // 2 query
                val eCandidates = queryEdges(patternEdge.type, nodeFrom = listOf(fromSol.second!!))
                if (eCandidates.isEmpty()) return emptyList()
                else {
                    val toPattern = patternNodeDict[patternEdge.toPatternId]!!
                    val toTypeName = toPattern.type
                    val toCandidates = queryNodeWithConstraints(
                        toTypeName,
                        eCandidates.map { e -> e.toId }
                    )
                    val filteredToCandidates = toCandidates.filter {
                        nodeConstraintPair[toPattern.patternId].validate(it)
                    }
                    return filteredToCandidates.map {
                        val correspondingEdge = eCandidates.first { e -> e.toId == it.nodeId }
                        return@map solutionToSolve.copy(
                            nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
                            edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                        )
                    }
                }
            }
            toSol.second != null -> {
                // 2 query
                val eCandidates = queryEdges(patternEdge.type, nodeTo = listOf(toSol.second!!))
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
                    return filteredFromCandidates.map {
                        val correspondingEdge = eCandidates.first { e -> e.fromId == it.nodeId }
                        return@map solutionToSolve.copy(
                            nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
                            edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                        )
                    }
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
                    queryNodeWithConstraints(fromPattern.type, fromNodeIds+toNodeIds)
                } else {
                    (queryNodeWithConstraints(fromPattern.type, fromNodeIds) + queryNodeWithConstraints(toPattern.type, toNodeIds))
                }.associateBy { it.nodeId }
                return workspaceEdges.map {
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
            }
        }
    }


    val solutionPool = mutableListOf(
        PatternSolutionUnderEvaluation(
            nodes = pattern.nodes.map { it.patternId to null },
            edges = pattern.edges.orEmpty().map { it.patternId to null }
        )
    )

    var batchIndices = listOf(0)

    while (true) {
        if (solutionPool.isEmpty()) break
        val solutionToSolve = solutionPool.removeAt(0)
        if (solutionToSolve.isValid) // all solutions are evaluated
            break

        // find target to evaluate

        val patternNodeToEvaluate = solutionToSolve.nodes.firstOrNull {
            it.second == null && patternNodeCandidateCounts[it.first]!! < IdeographContext.EVALUABLE_CANDIDATE_LIMIT
        }

        if (patternNodeToEvaluate != null) {
            nodeConstraintPair[patternNodeToEvaluate.first]?.let {
                val nCandidates = queryNodeWithConstraints(
                    patternNodeDict[patternNodeToEvaluate.first]!!,
                    *it.toTypedArray()
                )
                val filledSolutions = nCandidates.map { wn ->
                    val newNodes = solutionToSolve.nodes.toMutableList()
                    val index = newNodes.indexOfFirst { n -> n.first == patternNodeToEvaluate.first }
                    newNodes[index] = patternNodeToEvaluate.first to wn
                    solutionToSolve.copy(nodes = newNodes)
                }
                solutionPool += filledSolutions
            }
        } else {
            solutionToSolve.edges.firstOrNull { (patternId, candidate) ->
                val patternEdge = patternEdgeDict[patternId]!!
                if (candidate != null) return@firstOrNull false
                val (fromIndex, toIndex) = solutionToSolve.getNodeIndices(patternEdge)
                val fromSol = solutionToSolve.nodes[fromIndex]
                val toSol = solutionToSolve.nodes[toIndex]

                if ((fromSol.second != null) && (toSol.second == null)) {
                    val eCandidates = queryEdges(patternEdge.type, nodeFrom = listOf(fromSol.second!!))
                    if (eCandidates.isEmpty()) return@firstOrNull false
                    else {
                        val toPattern = patternNodeDict[patternEdge.toPatternId]!!
                        val toTypeName = toPattern.type
                        val toCandidates = queryNodeWithConstraints(
                            toTypeName,
                            eCandidates.map { e -> e.toId }
                        )
                        val filteredToCandidates = toCandidates.filter {
                            nodeConstraintPair[toPattern.patternId].validate(it)
                        }
                        val filledSolutions = filteredToCandidates.map {
                            val correspondingEdge = eCandidates.firstOrNull { e -> e.toId == it.nodeId }!!
                            return@map solutionToSolve.copy(
                                nodes = solutionToSolve.getUpdatedNodes(toPattern, it),
                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                            )
                        }
                        solutionPool += filledSolutions
                        return@firstOrNull true
                    }
                } else if ((fromSol.second == null) && (toSol.second != null)) {
                    val eCandidates = queryEdges(patternEdge.type, nodeTo = listOf(toSol.second!!))
                    if (eCandidates.isEmpty()) return@firstOrNull false
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
                        val filledSolutions = filteredFromCandidates.map {
                            val correspondingEdge = eCandidates.firstOrNull { e -> e.fromId == it.nodeId }!!
                            return@map solutionToSolve.copy(
                                nodes = solutionToSolve.getUpdatedNodes(fromPattern, it),
                                edges = solutionToSolve.getUpdatedEdges(patternEdge, correspondingEdge)
                            )
                        }
                        solutionPool += filledSolutions
                        return@firstOrNull true
                    }
                } else if (fromSol.second != null) {
                    val eCandidates = queryEdges(
                        patternEdge.type,
                        nodeFrom = listOf(fromSol.second!!),
                        nodeTo = listOf(toSol.second!!)
                    )
                    if (eCandidates.isEmpty()) return@firstOrNull false
                    else {
                        val filledSolutions = eCandidates.map { ec ->
                            return@map solutionToSolve.copy(
                                edges = solutionToSolve.getUpdatedEdges(patternEdge, ec)
                            )
                        }
                        solutionPool += filledSolutions
                        return@firstOrNull true
                    }
                } else return@firstOrNull false
            }
        }
    }

    return solutionPool.mapNotNull { it.completed() }
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
}

