package me.lizhen.algorithms

import org.litote.kmongo.util.idValue

open class DisjointSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private val getId: T.() -> K
) {
    private val parentMap = nodes.associateBy(getId).toMutableMap()
    private val rankMap = nodes.associate { it.getId() to 0 }.toMutableMap()
    public val sizeMap = nodes.associate { it.getId() to 1 }.toMutableMap()
    private val nodeMap = nodes.associateBy(getId)
    public var rootCount: Int = nodes.count()

    private fun findSet(item: T): T {
        val id = item.getId()
        if (parentMap[id] !== item) {
            parentMap[id] = findSet(parentMap[id]!!)
        }
        return parentMap[id]!!
    }

    private fun findSet(id: K): T {
        val item = nodeMap[id]
        if (parentMap[id] !== item) {
            parentMap[id] = findSet(parentMap[id]!!)
        }
        return parentMap[id]!!
    }

    public fun union(parent: T, child: T): DisjointSet<T, K> {
        if (includes(parent) && includes(child)) {
            var xRep = findSet(parent)
            var yRep = findSet(child)
            var xRepId = xRep.getId()
            var yRepId = yRep.getId()
            if (xRepId !== yRepId) {
                val rankDiff = rankMap[xRepId]!! - rankMap[yRepId]!!
                if (rankDiff == 0) {
                    rankMap[xRepId] = rankMap[xRepId]!! + 1
                } else if (rankDiff < 0) {
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

    private inline fun includes(item: T) = parentMap.contains(item.getId())
    private inline fun includes(item: K) = parentMap.contains(item)

    public fun isRepresentative(item: T): Boolean {
        if (!includes(item)) return false
        return parentMap[item.getId()] == item
    }

    public fun isSingleton(item: T) = sizeMap[item.getId()] == 1

}

//class DynamicDisjointSet<T, K : Comparable<K>>(
//    private val nodes: Collection<T>,
//    private val getId: T.() -> K
//) : DisjointSet<T, K>(nodes, getId) {
//    public fun addNode(item: T): DynamicDisjointSet<T, K> {
//        val id = item.getId()
//        parentMap[id] = item
//        sizeMap[id] = 1
//        rankMap[id] = 1
//        rootCount ++
//        return this
//    }
//}