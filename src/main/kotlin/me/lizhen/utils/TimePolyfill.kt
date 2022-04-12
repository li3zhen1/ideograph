package me.lizhen.utils

import java.time.LocalTime

inline fun <T>measureElapsedTime(closure: ()->T): Pair<Int, T> {
    val start = LocalTime.now().nano
    val result = closure()
    val end = LocalTime.now().nano
    return Pair(
        (end - start) / 1000,
        result
    )
}