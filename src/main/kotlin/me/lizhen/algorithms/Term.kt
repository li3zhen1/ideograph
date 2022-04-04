@file:Suppress("NOTHING_TO_INLINE")

package me.lizhen.algorithms

data class Term(
    val chars: CharArray
) : Comparable<Term> {

    constructor(content: String) : this(content.toCharArray())
    constructor(size: Int) : this(CharArray(size))
    constructor(size: Int, predicate: (Int) -> Char) : this(CharArray(size, predicate))

    public operator fun get(index: Int): Char = chars[index]

    public operator fun set(index: Int, value: Char) {
        chars[index] = value
    }

    public val size: Int = chars.size

    public operator fun iterator() = chars.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Term

        if (!chars.contentEquals(other.chars)) return false

        return true
    }

    override fun hashCode(): Int {
        return chars.contentHashCode()
    }

    override fun toString() = String(chars)

    val indices
        get() = chars.indices

    public inline fun count() = chars.count();

    /**
     * Returns the number of elements matching the given [predicate].
     */
    public inline fun count(predicate: (Char) -> Boolean): Int {
        var count = 0
        for (element in this) if (predicate(element)) ++count
        return count
    }

    public inline fun forEachIndexed(predicate: (Int, Char) -> Unit) = chars.forEachIndexed(predicate)

    public inline fun copyOf() = Term(chars.copyOf())

    public inline fun <T> map(transform: (Char) -> T) = chars.map(transform)

    public inline fun <T> mapIndexed(transform: (Int, Char) -> T) = chars.mapIndexed(transform)

    public inline fun zip(other: Term) = chars.zip(other.chars)

    public inline fun forEach(action: (Char) -> Unit) = chars.forEach(action)

    inline fun replace(from: Char, to: Char) = this.apply {
        indices.forEach {
            if (this[it] == from) this[it] = to
        }
    }

//    inline fun lessThan(other: Term): Boolean = this.toString() <= other.toString()


    inline fun getIndicesOf(char: Char) = getIndicesFromTerm(this, char)

    inline fun getTermFromImplicants() = listOf('1', '0', '^', '~', '-').map { this.getIndicesOf(it).toList() }

    inline fun getComplexity() = this.getTermFromImplicants().run {
        this[0].size + this[1].size * 1.5 + this[2].size * 1.25 + this[3].size * 1.75
    }



    override fun compareTo(other: Term): Int = String(chars).compareTo(String(other.chars))

}


inline fun Int.toTerm(bits: Int) = Integer.toBinaryString(this).run {
    if (bits >= length)
        Term(this.toCharArray(CharArray(bits) { '0' }, bits - length))
    else
        Term(this.toCharArray(CharArray(bits) { '0' }, 0, length - bits, length))
}
