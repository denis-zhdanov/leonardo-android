package tech.harmonysoft.oss.leonardo.model.util

import tech.harmonysoft.oss.leonardo.model.Range
import kotlin.math.max

class RangesList {

    private val _ranges = mutableListOf<Range>()
    var ranges: List<Range> = _ranges

    fun add(range: Range) {
        var i = _ranges.binarySearch(range, COMPARATOR)
        if (i >= 0) {
            val r = _ranges[i]
            if (r.end >= range.end) {
                return
            }
            _ranges[i] = range
            mayBeMerge(i)
            return
        }

        i = -(i + 1)
        _ranges.add(i, range)
        if (i > 0) {
            mayBeMerge(i - 1)
        } else {
            mayBeMerge(i)
        }
    }

    private fun mayBeMerge(startIndex: Int) {
        var previous = _ranges[startIndex]
        val i = startIndex + 1
        while (i < _ranges.size) {
            val next = _ranges[i]
            if (previous.end < next.start - 1) {
                // Disjoint
                return
            }
            previous = Range(previous.start, max(previous.end, next.end))
            _ranges[i - 1] = previous
            _ranges.removeAt(i)
        }
    }

    operator fun contains(range: Range): Boolean {
        var i = _ranges.binarySearch(range, COMPARATOR)
        if (i >= 0) {
            return _ranges[i].end >= range.end
        }
        i = -(i + 1)
        return i > 0 && _ranges[i - 1].end >= range.end
    }

    fun keepOnly(range: Range) {
        val toKeep = mutableListOf<Range>()
        var i = _ranges.binarySearch(range, COMPARATOR)
        if (i < 0) {
            i = -(i + 1)
            if (i > 0) {
                val previous = _ranges[i - 1]
                if (previous.end >= range.start) {
                    toKeep.add(Range(range.start, Math.min(previous.end, range.end)))
                }
            }
        }
        while (i < _ranges.size) {
            val r = _ranges[i]
            if (r.end < range.end) {
                toKeep += r
            } else {
                if (r.start <= range.end) {
                    toKeep += Range(r.start, range.end)
                }
                break
            }
            i++
        }
        _ranges.clear()
        _ranges.addAll(toKeep)
    }

    fun getMissing(target: Range): Collection<Range> {
        val result = mutableListOf<Range>()
        var i = _ranges.binarySearch(target, COMPARATOR)
        if (i < 0) {
            i = -(i + 1)
            if (i > 0) {
                i--
            }
        }

        var targetStart = target.start
        while (i < _ranges.size) {
            val range = _ranges[i]
            if (range.start > target.end) {
                break
            }

            if (range.start > targetStart) {
                result.add(Range(targetStart, range.start - 1))
            }
            targetStart = range.end + 1
            i++
        }

        if (targetStart <= target.end) {
            result.add(Range(targetStart, target.end))
        }
        return result
    }

    override fun toString(): String {
        return ranges.toString()
    }

    companion object {
        private val COMPARATOR = Comparator<Range> { r1, r2 -> r1.start.compareTo(r2.start) }
    }
}