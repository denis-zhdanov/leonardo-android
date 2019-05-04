package tech.harmonysoft.oss.leonardo.collection

import java.util.*
import kotlin.math.max

/**
 * An AVL tree which assumes that it's accessed only from a single thread at the time and that the access
 * is iteration-like:
 *
 *   1. [getPrevious] (returns, say, *k1*)
 *   2. [getNext(k1)][getNext] (returns *k2*)
 *   3. [getNext(k2)][getNext] (returns *k3*)
 *   4. [getNext(k3)][getNext] (returns *k4*)
 *   5. etc
 *
 * It caches last call's results and tries re-using them on subsequent calls.
 */
class DataTreeImpl<K, V>(private val comparator: Comparator<K>) : DataTree<K, V> {

    private var root: Entry<K, V>? = null
    private var cached: Entry<K, V>? = null

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

    override fun getPrevious(key: K): K? {
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

    override fun getNext(key: K): K? {
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

    override fun put(key: K, value: V) {
        val newEntry = insert(key, value) ?: return
        updateHeights(newEntry)
        val parent = findUnbalancedParent(newEntry) ?: return
        if (parent.left.height > parent.right.height) {
            val left = parent.left
            if (left != null) {
                if (comparator.compare(key, left.key) < 0) {
                    rotateRight(parent)
                } else {
                    rotateLeft(left)
                    rotateRight(parent)
                }
            }
        } else {
            val right = parent.right
            if (right != null) {
                if (comparator.compare(key, right.key) > 0) {
                    rotateLeft(parent)
                } else {
                    rotateRight(right)
                    rotateLeft(parent)
                }
            }
        }

        if (VALIDATE) {
            validateState()
        }
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

    private fun findUnbalancedParent(child: Entry<K, V>): Entry<K, V>? {
        var parent = child.parent
        while (parent != null) {
            val diff = parent.left.height - parent.right.height
            if (diff < -1 || diff > 1) {
                return parent
            } else {
                parent = parent.parent
            }
        }
        return null
    }

    override fun removeLowerThen(key: K) {
        val limitPoint = getThisOrHigher(key)

        if (limitPoint == null) {
            val r = root
            if (r != null && comparator.compare(r.key, key) < 0) {
                r.right?.parent = null
                root = r.right
                applyHeightDiff(-1, root)
            } else {
                root = null
            }
            cached = null
            return
        }

        limitPoint.left = null
        refreshHeightUp(limitPoint)

        // There is a possible case that we have a tree like below:
        //     2
        //    / \
        //   1  4
        //     / \
        //    3   5
        // And we are asked to remove everything lower than 3. In this situation we need to go up from
        // 'boundary node' 3 and replace a parent while current node is its right child. E.g. here we replace 2 by 4
        //
        // It's important to do that only for left parents. Consider that following situation:
        //     4
        //    / \
        //   2   5
        //  / \  \
        // 1   3  6
        // Suppose we're asked to remove everything lower then 3. Then we need to replace 2 by 3 first and
        // stop because 3 is a left child.

        var e: Entry<K, V>? = limitPoint
        while (e != null) {
            if (e.leftChild) {
                e = e.parent
                continue
            } else if (e.rightChild) {
                val parent = e.parent
                val grandFather = parent?.parent

                if (grandFather == null) {
                    root = e
                    e.parent = null
                    break
                } else {
                    if (parent.rightChild) {
                        grandFather.right = e
                    } else {
                        grandFather.left = e
                    }
                    e.parent = grandFather
                    refreshHeightUp(e.parent)
                }
            } else {
                break
            }
        }

        e = limitPoint
        do {
            while (e != null && !e.balanced) {
                rotateLeft(e)
            }
            e = e?.parent
        } while (e != null)


        cached = null

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

    private fun getThisOrHigher(key: K): Entry<K, V>? {
        var entry: Entry<K, V>? = root ?: return null
        var result: Entry<K, V>? = null
        while (entry != null) {
            val diff = comparator.compare(entry.key, key)
            if (diff == 0) {
                result = entry
                break
            } else if (diff > 0) {
                result = entry
                entry = entry.left
            } else {
                entry = entry.right
            }
        }
        return result
    }

    private fun applyHeightDiff(diff: Int, entry: Entry<K, V>?) {
        if (entry == null) {
            return
        }
        entry._height += diff
        applyHeightDiff(diff, entry.left)
        applyHeightDiff(diff, entry.right)
    }

    override fun removeGreaterThen(key: K) {
        val limitPoint = getThisOrLower(key)

        if (limitPoint == null) {
            val r = root
            if (r != null && comparator.compare(r.key, key) > 0) {
                root = r.left
                r.left?.parent = null
                applyHeightDiff(-1, root)
            } else {
                root = null
            }
            cached = null
            return
        }

        limitPoint.right = null
        refreshHeightUp(limitPoint)

        // Analogous to removeLowerThen() but mirror-reversed

        var e: Entry<K, V>? = limitPoint
        while (e != null) {
            if (e.rightChild) {
                e = e.parent
                continue
            } else if (e.leftChild) {
                val parent = e.parent
                val grandFather = parent?.parent

                if (grandFather == null) {
                    root = e
                    e.parent = null
                    break
                } else {
                    if (parent.leftChild) {
                        grandFather.left = e
                    } else {
                        grandFather.right = e
                    }
                    e.parent = grandFather
                    refreshHeightUp(e.parent)
                }
            } else {
                break
            }
        }

        e = limitPoint
        do {
            while (e != null && !e.balanced) {
                rotateRight(e)
            }
            e = e?.parent
        } while (e != null)


        cached = null

        if (VALIDATE) {
            validateState()
        }
    }

    private fun getThisOrLower(key: K): Entry<K, V>? {
        var entry: Entry<K, V>? = root ?: return null
        var result: Entry<K, V>? = null
        while (entry != null) {
            val diff = comparator.compare(entry.key, key)
            if (diff == 0) {
                result = entry
                break
            } else if (diff > 0) {
                entry = entry.left
            } else {
                result = entry
                entry = entry.right
            }
        }
        return result
    }

    private fun rotateLeft(x: Entry<K, V>): Entry<K, V>? {
        //     x             y
        //    / \           / \
        //  t1  y     ->   x  t3
        //     / \        / \
        //   t2  t3     t1  t2

        val y = x.right ?: return null
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

    private fun rotateRight(y: Entry<K, V>): Entry<K, V>? {
        //       y            x
        //      / \          / \
        //     x  t3   ->  t1  y
        //    / \             / \
        //  t1  t2          t2  t3

        val x = y.left ?: return null
        val t2 = x.right

        x.right = y
        y.left = t2
        if (t2 != null) {
            t2.parent = y
        }

        y._height = max(y.left.height, y.right.height) + 1
        x._height = max(x.left.height, x.right.height) + 1

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
            throw IllegalStateException("Expected that ${entry.key} has parent ${parent?.key ?: "<null>"} but it has ${entry.parent?.key ?: "<null>"}. Current keys: $keys")
        }

        val realHeight = calculateHeight(entry)
        if (realHeight != entry._height) {
            throw IllegalStateException("Height ${entry._height} is stored for ${entry.key} but real height is $realHeight. Current keys: $keys")
        }

        val leftHeight = calculateHeight(entry.left)
        val rightHeight = calculateHeight(entry.right)
        val diff = leftHeight - rightHeight
        if (diff < -1 || diff > 1) {
            throw IllegalStateException("${entry.key} is unbalanced - left height is $leftHeight, right height is $rightHeight. Current keys: $keys")
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

    companion object {
        private val VALIDATE = java.lang.Boolean.getBoolean("validate.data.tree")
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

        val balanced: Boolean
            get() {
                val diff = (left?._height ?: 0) - (right?._height ?: 0)
                return diff >= -1 && diff <= 1
            }

        fun refreshHeight() {
            _height = max(left?._height ?: 0, right?._height ?: 0) + 1
        }

        override fun toString(): String {
            return key.toString()
        }
    }
}