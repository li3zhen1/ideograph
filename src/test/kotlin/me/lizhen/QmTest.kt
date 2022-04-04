package me.lizhen

import me.lizhen.algorithms.QuineMcCluskey
import me.lizhen.algorithms.Term
//import me.lizhen.algorithms.Term
import me.lizhen.algorithms.toTerm
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime



class QmTest {

    @Test
    fun test1() {
        val res = QuineMcCluskey.simplify(intArrayOf(1, 2, 5, 6, 9, 10, 13, 14))
        assert(res == setOf(Term("--01"), Term("--10")))

    }


    @Test
    fun test2() {
        val res = QuineMcCluskey.simplify(intArrayOf(2, 6, 10, 14))
        assert(res == setOf(Term("--10")))
    }


    @Test
    fun test3() {
        val res = QuineMcCluskey.simplify(intArrayOf(3, 4, 5, 7, 9, 13, 14, 15))
        println(res)

        assert(res == listOf("010-", "1-01", "111-", "0-11").map { Term(it) }.toSet())
    }
//    @OptIn(ExperimentalTime::class)
//    @Test
//    fun permutationTest() {
//        val qm = QuineMcCluskey()
////        qm.permutations("1--^^1--01").forEach {
////            println(it)
////        }
//        val mt2 = measureTime {
//            for(i in 0..30){
//                qm.permutationsByBoolean("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt2)
//
//
//        val mt = measureTime {
//            for(i in 0..30){
//                qm.permutations("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt)
//
//        val mt3 = measureTime {
//            for(i in 0..30){
//                qm.permutationsByBoolean("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt3)
//
//
//        val mt4 = measureTime {
//            for(i in 0..30){
//                qm.permutations("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt4)
//
//        val mt5 = measureTime {
//            for(i in 0..30){
//                qm.permutationsByBoolean("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt5)
//
//
//        val mt6 = measureTime {
//            for(i in 0..30){
//                qm.permutations("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt6)
//
//
//
//        val mt7 = measureTime {
//            for(i in 0..30){
//                qm.permutationsByBoolean("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt7)
//
//
//        val mt8 = measureTime {
//            for(i in 0..30){
//                qm.permutations("1--^^1--01").forEach {
////                    println(it)
//                }
//            }
//        }
//        println(mt8)
//    }
}