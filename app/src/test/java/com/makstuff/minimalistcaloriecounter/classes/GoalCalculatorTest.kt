package com.makstuff.minimalistcaloriecounter.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GoalCalculatorTest {
    private val today = LocalDate.of(2026, 7, 2)

    @Test
    fun `mifflin st jeor calculates male bmr`() {
        val profile = completeProfile(sex = GoalSex.Male, weightKg = 90.0, heightCm = 180.0)

        val bmr = GoalCalculator.bmrMifflinStJeor(profile, today)

        assertEquals(1850.0, bmr!!, 0.01)
    }

    @Test
    fun `mifflin st jeor calculates female bmr`() {
        val profile = completeProfile(sex = GoalSex.Female, weightKg = 70.0, heightCm = 165.0)

        val bmr = GoalCalculator.bmrMifflinStJeor(profile, today)

        assertEquals(1390.25, bmr!!, 0.01)
    }

    @Test
    fun `recommendation applies activity factor and deficit`() {
        val profile = completeProfile(
            sex = GoalSex.Male,
            weightKg = 90.0,
            heightCm = 180.0,
            activityLevel = ActivityLevel.LightlyActive,
            weightLossTarget = WeeklyWeightLossTarget.OnePound,
            leanMassKg = 72.0,
        )

        val recommendation = GoalCalculator.recommendTargets(profile, date = today)!!

        assertEquals(1850.0, recommendation.bmr, 0.01)
        assertEquals(2543.75, recommendation.tdee, 0.01)
        assertEquals(2044.0, recommendation.targets.calories!!, 0.01)
        assertEquals(158.7, recommendation.targets.protein!!, 0.01)
        assertEquals(56.8, recommendation.targets.fat!!, 0.01)
        assertEquals(224.5, recommendation.targets.carbs!!, 0.01)
        assertEquals(28.6, recommendation.targets.fiber!!, 0.01)
    }

    @Test
    fun `recommendation can use estimated lean mass when lean mass and body fat are unavailable`() {
        val profile = completeProfile(leanMassKg = null, bodyFatPercent = null)

        val recommendation = GoalCalculator.recommendTargets(profile, date = today)

        assertEquals(144.4, recommendation!!.targets.protein!!, 0.1)
    }

    @Test
    fun `manual macro lock preserves value`() {
        val lockedTargets = MacroTargets().withValue(GoalMacro.Protein, 200.0)

        val generated = GoalCalculator.generateMacros(
            calories = 2200.0,
            leanMassKg = 70.0,
            existingTargets = lockedTargets,
        )

        assertEquals(200.0, generated.protein!!, 0.01)
        assertTrue(GoalMacro.Protein in generated.lockedMacros)
    }

    @Test
    fun `health connect updates unlocked fields and preserves locked manual fields`() {
        val profile = completeProfile(weightKg = 90.0).copy(
            weightKg = GoalMeasurement(90.0, locked = true, source = GoalValueSource.Manual),
            heightCm = GoalMeasurement(180.0, locked = false, source = GoalValueSource.Manual),
        )
        val snapshot = HealthConnectGoalSnapshot(
            weightKg = 88.0,
            weightUpdatedAt = LocalDateTime.of(2026, 7, 1, 8, 0),
            heightCm = 181.0,
            heightUpdatedAt = LocalDateTime.of(2026, 7, 1, 8, 1),
        )

        val updated = GoalCalculator.applyHealthSnapshot(profile, snapshot)

        assertEquals(90.0, updated.weightKg.value!!, 0.01)
        assertEquals(GoalValueSource.Manual, updated.weightKg.source)
        assertEquals(181.0, updated.heightCm.value!!, 0.01)
        assertEquals(GoalValueSource.HealthConnect, updated.heightCm.source)
    }

    @Test
    fun `health connect direct lean mass wins when available`() {
        val profile = completeProfile(leanMassKg = null, bodyFatPercent = null)
        val snapshot = HealthConnectGoalSnapshot(
            weightKg = 90.0,
            heightCm = 180.0,
            bodyFatPercent = 18.0,
            bodyWaterMassKg = 53.0,
            leanMassKg = 75.5,
            leanMassUpdatedAt = LocalDateTime.of(2026, 7, 1, 8, 0),
        )

        val updated = GoalCalculator.applyHealthSnapshot(profile, snapshot)

        assertEquals(75.5, updated.leanMassKg.value!!, 0.01)
        assertEquals(GoalValueSource.HealthConnect, updated.leanMassKg.source)
    }

    @Test
    fun `health connect derives lean mass from body fat and weight before estimate formulas`() {
        val profile = completeProfile(leanMassKg = null, bodyFatPercent = null)
        val snapshot = HealthConnectGoalSnapshot(
            weightKg = 90.0,
            heightCm = 180.0,
            bodyFatPercent = 18.0,
            bodyWaterMassKg = 53.0,
        )

        val updated = GoalCalculator.applyHealthSnapshot(profile, snapshot)

        assertEquals(73.8, updated.leanMassKg.value!!, 0.01)
        assertEquals(GoalValueSource.HealthConnect, updated.leanMassKg.source)
    }

    @Test
    fun `health connect estimates lean mass from body water when body fat is unavailable`() {
        val profile = completeProfile(leanMassKg = null, bodyFatPercent = null)
        val snapshot = HealthConnectGoalSnapshot(
            weightKg = 90.0,
            heightCm = 180.0,
            bodyWaterMassKg = 53.0,
        )

        val updated = GoalCalculator.applyHealthSnapshot(profile, snapshot)

        assertEquals(72.6, updated.leanMassKg.value!!, 0.1)
    }

    @Test
    fun `profile can estimate lean mass from boer formula as final fallback`() {
        val profile = completeProfile(leanMassKg = null, bodyFatPercent = null)

        assertEquals(65.5, profile.leanMassOrCalculatedKg()!!, 0.1)
    }

    @Test
    fun `goals required fields need age sex height and weight`() {
        assertFalse(GoalProfile().isRequiredComplete(today))
        assertTrue(completeProfile().isRequiredComplete(today))
    }

    @Test
    fun `goal history lookup uses date effective targets`() {
        val oldTargets = MacroTargets(calories = 2000.0, protein = 150.0, carbs = 200.0, fat = 70.0, fiber = 28.0)
        val newTargets = MacroTargets(calories = 1800.0, protein = 160.0, carbs = 150.0, fat = 60.0, fiber = 25.0)
        val goals = Goals(
            currentTargets = newTargets,
            history = listOf(
                GoalHistoryEntry(LocalDate.of(2026, 6, 1), oldTargets, "manual"),
                GoalHistoryEntry(LocalDate.of(2026, 7, 1), newTargets, "recommended"),
            ),
        )

        assertEquals(2000.0, goals.activeTargetsFor(LocalDate.of(2026, 6, 20)).calories!!, 0.01)
        assertEquals(1800.0, goals.activeTargetsFor(LocalDate.of(2026, 7, 2)).calories!!, 0.01)
    }

    @Test
    fun `goals csv round trips profile locks history and recommendation`() {
        val goals = Goals(
            profile = completeProfile().copy(
                weightKg = GoalMeasurement(
                    value = 90.0,
                    locked = true,
                    source = GoalValueSource.Manual,
                    updatedAt = LocalDateTime.of(2026, 7, 2, 7, 30),
                ),
            ),
            currentTargets = completeTargets(2100.0).withValue(GoalMacro.Protein, 190.0),
            history = listOf(
                GoalHistoryEntry(
                    effectiveDate = today,
                    targets = completeTargets(2100.0),
                    source = "recommended",
                    generatedDate = today.minusDays(1),
                    bmr = 1850.0,
                    tdee = 2550.0,
                    weightKg = 90.0,
                    bodyFatPercent = 20.0,
                    leanMassKg = 72.0,
                    activityLevel = ActivityLevel.ModeratelyActive,
                    weightLossTarget = WeeklyWeightLossTarget.OnePound,
                    applied = true,
                )
            ),
            recommendation = GoalRecommendation(
                generatedDate = today,
                targets = completeTargets(2050.0),
                bmr = 1850.0,
                tdee = 2550.0,
                warning = null,
            ),
        )

        val roundTripped = GoalsCsv.fromRows(GoalsCsv.toRows(goals))

        assertEquals(GoalSex.Male, roundTripped.profile.sex)
        assertEquals(90.0, roundTripped.profile.weightKg.value!!, 0.01)
        assertTrue(roundTripped.profile.weightKg.locked)
        assertEquals(190.0, roundTripped.currentTargets.protein!!, 0.01)
        assertTrue(GoalMacro.Protein in roundTripped.currentTargets.lockedMacros)
        assertEquals(1, roundTripped.history.size)
        assertEquals(1850.0, roundTripped.history.single().bmr!!, 0.01)
        assertEquals(2550.0, roundTripped.history.single().tdee!!, 0.01)
        assertEquals(72.0, roundTripped.history.single().leanMassKg!!, 0.01)
        assertEquals(ActivityLevel.ModeratelyActive, roundTripped.history.single().activityLevel)
        assertEquals(2050.0, roundTripped.recommendation!!.targets.calories!!, 0.01)
    }

    @Test
    fun `recommendation history captures source measurements`() {
        val recommendation = GoalRecommendation(
            generatedDate = today,
            targets = completeTargets(2050.0),
            bmr = 1850.0,
            tdee = 2550.0,
        )

        val history = recommendation.toHistoryEntry(today.plusDays(1), completeProfile())

        assertEquals(today, history.generatedDate)
        assertEquals(1850.0, history.bmr!!, 0.01)
        assertEquals(2550.0, history.tdee!!, 0.01)
        assertEquals(90.0, history.weightKg!!, 0.01)
        assertEquals(20.0, history.bodyFatPercent!!, 0.01)
        assertEquals(72.0, history.leanMassKg!!, 0.01)
        assertEquals(ActivityLevel.Sedentary, history.activityLevel)
        assertEquals(WeeklyWeightLossTarget.Maintain, history.weightLossTarget)
        assertTrue(history.applied)
    }

    private fun completeProfile(
        sex: GoalSex = GoalSex.Male,
        weightKg: Double = 90.0,
        heightCm: Double = 180.0,
        activityLevel: ActivityLevel = ActivityLevel.Sedentary,
        weightLossTarget: WeeklyWeightLossTarget = WeeklyWeightLossTarget.Maintain,
        leanMassKg: Double? = 72.0,
        bodyFatPercent: Double? = 20.0,
    ) = GoalProfile(
        birthday = LocalDate.of(1990, 7, 2),
        sex = sex,
        heightCm = GoalMeasurement(heightCm),
        weightKg = GoalMeasurement(weightKg),
        bodyFatPercent = GoalMeasurement(bodyFatPercent),
        leanMassKg = GoalMeasurement(leanMassKg),
        activityLevel = activityLevel,
        weightLossTarget = weightLossTarget,
    )

    private fun completeTargets(calories: Double) = MacroTargets(
        calories = calories,
        protein = 180.0,
        carbs = 220.0,
        fat = 70.0,
        fiber = 30.0,
    )
}
