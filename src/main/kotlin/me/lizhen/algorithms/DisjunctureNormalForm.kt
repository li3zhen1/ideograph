package me.lizhen.algorithms


enum class LogicOperator(val value: Int) {
    And(0), Or(1), Not(2), Xor(3), Xnor(4)
}


interface IPredictable {
    fun predicate(): Boolean
}


open class CompositePredictable<T : IPredictable>(
    public var children: List<T>,
    public val operator: LogicOperator
) : IPredictable {
    override fun predicate() = when (operator) {
        LogicOperator.And -> children.all { it.predicate() }
        LogicOperator.Or -> children.any { it.predicate() }
        LogicOperator.Not -> !(children[0].predicate())
        LogicOperator.Xor -> children[0].predicate() xor children[1].predicate()
        LogicOperator.Xnor -> !(children[0].predicate() xor children[1].predicate())
    }
}


sealed class NormalForm<T : IPredictable> {
    abstract fun reduce(): IPredictable
}


class DisjunctureNormalForm<T : IPredictable>(
    private val rootClause: T
) : NormalForm<T>(), IPredictable {

    private var flattenedClause = CompositePredictable<T>(listOf(), LogicOperator.Or)

    override fun reduce(): CompositePredictable<T> {
        // TODO
        flattenedClause.children = listOf(rootClause)
        return flattenedClause
    }

    override fun predicate(): Boolean {
        return flattenedClause.predicate()
    }

    companion object {
        public fun <T : IPredictable> transfrom(rootClause: T): List<T> =
            DisjunctureNormalForm(rootClause)
                .reduce()
                .children
    }
}


