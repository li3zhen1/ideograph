package me.lizhen.algorithms

/**
 *
 */

//enum class LogicOperator(val value: Int) {
//    And(0), Or(1), Not(2)
//}
//
//
//interface IPredictable {
//    fun predicate(): Boolean
//}
//
//
//data class CompositePredictableClause(
//    val children: List<IPredictable>,
//    val operator: LogicOperator
//) : IPredictable {
//    override fun predicate() = when (operator) {
//        LogicOperator.And -> children.all { it.predicate() }
//        LogicOperator.Or -> children.any { it.predicate() }
//        LogicOperator.Not -> !children.first().predicate()
//    }
//}


class ResolutionPrinciple(
    val rootClause: IPredictable,
) {

    init {

    }

    fun flatten(): IPredictable {
        throw Error("Deprecated")
    }

    companion object {

    }
}