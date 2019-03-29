package tech.harmonysoft.oss.leonardo.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
class RangeTest {

    @Test
    fun `when range has negative values and exact step match then first step value is correctly calculated`() {
        val range = Range(-10, 10)
        assertThat(range.findFirstStepValue(5)).isEqualTo(-10)
    }

    @Test
    fun `when range has negative values and not exact step match then first step value is correct`() {
        val range = Range(-10, 10)
        assertThat(range.findFirstStepValue(3)).isEqualTo(-9)
    }

    @Test
    fun `when range has negative values and step is too big then there is no match`() {
        assertThat(Range(-2, 2).findFirstStepValue(4)).isEqualTo(Long.MIN_VALUE)
    }

    @Test
    fun `when start is positive and end is positive then pad works correctly`() {
        val pad: Long = 5
        for (start in 0 until pad) {
            for (end in pad + 1 until pad * 2) {
                assertThat(Range(start, end).padBy(pad)).isEqualTo(Range(0, pad * 2))
            }
        }
    }

    @Test
    fun `when start is negative and end is positive then pad works correctly`() {
        val pad: Long = 5
        for (start in -pad + 1..-1) {
            for (end in 1 until pad) {
                assertThat(Range(start, end).padBy(pad)).isEqualTo(Range(-pad, pad))
            }
        }
    }

    @Test
    fun `when start is negative and end is negative then pad works correctly`() {
        val pad: Long = 5
        for (start in -pad * 2 + 1 until -pad) {
            for (end in -pad * 2 + 1 until -pad) {
                val range = Range(start, end)
                assertThat(range.padBy(pad)).describedAs(range.toString()).isEqualTo(Range(-pad * 2, -pad))
            }
        }
    }
}