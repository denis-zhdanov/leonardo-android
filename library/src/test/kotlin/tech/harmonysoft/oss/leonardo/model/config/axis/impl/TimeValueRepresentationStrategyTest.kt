package tech.harmonysoft.oss.leonardo.model.config.axis.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.DAY
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.HOUR
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.MILLISECOND
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.MINUTE
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.MONTH
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.SECOND
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy.Companion.YEAR
import java.text.SimpleDateFormat
import java.util.*


internal class TimeValueRepresentationStrategyTest {

    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val strategy = TimeValueRepresentationStrategy(TimeZone.getTimeZone("UTC"))

    private fun parse(date: String): Long {
        return format.parse(date).time
    }

    @Test
    fun `when step milliseconds is used the formatted value is correct`() {
        assertThat(strategy.getLabel(parse("2019-03-21 01:10:10.456"), MILLISECOND).toString())
            .isEqualTo("10.456")

        assertThat(strategy.getLabel(parse("2019-03-21 01:01:01.456"), MILLISECOND).toString())
            .isEqualTo("01.456")

        assertThat(strategy.getLabel(parse("2019-03-21 01:12:13.456"), MILLISECOND).toString())
            .isEqualTo("13.456")

        assertThat(strategy.getLabel(parse("2019-03-21 01:12:13.100"), MILLISECOND).toString())
            .isEqualTo("13.100")

        assertThat(strategy.getLabel(parse("2019-03-21 01:12:13.010"), MILLISECOND).toString())
            .isEqualTo("13.010")

        assertThat(strategy.getLabel(parse("2019-03-21 01:12:13.001"), MILLISECOND).toString())
            .isEqualTo("13.001")
    }

