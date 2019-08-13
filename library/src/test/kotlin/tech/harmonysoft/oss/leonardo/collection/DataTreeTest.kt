package tech.harmonysoft.oss.leonardo.collection

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

class DataTreeTest {

    private lateinit var tree: DataTree<Int, String>

    @BeforeEach
    fun setUp() {
        System.setProperty("validate.data.tree", "true")
        tree = DataTreeImpl(Comparator { i1, i2 ->
            i1 - i2
        })
    }

    @Test
    fun `when tree is empty then all gets return null`() {
        assertThat(tree.get(1)).isNull()
        assertThat(tree.getPreviousKey(1)).isNull()
        assertThat(tree.getNextKey(1)).isNull()
    }

    @Test
    fun `when tree is empty then keys set is empty`() {
        assertThat(tree.keys).isEmpty()
    }

    @Test
    fun `when right-right case is encountered then get() works correctly`() {
        doTest(listOf(1, 2, 3))
    }

    @Test
    fun `when right-left case is encountered then get() works correctly`() {
        doTest(listOf(1, 3, 2))
    }

    @Test
    fun `when left-left case is encountered then get() works correctly`() {
        doTest(listOf(3, 2, 1))
    }

    @Test
    fun `when left-right case is encountered then get() works correctly`() {
        doTest(listOf(3, 1, 2))
    }

    private fun doTest(input: List<Int>) {
        input.forEach {
            tree.put(it, it.toString())
            assertThat(tree.get(it)).isEqualTo(it.toString())
        }
        input.forEach {
            assertThat(tree.get(it)).isEqualTo(it.toString())
        }
    }

    @Test
    fun `when a key is put to a tree then it can be retrieved`() {
        (1..1000).forEach { i ->
            tree.put(i, i.toString())
            (1..i).forEach { j ->
                assertThat(tree.get(j)).isEqualTo(j.toString())
            }
        }
    }

    @Test
    fun `when previous() is called then it works as expected`() {
        (1..100).forEach { max ->
            (1..max).forEach { step ->
                doTestPrevious(max, step)
            }
        }
    }

    private fun doTestPrevious(max: Int, step: Int) {
        setUp()
        val keys = TreeSet<Int>()
        (1..max step step).forEach {
            keys += it
            tree.put(it, it.toString())
        }

        (-1..(max + 2)).forEach {
            val expected = keys.lower(it)
            if (expected == null) {
                assertThat(tree.getPreviousKey(it))
                    .describedAs("getPreviousKey($it) doesn't work correctly when tree contains keys "
                                 + "from 1 to $max with step $step")
                    .isNull()
            } else {
                assertThat(tree.getPreviousKey(it))
                    .describedAs("getPreviousKey($it) doesn't work correctly when tree contains keys "
                                 + "from 1 to $max with step $step")
                    .isEqualTo(expected)
            }
        }
    }

    @Test
    fun `when next() is called then it works as expected`() {
        (1..100).forEach { max ->
            (1..max).forEach { step ->
                doTestNext(max, step)
            }
        }
    }

    private fun doTestNext(max: Int, step: Int) {
        setUp()
        val keys = TreeSet<Int>()
        (1..max step step).forEach {
            keys += it
            tree.put(it, it.toString())
        }

        (-1..(max + 2)).forEach {
            val expected = keys.higher(it)
            if (expected == null) {
                assertThat(tree.getNextKey(it))
                    .describedAs("getNextKey($it) doesn't work correctly when tree contains keys "
                                 + "from 1 to $max with step $step")
                    .isNull()
            } else {
                assertThat(tree.getNextKey(it))
                    .describedAs("getNextKey($it) doesn't work correctly when tree contains keys "
                                 + "from 1 to $max with step $step")
                    .isEqualTo(expected)
            }
        }
    }

    @Test
    fun `when lower keys are removed then tree works correctly`() {
        (1..100).forEach { i ->
            (1 until i).forEach { j ->
                doTestLowerKeysRemoval(j, i, 1)
                doTestLowerKeysRemoval(j, i, 2)
            }
        }
    }

