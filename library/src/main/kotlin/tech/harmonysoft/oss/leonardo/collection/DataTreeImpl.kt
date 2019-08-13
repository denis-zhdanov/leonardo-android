package tech.harmonysoft.oss.leonardo.collection

import java.lang.StringBuilder
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

/**
 * An AVL tree which assumes that it's accessed only from a single thread at the time and that the access
 * is iteration-like:
 *
 *   1. [getPreviousKey] (returns, say, *k1*)
 *   2. [getNextKey(k1)][getNextKey] (returns *k2*)
 *   3. [getNextKey(k2)][getNextKey] (returns *k3*)
 *   4. [getNextKey(k3)][getNextKey] (returns *k4*)
 *   5. etc
 *
 * It caches last call's results and tries re-using them on subsequent calls.
 */
class DataTreeImpl<K, V>(private val comparator: Comparator<K>) : DataTree<K, V> {

    private val id = COUNTER.incrementAndGet()

    private var root: Entry<K, V>? = null
    private var cached: Entry<K, V>? = null

    var log: (String) -> Unit = {
        println(it)
    }

    override val empty: Boolean
        get() = root == null

    override val keys: Set<K>
        get() {
            if (root == null) {
                return emptySet()
            }
            val result = mutableSetOf<K>()
            val toProcess = Stack<Entry<K, V>>()
            toProcess.add(root)
            while (toProcess.isNotEmpty()) {
                val entry = toProcess.pop()
                result += entry.key
                entry.left?.let { toProcess += it }
                entry.right?.let { toProcess += it }
            }
            return result
        }

    override val first: V?
        get() {
            var entry: Entry<K, V>? = root
            while (entry != null) {
                if (entry.left != null) {
                    entry = entry.left
                } else {
                    return entry.value
                }
            }
            return null
        }

    override val last: V?
        get() {
            var entry: Entry<K, V>? = root
            while (entry != null) {
                if (entry.right != null) {
                    entry = entry.right
                } else {
                    return entry.value
                }
            }
            return null
        }

    override fun get(key: K): V? {
        return getEntry(key)?.value
    }

    private fun getEntry(key: K): Entry<K, V>? {
        if (root == null) {
            return null
        }

        var entry = root
        while (entry != null) {
            val cmp = comparator.compare(key, entry.key)
            entry = when {
                cmp == 0 -> {
                    cached = entry
                    return entry
                }
                cmp < 0 -> entry.left
                else -> entry.right
            }
        }
        return null
    }

    override fun getPreviousKey(key: K): K? {
        val c = cached
        var entry: Entry<K, V>? = null
        if (c != null && comparator.compare(key, c.key) == 0) {
            if (c.rightChild) {
                // There is a possible case like this:
                //        4
                //       / \
                //      2   5
                //     / \
                //    1   3
                // and cached value = 3. We need to go up then
                entry = c.parent
            } else if (c.leftChild) {
                // There is a possible case like this:
                //         2
                //        / \
                //       1   4
                //          / \
                //         3   5
                // and key = 3 and cached value = 3. We need to go up then
                var p = c.parent
                while (p != null) {
                    if (comparator.compare(key, p.key) > 0) {
                        entry = p
                        break
                    } else {
                        p = p.parent
                    }
                }
            }
        }
        if (entry == null) {
            entry = root ?: return null
        }
        var result: Entry<K, V>? = null
        while (entry != null) {
            val cmp = comparator.compare(key, entry.key)
            when {
                cmp <= 0 -> entry = entry.left
                else -> {
                    result = entry
                    entry = entry.right
                }
            }
        }

        return if (result == null) {
            null
        } else {
            cached = result
            result.key
        }
    }

    override fun getPreviousValue(key: K): V? {
        val previousKey = getPreviousKey(key) ?: return null
        return get(previousKey)
    }

    override fun getNextKey(key: K): K? {
        val c = cached
        var entry: Entry<K, V>? = null
        if (c != null && comparator.compare(key, c.key) == 0) {
            if (c.leftChild) {
                // There is a possible case like this:
                //     3
                //    / \
                //   1   5
                // and key = 1 and cached value = 1. We need to go up then
                entry = c.parent
            }
        }
        if (entry == null) {
            entry = root ?: return null
        }
        var result: Entry<K, V>? = null
        while (entry != null) {
            val cmp = comparator.compare(key, entry.key)
            when {
                cmp >= 0 -> entry = entry.right
                else -> {
                    result = entry
                    entry = entry.left
                }
            }
        }

        return if (result == null) {
            null
        } else {
            cached = result
            result.key
        }
    }

    override fun getNextValue(key: K): V? {
        val nextKey = getNextKey(key) ?: return null
        return get(nextKey)
    }

    override fun put(key: K, value: V) {
        if (DEBUG) {
            log("$id: DataTreeImpl.put($key, $value)")
        }
        cached = null
        val newEntry = insert(key, value) ?: return
        updateHeights(newEntry)
        balance(newEntry)
    }

