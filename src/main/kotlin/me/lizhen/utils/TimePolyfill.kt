package me.lizhen.utils

import java.time.LocalDateTime
import java.time.LocalTime

inline fun <T>withTimeMeasure(closure: ()->T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = closure()
    val end = System.currentTimeMillis()
    return Pair(
        (end - start),
        result
    )
}


inline fun measureElapsedTime(closure: ()->Unit): Long {
    val start = System.currentTimeMillis()
    closure()
    val end = System.currentTimeMillis()
    return (end - start)
}