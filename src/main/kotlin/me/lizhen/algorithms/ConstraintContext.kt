package me.lizhen.algorithms

import me.lizhen.schema.PatternConstraint
import me.lizhen.schema.PatternLogicOperator


class ConstraintContext(
    private val constraints: List<PatternConstraint>,
    private val logicOperators: List<PatternLogicOperator>,
    private val connections: List<Pair<String, String>>
) {

    private val allPatterns = (constraints + logicOperators)

    private val allPatternMap = allPatterns.associateBy { it.patternId }

    private fun getRootId(): String? {

        val disjointSet = DirectedDisjointSet(allPatterns) { patternId }

        connections.forEach { (from, to) ->
            val fromPattern = allPatternMap[from] ?: return@forEach
            val toPattern = allPatternMap[to] ?: return@forEach
            disjointSet.union(toPattern, fromPattern)
        }

        return disjointSet.sizeMap.maxByOrNull { it.value }?.key
    }

    private fun getSyntaxTree(): ConstraintSyntaxNode? {
        val rootId = getRootId() ?: return null
        val rootPattern = allPatternMap[rootId]!!
        if (rootPattern is PatternConstraint) {
            return ConstraintSyntaxNode(rootPattern)
        } else {
            rootPattern as PatternLogicOperator
            val root = ConstraintSyntaxNode(rootPattern)
            var visiting = listOf(root)
            while (visiting.isNotEmpty()) {
                visiting = visiting.flatMap {
                    it.fill {
                        connections
                            .filter { (_, to) -> patternId == to }
                            .mapNotNull { (from, _) -> allPatternMap[from] }
                    }.orEmpty()
                }
            }

            return root
        }
    }

    @Deprecated("use `splitSyntaxTree`")
    fun calculateSimplifiedSyntax(): ConstraintSyntaxNode? {
        val nativeSyntaxTree = getSyntaxTree() ?: return null
        val (idList, ones) = nativeSyntaxTree.getOnes()

        val quineMcCluskey = QuineMcCluskey.simplify(ones)
        val simplifiedSyntaxTree = ConstraintSyntaxNode(
            PatternLogicOperator("root", LogicOperator.Or),
            quineMcCluskey.toList().mapIndexed { index, term ->
                ConstraintSyntaxNode(
                    PatternLogicOperator("subroot$index", LogicOperator.And),
                    term.chars.filter { it != '-' }.mapIndexed { i, it ->
                        val pattern = allPatternMap[idList[i]]!!
                        when (it) {
                            '0' -> ConstraintSyntaxNode(
                                PatternLogicOperator("not${pattern.patternId}", LogicOperator.Not),
                                listOf(ConstraintSyntaxNode(pattern))
                            )
                            '1' -> ConstraintSyntaxNode(pattern)
                            else -> throw Error("Unexpected term char.")
                        }
                    }
                )
            }
        )
        return simplifiedSyntaxTree
    }

    fun splitSyntaxTree()
            : List<List<Pair<PatternConstraint, Boolean>>>? {
        val nativeSyntaxTree = getSyntaxTree() ?: return null
        val (idList, ones) = nativeSyntaxTree.getOnes()
        val terms = QuineMcCluskey.simplify(ones).toList()
        return terms.map { term ->
            term.chars.mapIndexed { index, char ->
                if (char == '-') return@mapIndexed null
                val pattern = allPatternMap[idList[index]] as PatternConstraint
                val isReversed = char == '0'
                pattern to isReversed
            }.filterNotNull()
        }.filter {
            it.isPossible()
        }
    }

    companion object {
        /**
         * test if impossible constraint clauses
         */
        fun List<Pair<PatternConstraint, Boolean>>.isPossible(): Boolean {
            val groupedByTarget = groupBy { it.first.targetPatternId to it.first.property }
            if (groupedByTarget.maxOf { it.value.size } <= 1) return true
            groupedByTarget.filter { it.value.size > 1 }.forEach { (target, constraints) ->
                // TODO: intersect constraints
            }
            return true
        }
    }
}