    private fun insert(key: K, value: V): Entry<K, V>? {
        val r = root ?: return Entry(key, value).apply {
            root = this
        }

        var entry: Entry<K, V> = r
        while (true) {
            val cmp = comparator.compare(key, entry.key)
            if (cmp == 0) {
                entry.value = value
                return null
            } else if (cmp < 0) {
                val left = entry.left
                if (left == null) {
                    return Entry(key, value).apply {
                        entry.left = this
                        parent = entry
                    }
                } else {
                    entry = left
                }
            } else if (cmp > 0) {
                val right = entry.right
                if (right == null) {
                    return Entry(key, value).apply {
                        entry.right = this
                        parent = entry
                    }
                } else {
                    entry = right
                }
            }
        }
    }

    private fun updateHeights(leaf: Entry<*, *>) {
        var parent = leaf.parent
        var current = leaf
        while (parent != null) {
            if (parent._height <= current._height) {
                parent._height = current._height + 1
                current = parent
                parent = parent.parent
            } else {
                break
            }
        }
    }

    private fun balance(entry: Entry<K, V>) {
        while (true) {
            val unbalanced = findUnbalanced(entry) ?: break
            val left = unbalanced.left
            val right = unbalanced.right
            if (left != null && left.height > right.height) {
                if (left.left.height < left.right.height ) {
                    rotateLeft(left)
                }
                rotateRight(unbalanced)
            } else if (right != null && right.height > left.height) {
                if (right.left.height > right.right.height) {
                    rotateRight(right)
                }
                rotateLeft(unbalanced)
            }
        }

        if (VALIDATE) {
            validateState()
        }
    }

    private fun findUnbalanced(child: Entry<K, V>): Entry<K, V>? {
        var entry: Entry<K, V>? = child
        while (entry != null) {
            val diff = entry.left.height - entry.right.height
            if (diff < -1 || diff > 1) {
                return entry
            } else {
                entry = entry.parent
            }
        }
        return null
    }

    override fun removeLowerThen(key: K) {
        if (DEBUG) {
            log("$id: DataTreeImpl.removeLowerThen($key)")
        }
        cached = null
        var shouldContinue = true
        while (shouldContinue) {
            var entry = root
            shouldContinue = false
            while (entry != null) {
                val cmp = comparator.compare(key, entry.key)
                if (cmp <= 0) {
                    entry = entry.left
                } else {
                    doRemove(entry)?.let {
                        balance(it)
                    }
                    if (VALIDATE) {
                        validateState()
                    }
                    shouldContinue = true
                    break
                }
            }
        }

        if (VALIDATE) {
            validateState()
        }
    }

    private fun refreshHeightUp(entry: Entry<K, V>?) {
        var e = entry
        while (e != null) {
            e.refreshHeight()
            e = e.parent
        }
    }

    override fun remove(key: K): V? {
        cached = null
        val (value, toBalance) = doRemove(key) ?: return null
        if (toBalance != null) {
            balance(toBalance)
        }
        if (VALIDATE) {
            validateState()
        }
        return value
    }

    private fun doRemove(key: K): Pair<V, Entry<K, V>?>? {
        var entry = root ?: return null
        while (true) {
            val cmp = comparator.compare(key, entry.key)
            entry = when {
                cmp < 0 -> entry.left ?: return null
                cmp > 0 -> entry.right ?: return null
                else -> {
                    return entry.value to doRemove(entry)
                }
            }
        }
    }

    private fun doRemove(entry: Entry<K, V>): Entry<K, V>? {
        if (entry.left == null || entry.right == null) {
            entry.parent?.let { parent ->
                val newChild = entry.left ?: entry.right
                if (entry.leftChild) {
                    parent.left = newChild
                } else {
                    parent.right = newChild
                }
                refreshHeightUp(parent)
            }
            entry.left?.parent = entry.parent
            entry.right?.parent = entry.parent
            if (entry.parent == null) {
                root = entry.left ?: entry.right
            }
            return entry.parent
        }
        else {
            val replacement = removeMin(entry.right!!)
            when {
                root == entry -> root = replacement
                entry.leftChild -> entry.parent?.left = replacement
                entry.rightChild -> entry.parent?.right = replacement
            }
            val replacementParent = replacement.parent
            replacement.parent = entry.parent
            replacement.left = entry.left
            replacement.right = entry.right
            entry.left?.let {
                it.parent = replacement
            }
            entry.right?.let {
                it.parent = replacement
            }
            refreshHeightUp(replacement)
            return if (replacementParent == null || replacementParent == entry) {
                replacement
            } else {
                replacementParent
            }
        }
    }

    private fun removeMin(entry: Entry<K, V>): Entry<K, V> {
        entry.left?.let {
            return removeMin(it)
        }
        if (entry.leftChild) {
            entry.parent?.left = entry.right
        } else if (entry.rightChild) {
            entry.parent?.right = entry.right
        }
        entry.left?.parent = entry.parent
        entry.right?.parent = entry.parent
        refreshHeightUp(entry.parent)
        return entry
    }

