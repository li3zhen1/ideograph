package me.lizhen.solvers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.lizhen.algorithms.ConstraintContext
import me.lizhen.schema.CompositePattern
import me.lizhen.schema.Pattern
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.toList
import kotlinx.coroutines.selects.select
import kotlin.coroutines.coroutineContext

/**
 * TODO: Chunk to prevent overflow
 */
const val maxCoroutineChannels = 64

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun IdeographContext.solveCompositePattern(pattern: CompositePattern): List<PatternSolution> {
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


        val channel = Channel<List<PatternSolution>>()
        splitConstraints.forEachIndexed { index, it ->
            CoroutineScope(Dispatchers.IO).launch {
                produce<List<PatternSolution>> {
                    val pattern = Pattern(
                        pattern.nodes,
                        pattern.edges,
                        it.unzip().first
                    )
                    println("[Composite Solver] Starting Coroutine $index: ${pattern}\n\n")

                    val result = solvePatternBatched(
                        Pattern(
                            pattern.nodes,
                            pattern.edges,
                            it.unzip().first
                        )
                    )
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

//        val solutionChannel = Channel<List<PatternSolution>>()
//
////        println(splitConstraints)
//
//        splitConstraints.forEach {
//            println(it)
//            withContext(Dispatchers.IO) {
//                launch {
//                    solutionChannel.send(
//                        solvePatternBatched(
//                            Pattern(
//                                pattern.nodes,
//                                pattern.edges,
//                                it.unzip().first
//                            )
//                        ).also { println(it) }
//                    )
//                }
//            }
//        }
//
//            val allSolutions = mutableListOf<PatternSolution>()
//            val selected = select<List<PatternSolution>> {
//                solutionChannels.map { channel ->
//                    channel.onReceive { it }
//                }
//            }
//            allSolutions += selected
//
//            return@withContext allSolutions

//        val result = mutableListOf<PatternSolution>()
//        repeat(splitConstraints.size) {
//            result += solutionChannel.receive()
//        }
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


