package me.lizhen.client

import me.lizhen.schema.*
import me.lizhen.service.DgraphService
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import kotlin.test.Test

class DgraphTest {

//    private val testPattern1 = Pattern(
//        listOf(
//            PatternNode(id = "61837ed3bb99e612c187321b")
//        )
//    )

    private val mongoService = MongoService()
    private val dgraphService = DgraphService(mongoService)
    private val ctx = IdeographContext(mongoService, dgraphService)

    @Test
    fun testStructuralSolver() {
        val fromPN = PatternNode(
            patternId = "",
            type = "报警人"
        )

        val toPN = PatternNode(
            patternId = "",
            type = "急救报警"
        )

        val constraints = arrayOf(
            PatternConstraint(
                patternId = "",
                targetType = PatternType.Edge,
                property = "呼叫人*",
                operator = ComparisonOperator.Equal,
                value = "李*"
            ),
            PatternConstraint(
                patternId = "",
                targetType = PatternType.Edge,
                property = "呼叫人*",
                operator = ComparisonOperator.Equal,
                value = "女士*"
            )
        )

        val nodes = ctx.getSingleNodeCandidates(fromPN, *constraints)
        println("\n\n\n${nodes.size}")


        val secondaryNodes = ctx.getConnectedNodeCandidates(fromPN, toPN, nodes)
        println(secondaryNodes)
    }
}