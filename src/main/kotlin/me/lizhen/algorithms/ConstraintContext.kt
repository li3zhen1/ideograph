package me.lizhen.algorithms

import me.lizhen.schema.PatternConstraint

data class PatternLogicOperator(
    val id: String,
    val type: LogicOperator
)

class ConstraintContext(
    val constraints: List<PatternConstraint>,
    val logicOperators: List<LogicOperator>,
    val connections: List<Pair<String, String>>
){

}