package me.lizhen.algorithms

open class DisjointSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private val getId: T.() -> K
) {
    protected val parentMap = nodes.associateBy(getId).toMutableMap()
    protected val rankMap = nodes.associate { it.getId() to 0 }.toMutableMap()
    public val sizeMap = nodes.associate { it.getId() to 1 }.toMutableMap()
    protected val nodeMap = nodes.associateBy(getId)
    public var rootCount: Int = nodes.count()

    protected fun findSet(item: T): T {
        val id = item.getId()
        if (parentMap[id] !== item) {
            parentMap[id] = findSet(parentMap[id]!!)
        }
        return parentMap[id]!!
    }

    protected fun findSet(id: K): T {
        val item = nodeMap[id]
        if (parentMap[id] !== item) {
            parentMap[id] = findSet(parentMap[id]!!)
        }
        return parentMap[id]!!
    }

    public open fun union(parent: T, child: T): DisjointSet<T, K> {
        if (includes(parent) && includes(child)) {
            var xRep = findSet(parent)
            var yRep = findSet(child)
            var xRepId = xRep.getId()
            var yRepId = yRep.getId()
            if (xRepId !== yRepId) {
                val rankDiff = rankMap[xRepId]!! - rankMap[yRepId]!!
                if (rankDiff == 0) {
                    rankMap[xRepId] = rankMap[xRepId]!! + 1
                }
                else if (rankDiff < 0) {
                    xRep = yRep.also { yRep = xRep }
                    xRepId = yRepId.also { yRepId = xRepId }
                }
                this.parentMap[yRepId] = xRep
                sizeMap[xRepId] = sizeMap[xRepId]!! + sizeMap[yRepId]!!
                sizeMap.remove(yRepId)
                rootCount -= 1
            }
        }
        return this
    }

    protected fun includes(item: T) = parentMap.contains(item.getId())
    protected fun includes(item: K) = parentMap.contains(item)

    public fun isRepresentative(item: T): Boolean {
        if (!includes(item)) return false
        return parentMap[item.getId()] == item
    }

    public fun isSingleton(item: T) = sizeMap[item.getId()] == 1

}


class DirectedDisjointSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private val getId: T.() -> K
): DisjointSet<T, K>(nodes, getId) {
    override fun union(parent: T, child: T): DisjointSet<T, K> {
        if (includes(parent) && includes(child)) {
            val xRep = findSet(parent)
            val yRep = findSet(child)
            val xRepId = xRep.getId()
            val yRepId = yRep.getId()
            if (xRepId !== yRepId) {
                val rankDiff = rankMap[xRepId]!! - rankMap[yRepId]!!
                if (rankDiff == 0) {
                    rankMap[xRepId] = rankMap[xRepId]!! + 1
                }
                this.parentMap[yRepId] = xRep
                sizeMap[xRepId] = sizeMap[xRepId]!! + sizeMap[yRepId]!!
                sizeMap.remove(yRepId)
                rootCount -= 1
            }
        }
        return this
    }
}