    private fun doTestLowerKeysRemoval(maxToKeep: Int, maxToStore: Int, storeStep: Int) {
        setUp()
        (1..maxToStore step storeStep).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(maxToKeep)

        val keys = TreeSet<Int>()
        for (i in 1..maxToStore step storeStep) {
            if (i >= maxToKeep) {
                keys += i
            }
        }

        assertThat(tree.keys)
            .describedAs("tree with values from 1 to $maxToStore and step $storeStep with removed "
                         + "values less then $maxToKeep")
            .isEqualTo(keys)
        keys.forEach {
            assertThat(tree.get(it)).isEqualTo(it.toString())

            val expectedGreaterKey = keys.higher(it)
            val nextDescription = "getNextKey($it) mismatch in tree ${tree.keys}, built from values " +
                                  "from 1 to $maxToStore with step $storeStep and removed values " +
                                  "greater than $maxToKeep"
            if (expectedGreaterKey == null) {
                assertThat(tree.getNextKey(it)).describedAs(nextDescription).isNull()
            } else {
                assertThat(tree.getNextKey(it)).describedAs(nextDescription).isEqualTo(expectedGreaterKey)
            }

            val expectedLowerKey = keys.lower(it)
            val previousDescription = "getPreviousKey($it) mismatch in tree ${tree.keys}, built from values " +
                                      "from 1 to $maxToStore with step $storeStep and removed values " +
                                      "greater than $maxToKeep"
            if (expectedLowerKey == null) {
                assertThat(tree.getPreviousKey(it)).describedAs(previousDescription).isNull()
            } else {
                assertThat(tree.getPreviousKey(it)).describedAs(previousDescription).isEqualTo(expectedLowerKey)
            }
        }
    }

    @Test
    fun `when greater keys are removed then the tree works correctly`() {
        (1..100).forEach { i ->
            (1 until i).forEach { j ->
                doTestGreaterKeysRemoval(j, i, 1)
                doTestGreaterKeysRemoval(j, i, 2)
            }
        }
    }

    private fun doTestGreaterKeysRemoval(maxToKeep: Int, maxToStore: Int, storeStep: Int) {
        setUp()
        (1..maxToStore step storeStep).forEach { tree.put(it, it.toString()) }
        tree.removeGreaterThen(maxToKeep)

        val keys = TreeSet<Int>()
        for (i in 1..maxToStore step storeStep) {
            if (i <= maxToKeep) {
                keys += i
            }
        }

        assertThat(tree.keys)
            .describedAs("keys of tree with values from 1 to $maxToStore and step $storeStep with removed "
                         + "values greater then $maxToKeep")
            .isEqualTo(keys)
        keys.forEach {
            assertThat(tree.get(it)).isEqualTo(it.toString())

            val expectedGreaterKey = keys.higher(it)
            val nextDescription = "getNextKey($it) mismatch in tree ${tree.keys}, built from values " +
                                  "from 1 to $maxToStore with step $storeStep and removed values " +
                                  "greater than $maxToKeep"
            if (expectedGreaterKey == null) {
                assertThat(tree.getNextKey(it)).describedAs(nextDescription).isNull()
            } else {
                assertThat(tree.getNextKey(it)).describedAs(nextDescription).isEqualTo(expectedGreaterKey)
            }

            val expectedLowerKey = keys.lower(it)
            val previousDescription = "getPreviousKey($it) mismatch in tree ${tree.keys}, built from values " +
                                  "from 1 to $maxToStore with step $storeStep and removed values " +
                                  "greater than $maxToKeep"
            if (expectedLowerKey == null) {
                assertThat(tree.getPreviousKey(it)).describedAs(previousDescription).isNull()
            } else {
                assertThat(tree.getPreviousKey(it)).describedAs(previousDescription).isEqualTo(expectedLowerKey)
            }
        }
    }

