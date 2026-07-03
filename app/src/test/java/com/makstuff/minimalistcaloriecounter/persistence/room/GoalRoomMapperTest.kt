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
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalRoomMapperTest {
    @Test
    fun roundTripsGoalsSeed() {
        val updatedAt = LocalDateTime.of(2026, 7, 3, 8, 30)
        val goals = Goals(
            profile = GoalProfile(
                birthday = LocalDate.of(1985, 1, 2),
                sex = GoalSex.Male,
                heightCm = GoalMeasurement(180.0, locked = true, updatedAt = updatedAt),
                weightKg = GoalMeasurement(92.5, source = GoalValueSource.HealthConnect, updatedAt = updatedAt),
                bodyFatPercent = GoalMeasurement(22.0),
                leanMassKg = GoalMeasurement(72.15, locked = true),
                activityLevel = ActivityLevel.ModeratelyActive,
                weightLossTarget = WeeklyWeightLossTarget.OnePound,
            ),
            currentTargets = MacroTargets(
                calories = 2200.0,
                protein = 180.0,
                carbs = 210.0,
                fat = 70.0,
                fiber = 35.0,
                lockedMacros = setOf(GoalMacro.Protein, GoalMacro.Fiber),
            ),
            history = listOf(
                GoalHistoryEntry(
                    effectiveDate = LocalDate.of(2026, 7, 5),
                    targets = MacroTargets(2200.0, 180.0, 210.0, 70.0, 35.0),
                    source = "recommended",
                    generatedDate = LocalDate.of(2026, 7, 3),
                    bmr = 1900.0,
                    tdee = 2600.0,
                    weightKg = 92.5,
                    bodyFatPercent = 22.0,
                    leanMassKg = 72.15,
                    activityLevel = ActivityLevel.ModeratelyActive,
                    weightLossTarget = WeeklyWeightLossTarget.OnePound,
                )
            ),
            recommendation = GoalRecommendation(
                generatedDate = LocalDate.of(2026, 7, 3),
                targets = MacroTargets(2200.0, 180.0, 210.0, 70.0, 35.0),
                bmr = 1900.0,
                tdee = 2600.0,
                warning = "Review weekly.",
            ),
        )

        val restored = GoalRoomMapper.fromSeed(GoalRoomMapper.toSeed(goals))

        assertEquals(goals.copy(settingsVisible = false, message = null), restored)
    }
}
