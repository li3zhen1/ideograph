package me.lizhen.solvers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.lizhen.algorithms.ConstraintContext
import me.lizhen.schema.CompositePattern
import me.lizhen.schema.Pattern
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.selects.select
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/**
 * TODO: Chunk to prevent overflow
 */
const val maxCoroutineChannels = 64

@OptIn(ExperimentalCoroutinesApi::class)
public suspend fun IdeographContext.solveCompositePattern(pattern: CompositePattern): List<PatternSolution> {
    println("=========== new session ==========")
    println(pattern.nodes.size)
    println(pattern.edges?.size)
    println(pattern.connections?.size)
    println(pattern.constraints?.size)
    println(pattern.logicOperators?.size)
    if (pattern.constraints !== null
        && pattern.logicOperators !== null
        && pattern.connections !== null
    ) {
        val constraintContext = ConstraintContext(
            pattern.constraints,
            pattern.logicOperators,
            pattern.connections.filter { it.from !== it.to }.map { it.from to it.to }
        )
        val splitConstraints = constraintContext.splitSyntaxTree() ?: return emptyList()

        splitConstraints.forEachIndexed { index, it ->
            println("[Logic $index] $it")
        }
        val channel = Channel<List<PatternSolution>>()
        splitConstraints.forEachIndexed { index, it ->
            CoroutineScope(Dispatchers.IO).launch {
                produce<List<PatternSolution>> {
                    val narrowedPattern = Pattern(
                        pattern.nodes,
                        pattern.edges,
                        it.unzip().first
                    )
                    println("[Composite Solver] Starting Coroutine $index: ${narrowedPattern}")
                    val result = solvePatternBatched(narrowedPattern)
                    println("[Composite Solver] Finishing Coroutine $index with ${result.size} results.")
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

        return solvePatternBatched(
            Pattern(
                pattern.nodes,
                pattern.edges,
                pattern.constraints
            )
        )
    }
}
