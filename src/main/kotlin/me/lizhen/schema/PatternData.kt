package me.lizhen.schema

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import me.lizhen.algorithms.ConstraintContext
import me.lizhen.algorithms.LogicOperator

interface IdentifiablePattern {
    val patternId: String
}

@Serializable
data class PatternNode(
    override val patternId: String,
    val type: String
): IdentifiablePattern

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
    override val patternId: String,
    val targetType: PatternType,
    val targetPatternId: String,

    val property: String,
    val operator: ComparisonOperator,
    val value: String
): IdentifiablePattern


@Serializable
data class PatternEdge(
    override val patternId: String,

    val type: String,

    val fromPatternId: String,
    val toPatternId: String,
): IdentifiablePattern


@Serializable
data class Pattern(
    val nodes: List<PatternNode>,
    val edges: List<PatternEdge>?,
    val constraints: List<PatternConstraint>?
)

@Serializable
data class PatternLogicOperator(
    override val patternId: String,
    val type: LogicOperator
) : IdentifiablePattern

@Serializable
data class ConstraintConnection(
    val from: String,
    val to: String
)

@Serializable
data class CompositePattern(
    val nodes: List<PatternNode>,
    val edges: List<PatternEdge>?,
    val constraints: List<PatternConstraint>?,

    val logicOperators: List<PatternLogicOperator>?,
    val connections: List<ConstraintConnection>?
)