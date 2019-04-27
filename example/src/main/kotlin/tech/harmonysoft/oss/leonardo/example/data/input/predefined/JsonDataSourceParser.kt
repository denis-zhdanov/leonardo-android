package tech.harmonysoft.oss.leonardo.example.data.input.predefined

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import tech.harmonysoft.oss.leonardo.model.DataPoint
import tech.harmonysoft.oss.leonardo.model.Range
import tech.harmonysoft.oss.leonardo.model.config.axis.impl.TimeValueRepresentationStrategy
import tech.harmonysoft.oss.leonardo.model.data.ChartDataSource
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

class JsonDataSourceParser {

    private val JSONArray.names: Iterable<String>
        get() {
            return object : Iterable<String> {
                override fun iterator(): Iterator<String> {
                    return (0 until length()).asSequence().map { getString(it) }.iterator()
                }
            }
        }

    private val JSONObject.names: Iterable<String>
        get() {
            return object : Iterable<String> {
                override fun iterator(): Iterator<String> {
                    return names().names.iterator()
                }
            }
        }

    private val JSONArray.objects: Iterable<JSONObject>
        get() {
            return object : Iterable<JSONObject> {
                override fun iterator(): Iterator<JSONObject> {
                    return (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                }
            }
        }

    private val JSONArray.arrays: Iterable<JSONArray>
        get() {
            return object : Iterable<JSONArray> {
                override fun iterator(): Iterator<JSONArray> {
                    return (0 until length()).asSequence().map { getJSONArray(it) }.iterator()
                }
            }
        }

    fun parse(input: InputStream): Collection<ChartData> {
        return parse(String(input.readBytes(), StandardCharsets.UTF_8))
    }

    private fun parse(s: String): Collection<ChartData> {
        val legends = Stack<String>().apply {
            addAll(LEGENDS)
        }
        var counter = 0
        return JSONArray(s).objects.map {
            val name = if (legends.isEmpty()) {
                "SomeStats" + ++counter
            } else {
                legends.pop()
            }
            val (xRange, dataSources) = parse(it)
            ChartData(name, xRange, dataSources, TimeValueRepresentationStrategy.INSTANCE)
        }
    }

    private fun parse(json: JSONObject): Pair<Range, Collection<ChartDataSource>> {
        val context = ParseContext()
        val columnsDataJson = json.getJSONArray(JsonColumn.DATA)
        columnsDataJson.arrays.forEachIndexed { i, array ->
            val rawColumnData = parseRawColumnData(array, i)
            context.columnData[rawColumnData.columnName] = rawColumnData.values
        }

        fillColumnTypes(json.getJSONObject(JsonColumn.TYPES), context)
        fillColumnNames(json.getJSONObject(JsonColumn.NAMES), context)
        fillColumnColors(json.getJSONObject(JsonColumn.COLORS), context)
        validate(context)
        return build(context)
    }

    private fun parseRawColumnData(columnJson: JSONArray, i: Int): RawColumnData {
        if (columnJson.length() <= 0) {
            throw IllegalArgumentException(
                    "Bad input: every column data array (defined in the '${JsonColumn.DATA}' element) is expected "
                    + "to have its name as the first element and values as subsequent. However, column data #$i "
                    + "(zero-based) is empty"
            )
        }
        val columnName = columnJson.getString(0)
        val values = (1 until columnJson.length()).map { j ->
            try {
                columnJson.getLong(j)
            } catch (e: JSONException) {
                throw RuntimeException("Failed parsing data for column '$columnName'. Expected to get a numeric "
                                       + "value but got '${columnJson.get(j)}' (index $j)",
                                       e)
            }
        }
        return RawColumnData(columnName, values)
    }

    private fun fillColumnTypes(json: JSONObject, context: ParseContext) {
        json.names.forEach { columnId ->
            context.types[columnId.trim { it <= ' ' }] = json.getString(columnId).trim { it <= ' ' }
        }
    }

    private fun fillColumnNames(json: JSONObject, context: ParseContext) {
        json.names.forEach { columnId ->
            context.names[columnId.trim { it <= ' ' }] = json.getString(columnId).trim { it <= ' ' }
        }
    }

    private fun fillColumnColors(json: JSONObject, context: ParseContext) {
        json.names.forEach { columnId ->
            val colorHex = json.getString(columnId)
            var colorInt = Integer.parseInt(colorHex.trim { it <= ' ' }.substring(1), 16)
            colorInt = colorInt or -0x1000000
            context.colors[columnId.trim { it <= ' ' }] = colorInt
        }
    }

    private fun validate(context: ParseContext) {
        validateColumnDataSizeIsConsistent(context)
        validateAllColumnsAreNamed(context)
        validateColumnTypes(context)
        validateColors(context)
    }

    private fun validateColumnDataSizeIsConsistent(context: ParseContext) {
        var previousColumnName: String? = null
        var size = -1
        for ((key, value) in context.columnData) {
            if (previousColumnName == null) {
                previousColumnName = key
                size = value.size
            } else {
                if (size != value.size) {
                    throw IllegalArgumentException(
                            "Expected that all data columns have the same number of values but detected that " +
                            "column '$previousColumnName' has $size values and column '$key' has ${value.size} values"
                    )
                }
            }
        }
    }

    private fun validateAllColumnsAreNamed(context: ParseContext) {
        if (context.columnData.size - 1 != context.names.size) {
            throw IllegalArgumentException(String.format(
                    "Found %d data columns (%s) but there are %d names (%s)",
                    context.columnData.size, context.columnData.keys, context.names.size, context.names
            ))
        }
        for (columnId in context.columnData.keys) {
            if (JsonValue.X == columnId) {
                continue
            }
            val name = context.names[columnId]
            if (name == null || name.isEmpty()) {
                throw IllegalArgumentException("No name is given for column '$columnId'")
            }
        }
    }

    private fun validateColumnTypes(context: ParseContext) {
        if (context.columnData.size != context.types.size) {
            throw IllegalArgumentException(
                    "Found ${context.columnData.size} data columns (${context.columnData.keys}) but there are "
                    + "${context.types.size} types (${context.types})"
            )
        }
        var xColumnId: String? = null
        for (columnId in context.columnData.keys) {
            if (JsonValue.X == context.types[columnId]) {
                if (xColumnId != null) {
                    throw IllegalArgumentException("Found more than one X column - '$xColumnId' and '$columnId'")
                }
                xColumnId = columnId
            }
        }

        if (xColumnId == null) {
            throw IllegalArgumentException("No X column is found. Available mappings: ${context.types}")
        }
    }

    private fun validateColors(context: ParseContext) {
        if (context.columnData.size - 1 != context.colors.size) {
            throw IllegalArgumentException(
                    "Found ${context.columnData.size} data columns (${context.columnData.keys}) but there are "
                    + "${context.colors.size} colors (${context.colors})"
            )
        }
        for (columnId in context.columnData.keys) {
            if (JsonValue.X == columnId) {
                continue
            }
            if (!context.colors.containsKey(columnId)) {
                throw IllegalArgumentException(String.format("No color is provided for column '%s'", columnId))
            }
        }
    }

    private fun build(context: ParseContext): Pair<Range, Collection<ChartDataSource>> {
        val xValues = getXValues(context)
        val dataSources = context.columnData.mapNotNull { (columnId, yValues) ->
            if (JsonValue.X == columnId) {
                return@mapNotNull null
            }

            val dataPoints = yValues.mapIndexed { i, y ->
                DataPoint(xValues[i], y)
            }

            val legend = context.names[columnId] ?: throw IllegalArgumentException(
                    "No legend is defined for data source '$columnId'"
            )

            val color = context.colors[columnId] ?: throw IllegalArgumentException(
                    "No color is defined for column '$columnId'"
            )

            ChartDataSource(legend, color, PreDefinedChartDataLoader(dataPoints))
        }
        val sortedXValues = xValues.sorted()
        return Range(sortedXValues.first(), sortedXValues.last()) to dataSources
    }

    private fun getXValues(context: ParseContext): List<Long> {
        for ((key, value) in context.types) {
            if (JsonValue.X == value) {
                return context.columnData[key] as List<Long>
            }
        }
        throw IllegalArgumentException("No X column is found in '$context'")
    }

    companion object {
        private val LEGENDS = listOf("Followers", "Notifications", "Sheldons Population", "Zergs", "Wasted Hours")
    }

    private data class RawColumnData(val columnName: String, val values: List<Long>)

    private class ParseContext {
        val columnData = mutableMapOf<String, List<Long>>()
        val types = mutableMapOf<String, String>()
        val names = mutableMapOf<String, String>()
        val colors = mutableMapOf<String, Int>()
    }
}