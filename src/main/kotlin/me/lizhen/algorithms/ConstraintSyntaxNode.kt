package me.lizhen.algorithms

import me.lizhen.schema.IdentifiablePattern
import me.lizhen.schema.PatternConstraint
import me.lizhen.schema.PatternLogicOperator
import kotlin.math.pow

public enum class LogicOperator(val value: Int) {
    And(0), Or(1), Not(2), Xor(3), Xnor(4)
}


interface Traversable {
    val children: List<Traversable>?
    fun traverse(action: (Traversable) -> Unit) {
        action(this)
        children?.forEach {
            it.traverse(action)
        }
    }
}


data class ConstraintSyntaxNode(
    val content: IdentifiablePattern,
    override var children: List<ConstraintSyntaxNode>? = null,
) : Traversable {
    inline fun fill(
        getChildren: PatternLogicOperator.() -> List<IdentifiablePattern>
    ): List<ConstraintSyntaxNode>? {
        return if (content is PatternConstraint) {
            null
        } else {
            children = (content as PatternLogicOperator).getChildren().map {
                ConstraintSyntaxNode(it)
            }
            return children
        }
    }

    private fun getAllId(): List<String> {
        val constraintIds = mutableListOf<String>()
        val logicIds = mutableListOf<String>()
        traverse {
            it as ConstraintSyntaxNode
            if (it.content is PatternConstraint) {
                constraintIds.add(it.content.patternId)
            }
            else {
                it.content as PatternLogicOperator
                logicIds.add(it.content.patternId)
            }
        }
        return constraintIds.toList()
    }

    private fun predicate(eval: (IdentifiablePattern) -> Boolean): Boolean {
        return if (content is PatternConstraint) {
            eval(content)
        } else {
            content as PatternLogicOperator
            when (content.type) {
                LogicOperator.And -> children!!.all { it.predicate(eval) }
                LogicOperator.Or -> children!!.any { it.predicate(eval) }
                LogicOperator.Not -> !(children!![0].predicate(eval))
                LogicOperator.Xor -> children!![0].predicate(eval) xor children!![1].predicate(eval)
                LogicOperator.Xnor -> !(children!![0].predicate(eval) xor children!![1].predicate(eval))
            }
        }
    }

    fun getOnes(): Pair<List<String>, IntArray> {
        val idList = getAllId()
        val idMap = idList.mapIndexed { index, s -> s to index }.toMap()
        val result = mutableListOf<Int>()
        for (i in 0 until 2.0.pow(idList.size).toInt()) {
            val predictArray = i.toTerm(idList.size)
            val predicated = predicate {
                predictArray[idMap[it.patternId]!!] == '1'
            }
            if (predicated) {
                result.add(i)
            }
        }
        return Pair(idList, result.toIntArray())
    }

}
