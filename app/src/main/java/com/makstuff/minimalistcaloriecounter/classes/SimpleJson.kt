package com.makstuff.minimalistcaloriecounter.classes

sealed class SimpleJson {
    data class Obj(val values: Map<String, SimpleJson>) : SimpleJson() {
        fun obj(name: String): Obj? = values[name] as? Obj
        fun array(name: String): Arr? = values[name] as? Arr
        fun string(name: String): String? = (values[name] as? Str)?.value
        fun number(name: String): Double? = (values[name] as? Num)?.value
        fun bool(name: String): Boolean? = (values[name] as? Bool)?.value
    }
    data class Arr(val values: List<SimpleJson>) : SimpleJson()
    data class Str(val value: String) : SimpleJson()
    data class Num(val value: Double) : SimpleJson()
    data class Bool(val value: Boolean) : SimpleJson()
    data object Null : SimpleJson()
}

object SimpleJsonParser {
    fun parseObject(input: String): SimpleJson.Obj {
        val parser = Parser(input)
        val value = parser.parse()
        parser.requireComplete()
        return value as? SimpleJson.Obj ?: error("JSON payload must be an object.")
    }

    private class Parser(private val input: String) {
        private var index = 0

        /** Parses one value while bounding recursion from untrusted external JSON. */
        fun parse(): SimpleJson {
            return parseValue(depth = 0)
        }

        private fun parseValue(depth: Int): SimpleJson {
            require(depth <= MAX_NESTING_DEPTH) { "JSON payload is nested too deeply." }
            skipWhitespace()
            return when (peek()) {
                '{' -> parseObject(depth)
                '[' -> parseArray(depth)
                '"' -> SimpleJson.Str(parseString())
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected JSON token.")
            }
        }

        fun requireComplete() {
            skipWhitespace()
            if (index != input.length) error("Unexpected content after JSON payload.")
        }

        private fun parseObject(depth: Int): SimpleJson.Obj {
            consume('{')
            val values = linkedMapOf<String, SimpleJson>()
            skipWhitespace()
            if (consumeIf('}')) return SimpleJson.Obj(values)
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                consume(':')
                values[key] = parseValue(depth + 1)
                skipWhitespace()
                if (consumeIf('}')) break
                consume(',')
            }
            return SimpleJson.Obj(values)
        }

        private fun parseArray(depth: Int): SimpleJson.Arr {
            consume('[')
            val values = mutableListOf<SimpleJson>()
            skipWhitespace()
            if (consumeIf(']')) return SimpleJson.Arr(values)
            while (true) {
                values += parseValue(depth + 1)
                skipWhitespace()
                if (consumeIf(']')) break
                consume(',')
            }
            return SimpleJson.Arr(values)
        }

        private fun parseString(): String {
            consume('"')
            val builder = StringBuilder()
            while (index < input.length) {
                val char = input[index++]
                when (char) {
                    '"' -> return builder.toString()
                    '\\' -> builder.append(parseEscape())
                    else -> builder.append(char)
                }
            }
            error("Unterminated JSON string.")
        }

        private fun parseEscape(): Char {
            val escaped = input.getOrNull(index++) ?: error("Unterminated JSON escape.")
            return when (escaped) {
                '"', '\\', '/' -> escaped
                'b' -> '\b'
                'f' -> '\u000C'
                'n' -> '\n'
                'r' -> '\r'
                't' -> '\t'
                'u' -> parseUnicodeEscape()
                else -> error("Unsupported JSON escape.")
            }
        }

        private fun parseUnicodeEscape(): Char {
            val hex = input.substring(index, (index + 4).coerceAtMost(input.length))
            require(hex.length == 4 && hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
                "Invalid JSON unicode escape."
            }
            index += 4
            return hex.toInt(16).toChar()
        }

        private fun parseNumber(): SimpleJson.Num {
            val start = index
            consumeIf('-')
            readDigits()
            if (consumeIf('.')) readDigits()
            if (peek() == 'e' || peek() == 'E') {
                index++
                if (peek() == '+' || peek() == '-') index++
                readDigits()
            }
            val number = input.substring(start, index).toDouble()
            require(number.isFinite()) { "JSON numbers must be finite." }
            return SimpleJson.Num(number)
        }

        private fun parseBoolean(): SimpleJson.Bool {
            if (input.startsWith("true", index)) {
                index += 4
                return SimpleJson.Bool(true)
            }
            if (input.startsWith("false", index)) {
                index += 5
                return SimpleJson.Bool(false)
            }
            error("Invalid JSON boolean.")
        }

        private fun parseNull(): SimpleJson {
            if (!input.startsWith("null", index)) error("Invalid JSON null.")
            index += 4
            return SimpleJson.Null
        }

        private fun readDigits() {
            val start = index
            while (peek() in '0'..'9') index++
            require(index > start) { "Invalid JSON number." }
        }

        private fun skipWhitespace() {
            while (peek()?.isWhitespace() == true) index++
        }

        private fun consume(expected: Char) {
            skipWhitespace()
            require(consumeIf(expected)) { "Expected '$expected'." }
        }

        private fun consumeIf(expected: Char): Boolean {
            if (peek() != expected) return false
            index++
            return true
        }

        private fun peek(): Char? = input.getOrNull(index)

        private companion object {
            const val MAX_NESTING_DEPTH = 32
        }
    }
}
