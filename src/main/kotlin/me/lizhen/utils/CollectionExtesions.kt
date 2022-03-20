
@file:Suppress("NOTHING_TO_INLINE")

public inline fun <A, B> List<Pair<A, B>>.toInvertedMap() = associate { Pair(it.second, it.first) }

public inline fun <T, V> List<T>.toInvertedMap(keyBy: (item: T) -> V) = mapIndexed { index, item -> Pair(index, keyBy(item)) }
    .associate { Pair(it.second, it.first) }

public inline fun <T> List<T>.toIndexedPair() = mapIndexed { index, it -> Pair(index, it)}

public inline fun <T> List<T>.toIndexedMap() = toIndexedPair().toMap()
