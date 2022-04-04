package me.lizhen

import me.lizhen.algorithms.DisjointSet
import org.junit.Test

class DisjointTest {
    @Test
    fun test() {
        val nodes = listOf( 1, 12, 32, 54, 99)
        val ds = DisjointSet(nodes) { this }
        ds.union(32, 54)
        ds.union(99, 54)
        ds.union(1, 12)
        println(ds)
    }
}