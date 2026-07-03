package com.makstuff.minimalistcaloriecounter.persistence.room

import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.GoalRecommendation
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.GoalValueSource
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget

data class GoalRoomSeed(
    val profile: GoalProfileEntity,
    val targets: List<GoalTargetEntity>,
    val history: List<GoalHistoryEntity>,
    val recommendation: GoalRecommendationEntity?,
)

object GoalRoomMapper {
    fun toSeed(goals: Goals): GoalRoomSeed = GoalRoomSeed(
        profile = goals.profile.toEntity(),
        targets = GoalMacro.entries.map { macro ->
            GoalTargetEntity(
                macro = macro.name,
                value = goals.currentTargets.valueFor(macro),
                locked = macro in goals.currentTargets.lockedMacros,
            )
        },
        history = goals.history.map { it.toEntity() },
        recommendation = goals.recommendation?.toEntity(),
    )

    fun fromSeed(seed: GoalRoomSeed): Goals = Goals(
        profile = seed.profile.toDomain(),
        currentTargets = seed.targets.toMacroTargets(),
        history = seed.history.map { it.toDomain() },
        recommendation = seed.recommendation?.toDomain(),
    )
}

private fun GoalProfile.toEntity(): GoalProfileEntity = GoalProfileEntity(
    birthday = birthday,
    sex = sex?.name,
    activityLevel = activityLevel.name,
    weightLossTarget = weightLossTarget.name,
    heightCm = heightCm.value,
    heightLocked = heightCm.locked,
    heightSource = heightCm.source.name,
    heightUpdatedAt = heightCm.updatedAt,
    weightKg = weightKg.value,
    weightLocked = weightKg.locked,
    weightSource = weightKg.source.name,
    weightUpdatedAt = weightKg.updatedAt,
    bodyFatPercent = bodyFatPercent.value,
    bodyFatLocked = bodyFatPercent.locked,
    bodyFatSource = bodyFatPercent.source.name,
    bodyFatUpdatedAt = bodyFatPercent.updatedAt,
    leanMassKg = leanMassKg.value,
    leanMassLocked = leanMassKg.locked,
    leanMassSource = leanMassKg.source.name,
    leanMassUpdatedAt = leanMassKg.updatedAt,
)

private fun GoalProfileEntity.toDomain(): GoalProfile = GoalProfile(
    birthday = birthday,
    sex = GoalSex.fromName(sex),
    heightCm = heightCm.measurement(heightLocked, heightSource, heightUpdatedAt),
    weightKg = weightKg.measurement(weightLocked, weightSource, weightUpdatedAt),
    bodyFatPercent = bodyFatPercent.measurement(bodyFatLocked, bodyFatSource, bodyFatUpdatedAt),
    leanMassKg = leanMassKg.measurement(leanMassLocked, leanMassSource, leanMassUpdatedAt),
    activityLevel = ActivityLevel.fromName(activityLevel),
    weightLossTarget = WeeklyWeightLossTarget.fromName(weightLossTarget),
)

private fun Double?.measurement(locked: Boolean, source: String, updatedAt: java.time.LocalDateTime?): GoalMeasurement {
    return GoalMeasurement(
        value = this,
        locked = locked,
        source = runCatching { GoalValueSource.valueOf(source) }.getOrDefault(GoalValueSource.Manual),
        updatedAt = updatedAt,
    )
}

private fun GoalHistoryEntry.toEntity(): GoalHistoryEntity = GoalHistoryEntity(
    effectiveDate = effectiveDate,
    source = source,
    calories = targets.calories,
    protein = targets.protein,
    carbs = targets.carbs,
    fat = targets.fat,
    fiber = targets.fiber,
    generatedDate = generatedDate,
    bmr = bmr,
    tdee = tdee,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent,
    leanMassKg = leanMassKg,
    activityLevel = activityLevel?.name,
    weightLossTarget = weightLossTarget?.name,
    applied = applied,
)

private fun GoalHistoryEntity.toDomain(): GoalHistoryEntry = GoalHistoryEntry(
    effectiveDate = effectiveDate,
    source = source,
    targets = MacroTargets(calories, protein, carbs, fat, fiber),
    generatedDate = generatedDate,
    bmr = bmr,
    tdee = tdee,
    weightKg = weightKg,
    bodyFatPercent = bodyFatPercent,
    leanMassKg = leanMassKg,
    activityLevel = activityLevel?.let(ActivityLevel::fromName),
    weightLossTarget = weightLossTarget?.let(WeeklyWeightLossTarget::fromName),
    applied = applied,
)

private fun GoalRecommendation.toEntity(): GoalRecommendationEntity = GoalRecommendationEntity(
    generatedDate = generatedDate,
    bmr = bmr,
    tdee = tdee,
    warning = warning,
    calories = targets.calories,
    protein = targets.protein,
    carbs = targets.carbs,
    fat = targets.fat,
    fiber = targets.fiber,
)

private fun GoalRecommendationEntity.toDomain(): GoalRecommendation = GoalRecommendation(
    generatedDate = generatedDate,
    bmr = bmr,
    tdee = tdee,
    warning = warning,
    targets = MacroTargets(calories, protein, carbs, fat, fiber),
)

private fun List<GoalTargetEntity>.toMacroTargets(): MacroTargets {
    return fold(MacroTargets()) { targets, entity ->
        val macro = runCatching { GoalMacro.valueOf(entity.macro) }.getOrNull() ?: return@fold targets
        targets.withValue(macro, entity.value, entity.locked)
    }
}