    @Test
    fun `endless rebalance 2019-06-09`() {
        (-700..700).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-716)
        tree.removeGreaterThen(712)
        (-716..-701).forEach { tree.put(it, it.toString()) }
        (701..712).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-740)
        tree.removeGreaterThen(730)
        (-740..-717).forEach { tree.put(it, it.toString()) }
        (713..730).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-764)
        tree.removeGreaterThen(748)
        (731..748).forEach { tree.put(it, it.toString()) }
        (-764..-741).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-780)
        tree.removeGreaterThen(760)
        (749..760).forEach { tree.put(it, it.toString()) }
        (-780..-765).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-792)
        tree.removeGreaterThen(769)
        (-792..-781).forEach { tree.put(it, it.toString()) }
        (761..769).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-800)
        tree.removeGreaterThen(775)
        (-800..-793).forEach { tree.put(it, it.toString()) }
        (770..775).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-808)
        tree.removeGreaterThen(781)
        (776..781).forEach { tree.put(it, it.toString()) }
        (-808..-801).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-816)
        tree.removeGreaterThen(787)
        (782..787).forEach { tree.put(it, it.toString()) }
        (-816..-809).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-820)
        tree.removeGreaterThen(790)
        (788..790).forEach { tree.put(it, it.toString()) }
        (-820..-817).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-832)
        tree.removeGreaterThen(799)
        (791..799).forEach { tree.put(it, it.toString()) }
        (-832..-821).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-836)
        tree.removeGreaterThen(802)
        (-836..-833).forEach { tree.put(it, it.toString()) }
        (800..802).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-840)
        tree.removeGreaterThen(805)
        (-840..-837).forEach { tree.put(it, it.toString()) }
        (803..805).forEach { tree.put(it, it.toString()) }
        tree.removeLowerThen(-828)
        tree.removeGreaterThen(796)
        tree.removeLowerThen(-820)
        tree.removeGreaterThen(790)
        tree.removeLowerThen(-812)
        tree.removeGreaterThen(784)
        tree.removeLowerThen(-800)
        tree.removeGreaterThen(775)
        tree.removeLowerThen(-788)
    }

    @Test
    fun `when remove is called on root then processing is fine`() {
        (1..3).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(2)).isEqualTo("2")
        assertThat(tree.keys).containsOnly(1, 3)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        (2..3).forEach {
            checkPrevious(it, 1)
        }
        (3..5).forEach {
            checkNext(it, null)
        }
        (1..2).forEach {
            checkNext(it, 3)
        }
    }

    @Test
    fun `when remove is called on left child which doesn't have children then processing is fine`() {
        (1..3).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(1)).isEqualTo("1")
        assertThat(tree.keys).containsOnly(2, 3)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..2).forEach {
            checkPrevious(it, null)
        }
        assertThat(tree.getPreviousKey(3)).isEqualTo(2)
        assertThat(tree.getPreviousValue(3)).isEqualTo("2")
        (4..6).forEach {
            checkPrevious(it, 3)
        }
        (3..5).forEach {
            checkNext(it, null)
        }
        (-1..1).forEach {
            checkNext(it, 2)
        }
        checkNext(2, 3)
    }

    @Test
    fun `when remove is called on left child which has only left child then processing is fine`() {
        (4 downTo 1).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(2)).isEqualTo("2")
        assertThat(tree.keys).containsOnly(1, 3, 4)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        (2..3).forEach {
            checkPrevious(it, 1)
        }
        checkPrevious(3, 1)
        checkPrevious(4, 3)
        (5..6).forEach {
            checkPrevious(it, 4)
        }
        (4..5).forEach {
            checkNext(it, null)
        }
        checkNext(3, 4)
        (1..2).forEach {
            checkNext(it, 3)
        }
        (-1..0).forEach {
            checkNext(it, 1)
        }
    }

    @Test
    fun `when remove is called on left child which has only right child then processing is fine`() {
        listOf(3, 1, 4, 2).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(1)).isEqualTo("1")
        assertThat(tree.keys).containsOnly(2, 3, 4)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..2).forEach {
            checkPrevious(it, null)
        }
        checkPrevious(3, 2)
        checkPrevious(4, 3)
        (5..7).forEach {
            checkPrevious(it, 4)
        }
        (4..5).forEach {
            checkNext(it, null)
        }
        checkNext(3, 4)
        checkNext(2, 3)
        (-1..1).forEach {
            checkNext(it, 2)
        }
    }

    @Test
    fun `when remove is called on left child which has either left or right child then processing is fine`() {
        listOf(4, 2, 5, 1, 3).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(2)).isEqualTo("2")
        assertThat(tree.keys).containsOnly(1, 3, 4, 5)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        (2..3).forEach {
            checkPrevious(it, 1)
        }
        checkPrevious(4, 3)
        checkPrevious(5, 4)
        (6..7).forEach {
            checkPrevious(it, 5)
        }
        (5..7).forEach {
            checkNext(it, null)
        }
        checkNext(4, 5)
        checkNext(3, 4)
        (1..2).forEach {
            checkNext(it, 3)
        }
        (-2..0).forEach {
            checkNext(it, 1)
        }
    }

    @Test
    fun `when remove is called on right child which doesn't have children then processing is fine`() {
        (1..3).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(3)).isEqualTo("3")
        assertThat(tree.keys).containsOnly(1, 2)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        checkPrevious(2, 1)
        (3..6).forEach {
            checkPrevious(it, 2)
        }
        (2..5).forEach {
            checkNext(it, null)
        }
        (-2..0).forEach {
            checkNext(it, 1)
        }
        checkNext(1, 2)
    }

    @Test
    fun `when remove is called on right child which has only left child then processing is fine`() {
        listOf(2, 1, 4, 3).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(4)).isEqualTo("4")
        assertThat(tree.keys).containsOnly(1, 2, 3)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        checkPrevious(2, 1)
        checkPrevious(3, 2)
        (4..7).forEach {
            checkPrevious(it, 3)
        }
        (3..7).forEach {
            checkNext(it, null)
        }
        checkNext(2, 3)
        checkNext(1, 2)
        (-2..0).forEach {
            checkNext(it, 1)
        }
    }

    @Test
    fun `when remove is called on right child which has only right child then processing is fine`() {
        listOf(2, 1, 3, 4).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(3)).isEqualTo("3")
        assertThat(tree.keys).containsOnly(1, 2, 4)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        checkPrevious(2, 1)
        (3..4).forEach {
            checkPrevious(it, 2)
        }
        (5..7).forEach {
            checkPrevious(it, 4)
        }
        (4..7).forEach {
            checkNext(it, null)
        }
        (2..3).forEach {
            checkNext(it, 4)
        }
        checkNext(1, 2)
        (-2..0).forEach {
            checkNext(it, 1)
        }
    }

    @Test
    fun `when remove is called on right child which has either left or right child then processing is fine`() {
        listOf(2, 1, 4, 3, 5).forEach { tree.put(it, it.toString()) }
        assertThat(tree.remove(4)).isEqualTo("4")
        assertThat(tree.keys).containsOnly(1, 2, 3, 5)
        (tree.keys).forEach { assertThat(tree.get(it)).isEqualTo(it.toString()) }
        (-1..1).forEach {
            checkPrevious(it, null)
        }
        checkPrevious(2, 1)
        checkPrevious(3, 2)
        (4..5).forEach {
            checkPrevious(it, 3)
        }
        (6..7).forEach {
            checkPrevious(it, 5)
        }
        (5..7).forEach {
            checkNext(it, null)
        }
        (3..4).forEach {
            checkNext(it, 5)
        }
        checkNext(2, 3)
        checkNext(1, 2)
        (-2..0).forEach {
            checkNext(it, 1)
        }
    }

    @Test
    fun `endless rebalance 2019-06-22`() {
        testSteps("endless-rebalance-2019-06-22.txt")
    }

    private fun testSteps(fileName: String) {
        val input = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName)
        input.reader().forEachLine { line ->
            INSTRUCTION_PUT.matchEntire(line)?.apply {
                val value = groupValues[1]
                tree.put(value.toInt(), value)
            } ?: INSTRUCTION_REMOVE_GREATER.matchEntire(line)?.apply {
                tree.removeGreaterThen(groupValues[1].toInt())
            } ?: INSTRUCTION_REMOVE_LOWER.matchEntire(line)?.apply {
                tree.removeLowerThen(groupValues[1].toInt())
            } ?: throw AssertionError("Unmatched instruction '$line'")
        }
    }

    private fun checkPrevious(key: Int, expected: Int?) {
        if (expected == null) {
            assertThat(tree.getPreviousKey(key)).isNull()
            assertThat(tree.getPreviousValue(key)).isNull()
        } else {
            assertThat(tree.getPreviousKey(key)).isEqualTo(expected)
            assertThat(tree.getPreviousValue(key)).isEqualTo(expected.toString())
        }
    }

    private fun checkNext(key: Int, expected: Int?) {
        if (expected == null) {
            assertThat(tree.getNextKey(key)).isNull()
            assertThat(tree.getNextValue(key)).isNull()
        } else {
            assertThat(tree.getNextKey(key)).isEqualTo(expected)
            assertThat(tree.getNextValue(key)).isEqualTo(expected.toString())
        }
    }

    companion object {
        private val INSTRUCTION_PUT = """put\(([^,]+).*\)""".toRegex()
        private val INSTRUCTION_REMOVE_LOWER = """removeLowerThen\((.+)\)""".toRegex()
        private val INSTRUCTION_REMOVE_GREATER = """removeGreaterThen\((.+)\)""".toRegex()
    }
}