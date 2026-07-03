package com.makstuff.minimalistcaloriecounter.classes

object QuickImportSanitizer {
    fun databaseName(input: String): String {
        val cleaned = input.replace(',', ' ').replace(Regex("\\s+"), " ").trim()
        if (cleaned.isEmpty()) return "Add Meal Food"
        val capitalized = cleaned.replaceFirstChar { it.uppercase() }
        return if (capitalized.first() in 'A'..'Z') capitalized else "Food $capitalized"
    }
}
