package com.makstuff.minimalistcaloriecounter.classes

import java.time.LocalDate
import kotlin.math.abs

private const val CALORIE_RECOMMENDATION_THRESHOLD = 75.0
private const val MACRO_RECOMMENDATION_THRESHOLD = 5.0
private const val FIBER_RECOMMENDATION_THRESHOLD = 2.0

sealed class GoalStatusState {
    data class ProfileIncomplete(val missingFields: List<String>) : GoalStatusState()
    data class NewRecommendation(
        val recommendation: GoalRecommendation,
        val differences: List<GoalTargetDifference>,
    ) : GoalStatusState()
    data object CurrentGoal : GoalStatusState()
}

data class GoalTargetDifference(
    val macro: GoalMacro,
    val current: Double?,
    val recommended: Double?,
    val delta: Double?,
)

/**
 * Keeps Goals calm by surfacing only actionable state changes. Profile gaps win
 * first, then materially different recommendations, otherwise the active plan is
 * treated as current and the UI can offer a passive detail view.
 */
fun Goals.statusState(date: LocalDate = LocalDate.now()): GoalStatusState {
    val missingFields = profile.missingRequiredFields(date)
    if (missingFields.isNotEmpty()) return GoalStatusState.ProfileIncomplete(missingFields)
    val activeTargets = activeTargetsFor(date)
    val recommendation = recommendation ?: return GoalStatusState.CurrentGoal
    val differences = activeTargets.meaningfulDifferences(recommendation.targets)
    return if (differences.isEmpty()) {
        GoalStatusState.CurrentGoal
    } else {
        GoalStatusState.NewRecommendation(recommendation, differences)
    }
}

/**
 * Removes recommendation noise before it reaches persistence or display. Locked
 * macros are ignored because the user already said those values should win.
 */
fun GoalRecommendation.onlyIfMeaningfulComparedTo(currentTargets: MacroTargets): GoalRecommendation? {
    return takeIf { currentTargets.meaningfulDifferences(targets).isNotEmpty() }
}

fun MacroTargets.meaningfulDifferences(recommendedTargets: MacroTargets): List<GoalTargetDifference> {
    return GoalMacro.entries.mapNotNull { macro ->
        if (macro in lockedMacros) return@mapNotNull null
        val current = valueFor(macro)
        val recommended = recommendedTargets.valueFor(macro) ?: return@mapNotNull null
        val delta = current?.let { recommended - it }
        val isMeaningful = current == null || abs(delta ?: 0.0) >= macro.recommendationThreshold()
        if (isMeaningful) GoalTargetDifference(macro, current, recommended, delta) else null
    }
}

private fun GoalMacro.recommendationThreshold(): Double = when (this) {
    GoalMacro.Calories -> CALORIE_RECOMMENDATION_THRESHOLD
    GoalMacro.Fiber -> FIBER_RECOMMENDATION_THRESHOLD
    GoalMacro.Protein,
    GoalMacro.Carbs,
    GoalMacro.Fat -> MACRO_RECOMMENDATION_THRESHOLD
}
