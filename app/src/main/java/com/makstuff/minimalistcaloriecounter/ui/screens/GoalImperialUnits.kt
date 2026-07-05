package com.makstuff.minimalistcaloriecounter.ui.screens

import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import kotlin.math.roundToInt

private const val CentimetersPerInch = 2.54
private const val KilogramsPerPound = 0.45359237

internal data class ImperialHeight(val feet: Int, val inches: Int) {
    val totalInches: Int = (feet * 12) + inches
}

internal fun Double?.toImperialHeight(default: ImperialHeight = ImperialHeight(5, 10)): ImperialHeight {
    val totalInches = this?.let { (it / CentimetersPerInch).roundToInt() } ?: default.totalInches
    val clamped = totalInches.coerceIn(36, 107)
    return ImperialHeight(feet = clamped / 12, inches = clamped % 12)
}

internal fun ImperialHeight.toCentimeters(): Double = totalInches * CentimetersPerInch

internal fun Double?.toImperialPounds(default: Int = 180): Int {
    return this?.let { (it / KilogramsPerPound).roundToInt() }?.coerceIn(80, 500) ?: default
}

internal fun Int.toKilograms(): Double = this * KilogramsPerPound

internal fun GoalMeasurement.heightImperialLabel(): String {
    val height = value?.toImperialHeight() ?: return "Required"
    return "${height.feet}' ${height.inches}\""
}

internal fun GoalMeasurement.weightImperialLabel(): String {
    return value?.toImperialPounds()?.let { "$it lb" } ?: "Required"
}
