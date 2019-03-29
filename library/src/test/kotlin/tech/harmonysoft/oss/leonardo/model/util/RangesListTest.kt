package tech.harmonysoft.oss.leonardo.model.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.harmonysoft.oss.leonardo.model.Range

/**
 * @author Denis Zhdanov
 * @since 27/3/19
 */
internal class RangesListTest {

    private lateinit var mCollection: RangesList

    @BeforeEach
    fun setUp() {
        mCollection = RangesList()
    }

    @Test
    fun `when single range is given then it is kept as is`() {
        mCollection.add(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `when new range is adjacent to existing from right then they are merged`() {
        mCollection.add(Range(0, 1))
        mCollection.add(Range(1, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range is adjacent to existing from left then they are merged`() {
        mCollection.add(Range(1, 2))
        mCollection.add(Range(0, 1))
        assertThat(mCollection.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts before existing and ends before existing then they are merged`() {
        mCollection.add(Range(1, 3))
        mCollection.add(Range(0, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts before existing and ends with existing then they are merged`() {
        mCollection.add(Range(1, 2))
        mCollection.add(Range(0, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts before existing and ends after existing then they are merged`() {
        mCollection.add(Range(1, 2))
        mCollection.add(Range(0, 3))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts with existing and ends before existing then they are merged`() {
        mCollection.add(Range(0, 3))
        mCollection.add(Range(0, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts with existing and ends with existing then they are merged`() {
        mCollection.add(Range(0, 2))
        mCollection.add(Range(0, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts with existing and ends after existing then they are merged`() {
        mCollection.add(Range(0, 2))
        mCollection.add(Range(0, 3))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends before existing then they are merged`() {
        mCollection.add(Range(0, 3))
        mCollection.add(Range(1, 2))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends with existing then they are merged`() {
        mCollection.add(Range(0, 3))
        mCollection.add(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends after existing then they are merged`() {
        mCollection.add(Range(0, 2))
        mCollection.add(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range intersects with two existing then they are merged`() {
        mCollection.add(Range(0, 2))
        mCollection.add(Range(4, 6))
        mCollection.add(Range(1, 5))
        assertThat(mCollection.ranges).containsOnly(Range(0, 6))
    }

    @Test
    fun `when new range covers two existing then they are merged`() {
        mCollection.add(Range(2, 3))
        mCollection.add(Range(5, 6))
        mCollection.add(Range(1, 7))
        assertThat(mCollection.ranges).containsOnly(Range(1, 7))
    }

    @Test
    fun `given 1 range when it starts before start and ends before start_then contains() returns false`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.contains(Range(3, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then contains() returns false`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.contains(Range(2, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends before end then contains() returns false`() {
        mCollection.add(Range(1, 4))
        assertThat(mCollection.contains(Range(3, 5))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then contains() returns true`() {
        mCollection.add(Range(1, 3))
        assertThat(mCollection.contains(Range(2, 3))).isTrue()
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then contains() returns true`() {
        mCollection.add(Range(1, 4))
        assertThat(mCollection.contains(Range(2, 3))).isTrue()
    }

    @Test
    fun `given 1 range when it starts with start and ends before end  then contains() returns false`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts with start and ends with end then contains() returns true`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.contains(Range(1, 2))).isTrue()
    }

    @Test
    fun `given 1 range when it starts with start and ends after end then contains() returns true`() {
        mCollection.add(Range(1, 3))
        assertThat(mCollection.contains(Range(1, 2))).isTrue()
    }

    @Test
    fun `given 1 range when it starts after start and ends before end  then contains() returns false`() {
        mCollection.add(Range(2, 3))
        assertThat(mCollection.contains(Range(1, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then contains() returns false`() {
        mCollection.add(Range(2, 3))
        assertThat(mCollection.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then contains() returns false`() {
        mCollection.add(Range(2, 4))
        assertThat(mCollection.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts with end then contains() returns false`() {
        mCollection.add(Range(2, 4))
        assertThat(mCollection.contains(Range(1, 2))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after end then contains() returns false`() {
        mCollection.add(Range(3, 4))
        assertThat(mCollection.contains(Range(1, 2))).isFalse()
    }

    @Test
    fun `given 2 ranges then contains() returns false`() {
        mCollection.add(Range(1, 3))
        mCollection.add(Range(5, 7))
        assertThat(mCollection.contains(Range(2, 6))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends before start then it is not kept`() {
        mCollection.add(Range(1, 2))
        mCollection.keepOnly(Range(3, 4))
        assertThat(mCollection.ranges).isEmpty()
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then point is kept`() {
        mCollection.add(Range(1, 2))
        mCollection.keepOnly(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 2))
    }

    @Test
    fun `given 1 range when it starts before start and ends before end then it is kept`() {
        mCollection.add(Range(1, 3))
        mCollection.keepOnly(Range(2, 4))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then it is kept`() {
        mCollection.add(Range(1, 3))
        mCollection.keepOnly(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then it is kept`() {
        mCollection.add(Range(1, 4))
        mCollection.keepOnly(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when ItStartsWithStartand ends before end  then it is kept`() {
        mCollection.add(Range(1, 3))
        mCollection.keepOnly(Range(1, 4))
        assertThat(mCollection.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when ItStartsWithStartAndEndsWithEnd then it is kept`() {
        mCollection.add(Range(1, 3))
        mCollection.keepOnly(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends after end then it is cut`() {
        mCollection.add(Range(1, 4))
        mCollection.keepOnly(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when it starts after start and ends before end then it is kept`() {
        mCollection.add(Range(2, 4))
        mCollection.keepOnly(Range(1, 5))
        assertThat(mCollection.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then it is kept`() {
        mCollection.add(Range(2, 4))
        mCollection.keepOnly(Range(1, 4))
        assertThat(mCollection.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then it is cut`() {
        mCollection.add(Range(2, 4))
        mCollection.keepOnly(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts with end and ends after end then it is cut`() {
        mCollection.add(Range(3, 5))
        mCollection.keepOnly(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 2 ranges when they intersect then they are cut`() {
        mCollection.add(Range(1, 3))
        mCollection.add(Range(6, 9))
        mCollection.keepOnly(Range(2, 7))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3), Range(6, 7))
    }

    @Test
    fun `when there is point then it is merged from left`() {
        mCollection.add(Range(1, 2))
        mCollection.keepOnly(Range(2, 3))
        mCollection.add(Range(3, 4))
        assertThat(mCollection.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `when there is point then it is merged from start`() {
        mCollection.add(Range(1, 2))
        mCollection.keepOnly(Range(2, 3))
        mCollection.add(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `when there is point then it is merged in between`() {
        mCollection.add(Range(1, 2))
        mCollection.keepOnly(Range(2, 3))
        mCollection.add(Range(1, 3))
        assertThat(mCollection.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `when there is point then it is merged from end`() {
        mCollection.add(Range(3, 4))
        mCollection.keepOnly(Range(4, 5))
        mCollection.add(Range(3, 4))
        assertThat(mCollection.ranges).containsOnly(Range(3, 4))
    }

    @Test
    fun `when there is point then it is merged from right`() {
        mCollection.add(Range(3, 4))
        mCollection.keepOnly(Range(4, 5))
        mCollection.add(Range(2, 3))
        assertThat(mCollection.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts before start and ends before start then missing is correct`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.getMissing(Range(3, 4))).containsOnly(Range(3, 4))
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then missing is correct`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.getMissing(Range(2, 3))).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends after start then missing is correct`() {
        mCollection.add(Range(1, 3))
        assertThat(mCollection.getMissing(Range(2, 5))).containsOnly(Range(4, 5))
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then missing is correct`() {
        mCollection.add(Range(1, 3))
        assertThat(mCollection.getMissing(Range(2, 3))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then missing is correct`() {
        mCollection.add(Range(1, 4))
        assertThat(mCollection.getMissing(Range(2, 3))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts with start and ends with start then missing is correct`() {
        mCollection.add(Range(1, 1))
        assertThat(mCollection.getMissing(Range(1, 3))).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends before end then missing is correct`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.getMissing(Range(1, 3))).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends with end then missing is correct`() {
        mCollection.add(Range(1, 2))
        assertThat(mCollection.getMissing(Range(1, 2))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts with start and ends after end  then missing is correct`() {
        mCollection.add(Range(1, 3))
        assertThat(mCollection.getMissing(Range(1, 2))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts after start and ends after start then missing is correct`() {
        mCollection.add(Range(2, 3))
        assertThat(mCollection.getMissing(Range(1, 5))).containsOnly(Range(1, 1), Range(4, 5))
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then missing is correct`() {
        mCollection.add(Range(2, 3))
        assertThat(mCollection.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then missing is correct`() {
        mCollection.add(Range(2, 4))
        assertThat(mCollection.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts with end and ends with end then missing is correct`() {
        mCollection.add(Range(2, 2))
        assertThat(mCollection.getMissing(Range(1, 2))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts with end and ends after end then missing is correct`() {
        mCollection.add(Range(2, 4))
        assertThat(mCollection.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 2 ranges when they intersect with given then missing is correct`() {
        mCollection.add(Range(1, 3))
        mCollection.add(Range(6, 8))
        assertThat(mCollection.getMissing(Range(2, 7))).containsOnly(Range(4, 5))
    }

    @Test
    fun `given no ranges then missing is correct`() {
        assertThat(mCollection.getMissing(Range(2, 8))).containsOnly(Range(2, 8))
    }

    @Test
    fun `given no current range when range with negative start is given then missing is correct`() {
        assertThat(mCollection.getMissing(Range(-401, 543))).containsOnly(Range(-401, 543))
    }
}