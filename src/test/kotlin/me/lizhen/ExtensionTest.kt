package me.lizhen

//import me.lizhen.algorithms.toBinaryArray
import org.junit.Test
//import kotlin.time.ExperimentalTime
//import kotlin.time.measureTime

class ExtensionTest {
//    @OptIn(ExperimentalTime::class)
    @Test
    fun testBooleanArray() {

        val a = setOf(1,2)
        val b = a.union(listOf(1,3))
        print(b)
//        val a = 723;
//        val t = measureTime {
//            for (i in 1..Int.MAX_VALUE) {
//                val b = Integer.highestOneBit(i)
//            }
//        }
//        println(t.inWholeMilliseconds)
//        val t2 = measureTime {
//            for (i in 1..Int.MAX_VALUE) {
////                val b = i.toBinaryArray();
//            }
//        }
//        println(t2.inWholeMilliseconds)
//        println(Integer.highestOneBit(a))
//        println(Integer.toBinaryString(723))
//        a.toBinaryArray().forEach {
//            print(if (it) 1 else 0)
//        }
    }
}