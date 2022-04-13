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
//    val dgraphService = DgraphService(mongoService)
    val ctx = IdeographContext(mongoService)


    val cp1 = CompositePattern(
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


    val cp2 = CompositePattern(
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
                value = "李.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "C2",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = "吴.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "C3",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = ".+小姐"
            )
        ),
        listOf(
            PatternLogicOperator(
                patternId = "or-subroot",
                type = LogicOperator.Or,
            ),
            PatternLogicOperator(
                patternId = "and-root",
                type = LogicOperator.And,
            ),
        ),
        listOf(
            ConstraintConnection("C1", "or-subroot"),
            ConstraintConnection("C2", "or-subroot"),
            ConstraintConnection("C3", "and-root"),
            ConstraintConnection("or-subroot", "and-root"),
        )
    )


    val cp3 = CompositePattern(
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
                value = "李.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "C2",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = "吴.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "female",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = ".+小姐"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "male",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = ".+先生"
            )
        ),
        listOf(
            PatternLogicOperator(
                patternId = "or-root",
                type = LogicOperator.Or,
            ),
            PatternLogicOperator(
                patternId = "and-male",
                type = LogicOperator.And,
            ),
            PatternLogicOperator(
                patternId = "and-female",
                type = LogicOperator.And,
            ),
        ),
        listOf(
            ConstraintConnection("C1", "and-female"),
            ConstraintConnection("female", "and-female"),

            ConstraintConnection("C2", "and-male"),
            ConstraintConnection("male", "and-male"),

            ConstraintConnection("and-male", "or-root"),
            ConstraintConnection("and-female", "or-root"),
        )
    )



    val flattenTest = CompositePattern(
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
                value = "李.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "C2",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = "吴.+"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "female",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = ".+小姐"
            ),
            PatternConstraint(
                targetType = PatternType.Node,
                targetPatternId = "A",
                patternId = "male",
                property = "呼叫人*",
                operator = ComparisonOperator.MatchRegex,
                value = ".+先生"
            )
        ),
        listOf(
            PatternLogicOperator(
                patternId = "or-root",
                type = LogicOperator.And,
            ),
            PatternLogicOperator(
                patternId = "and-male",
                type = LogicOperator.And,
            ),
            PatternLogicOperator(
                patternId = "and-female",
                type = LogicOperator.And,
            ),
        ),
        listOf(
            ConstraintConnection("C1", "and-female"),
            ConstraintConnection("female", "and-female"),

            ConstraintConnection("C2", "and-male"),
            ConstraintConnection("male", "and-male"),

            ConstraintConnection("and-male", "or-root"),
            ConstraintConnection("and-female", "or-root"),
        )
    )

    /**
     * 报警人姓李或吴
     */
//    @Test
//    fun test1() {
//        val sol = runBlocking {
//            ctx.solveCompositePattern(cp1)
//        }
//        println(sol)
//    }


    /**
     * 报警人 (姓李或吴) 且 (为小姐)
     */
//    @Test
//    fun test2() {
//        val sol = runBlocking {
//            ctx.solveCompositePattern(cp2)
//        }
//        println(sol)
//    }

    /**
     * 报警人 (姓李 且 小姐) 或 (姓吴 且 先生)
     */
    @Test
    fun test3() {
        val sol = runBlocking {
            ctx.solveCompositePattern(cp3)
        }
        println(sol.size)
        sol.forEach {
            println(it.nodes.values.joinToString { n -> n.name })
        }
    }


    /**
     * 报警人 (姓李 且 小姐) and (姓吴 且 先生)
     */
//    @Test
//    fun flattenTest() {
//        val sol = runBlocking {
//            ctx.solveCompositePattern(flattenTest)
//        }
//        println(sol)
//    }


    val dsf = CompositePattern(
        nodes = listOf(
            PatternNode(
                patternId = "dsf",
                type = "人"
            )
        ),
        edges = listOf(),
        constraints = listOf(
            PatternConstraint(
                targetPatternId = "dsf",
                targetType = PatternType.Node,
                patternId = "name",
                property = "姓名*",
                operator = ComparisonOperator.MatchRegex,
                value = "董淑.*"
            )
        ),
        connections = null,
        logicOperators = null,
    )

    @Test
    fun testDsf2() {
        val sol = runBlocking {
            ctx.solveCompositePattern(dsf)
        }
        println(sol)
    }
}