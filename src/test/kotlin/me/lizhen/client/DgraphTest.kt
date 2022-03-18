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
                targetType = PatternType.Node,
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = "李.+",
                targetPatternId = ""
            ),
            PatternConstraint(
                patternId = "",
                targetType = PatternType.Node,
                property = "联系电话",
                operator = ComparisonOperator.Equal,
                value = "15[0-9]*",
                targetPatternId = ""
            )
        )

        val nodes = ctx.queryNodeWithConstraints(fromPN, *constraints)
        for (node in nodes) {
            println(node.properties["呼叫人*"])
        }
        println("\n\n\n${nodes.size}")


        val secondaryNodes = ctx.queryEdges("发起", nodes)
        println(secondaryNodes.size)
    }


    @Test
    fun testNodeToNodePattern() {
        // WorkspaceNode::properties.keyProjection("呼叫人*") regex "李.+",
        // WorkspaceNode::properties.keyProjection("联系电话") regex "15[0-9]+",

        val pnA = PatternNode("A", "报警人")
        val pnB = PatternNode("B", "急救报警")
        val pe = PatternEdge(
            fromPatternId = "A",
            toPatternId = "B",
            patternId = "e0",
            type = "发起",
        )
        val constraints = listOf(
            PatternConstraint(
                patternId = "c0",
                targetType = PatternType.Node,
                targetPatternId = "A",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = "李.+",
            ),
            PatternConstraint(
                patternId = "c1",
                targetType = PatternType.Node,
                targetPatternId = "A",
                property = "联系电话",
                operator = ComparisonOperator.MatchRegex,
                value = "15[0-9].+",
            ),
//            PatternConstraint(
//                patternId = "c2",
//                targetType = PatternType.Node,
//                targetPatternId = "B",
//                property = "联系电话",
//                operator = ComparisonOperator.MatchRegex,
//                value = "15[0-9].+",
//            )
        )

        val pattern = Pattern(
            nodes = listOf(pnA, pnB),
            edges = listOf(pe),
            constraints = constraints
        )
        val solutions = ctx.solvePattern(
            pattern
        )

        println(solutions.size)

    }
}