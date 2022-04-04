package me.lizhen.algorithms

class DisjointSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private val getId: T.() -> K
) {
    private val parent = nodes.associateBy { it.getId() }.toMutableMap()
    private val rank = nodes.associate { it.getId() to 0 }.toMutableMap()
    private val size = nodes.associate { it.getId() to 1 }.toMutableMap()

    public var setCount: Int = nodes.count()

    private fun findSet(item: T): T {
        val id = item.getId()
        if (parent[id] !== item) {
            parent[id] = findSet(parent[id]!!)
        }
        return parent[id]!!
    }

    public fun union(parent: T, child: T): DisjointSet<T, K> {
        if (includes(parent) && includes(child)) {
            var xRep = findSet(parent)
            var yRep = findSet(child)
            var xRepId = xRep.getId()
            var yRepId = yRep.getId()
            if (xRepId !== yRepId) {
                val rankDiff = rank[xRepId]!! - rank[yRepId]!!
                if (rankDiff == 0) {
                    rank[xRepId] = rank[xRepId]!! + 1
                } else if (rankDiff < 0) {
                    xRep = yRep.also { yRep = xRep }
                    xRepId = yRepId.also { yRepId = xRepId }
                }
                this.parent[yRepId] = xRep
                size[xRepId] = size[yRepId]!!
                size.remove(yRepId)
                setCount -= 1
            }
        }
        return this
    }

    private fun includes(item: T) = parent.contains(item.getId())

    public fun isRepresentative(item: T): Boolean {
        if (!includes(item)) return false
        return parent[item.getId()] == item
    }

    public fun isSingleton(item: T) = size[item.getId()] == 1

}