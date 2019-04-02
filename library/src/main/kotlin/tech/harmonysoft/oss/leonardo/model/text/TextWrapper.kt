package tech.harmonysoft.oss.leonardo.model.text

/**
 * Is introduced just for performance optimization - it's necessary to measure visual dimensions
 * a lot during graph drawing and rescaling, hence, we want to avoid creating unnecessary objects.
 */
class TextWrapper {

    private var _data = CharArray(64)
    val data get() = _data

    private var _length = 0
    val length get() = _length

    fun append(c: Char) {
        ensureCapacity(1)
        _data[_length++] = c
    }

    fun append(data: CharArray) {
        ensureCapacity(data.size)
        System.arraycopy(data, 0, _data, _length, data.size)
        _length += data.size
    }

    fun append(s: String) {
        append(s, 0, s.length)
    }

    fun append(s: String, start: Int, length: Int) {
        ensureCapacity(length)
        var i = start
        val max = start + length
        while (i < max) {
            _data[_length++] = s[i]
            i++
        }
    }

    fun append(i: Int) {
        append(i.toLong())
    }

    fun append(l: Long) {
        var value = l
        if (l < 0) {
            append('-')
            value = -value
        }
        var divider: Long = 10
        while (divider in 1..value) {
            divider *= 10
        }
        divider /= 10

        while (divider > 0) {
            append(DIGITS[(value / divider).toInt()])
            value %= divider
            divider /= 10
        }
    }

    fun reset() {
        _length = 0
    }

    private fun ensureCapacity(toAdd: Int) {
        if (_data.size - _length < toAdd) {
            val newData = CharArray(_data.size * 3 / 2)
            System.arraycopy(_data, 0, newData, 0, _length)
            _data = newData
        }
    }

    override fun toString(): String {
        return String(_data, 0, _length)
    }

    companion object {

        private val DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    }
}