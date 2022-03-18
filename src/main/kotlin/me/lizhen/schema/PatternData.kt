package me.lizhen.schema

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class PatternNode(
    val patternId: String,
    val type: String
)

@Serializable
enum class PatternType(val value: Int) {
    Node(0), Edge(1), Constraint(2)
}

@Serializable
enum class ComparisonOperator(val value: Int) {
    Equal(0), NotEqual(1),
    Greater(2), GreaterOrEqual(3), Less(4), LessOrEqual(5),
    MatchRegex(6)
}


@Serializable
data class PatternConstraint(
    val patternId: String,
    val targetType: PatternType,
    val targetPatternId: String,

    val property: String,
    val operator: ComparisonOperator,
    val value: String
)


@Serializable
data class PatternEdge(
    val patternId: String,

    val type: String,

    val fromPatternId: String,
    val toPatternId: String,
)


@Serializable
data class Pattern(
    val nodes: List<PatternNode>,
    val edges: List<PatternEdge>?,
    val constraints: List<PatternConstraint>?
)