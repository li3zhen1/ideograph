package me.lizhen

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import org.junit.Test


class LanguageTest {
    @Test
    fun sliceTest() {
        val a = listOf(1, 2, 3)
        val c = a
        val b = a.slice(0 until 2)
        println(a)
        println(b)
        println(c)
        println(a == b)
        println(a === b)
        println(a == c)
        println(a === c)
        println(b == c)
        println(b === c)
    }

//    @Test
//    fun rt() {
//        val codiconStyleCssString = Regex("""\t@font-face \{\n\t\tfont-family: "codicon";\n\t\t.+\n\t\t.+\n\t}""")
//        Runtime.getRuntime().exec("")
//        println("""/*!-----------------------------------------------------------------------------
// * Copyright (c) Microsoft Corporation. All rights reserved.
// * Version: 0.33.0(4b1abad427e58dbedc1215d99a0902ffc885fcd4)
// * Released under the MIT license
// * https://github.com/microsoft/monaco-editor/blob/main/LICENSE.txt
// *-----------------------------------------------------------------------------*/""".replace(codiconStyleCssString, ""))
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun flowTest() {
//        runBlocking {
//            withContext(Dispatchers.IO) {
//                val i = flow {
//
//                }.first()
//                println(i)
//            }
//        }


        runBlocking {
            withContext(Dispatchers.IO) {
                val a = produce {
                    delay(3000)
                    println("[1]")
                    send(1)
                }
                val b = produce {
                    delay(2000)
                    println("[2]")
                    send(2)
                }
                val c = produce {
                    delay(1000)
                    println("[3]")
                    send(3)
                }
                val res = select<Int> {
                    listOf(a, b, c).map {
                        it.onReceive{ result -> result }
                    }
                }
                println("selected $res")
                coroutineContext.cancelChildren()
            }
        }

        println("coroutine ended")
        runBlocking { delay(5000) }
    }
}