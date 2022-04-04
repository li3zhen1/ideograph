package me.lizhen.utils

import me.lizhen.algorithms.Term

val String.term: Term
    get() = Term(this)