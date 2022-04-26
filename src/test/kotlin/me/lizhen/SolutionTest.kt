package me.lizhen

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.lizhen.algorithms.LogicOperator
import me.lizhen.schema.*
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import me.lizhen.solvers.solveCompositePattern
import me.lizhen.solvers.validate
import org.junit.Test


class SolutionTest {
    val mongoService = MongoService(27026)
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
                patternId = "comp1",
                type = "企业"
            ),
            PatternNode(
                patternId = "holder1",
                type = "股东"
            ),
            PatternNode(
                patternId = "dsf",
                type = "人"
            ),
            PatternNode(
                patternId = "holder2",
                type = "人"
            ),
            PatternNode(
                patternId = "comp2",
                type = "企业"
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
        val js = Json.decodeFromString<CompositePattern>("{\n" +
                "    \"nodes\": [\n" +
                "        {\n" +
                "            \"patternId\": \"jhFec_O3fOiOJ4sn8Oare\",\n" +
                "            \"type\": \"企业\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"7iY9ejCrA3FeGWwngOk5C\",\n" +
                "            \"type\": \"股东\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"REbWXCWCNvn52K6FW7tyb\",\n" +
                "            \"type\": \"人\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"D6jgotz5kMZiGHX4EN4or\",\n" +
                "            \"type\": \"股东\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"xn8JuS3utkxnf71k9NkF5\",\n" +
                "            \"type\": \"人\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"edges\": [\n" +
                "        {\n" +
                "            \"patternId\": \"PVSpKlWvDc9xzWgK94zBh\",\n" +
                "            \"fromPatternId\": \"REbWXCWCNvn52K6FW7tyb\",\n" +
                "            \"toPatternId\": \"7iY9ejCrA3FeGWwngOk5C\",\n" +
                "            \"type\": \"参与股东\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"pHZ6Z-ISO4KP8p2fgbq_u\",\n" +
                "            \"fromPatternId\": \"7iY9ejCrA3FeGWwngOk5C\",\n" +
                "            \"toPatternId\": \"jhFec_O3fOiOJ4sn8Oare\",\n" +
                "            \"type\": \"被投企业\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"YM-pW7yNJNn783NKB8zsh\",\n" +
                "            \"fromPatternId\": \"D6jgotz5kMZiGHX4EN4or\",\n" +
                "            \"toPatternId\": \"jhFec_O3fOiOJ4sn8Oare\",\n" +
                "            \"type\": \"被投企业\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"4tDRO9SvR-0zrKaaESM5L\",\n" +
                "            \"fromPatternId\": \"xn8JuS3utkxnf71k9NkF5\",\n" +
                "            \"toPatternId\": \"D6jgotz5kMZiGHX4EN4or\",\n" +
                "            \"type\": \"参与股东\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"constraints\": [\n" +
                "        {\n" +
                "            \"patternId\": \"O2g6rnREzMf9bxKv5oEep\",\n" +
                "            \"targetType\": \"Node\",\n" +
                "            \"targetPatternId\": \"REbWXCWCNvn52K6FW7tyb\",\n" +
                "            \"property\": \"姓名*\",\n" +
                "            \"operator\": \"MatchRegex\",\n" +
                "            \"value\": \"董淑.+\"\n" +
                "        },\n" +
                "        {\n" +
                "            \"patternId\": \"O2g6rnREzMf9bEep\",\n" +
                "            \"targetType\": \"Node\",\n" +
                "            \"targetPatternId\": \"D6jgotz5kMZiGHX4EN4or\",\n" +
                "            \"property\": \"姓名*\",\n" +
                "            \"operator\": \"Equal\",\n" +
                "            \"value\": \"田其东\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"logicOperators\": [],\n" +
                "    \"connections\": []\n" +
                "}")

        val sol = runBlocking {
            ctx.solveCompositePattern(
                js
            )
        }

        println(sol.size)
        println(sol.joinToString("\n") {
            it.nodes.entries.joinToString { e -> e.value.name }
        })



    }
}