    override fun removeGreaterThen(key: K) {
        if (DEBUG) {
            log("$id: DataTreeImpl.removeGreaterThen($key)")
        }
        cached = null
        var shouldContinue = true
        while (shouldContinue) {
            var entry = root
            shouldContinue = false
            while (entry != null) {
                val cmp = comparator.compare(key, entry.key)
                if (cmp >= 0) {
                    entry = entry.right
                } else {
                    doRemove(entry)?.let {
                        balance(it)
                    }
                    if (VALIDATE) {
                        validateState()
                    }
                    shouldContinue = true
                    break
                }
            }
        }

        if (VALIDATE) {
            validateState()
        }
    }

    private fun rotateLeft(x: Entry<K, V>): Entry<K, V> {
        //     x             y
        //    / \           / \
        //  t1  y     ->   x  t3
        //     / \        / \
        //   t2  t3     t1  t2

        val y = x.right ?: return x
        val t2 = y.left

        y.left = x
        x.right = t2
        if (t2 != null) {
            t2.parent = x
        }

        x._height = max(x.left.height, x.right.height) + 1
        y._height = max(y.left.height, y.right.height) + 1

        val parent = x.parent
        y.parent = parent
        x.parent = y
        if (parent == null) {
            root = y
        } else {
            if (parent.right === x) {
                parent.right = y
            } else {
                parent.left = y
            }
            var p = parent
            while (p != null) {
                p.refreshHeight()
                p = p.parent
            }
        }
        return y
    }

    private fun rotateRight(y: Entry<K, V>): Entry<K, V> {
        //       y            x
        //      / \          / \
        //     x  t3   ->  t1  y
        //    / \             / \
        //  t1  t2          t2  t3

        val x = y.left ?: return y
        val t2 = x.right

        x.right = y
        y.left = t2
        if (t2 != null) {
            t2.parent = y
        }

        y.refreshHeight()
        x.refreshHeight()

        val parent = y.parent
        x.parent = parent
        y.parent = x
        if (parent == null) {
            root = x
        } else {
            if (parent.right === y) {
                parent.right = x
            } else {
                parent.left = x
            }
            var p = parent
            while (p != null) {
                p.refreshHeight()
                p = p.parent
            }
        }
        return x
    }

    private fun validateState() {
        val entry = root ?: return
        validateState(entry, null)
    }

    private fun validateState(entry: Entry<K, V>, parent: Entry<K, V>?) {
        if (entry.parent !== parent) {
            throw IllegalStateException("Expected that ${entry.key} has parent ${parent?.key ?: "<null>"} but "
                                        + "it has ${entry.parent?.key ?: "<null>"}. Current keys: $keys")
        }

        val realHeight = calculateHeight(entry)
        if (realHeight != entry._height) {
            throw IllegalStateException("Height ${entry._height} is stored for ${entry.key} but real height "
                                        + "is $realHeight. Current keys: $keys")
        }

        val leftHeight = calculateHeight(entry.left)
        val rightHeight = calculateHeight(entry.right)
        val diff = leftHeight - rightHeight
        if (diff < -1 || diff > 1) {
            throw IllegalStateException("${entry.key} is unbalanced - left height is $leftHeight, right height"
                                        + " is $rightHeight. Current keys: $keys")
        }

        entry.left?.let { validateState(it, entry) }
        entry.right?.let { validateState(it, entry) }
    }

    private fun calculateHeight(entry: Entry<K, V>?): Int {
        if (entry == null) {
            return 0
        }
        return max(calculateHeight(entry.left), calculateHeight(entry.right)) + 1
    }

    /**
     * Allows to generate tree visualization instruction for graphviz - http://www.webgraphviz.com
     */
    @Suppress("unused")
    private fun generateGraphviz(): String {
        val buffer = StringBuilder()
        buffer.append("digraph G {\n")
        val toProcess = Stack<Entry<K, V>>()
        root?.let { toProcess += it }
        while (!toProcess.isEmpty()) {
            val entry = toProcess.pop()
            entry.left?.let { left ->
                buffer.append("  ${entry.key} -> ${left.key}\n")
                toProcess += left
            }
            entry.right?.let { right ->
                buffer.append("  ${entry.key} -> ${right.key}\n")
                toProcess += right
            }
        }
        buffer.append("}")
        return buffer.toString()
    }

    companion object {
        private val DEBUG = java.lang.Boolean.getBoolean("debug.data.tree")
        private val VALIDATE = java.lang.Boolean.getBoolean("validate.data.tree")
        private val COUNTER = AtomicInteger()
    }

    private val Entry<*, *>?.height: Int
        get() {
            return this?._height ?: 0
        }

    private class Entry<K, V>(
        var key: K,
        var value: V,
        var parent: Entry<K, V>? = null,
        var left: Entry<K, V>? = null,
        var right: Entry<K, V>? = null,
        var _height: Int = 1
    ) {

        val leftChild: Boolean
            get() {
                return parent?.left === this
            }

        val rightChild: Boolean
            get() {
                return parent?.right === this
            }

        fun refreshHeight() {
            _height = max(left?._height ?: 0, right?._height ?: 0) + 1
        }

        override fun toString(): String {
            return key.toString()
        }
    }
}