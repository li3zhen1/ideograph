package me.lizhen.algorithms

import java.util.Dictionary


/**
 * TODO: replace DisjointSet
 */
class TopologicalSorter<T>(
    public val nodes: MutableList<T>,
) {
    private val items = mutableListOf<SortItem<T>>()
    public fun add(
        nodes: List<T>,
        options: TopologicalSortOption?
    ): MutableList<T> {
        val before = options?.before.orEmpty()
        val after = options?.after.orEmpty()
        val group = options?.group ?: "?"
        val sort = options?.sort ?: 0

        assert(!before.contains(group)) { "Item cannot come before itself: $group" }
        assert(!before.contains("?")) { "Item cannot come before unassociated items" }
        assert(!after.contains(group)) { "Item cannot come after itself: $group" }
        assert(!after.contains("?")) { "Item cannot come after unassociated items" }

        nodes.forEach {
            items.add(
                SortItem(
                    items.size,
                    sort,
                    before,
                    after,
                    group,
                    it
                )
            )
        }

        if (options?.manual == false) {
            val valid = internalSort()
        }
        return this.nodes
    }

    public fun merge(vararg others: TopologicalSorter<T>): List<T> {
        this.items += others.flatMap { ts -> ts.items.map { it.copy() } }
        items.sortedBy { it.rank }
        this.items.forEachIndexed { id, it -> it.seq = id }
        val valid = internalSort()
        assert(valid) { "merge created a dependencies error" }
        return nodes
    }

    public fun sort(): List<T> {
        val valid = internalSort()
        assert(valid) { "sort created a dependencies error" }
        return nodes
    }

    private fun internalSort(): Boolean {
        val graph = mutableMapOf<Int, List<Int>>()
        val graphAfters = mutableMapOf<String, MutableList<Int>>()
        val groups = mutableMapOf<String, MutableList<Int>>()
        items.forEach {
            val seq = it.seq
            val group = it.group

            groups.getOrPut(group) { mutableListOf() }.add(seq)

//            graph[seq] = it.before

            it.after.forEach { afterIt ->
                graphAfters.getOrPut(afterIt) { mutableListOf() }.add(seq)
            }
        }

//        graph.forEach { (key, value) ->
//            val expandedGroups = value.flatMap {
//                groups.getOrPut(it) { mutableListOf() }
//            }
//            graph[key] = expandedGroups
//        }

        throw NotImplementedError()
    }

    data class SortItem<T>(
        var seq: Int,
        val rank: Int,
        val before: List<String>,
        val after: List<String>,
        val group: String,
        val node: T,
    )

    companion object {
        data class TopologicalSortOption(
            val group: String?,
            val before: List<String>?,
            val after: List<String>?,
            val sort: Int?,
            val manual: Boolean?,
        )
    }
}