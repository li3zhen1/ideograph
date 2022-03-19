package me.lizhen

import org.junit.Test

class LanguageTest {
    @Test
    fun sliceTest() {
        val a = listOf(1,2,3)
        val c = a
        val b = a.slice(0 until 2)
        println(a)
        println(b)
        println(c)
        println(a==b)
        println(a===b)
        println(a==c)
        println(a===c)
        println(b==c)
        println(b===c)
    }
}