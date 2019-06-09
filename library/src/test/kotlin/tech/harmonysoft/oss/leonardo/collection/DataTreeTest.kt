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
            (1..(i - 1)).forEach { j ->
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
            (1..(i - 1)).forEach { j ->
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
}