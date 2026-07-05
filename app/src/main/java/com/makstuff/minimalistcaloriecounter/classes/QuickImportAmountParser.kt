package com.makstuff.minimalistcaloriecounter.classes

object QuickImportAmountParser {
    private const val OUNCES_TO_GRAMS = 28.3495

    fun parseLeading(title: String): ParsedAmount? {
        val match = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*(g|oz)\\s+(.+)$", RegexOption.IGNORE_CASE).find(title)
            ?: return null
        return ParsedAmount(
            amountText = "${match.groupValues[1]} ${match.groupValues[2]}",
            name = match.groupValues[3].trim(),
            grams = match.groupValues[1].toGrams(match.groupValues[2]),
        )
    }

    fun parseTrailing(title: String): ParsedAmount? {
        val match = Regex("^\\s*(.+?)\\s*,?\\s+(\\d+(?:\\.\\d+)?)\\s*(g|oz)\\s*$", RegexOption.IGNORE_CASE).find(title)
            ?: return null
        return ParsedAmount(
            amountText = "${match.groupValues[2]} ${match.groupValues[3]}",
            name = match.groupValues[1].trim().trimEnd(','),
            grams = match.groupValues[2].toGrams(match.groupValues[3]),
        )
    }

    fun gramsFromAmountText(amountText: String): Double? {
        val match = Regex("^\\s*(\\d+(?:\\.\\d+)?)\\s*(g|oz)\\s*$", RegexOption.IGNORE_CASE).find(amountText)
            ?: return null
        return match.groupValues[1].toGrams(match.groupValues[2])
    }

    private fun String.toGrams(unit: String): Double {
        val amount = toDouble()
        return if (unit.equals("oz", ignoreCase = true)) amount * OUNCES_TO_GRAMS else amount
    }
}

data class ParsedAmount(
    val amountText: String,
    val name: String,
    val grams: Double,
)
