package me.lizhen.algorithms

import kotlin.math.*

fun permutations(value: Term, exclude: Set<Term> = setOf()) = sequence {
    val nBits = value.size
    val nXor = value.count { it == '^' || it == '~' }
    var xorValue = 0
    var seenXor = 0
    val res = Term(nBits) //value.map { '0' }.toMutableList()
    var i = 0
    var direction = +1
    while (i >= 0) {
        when (value[i]) {
            '1', '0' -> res[i] = value[i]
            '-' -> {
                if (direction == +1) {
                    res[i] = '0'
                } else if (res[i] == '0') {
                    res[i] = '1'
                    direction = +1
                }
            }
            '^' -> {
                seenXor += direction
                if (direction == +1) {
                    res[i] = if (seenXor == nXor && xorValue == 0) '1' else '0'
                } else if (res[i] == '0' && seenXor < nXor - 1) {
                    res[i] = '1'
                    direction = +1
                    seenXor += 1
                }
                if (res[i] == '1') {
                    xorValue = xorValue xor 1
                }
            }
            '~' -> {
                seenXor += direction
                if (direction == +1) {
                    res[i] = if (seenXor == nXor && xorValue == 1) '1' else '0'
                } else if (res[i] == '0' && seenXor < nXor - 1) {
                    res[i] = '1'
                    direction = +1
                    seenXor += 1
                }
                if (res[i] == '1') {
                    xorValue = xorValue xor 1
                }
            }
            else -> res[i] = '#'
        }
        i += direction
        if (i == nBits) {
            direction = -1
            i = nBits - 1

            if (!exclude.contains(res)) {
                yield(res)
            }
        }
    }
}.toList()

fun getIndicesFromTerm(term: Term, char: Char) = sequence {
    term.forEachIndexed { i, c -> if (c == char) yield(i) }
}

fun combine(a: Term, b: Term, dontCares: Set<Term>): Term? {
    val permutationsA = permutations(a, dontCares).toSet()
    val permutationsB = permutations(b, dontCares).toSet()
    val aTermDontCares = a.getIndicesOf('-').toList()
    val bTermDontCares = b.getIndicesOf('-').toList()
    val aPotential = a.copyOf()
    val bPotential = b.copyOf()
    aTermDontCares.indices.forEach { aPotential[it] = b[it] }
    bTermDontCares.indices.forEach { bPotential[it] = a[it] }
    val valid = listOf(aPotential, bPotential).filter {
        permutations(it, dontCares) == permutationsA + permutationsB
    }
    if (valid.isEmpty()) return null
    return valid.sortedBy { it.getComplexity() }[0]
}

object QuineMcCluskey{
    private var nBits = 0
    private val useXor: Boolean = false
    fun simplify(
        ones: IntArray,
        dontCares: IntArray = IntArray(0),
    ): Set<Term> {
        val terms = ones + dontCares
        if (terms.isEmpty()) return setOf()
        nBits = ceil(log2(1.0 + (ones + dontCares).maxOrNull()!!)).toInt()

        return simplify(
            ones.map { it.toTerm(nBits) }.toSet(),
            dontCares.map { it.toTerm(nBits) }.toSet(),
            nBits
        )
    }

    fun simplify(
        ones: Set<Term>,
        dontCares: Set<Term> = setOf(),
        numBits: Int? = null,
    ): Set<Term> {
        val terms = (ones + dontCares)
        if (terms.isEmpty()) return setOf()

        val nBits = numBits ?: terms.maxOf { it.size }
        if (nBits != terms.minOf { it.size }) return setOf()

        val primeImplicants = getPrimeImplicants(terms)
        val essentialImplicants = getEssentialImplicants(primeImplicants, dontCares)
        return reduceImplicants(essentialImplicants, dontCares)
    }

    private fun reduceImplicants(implicants: MutableSet<Term>, dontCares: Set<Term>): Set<Term> {
        while (true) {
            val combinations = implicants.flatMap { a -> implicants.map { b -> a to b } }

            for ((a, b) in combinations) {
                val combined = combine(a, b, dontCares)
                if (combined !== null) {
                    implicants.add(a)
                    implicants.add(b)
                    implicants.add(combined)
                    break
                }
            }
            // ?
            break
        }
        var coverage = implicants.associateWith {
            permutations(it).filter { p -> !dontCares.contains(p) }
        }.toMutableMap()
        while (true) {
            val redundant = mutableListOf<Term>()
            coverage.forEach { (thisImplicant, thisCoverage) ->
                val othersCoverage = coverage.keys.filter { k ->
                    k != thisImplicant
                }.flatMap { coverage[it]!! }.toSet()
                if (othersCoverage.containsAll(thisCoverage)) {
                    redundant.add(thisImplicant)
                }
            }
            if (redundant.isNotEmpty()) {
                val worst = redundant.maxByOrNull { it.getComplexity() }
                coverage.remove(worst)
            } else {
                break
            }
        }
        if (coverage.isEmpty()) coverage = mutableMapOf(Term(nBits) { '-' } to emptyList())
        return coverage.keys.toSet()
    }


    private fun reduceSimpleXnorTems(t1: Term, t2: Term): Term? {
        val ret = Term(t1.size)
        var diff10 = 0
        var diff20 = 0
        t1.zip(t2).forEachIndexed { i, (c1, c2) ->
            if (
                c1 == '^' || c2 == '^'
                || c1 == '~' || c2 == '~'
            ) return null
            else if (c1 != c2) {
                ret[i] = '~'
                if (c2 == '0') diff10 += 1
                else diff20 += 1
            } else ret[i] = c1
        }
        return if (
            (diff10 == 2 && diff20 == 0)
            || (diff10 == 0 && diff20 == 2)
        ) ret else null
    }

