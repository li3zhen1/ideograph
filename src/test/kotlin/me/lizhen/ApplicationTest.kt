package me.lizhen

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import me.lizhen.schema.*
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import me.lizhen.solvers.PatternSolution
import me.lizhen.solvers.solvePatternBatched
import me.lizhen.solvers.validate
import org.litote.kmongo.json
//import kotlin.time.*

class ApplicationTest {


    private val mongoService = MongoService()
//    private val dgraphService = DgraphService(mongoService)
    private val ctx = IdeographContext(mongoService)


    val pnA = PatternNode("A", "报警人")
    val pnB = PatternNode("B", "急救报警")

    val pnC = PatternNode("C", "急救报警")


    val pe = PatternEdge(
        fromPatternId = "A",
        toPatternId = "B",
        patternId = "e0",
        type = "发起",
    )

    val pe2 = PatternEdge(
        fromPatternId = "A",
        toPatternId = "C",
        patternId = "e2",
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
    )


    val p1 = Pattern(
        nodes = listOf(pnA, pnB),
        edges = listOf(pe),
        constraints = constraints
    )

    val p2 = Pattern(
        nodes = listOf(pnA, pnB),
        edges = listOf(pe),
        constraints = constraints + listOf(
            PatternConstraint(
                patternId = "c2",
                targetType = PatternType.Node,
                targetPatternId = "B",
                property = "流水号*",
                operator = ComparisonOperator.MatchRegex,
                value = "2019[0-9]+",
            )
        )
    )

    val p3 = Pattern(
        nodes = listOf(pnC, pnB, pnA),
        edges = listOf(pe, pe2),
        constraints = constraints + listOf(
            PatternConstraint(
                patternId = "c2",
                targetType = PatternType.Node,
                targetPatternId = "B",
                property = "流水号*",
                operator = ComparisonOperator.MatchRegex,
                value = "2019[0-9]+",
            )
        )
    )


    val break0328 = Json.decodeFromString<Pattern>("{\"nodes\":[{\"patternId\":\"YKQ7dAi9MoXVKFsYk6LGn\",\"type\":\"报警人\"},{\"patternId\":\"CrJMwVx6hqpL9PYXjCGDl\",\"type\":\"急救报警\"},{\"patternId\":\"CJXsc6VOBwA-pCXCEL0pk\",\"type\":\"受理人\"}],\"edges\":[{\"patternId\":\"iKzpaRzSQ31Huqw_Zzwa4\",\"fromPatternId\":\"YKQ7dAi9MoXVKFsYk6LGn\",\"toPatternId\":\"CrJMwVx6hqpL9PYXjCGDl\",\"type\":\"发起\"},{\"patternId\":\"a9dnXxdo47wVEWRNT2K4t\",\"fromPatternId\":\"CJXsc6VOBwA-pCXCEL0pk\",\"toPatternId\":\"CrJMwVx6hqpL9PYXjCGDl\",\"type\":\"受理\"}],\"constraints\":[{\"patternId\":\"fenKbcnNl5Thkpi0PcueK\",\"targetType\":\"Node\",\"targetPatternId\":\"CrJMwVx6hqpL9PYXjCGDl\",\"property\":\"流水号*\",\"operator\":\"MatchRegex\",\"value\":\"2019[0-9]+\"},{\"patternId\":\"Eqkd5n8Qmsjujus5RIIvx\",\"targetType\":\"Node\",\"targetPatternId\":\"YKQ7dAi9MoXVKFsYk6LGn\",\"property\":\"呼叫人*\",\"operator\":\"MatchRegex\",\"value\":\"李.+\"}]}")

//    @OptIn(ExperimentalTime::class)
    private fun testPattern(pattern: Pattern): List<PatternSolution> {
        // WorkspaceNode::properties.keyProjection("呼叫人*") regex "李.+",
        // WorkspaceNode::properties.keyProjection("联系电话") regex "15[0-9]+",

//        var solutions: List<PatternSolution>
////        val time = measureTime {
            val solutions = runBlocking { ctx.solvePatternBatched(pattern) }
//        }
//        println("\n\n\n\n$time, ${solutions.size}")
        return solutions
    }

    @Test
    fun getPatternJson() {
        println(p3.json)
    }

    @Test
    fun test1() {
        val sol = testPattern(p1)
        assert(sol.all { it.validate(p1) })
    }

    @Test
    fun testRoot() {
        val sol = testPattern(break0328)
//        sol.forEach {
//            println(it.nodes["B"]?.properties)
//        }
        assert(sol.all { it.validate(break0328) })



//        val sol2 = testPattern(p2)
//        sol2.forEach {
//            println(it.nodes["B"]?.properties)
//        }
//        assert(sol2.all { it.validate(p2) })
//
//        val sol3 = testPattern(p3)
//        sol3.forEach {
//            println(it.nodes["B"]?.properties)
//            // TODO: Run distinct
//        }
//        assert(sol3.all { it.validate(p3) })

        // TODO: Lazy?
    }

}