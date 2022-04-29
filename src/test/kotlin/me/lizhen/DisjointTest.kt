package me.lizhen

import me.lizhen.algorithms.DisjointSet
import me.lizhen.algorithms.MergeSet
import org.junit.Test

class DisjointTest {
    @Test
    fun test() {
        val nodes = listOf( 1, 12, 32, 54, 99)
        val ds = MergeSet(nodes) { this }
        ds.union(32, 54)
        ds.union(99, 54)
        ds.union(1, 12)
        ds.union(32, 12)
        println(ds.rootGroupMap)
    }
}