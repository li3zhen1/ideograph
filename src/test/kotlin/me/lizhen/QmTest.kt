package me.lizhen

import me.lizhen.algorithms.QuineMcCluskey
import me.lizhen.algorithms.Term
import kotlin.test.*



class QmTest {

    @Test
    fun test1() {
        val res = QuineMcCluskey().simplify(intArrayOf(1, 2, 5, 6, 9, 10, 13, 14))
        println(res)
        assert(res == setOf(Term("--01"), Term("--10")))
    }


    @Test
    fun test2() {
        val res = QuineMcCluskey().simplify(intArrayOf(2, 6, 10, 14))
        println(res)
        assert(res == setOf(Term("--10")))
    }


    @Test
    fun test3() {
        val res = QuineMcCluskey().simplify(intArrayOf(3, 4, 5, 7, 9, 13, 14, 15))
        println(res)

        assert(res == listOf("010-", "1-01", "111-", "0-11").map { Term(it) }.toSet())
    }

}