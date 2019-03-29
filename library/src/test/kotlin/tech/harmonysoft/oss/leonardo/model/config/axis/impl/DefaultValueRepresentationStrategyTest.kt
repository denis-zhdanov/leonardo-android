package tech.harmonysoft.oss.leonardo.model.config.axis.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * @author Denis Zhdanov
 * @since 26/3/19
 */
internal class DefaultValueRepresentationStrategyTest {

    private val mStrategy = DefaultValueRepresentationStrategy()

    @Test
    fun `when value is less than 1000 then it's returned as is`() {
        assertThat(mStrategy.getLabel(999, 100).toString()).isEqualTo("999")
    }

    @Test
    fun `when value is 1000 then it's shortened to 1K`() {
        assertThat(mStrategy.getLabel(1000, 100).toString()).isEqualTo("1K")
    }

    @Test
    fun `when value is more than 1K and divisible by 1K and less than 1M then it's shortened to K`() {
        assertThat(mStrategy.getLabel(900000, 100).toString()).isEqualTo("900K")
    }

    @Test
    fun `when value is more than 1K and not divisible by 1K then it's returned as is`() {
        assertThat(mStrategy.getLabel(900001, 100).toString()).isEqualTo("900001")
    }

    @Test
    fun `when value is 1M than it's shortened to 1M`() {
        assertThat(mStrategy.getLabel(1000000, 100).toString()).isEqualTo("1M")
    }

    @Test
    fun `when value is more than 1M and divisible by 1M then it's shortened to M`() {
        assertThat(mStrategy.getLabel(9000000, 100).toString()).isEqualTo("9M")
    }

    @Test
    fun `when value is more than 1M and not divisible by 1M then it's returned as is`() {
        assertThat(mStrategy.getLabel(9000001, 100).toString()).isEqualTo("9000001")
    }

    @Test
    fun `when value is divisible by 100 then it's shortened correctly`() {
        assertThat(mStrategy.getLabel(6500, 100).toString()).isEqualTo("6.5K")
        assertThat(mStrategy.getLabel(6300, 100).toString()).isEqualTo("6.3K")
        assertThat(mStrategy.getLabel(6500, 500).toString()).isEqualTo("6.5K")
        assertThat(mStrategy.getLabel(6500, 1000).toString()).isEqualTo("6.5K")
        assertThat(mStrategy.getLabel(6500, 5000).toString()).isEqualTo("6.5K")
        assertThat(mStrategy.getLabel(16500, 5000).toString()).isEqualTo("16.5K")
        assertThat(mStrategy.getLabel(923700, 5000).toString()).isEqualTo("923.7K")
        assertThat(mStrategy.getLabel(18900000, 5000).toString()).isEqualTo("18.9M")
    }

    @Test
    fun `when minified value is requested then it's produced correctly`() {
        assertThat(mStrategy.getMinifiedLabel(183, 100).toString()).isEqualTo("183")
        assertThat(mStrategy.getMinifiedLabel(183, 1000).toString()).isEqualTo("183")
        assertThat(mStrategy.getMinifiedLabel(1582, 1000).toString()).isEqualTo("~2K")
        assertThat(mStrategy.getMinifiedLabel(2000, 1000).toString()).isEqualTo("2K")
        assertThat(mStrategy.getMinifiedLabel(2387, 1000).toString()).isEqualTo("~2K")
        assertThat(mStrategy.getMinifiedLabel(23687, 1000).toString()).isEqualTo("~24K")
        assertThat(mStrategy.getMinifiedLabel(15823, 1000).toString()).isEqualTo("~16K")
        assertThat(mStrategy.getMinifiedLabel(19823, 1000).toString()).isEqualTo("~20K")
        assertThat(mStrategy.getMinifiedLabel(7000000, 1000000).toString()).isEqualTo("7M")
        assertThat(mStrategy.getMinifiedLabel(7123456, 1000000).toString()).isEqualTo("~7M")
        assertThat(mStrategy.getMinifiedLabel(7500000, 1000000).toString()).isEqualTo("~8M")
        assertThat(mStrategy.getMinifiedLabel(7987654, 1000000).toString()).isEqualTo("~8M")
        assertThat(mStrategy.getMinifiedLabel(312345678L, 1000000).toString()).isEqualTo("~312M")
    }
}