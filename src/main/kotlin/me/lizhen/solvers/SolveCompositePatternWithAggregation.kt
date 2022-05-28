package me.lizhen.solvers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import me.lizhen.algorithms.ConstraintContext
import me.lizhen.schema.*
import kotlin.coroutines.coroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun IdeographContext.solveCompositePatternWithAggregation(
    pattern: AggregatedPattern
): List<PatternSolution> {
    val sortedAggregations = pattern.aggregations.orEmpty().sortedBy { it.nodes.size }

    if (!pattern.constraints.isNullOrEmpty()
        && pattern.connections !== null
        && pattern.logicOperators !== null
    ) {
        val constraintContext = ConstraintContext(
            pattern.constraints,
            pattern.logicOperators,
            pattern.connections.filter { it.from !== it.to }.map { it.from to it.to }
        )

        val splitConstraints = constraintContext.splitSyntaxTree() ?: emptyList()

        splitConstraints.forEachIndexed { index, it ->
            println("[Logic $index] $it")
        }

        val channel = Channel<List<PatternSolution>>()

        splitConstraints.forEachIndexed { index, it ->
            CoroutineScope(Dispatchers.IO).launch {
                produce<List<PatternSolution>> {
                    val narrowedPattern = AggregatedMinimalPattern(
                        pattern.nodes,
                        pattern.edges,
                        it.map { (c, reversed) ->
                            if (reversed) c.copy(isReversed = !c.isReversed)
                            else c
                        },
                        sortedAggregations
                    )
                    println("[Aggregated Solver] Starting Coroutine $index: $narrowedPattern")
                    val result = solvePatternWithAggregation(narrowedPattern)
                    println("[Aggregated Solver] Finishing Coroutine $index with ${result.size} results.")
                    channel.send(result)
                }
            }
        }
        val solutions = mutableListOf<PatternSolution>()
        repeat(splitConstraints.size) {
            solutions += channel.receive()
        }
        coroutineContext.cancelChildren()
        return solutions
    } else {
        return solvePatternWithAggregation(
            AggregatedMinimalPattern(
                pattern.nodes,
                pattern.edges,
                pattern.constraints,
                sortedAggregations,
            )
        )
    }
}

data class AggregatedMinimalPattern(
    val nodes: List<PatternNode>,
    val edges: List<PatternEdge>?,
    val constraints: List<PatternConstraint>?,
    val aggregations: List<PatternAggregation>,
)

internal suspend fun IdeographContext.solvePatternWithAggregation(
    pattern: AggregatedMinimalPattern
): List<PatternSolution> {

    val nodePool = pattern.nodes.associateBy { it.patternId }.toMutableMap()
    val edgePool = pattern.edges.orEmpty().associateBy { it.toPatternId }.toMutableMap()
    val constraintPool = pattern.constraints.orEmpty().groupBy { it.targetPatternId }.toMutableMap()

    val clonedNodes = mutableMapOf<String, List<PatternNode>>()
    val clonedEdges = mutableMapOf<String, List<PatternEdge>>()
    val clonedConstraints = mutableMapOf<String, List<PatternConstraint>>()

    pattern.aggregations.forEach { aggregation ->
        aggregation.nodes.forEach { nId ->
            nodePool.remove(nId)?.let {
                clonedNodes[nId] = List(aggregation.multiplier) { index ->
                    it.copy(patternId = it.patternId + ":$index")
                }
            }

            constraintPool.remove(nId)?.let { targetingConstraints ->
                targetingConstraints.forEach {
                    clonedConstraints[it.patternId] = List(aggregation.multiplier) { index ->
                        it.copy(
                            patternId = it.patternId + ":$index",
                            targetPatternId = it.targetPatternId + ":$index"
                        )
                    }
                }
            }
        }
        aggregation.edges.forEach { eId ->
            edgePool.remove(eId)?.let {
                when {
                    (aggregation.nodes.contains(it.fromPatternId)
                            && aggregation.nodes.contains(it.toPatternId)) -> {
                        clonedEdges[eId] = List(aggregation.multiplier) { index ->
                            it.copy(
                                patternId = it.patternId + ":$index",
                                toPatternId = it.toPatternId + ":$index",
                                fromPatternId = it.fromPatternId + ":$index"
                            )
                        }
                    }
                    (aggregation.nodes.contains(it.fromPatternId)
                            && !aggregation.nodes.contains(it.toPatternId)) -> {
                        clonedEdges[eId] = List(aggregation.multiplier) { index ->
                            it.copy(
                                patternId = it.patternId + ":$index",
                                fromPatternId = it.fromPatternId + ":$index"
                            )
                        }
                    }
                    (!aggregation.nodes.contains(it.fromPatternId)
                            && aggregation.nodes.contains(it.toPatternId)) -> {
                        clonedEdges[eId] = List(aggregation.multiplier) { index ->
                            it.copy(
                                patternId = it.patternId + ":$index",
                                toPatternId = it.toPatternId + ":$index"
                            )
                        }
                    }
                    else -> {
                        throw Error("Invalid aggregation")
                    }
                }
            }
        }
    }


    val brokenPattern = Pattern(
        nodes = nodePool.map { it.value } + clonedNodes.flatMap { it.value },
        edges = edgePool.map {it.value} + clonedEdges.flatMap { it.value },
        constraints = constraintPool.flatMap { it.value } + clonedConstraints.flatMap { it.value }
    )

    return solvePatternBatched(brokenPattern)
}