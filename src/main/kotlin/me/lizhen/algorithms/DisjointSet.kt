package me.lizhen.algorithms


open class DisjointSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private val getId: T.() -> K
) {
    protected val parentMap = nodes.associateBy(getId).toMutableMap()
    protected val rankMap = nodes.associate { it.getId() to 0 }.toMutableMap()
    public val sizeMap = nodes.associate { it.getId() to 1 }.toMutableMap()
    private val nodeMap = nodes.associateBy(getId)
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
) : DisjointSet<T, K>(nodes, getId) {
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


data class MergeSet<T, K : Comparable<K>>(
    private val nodes: Collection<T>,
    private inline val getId: T.() -> K
) {
    private val idList = nodes.map(getId)
    private val rootChildrenMap = idList.associateWith { mutableListOf(it) }.toMutableMap()
    private val childRootMap = idList.associateBy { it }.toMutableMap()

    fun union(a: T, b: T) {
        val aRootId = childRootMap[a.getId()]!!
        val bRootId = childRootMap[b.getId()]!!

        if (aRootId === bRootId) return

        val aGroup = rootChildrenMap[aRootId]!!
        val bGroup = rootChildrenMap[bRootId]!!

        if (aGroup.size >= bGroup.size) {
            bGroup.forEach {
                childRootMap[it] = aRootId
            }
            rootChildrenMap[aRootId]!! += bGroup
            rootChildrenMap.remove(bRootId)
        } else {
            aGroup.forEach {
                childRootMap[it] = bRootId
            }
            rootChildrenMap[bRootId]!! += aGroup
            rootChildrenMap.remove(aRootId)
        }
    }

    fun union(pairs: Map<T, T>) {
        pairs.forEach { (a, b) ->
            union(a, b)
        }
    }


    public fun directedUnion(parent: T, child: T) {
        val parentRootId = childRootMap[parent.getId()]!!
        val childRootId = childRootMap[child.getId()]!!

        if (parentRootId === childRootId) return

//        val parentGroup = rootChildrenMap[parentRootId]!!
        val childGroup = rootChildrenMap[childRootId]!!

        childGroup.forEach {
            childRootMap[it] = parentRootId
        }
        rootChildrenMap[parentRootId]!! += childGroup
        rootChildrenMap.remove(childRootId)
    }

    public fun directedUnion(pairs: Map<T, T>) {
        pairs.forEach { (a, b) ->
            directedUnion(a, b)
        }
    }

    val rootGroupMap get(): Map<K, List<K>> = rootChildrenMap
}