    private fun reduceSimpleXorTems(t1: Term, t2: Term): Term? {
        val ret = Term(t1.size)
        var diff10 = 0
        var diff20 = 0
        t1.zip(t2).forEachIndexed { i, (c1, c2) ->
            if (
                c1 == '^' || c2 == '^'
                || c1 == '~' || c2 == '~'
            ) return null
            else if (c1 != c2) {
                ret[i] = '^'
                if (c2 == '0') diff10 += 1
                else diff20 += 1
            } else ret[i] = c1
        }
        return if (diff10 == 1 && diff20 == 1) ret else null
    }

    private fun getPrimeImplicants(terms: Set<Term>): Set<Term> {
        val nGroups = nBits + 1
        var marked = setOf<Term>()

        var mutableTerms = terms.toMutableSet()

        val groupLists = List(nGroups) { mutableSetOf<Term>() }
        terms.forEach {
            groupLists[it.count { c -> c == '1' }].add(it)
        }
        if (useXor) {
            groupLists.forEachIndexed { index, group ->
                group.forEach { t1 ->
                    group.forEach { t2 ->
                        reduceSimpleXorTems(t1, t2)?.let {
                            mutableTerms.add(it)
                        }
                    }
                    if (index < nGroups - 2) {
                        groupLists[index + 2].forEach { t2 ->
                            reduceSimpleXnorTems(t1, t2)?.let {
                                mutableTerms.add(it)
                            }
                        }
                    }
                }
            }
        }
        var done = false
        var groups = mapOf<Triple<Int, Int, Int>, List<Term>>()
        while (!done) {

            groups = mutableTerms.groupBy { t ->
                val nOnes = t.count { it == '1' }
                val nXor = t.count { it == '^' }
                val nXnor = t.count { it == '~' }
                Triple(nOnes, nXor, nXnor)
            }.mapValues { it.value.distinct() }

            mutableTerms = mutableSetOf()
            val used = mutableSetOf<Term>()

            groups.forEach { (key, group) ->
                val keyNext = Triple(key.first + 1, key.second, key.third)
                if (groups.contains(keyNext)) {
                    val groupNext = groups[keyNext]!!
                    group.forEach { t ->
                        t.mapIndexed { i, c ->
                            if (c == '0') {
                                val t2: Term = t.copyOf().apply {
                                    this[i] = '1'
                                }
                                if (groupNext.contains(t2)) {
                                    val t12 = t.copyOf().apply {
                                        this[i] = '-'
                                    }
                                    used.add(t)
                                    used.add(t2)
                                    mutableTerms.add(t12)
                                }
                            }
                        }
                    }
                }
            }

            groups.filterKeys { it.second > 0 }.forEach { (key, _) ->
                val keyComplete = Triple(key.first + 1, key.third, key.second)
                groups[keyComplete]?.forEach {
                    val t1Complement: Term = it.replace('~', '^')
                    t1Complement.forEachIndexed { i, c ->
                        if (c == '0') {
                            val t2: Term = t1Complement.copyOf().apply { this[i] = '1' }
                            if (groups[keyComplete]!!.contains(t2)) {
                                val t12 = it.copyOf().apply { this[i] = '^' }
                                used.add(it)
                                mutableTerms.add(t12)
                            }
                        }
                    }
                }
            }

            groups.filterKeys { it.third > 0 }.forEach { (key, _) ->
                val keyComplete = Triple(key.first + 1, key.third, key.second)
                groups[keyComplete]?.forEach {
                    val t1Complement = it.replace('~', '^')
                    t1Complement.forEachIndexed { i, c ->
                        if (c == '0') {
                            val t2: Term = t1Complement.copyOf().apply { this[i] = '1' }
                            if (groups[keyComplete]!!.contains(t2)) {
                                val t12: Term = it.copyOf().apply { this[i] = '~' }
                                used.add(it)
                                mutableTerms.add(t12)
                            }
                        }
                    }
                }
            }

            groups.values.forEach {
                marked = marked + (it - used)
            }
            if (used.isEmpty()) { done = true }
        }

        val pi = marked.toMutableSet()
        groups.values.forEach {
            pi += it
        }

        return pi
    }

    private fun getEssentialImplicants(
        terms: Set<Term>, dontCareSets: Set<Term>
    ): MutableSet<Term> {
        val perms = terms.associateWith { permutations(it).filter { p -> dontCareSets.contains(p) } }
        var eiRanges = setOf<Term>()
        val ei = mutableSetOf<Term>()
        val groups = terms.groupBy { getTermRank(it, perms[it]!!.size) }
        groups.keys.sortedDescending().forEach { i ->
            groups[i]!!.forEach {
                if (eiRanges.containsAll(perms[it]!!)) {
                    ei.add(it)
                    eiRanges = eiRanges + perms[it]!!
                }
            }
        }
        if (ei.isEmpty()) return mutableSetOf(Term(nBits) { '-' })
        return ei
    }

    private fun getTermRank(
        term: Term,
        termRange: Int
    ) = term.chars.fold(termRange * 4) { acc, c ->
        acc + when (c) {
            '-' -> 8
            '^' -> 4
            '~' -> 2
            '1' -> 1
            else -> 0
        }
    }
}