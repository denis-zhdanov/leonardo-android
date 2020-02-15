package tech.harmonysoft.oss.leonardo.model.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.harmonysoft.oss.leonardo.model.Range

internal class RangesListTest {

    private lateinit var rangeList: RangesList

    @BeforeEach
    fun setUp() {
        rangeList = RangesList()
    }

    @Test
    fun `when single range is given then it is kept as is`() {
        rangeList.add(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `when new range is adjacent to existing from right then they are merged`() {
        rangeList.add(Range(0, 1))
        rangeList.add(Range(1, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range is adjacent to existing from left then they are merged`() {
        rangeList.add(Range(1, 2))
        rangeList.add(Range(0, 1))
        assertThat(rangeList.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts before existing and ends before existing then they are merged`() {
        rangeList.add(Range(1, 3))
        rangeList.add(Range(0, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts before existing and ends with existing then they are merged`() {
        rangeList.add(Range(1, 2))
        rangeList.add(Range(0, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts before existing and ends after existing then they are merged`() {
        rangeList.add(Range(1, 2))
        rangeList.add(Range(0, 3))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts with existing and ends before existing then they are merged`() {
        rangeList.add(Range(0, 3))
        rangeList.add(Range(0, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts with existing and ends with existing then they are merged`() {
        rangeList.add(Range(0, 2))
        rangeList.add(Range(0, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 2))
    }

    @Test
    fun `when new range starts with existing and ends after existing then they are merged`() {
        rangeList.add(Range(0, 2))
        rangeList.add(Range(0, 3))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends before existing then they are merged`() {
        rangeList.add(Range(0, 3))
        rangeList.add(Range(1, 2))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends with existing then they are merged`() {
        rangeList.add(Range(0, 3))
        rangeList.add(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range starts after existing and ends after existing then they are merged`() {
        rangeList.add(Range(0, 2))
        rangeList.add(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(0, 3))
    }

    @Test
    fun `when new range intersects with two existing then they are merged`() {
        rangeList.add(Range(0, 2))
        rangeList.add(Range(4, 6))
        rangeList.add(Range(1, 5))
        assertThat(rangeList.ranges).containsOnly(Range(0, 6))
    }

    @Test
    fun `when new range covers two existing then they are merged`() {
        rangeList.add(Range(2, 3))
        rangeList.add(Range(5, 6))
        rangeList.add(Range(1, 7))
        assertThat(rangeList.ranges).containsOnly(Range(1, 7))
    }

    @Test
    fun `given 1 range when it starts before start and ends before start_then contains() returns false`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.contains(Range(3, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then contains() returns false`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.contains(Range(2, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends before end then contains() returns false`() {
        rangeList.add(Range(1, 4))
        assertThat(rangeList.contains(Range(3, 5))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then contains() returns true`() {
        rangeList.add(Range(1, 3))
        assertThat(rangeList.contains(Range(2, 3))).isTrue()
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then contains() returns true`() {
        rangeList.add(Range(1, 4))
        assertThat(rangeList.contains(Range(2, 3))).isTrue()
    }

    @Test
    fun `given 1 range when it starts with start and ends before end  then contains() returns false`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts with start and ends with end then contains() returns true`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.contains(Range(1, 2))).isTrue()
    }

    @Test
    fun `given 1 range when it starts with start and ends after end then contains() returns true`() {
        rangeList.add(Range(1, 3))
        assertThat(rangeList.contains(Range(1, 2))).isTrue()
    }

    @Test
    fun `given 1 range when it starts after start and ends before end  then contains() returns false`() {
        rangeList.add(Range(2, 3))
        assertThat(rangeList.contains(Range(1, 4))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then contains() returns false`() {
        rangeList.add(Range(2, 3))
        assertThat(rangeList.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then contains() returns false`() {
        rangeList.add(Range(2, 4))
        assertThat(rangeList.contains(Range(1, 3))).isFalse()
    }

    @Test
    fun `given 1 range when it starts with end then contains() returns false`() {
        rangeList.add(Range(2, 4))
        assertThat(rangeList.contains(Range(1, 2))).isFalse()
    }

    @Test
    fun `given 1 range when it starts after end then contains() returns false`() {
        rangeList.add(Range(3, 4))
        assertThat(rangeList.contains(Range(1, 2))).isFalse()
    }

    @Test
    fun `given 2 ranges then contains() returns false`() {
        rangeList.add(Range(1, 3))
        rangeList.add(Range(5, 7))
        assertThat(rangeList.contains(Range(2, 6))).isFalse()
    }

    @Test
    fun `given 1 range when it starts before start and ends before start then it is not kept`() {
        rangeList.add(Range(1, 2))
        rangeList.keepOnly(Range(3, 4))
        assertThat(rangeList.ranges).isEmpty()
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then point is kept`() {
        rangeList.add(Range(1, 2))
        rangeList.keepOnly(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 2))
    }

    @Test
    fun `given 1 range when it starts before start and ends before end then it is kept`() {
        rangeList.add(Range(1, 3))
        rangeList.keepOnly(Range(2, 4))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then it is kept`() {
        rangeList.add(Range(1, 3))
        rangeList.keepOnly(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then it is kept`() {
        rangeList.add(Range(1, 4))
        rangeList.keepOnly(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when ItStartsWithStartand ends before end  then it is kept`() {
        rangeList.add(Range(1, 3))
        rangeList.keepOnly(Range(1, 4))
        assertThat(rangeList.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when ItStartsWithStartAndEndsWithEnd then it is kept`() {
        rangeList.add(Range(1, 3))
        rangeList.keepOnly(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends after end then it is cut`() {
        rangeList.add(Range(1, 4))
        rangeList.keepOnly(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `given 1 range when it starts after start and ends before end then it is kept`() {
        rangeList.add(Range(2, 4))
        rangeList.keepOnly(Range(1, 5))
        assertThat(rangeList.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then it is kept`() {
        rangeList.add(Range(2, 4))
        rangeList.keepOnly(Range(1, 4))
        assertThat(rangeList.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then it is cut`() {
        rangeList.add(Range(2, 4))
        rangeList.keepOnly(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts with end and ends after end then it is cut`() {
        rangeList.add(Range(3, 5))
        rangeList.keepOnly(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 2 ranges when they intersect then they are cut`() {
        rangeList.add(Range(1, 3))
        rangeList.add(Range(6, 9))
        rangeList.keepOnly(Range(2, 7))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3), Range(6, 7))
    }

    @Test
    fun `when there is point then it is merged from left`() {
        rangeList.add(Range(1, 2))
        rangeList.keepOnly(Range(2, 3))
        rangeList.add(Range(3, 4))
        assertThat(rangeList.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `when there is point then it is merged from start`() {
        rangeList.add(Range(1, 2))
        rangeList.keepOnly(Range(2, 3))
        rangeList.add(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 3))
    }

    @Test
    fun `when there is point then it is merged in between`() {
        rangeList.add(Range(1, 2))
        rangeList.keepOnly(Range(2, 3))
        rangeList.add(Range(1, 3))
        assertThat(rangeList.ranges).containsOnly(Range(1, 3))
    }

    @Test
    fun `when there is point then it is merged from end`() {
        rangeList.add(Range(3, 4))
        rangeList.keepOnly(Range(4, 5))
        rangeList.add(Range(3, 4))
        assertThat(rangeList.ranges).containsOnly(Range(3, 4))
    }

    @Test
    fun `when there is point then it is merged from right`() {
        rangeList.add(Range(3, 4))
        rangeList.keepOnly(Range(4, 5))
        rangeList.add(Range(2, 3))
        assertThat(rangeList.ranges).containsOnly(Range(2, 4))
    }

    @Test
    fun `given 1 range when it starts before start and ends before start then missing is correct`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.getMissing(Range(3, 4))).containsOnly(Range(3, 4))
    }

    @Test
    fun `given 1 range when it starts before start and ends with start then missing is correct`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.getMissing(Range(2, 3))).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 1 range when it starts before start and ends after start then missing is correct`() {
        rangeList.add(Range(1, 3))
        assertThat(rangeList.getMissing(Range(2, 5))).containsOnly(Range(4, 5))
    }

    @Test
    fun `given 1 range when it starts before start and ends with end then missing is correct`() {
        rangeList.add(Range(1, 3))
        assertThat(rangeList.getMissing(Range(2, 3))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts before start and ends after end then missing is correct`() {
        rangeList.add(Range(1, 4))
        assertThat(rangeList.getMissing(Range(2, 3))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts with start and ends with start then missing is correct`() {
        rangeList.add(Range(1, 1))
        assertThat(rangeList.getMissing(Range(1, 3))).containsOnly(Range(2, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends before end then missing is correct`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.getMissing(Range(1, 3))).containsOnly(Range(3, 3))
    }

    @Test
    fun `given 1 range when it starts with start and ends with end then missing is correct`() {
        rangeList.add(Range(1, 2))
        assertThat(rangeList.getMissing(Range(1, 2))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts with start and ends after end  then missing is correct`() {
        rangeList.add(Range(1, 3))
        assertThat(rangeList.getMissing(Range(1, 2))).isEmpty()
    }

    @Test
    fun `given 1 range when it starts after start and ends after start then missing is correct`() {
        rangeList.add(Range(2, 3))
        assertThat(rangeList.getMissing(Range(1, 5))).containsOnly(Range(1, 1), Range(4, 5))
    }

    @Test
    fun `given 1 range when it starts after start and ends with end then missing is correct`() {
        rangeList.add(Range(2, 3))
        assertThat(rangeList.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts after start and ends after end then missing is correct`() {
        rangeList.add(Range(2, 4))
        assertThat(rangeList.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts with end and ends with end then missing is correct`() {
        rangeList.add(Range(2, 2))
        assertThat(rangeList.getMissing(Range(1, 2))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 1 range when it starts with end and ends after end then missing is correct`() {
        rangeList.add(Range(2, 4))
        assertThat(rangeList.getMissing(Range(1, 3))).containsOnly(Range(1, 1))
    }

    @Test
    fun `given 2 ranges when they intersect with given then missing is correct`() {
        rangeList.add(Range(1, 3))
        rangeList.add(Range(6, 8))
        assertThat(rangeList.getMissing(Range(2, 7))).containsOnly(Range(4, 5))
    }

    @Test
    fun `given no ranges then missing is correct`() {
        assertThat(rangeList.getMissing(Range(2, 8))).containsOnly(Range(2, 8))
    }

    @Test
    fun `given no current range when range with negative start is given then missing is correct`() {
        assertThat(rangeList.getMissing(Range(-401, 543))).containsOnly(Range(-401, 543))
    }

    @Test
    fun `when a right point is not in the target range then it's not kept`() {
        rangeList.add(Range(-260, 265))
        rangeList.add(Range(701, 707))
        rangeList.keepOnly(Range(-267, 272))
        assertThat(rangeList.ranges).containsOnly(Range(-260, 265))
    }
}