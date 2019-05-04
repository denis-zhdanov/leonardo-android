package tech.harmonysoft.oss.leonardo.collection

interface DataTree<K, V> {

    val keys: Set<K>

    fun get(key: K): V?

    fun getPrevious(key: K): K?

    fun getNext(key: K): K?

    fun put(key: K, value: V)

    fun removeLowerThen(key: K)

    fun removeGreaterThen(key: K)
}