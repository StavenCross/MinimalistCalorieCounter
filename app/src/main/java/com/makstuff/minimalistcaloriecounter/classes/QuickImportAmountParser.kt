package com.makstuff.minimalistcaloriecounter.classes

object QuickImportAmountParser {
    private const val OUNCES_TO_GRAMS = 28.3495
    private const val FLUID_OUNCES_TO_GRAMS = 29.5735
    private const val UNIT_PATTERN = "fl\\s*oz|floz|g|oz"

    fun parseLeading(title: String): ParsedAmount? {
        val match = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*($UNIT_PATTERN)\\s+(.+)$", RegexOption.IGNORE_CASE).find(title)
            ?: return null
        return ParsedAmount(
            amountText = "${match.groupValues[1]} ${match.groupValues[2].normalizeUnit()}",
            name = match.groupValues[3].trim(),
            grams = match.groupValues[1].toGrams(match.groupValues[2]),
        )
    }

    fun parseTrailing(title: String): ParsedAmount? {
        val match = Regex("^\\s*(.+?)\\s*,?\\s+(\\d+(?:\\.\\d+)?)\\s*($UNIT_PATTERN)\\s*$", RegexOption.IGNORE_CASE).find(title)
            ?: return null
        return ParsedAmount(
            amountText = "${match.groupValues[2]} ${match.groupValues[3].normalizeUnit()}",
            name = match.groupValues[1].trim().trimEnd(','),
            grams = match.groupValues[2].toGrams(match.groupValues[3]),
        )
    }

    fun gramsFromAmountText(amountText: String): Double? {
        val match = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*($UNIT_PATTERN)\\s*$", RegexOption.IGNORE_CASE).find(amountText)
            ?: return null
        return match.groupValues[1].toGrams(match.groupValues[2])
    }

    private fun String.toGrams(unit: String): Double {
        val amount = toDouble()
        return when (unit.normalizeUnit()) {
            "oz" -> amount * OUNCES_TO_GRAMS
            "fl oz" -> amount * FLUID_OUNCES_TO_GRAMS
            else -> amount
        }
    }

    private fun String.normalizeUnit(): String {
        return lowercase().replace("\\s+".toRegex(), " ").let {
            when (it) {
                "floz", "fl oz" -> "fl oz"
                else -> it
            }
        }
    }
}

data class ParsedAmount(
    val amountText: String,
    val name: String,
    val grams: Double,
)
