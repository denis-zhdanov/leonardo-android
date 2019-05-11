package tech.harmonysoft.oss.leonardo.collection

interface DataTree<K, V> {

    val empty: Boolean

    val first: V?

    val last: V?

    val keys: Set<K>

    fun get(key: K): V?

    fun getPreviousKey(key: K): K?

    fun getPreviousValue(key: K): V?

    fun getNextKey(key: K): K?

    fun getNextValue(key: K): V?

    fun put(key: K, value: V)

    fun removeLowerThen(key: K)

    fun removeGreaterThen(key: K)
}