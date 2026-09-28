package com.example.fitcore.agent

internal sealed class JsonValue {
    data class Obj(val map: Map<String, JsonValue>) : JsonValue()
    data class Arr(val items: List<JsonValue>) : JsonValue()
    data class Str(val text: String) : JsonValue()
    data class IntNum(val number: Int) : JsonValue()
    data class Decimal(val raw: String) : JsonValue()
    data object True : JsonValue()
    data object False : JsonValue()
    data object Null : JsonValue()
}

internal object JsonText {
    fun parse(input: String): JsonValue? {
        return try {
            val reader = JsonReader(input)
            val value = reader.parseValue()
            reader.skipWs()
            if (!reader.eof()) null else value
        } catch (_: JsonParseException) {
            null
        }
    }

    fun encodeString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
    }
}

private class JsonParseException : Exception()

private class JsonReader(private val source: String) {
    private var index = 0

    fun eof(): Boolean = index >= source.length

    fun skipWs() {
        while (index < source.length && source[index].isWhitespace()) index++
    }

    fun parseValue(): JsonValue {
        skipWs()
        if (index >= source.length) throw JsonParseException()
        return when (source[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> literal("true", JsonValue.True)
            'f' -> literal("false", JsonValue.False)
            'n' -> literal("null", JsonValue.Null)
            '-', in '0'..'9' -> parseNumber()
            else -> throw JsonParseException()
        }
    }

    private fun parseObject(): JsonValue.Obj {
        expect('{')
        skipWs()
        val map = linkedMapOf<String, JsonValue>()
        if (peek('}')) {
            index++
            return JsonValue.Obj(map)
        }
        while (true) {
            skipWs()
            if (index >= source.length || source[index] != '"') throw JsonParseException()
            val key = parseString()
            skipWs()
            expect(':')
            val value = parseValue()
            if (map.containsKey(key)) throw JsonParseException()
            map[key] = value
            skipWs()
            when {
                peek(',') -> index++
                peek('}') -> {
                    index++
                    return JsonValue.Obj(map)
                }
                else -> throw JsonParseException()
            }
        }
    }

    private fun parseArray(): JsonValue.Arr {
        expect('[')
        skipWs()
        val items = mutableListOf<JsonValue>()
        if (peek(']')) {
            index++
            return JsonValue.Arr(items)
        }
        while (true) {
            items.add(parseValue())
            skipWs()
            when {
                peek(',') -> index++
                peek(']') -> {
                    index++
                    return JsonValue.Arr(items)
                }
                else -> throw JsonParseException()
            }
        }
    }

    private fun parseString(): String {
        if (index >= source.length || source[index] != '"') throw JsonParseException()
        index++
        val out = StringBuilder()
        while (index < source.length) {
            val ch = source[index++]
            when (ch) {
                '"' -> return out.toString()
                '\\' -> out.append(parseEscape())
                else -> {
                    if (ch.code < 0x20) throw JsonParseException()
                    out.append(ch)
                }
            }
        }
        throw JsonParseException()
    }

    private fun parseEscape(): Char {
        if (index >= source.length) throw JsonParseException()
        return when (val escaped = source[index++]) {
            '"', '\\', '/' -> escaped
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> {
                if (index + 4 > source.length) throw JsonParseException()
                val hex = source.substring(index, index + 4)
                val code = hex.toIntOrNull(16) ?: throw JsonParseException()
                index += 4
                code.toChar()
            }
            else -> throw JsonParseException()
        }
    }

    private fun parseNumber(): JsonValue {
        val start = index
        if (source[index] == '-') {
            index++
            if (index >= source.length || !source[index].isDigit()) throw JsonParseException()
        }
        if (index >= source.length || !source[index].isDigit()) throw JsonParseException()
        if (source[index] == '0') {
            index++
            if (index < source.length && source[index].isDigit()) throw JsonParseException()
        } else {
            while (index < source.length && source[index].isDigit()) index++
        }
        var decimal = false
        if (index < source.length && source[index] == '.') {
            decimal = true
            index++
            if (index >= source.length || !source[index].isDigit()) throw JsonParseException()
            while (index < source.length && source[index].isDigit()) index++
        }
        if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
            decimal = true
            index++
            if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
            if (index >= source.length || !source[index].isDigit()) throw JsonParseException()
            while (index < source.length && source[index].isDigit()) index++
        }
        val raw = source.substring(start, index)
        if (decimal) return JsonValue.Decimal(raw)
        val number = raw.toIntOrNull() ?: throw JsonParseException()
        return JsonValue.IntNum(number)
    }

    private fun literal(word: String, value: JsonValue): JsonValue {
        if (!source.startsWith(word, index)) throw JsonParseException()
        index += word.length
        return value
    }

    private fun expect(char: Char) {
        skipWs()
        if (index >= source.length || source[index] != char) throw JsonParseException()
        index++
    }

    private fun peek(char: Char): Boolean = index < source.length && source[index] == char
}
