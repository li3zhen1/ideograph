package me.lizhen

import kotlinx.coroutines.runBlocking
import me.lizhen.algorithms.LogicOperator
import me.lizhen.schema.*
import me.lizhen.service.DgraphService
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import me.lizhen.solvers.solveCompositePattern
import org.junit.Test


class SolutionTest {
    val mongoService = MongoService()
    val dgraphService = DgraphService(mongoService)
    val ctx = IdeographContext(mongoService, dgraphService)
    @Test
    fun compositePatternTest1() {
        val cp = CompositePattern(
            listOf(
                PatternNode("A", "报警人"),
                PatternNode("B", "急救报警")
            ),
            listOf(
                PatternEdge(
                    fromPatternId = "A",
                    toPatternId = "B",
                    patternId = "A->B",
                    type = "发起",
                )
            ),
            listOf(
                PatternConstraint(
                    targetType = PatternType.Node,
                    targetPatternId = "A",
                    patternId = "C1",
                    property = "呼叫人*",
                    operator = ComparisonOperator.MatchRegex,
                    value = "李.*"
                ),
                PatternConstraint(
                    targetType = PatternType.Node,
                    targetPatternId = "A",
                    patternId = "C2",
                    property = "呼叫人*",
                    operator = ComparisonOperator.MatchRegex,
                    value = "吴.*"
                )
            ),
            listOf(
                PatternLogicOperator(
                    patternId = "or-root",
                    type = LogicOperator.Or,
                ),
            ),
            listOf(
                ConstraintConnection("C1", "or-root"),
                ConstraintConnection("C2", "or-root")
            )
        )
        val sol = runBlocking {
            ctx.solveCompositePattern(cp)
        }
        println(sol)
    }
}