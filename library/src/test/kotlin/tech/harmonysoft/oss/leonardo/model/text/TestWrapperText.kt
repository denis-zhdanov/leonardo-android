package tech.harmonysoft.oss.leonardo.model.text

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class TestWrapperText {

    private lateinit var mText: TextWrapper

    @BeforeEach
    fun setUp() {
        mText = TextWrapper()
    }

    @Test
    fun `when zero number is put then it's correctly processed`() {
        mText.append(0)
        assertThat(mText.toString()).isEqualTo("0")
    }

    @Test
    fun `when minus one number is put then it's correctly processed`() {
        mText.append(-1)
        assertThat(mText.toString()).isEqualTo("-1")
    }

    @Test
    fun `when one number is put then it's correctly processed`() {
        mText.append(1)
        assertThat(mText.toString()).isEqualTo("1")
    }

    @Test
    fun `when nine number is put then it's correctly processed`() {
        mText.append(9)
        assertThat(mText.toString()).isEqualTo("9")
    }

    @Test
    fun `when minus nine number is put then it's correctly processed`() {
        mText.append(-9)
        assertThat(mText.toString()).isEqualTo("-9")
    }

    @Test
    fun `when ninety nine number is put then it's correctly processed`() {
        mText.append(99)
        assertThat(mText.toString()).isEqualTo("99")
    }

    @Test
    fun `when minus ninety nine number is put then it's correctly processed`() {
        mText.append(-99)
        assertThat(mText.toString()).isEqualTo("-99")
    }

    @Test
    fun `when one hundred number is put then it's correctly processed`() {
        mText.append(100)
        assertThat(mText.toString()).isEqualTo("100")
    }

    @Test
    fun `when minus one hundred number is put then it's correctly processed`() {
        mText.append(-100)
        assertThat(mText.toString()).isEqualTo("-100")
    }
}