    @Test
    fun `when step seconds is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("2019-03-21 01:02:14.456"), SECOND).toString())
            .isEqualTo("02:14")

        assertThat(strategy.getLabel(parse("2019-03-21 01:20:14.456"), SECOND).toString())
            .isEqualTo("20:14")

        assertThat(strategy.getLabel(parse("2019-03-21 01:20:04.456"), SECOND).toString())
            .isEqualTo("20:04")

        assertThat(strategy.getLabel(parse("2019-03-21 01:20:50.456"), SECOND).toString())
            .isEqualTo("20:50")

        assertThat(strategy.getLabel(parse("2019-03-21 01:12:14.456"), SECOND).toString())
            .isEqualTo("12:14")
    }

    @Test
    fun `when step minute is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("2019-03-21 20:12:14.456"), MINUTE).toString())
            .isEqualTo("20:12")

        assertThat(strategy.getLabel(parse("2019-03-21 05:12:14.456"), MINUTE).toString())
            .isEqualTo("05:12")

        assertThat(strategy.getLabel(parse("2019-03-21 15:10:14.456"), MINUTE).toString())
            .isEqualTo("15:10")

        assertThat(strategy.getLabel(parse("2019-03-21 15:01:14.456"), MINUTE).toString())
            .isEqualTo("15:01")

        assertThat(strategy.getLabel(parse("2019-03-21 15:12:14.456"), MINUTE).toString())
            .isEqualTo("15:12")
    }

    @Test
    fun `when step hour is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("2019-03-21 10:16:00.00"), HOUR).toString())
            .isEqualTo("10:16")

        assertThat(strategy.getLabel(parse("2019-03-21 03:16:00.00"), HOUR).toString())
            .isEqualTo("03:16")

        assertThat(strategy.getLabel(parse("2019-03-21 03:06:00.00"), HOUR).toString())
            .isEqualTo("03:06")

        assertThat(strategy.getLabel(parse("2019-03-21 03:10:00.00"), HOUR).toString())
            .isEqualTo("03:10")

        assertThat(strategy.getLabel(parse("2019-03-21 15:16:00.00"), HOUR).toString())
            .isEqualTo("15:16")
    }

    @Test
    fun `when step day is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("1979-01-16 15:00:00.000"), DAY).toString())
            .isEqualTo("Jan 16")

        assertThat(strategy.getLabel(parse("1980-01-01 00:00:00.000"), DAY).toString())
            .isEqualTo("Jan 1")

        assertThat(strategy.getLabel(parse("1980-12-31 23:59:59.999"), DAY).toString())
            .isEqualTo("Dec 31")

        assertThat(strategy.getLabel(parse("1980-02-29 00:00:00.000"), DAY).toString())
            .isEqualTo("Feb 29")

        assertThat(strategy.getLabel(parse("1980-02-29 23:59:59.999"), DAY).toString())
            .isEqualTo("Feb 29")

        assertThat(strategy.getLabel(parse("1980-03-01 00:00:00.000"), DAY).toString())
            .isEqualTo("Mar 1")

        assertThat(strategy.getLabel(parse("1981-01-01 00:00:00.000"), DAY).toString())
            .isEqualTo("Jan 1")

        assertThat(strategy.getLabel(parse("1981-12-31 23:59:59.999"), DAY).toString())
            .isEqualTo("Dec 31")

        assertThat(strategy.getLabel(parse("1981-01-01 01:00:00.000"), DAY).toString())
            .isEqualTo("Jan 1")

        assertThat(strategy.getLabel(parse("1981-01-16 15:00:00.000"), DAY).toString())
            .isEqualTo("Jan 16")

        assertThat(strategy.getLabel(parse("1989-01-16 15:00:00.000"), DAY).toString())
            .isEqualTo("Jan 16")

        assertThat(strategy.getLabel(parse("2019-01-16 15:00:00.000"), DAY).toString())
            .isEqualTo("Jan 16")

        assertThat(strategy.getLabel(parse("2019-03-04 15:00:00.000"), DAY).toString())
            .isEqualTo("Mar 4")

        assertThat(strategy.getLabel(parse("2019-12-18 15:00:00.000"), DAY).toString())
            .isEqualTo("Dec 18")
    }

    @Test
    fun `when step month is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("1979-01-16 15:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("1980-01-01 00:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("1980-12-31 23:59:59.999"), MONTH).toString())
            .isEqualTo("Dec")

        assertThat(strategy.getLabel(parse("1980-02-29 00:00:00.000"), MONTH).toString())
            .isEqualTo("Feb")

        assertThat(strategy.getLabel(parse("1980-02-29 23:59:59.999"), MONTH).toString())
            .isEqualTo("Feb")

        assertThat(strategy.getLabel(parse("1980-03-01 00:00:00.000"), MONTH).toString())
            .isEqualTo("Mar")

        assertThat(strategy.getLabel(parse("1981-01-01 00:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("1981-12-31 23:59:59.999"), MONTH).toString())
            .isEqualTo("Dec")

        assertThat(strategy.getLabel(parse("1981-01-01 01:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("1981-01-16 15:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("1989-01-16 15:00:00.000"), MONTH).toString())
            .isEqualTo("Jan")

        assertThat(strategy.getLabel(parse("2019-03-04 15:00:00.00"), MONTH).toString())
            .isEqualTo("Mar")
    }

    @Test
    fun `when step year is used then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("1979-01-16 15:00:00.000"), YEAR).toString())
            .isEqualTo("1979")

        assertThat(strategy.getLabel(parse("1980-01-01 00:00:00.000"), YEAR).toString())
            .isEqualTo("1980")

        assertThat(strategy.getLabel(parse("1980-12-31 23:59:59.999"), YEAR).toString())
            .isEqualTo("1980")

        assertThat(strategy.getLabel(parse("1980-02-29 00:00:00.000"), YEAR).toString())
            .isEqualTo("1980")

        assertThat(strategy.getLabel(parse("1980-02-29 23:59:59.999"), YEAR).toString())
            .isEqualTo("1980")

        assertThat(strategy.getLabel(parse("1980-03-01 00:00:00.000"), YEAR).toString())
            .isEqualTo("1980")

        assertThat(strategy.getLabel(parse("1981-01-01 00:00:00.000"), YEAR).toString())
            .isEqualTo("1981")

        assertThat(strategy.getLabel(parse("1981-12-31 23:59:59.999"), YEAR).toString())
            .isEqualTo("1981")

        assertThat(strategy.getLabel(parse("1981-01-01 01:00:00.000"), YEAR).toString())
            .isEqualTo("1981")

        assertThat(strategy.getLabel(parse("1981-01-16 15:00:00.000"), YEAR).toString())
            .isEqualTo("1981")

        assertThat(strategy.getLabel(parse("1989-01-16 15:00:00.000"), YEAR).toString())
            .isEqualTo("1989")

        assertThat(strategy.getLabel(parse("2019-01-16 15:00:00.000"), YEAR).toString())
            .isEqualTo("2019")

        assertThat(strategy.getLabel(parse("2019-03-04 15:00:00.000"), YEAR).toString())
            .isEqualTo("2019")
        assertThat(strategy.getLabel(parse("2019-03-04 15:00:00.00"), YEAR).toString())
            .isEqualTo("2019")
    }

    @Test
    fun `when step goes beyond year then formatted value is correct`() {
        assertThat(strategy.getLabel(parse("2019-03-04 15:00:00.00"), YEAR * 2).toString())
            .isEqualTo("2019")
    }
}