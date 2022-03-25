package me.lizhen

import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.shared.serialization.kotlinx.kotlinx.json.*
import io.ktor.server.plugins.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import me.lizhen.plugins.*
import me.lizhen.schema.*
import me.lizhen.service.DgraphService
import me.lizhen.service.MongoService
import me.lizhen.solvers.IdeographContext
import me.lizhen.solvers.PatternSolution
import me.lizhen.solvers.solvePatternBatched
import me.lizhen.solvers.validate
import org.litote.kmongo.json
import kotlin.time.*

class ApplicationTest {


    private val mongoService = MongoService()
    private val dgraphService = DgraphService(mongoService)
    private val ctx = IdeographContext(mongoService, dgraphService)


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

    @OptIn(ExperimentalTime::class)
    private fun testPattern(pattern: Pattern): List<PatternSolution> {
        // WorkspaceNode::properties.keyProjection("呼叫人*") regex "李.+",
        // WorkspaceNode::properties.keyProjection("联系电话") regex "15[0-9]+",

        var solutions: List<PatternSolution>
        val time = measureTime {
            solutions = runBlocking { ctx.solvePatternBatched(pattern) }
        }
        println("\n\n\n\n$time, ${solutions.size}")
        return solutions
    }

    @Test
    fun getPatternJson() {
        println(p3.json)
    }

    @Test
    fun testRoot() {
        val sol = testPattern(p1)
        sol.forEach {
            println(it.nodes["B"]?.properties)
        }
        assert(sol.all { it.validate(p1) })


        val sol2 = testPattern(p2)
        sol2.forEach {
            println(it.nodes["B"]?.properties)
        }
        assert(sol2.all { it.validate(p2) })

        val sol3 = testPattern(p3)
        sol3.forEach {
            println(it.nodes["B"]?.properties)
            // TODO: Run distinct
        }
        assert(sol3.all { it.validate(p3) })

        // TODO: Lazy?
    }

}