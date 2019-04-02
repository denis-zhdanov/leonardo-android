package tech.harmonysoft.oss.leonardo.model.config.axis.impl

import tech.harmonysoft.oss.leonardo.model.text.TextWrapper
import tech.harmonysoft.oss.leonardo.model.text.ValueRepresentationStrategy

class DefaultValueRepresentationStrategy : ValueRepresentationStrategy {

    private val text = TextWrapper()

    override fun getLabel(value: Long, step: Long): TextWrapper {
        text.reset()
        var divisionsNumber = 0
        var tmp = value
        while (tmp >= 1000 && tmp % 1000 == 0L) {
            tmp /= 1000
            divisionsNumber++
        }
        if (divisionsNumber == 0) {
            if (value in 1001..999999 && value % 100 == 0L) {
                text.append(value / 1000)
                text.append('.')
                text.append(value / 100 % 10)
                text.append('K')
                return text
            }
            text.append(value)
            return text
        } else if (divisionsNumber == 1) {
            if (value > 1000000 && tmp % 100 == 0L) {
                text.append(tmp / 1000)
                text.append('.')
                text.append(tmp / 100 % 10)
                text.append('M')
                return text
            }
            text.append(tmp)
            text.append("K")
            return text
        } else {
            text.append(tmp)
            text.append("M")
            return text
        }
    }

    override fun getMinifiedLabel(value: Long, step: Long): TextWrapper {
        text.reset()
        if (value < 1000) {
            text.append(value)
            return text
        }

        if (value < 1000000) {
            if (value % 1000 != 0L) {
                text.append('~')
            }
            text.append(Math.round(value / 1000.0))
            text.append('K')
            return text
        }

        if (value % 1000000 != 0L) {
            text.append('~')
        }
        text.append(Math.round(value / 1000000.0))
        text.append('M')
        return text
    }

    companion object {
        val INSTANCE = DefaultValueRepresentationStrategy()
    }
}