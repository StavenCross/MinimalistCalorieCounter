package com.makstuff.minimalistcaloriecounter

import com.makstuff.minimalistcaloriecounter.classes.ActivityLevel
import com.makstuff.minimalistcaloriecounter.classes.GoalCalculator
import com.makstuff.minimalistcaloriecounter.classes.GoalFieldKey
import com.makstuff.minimalistcaloriecounter.classes.GoalHistoryEntry
import com.makstuff.minimalistcaloriecounter.classes.GoalMacro
import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.WeeklyWeightLossTarget
import com.makstuff.minimalistcaloriecounter.classes.onlyIfMeaningfulComparedTo
import com.makstuff.minimalistcaloriecounter.classes.toHistoryEntry
import com.makstuff.minimalistcaloriecounter.health.HealthConnectDeleteResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectGoalProfileReadResult
import com.makstuff.minimalistcaloriecounter.health.HealthConnectGoalProfileWriteResult
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

internal class AppViewModelGoalsActions(private val env: AppViewModelEnvironment) {
    fun updateSettingsVisible(visible: Boolean) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(settingsVisible = visible))
        }
    }

    fun updateBirthday(date: LocalDate?) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(birthday = date)))
        }
        writeGoals()
    }

    fun updateSex(sex: GoalSex) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(sex = sex)))
        }
        writeGoals()
    }

    fun updateActivityLevel(activityLevel: ActivityLevel) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(activityLevel = activityLevel)))
        }
        writeGoals()
    }

    fun updateWeightLossTarget(target: WeeklyWeightLossTarget) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(profile = currentState.goals.profile.copy(weightLossTarget = target)))
        }
        writeGoals()
    }

    fun updateMeasurement(field: GoalFieldKey, value: Double?) {
        env.state.update { currentState ->
            val profile = currentState.goals.profile
            val updated = when (field) {
                GoalFieldKey.HeightCm -> profile.copy(heightCm = profile.heightCm.setManual(value))
                GoalFieldKey.WeightKg -> profile.copy(weightKg = profile.weightKg.setManual(value))
                GoalFieldKey.BodyFatPercent -> profile.copy(bodyFatPercent = profile.bodyFatPercent.setManual(value))
                GoalFieldKey.LeanMassKg -> profile.copy(leanMassKg = profile.leanMassKg.setManual(value))
                else -> profile
            }
            currentState.copy(goals = currentState.goals.copy(profile = updated))
        }
        writeGoals()
        writeManualMeasurementToHealthConnect(field, value)
    }

    fun toggleMeasurementLock(field: GoalFieldKey) {
        env.state.update { currentState ->
            val profile = currentState.goals.profile
            fun GoalMeasurement.toggled(): GoalMeasurement = copy(locked = !locked)
            val updated = when (field) {
                GoalFieldKey.HeightCm -> profile.copy(heightCm = profile.heightCm.toggled())
                GoalFieldKey.WeightKg -> profile.copy(weightKg = profile.weightKg.toggled())
                GoalFieldKey.BodyFatPercent -> profile.copy(bodyFatPercent = profile.bodyFatPercent.toggled())
                GoalFieldKey.LeanMassKg -> profile.copy(leanMassKg = profile.leanMassKg.toggled())
                else -> profile
            }
            currentState.copy(goals = currentState.goals.copy(profile = updated))
        }
        writeGoals()
    }

    fun updateMacro(macro: GoalMacro, value: Double?) {
        env.state.update { currentState ->
            val targets = currentState.goals.currentTargets.withValue(macro, value, lock = true)
            currentState.copy(goals = currentState.goals.copy(currentTargets = targets))
        }
        writeGoals()
    }

    fun toggleMacroLock(macro: GoalMacro) {
        env.state.update { currentState ->
            val targets = currentState.goals.currentTargets
            val updated = if (macro in targets.lockedMacros) {
                targets.unlocked(macro)
            } else {
                targets.withValue(macro, targets.valueFor(macro), lock = true)
            }
            currentState.copy(goals = currentState.goals.copy(currentTargets = updated))
        }
        writeGoals()
    }

    fun recalculateRecommendation(date: LocalDate = LocalDate.now()) {
        env.state.update { currentState ->
            val recommendation = GoalCalculator.recommendTargets(
                profile = currentState.goals.profile,
                existingTargets = currentState.goals.currentTargets,
                date = date,
            )?.onlyIfMeaningfulComparedTo(currentState.goals.activeTargetsFor(date))
            currentState.copy(
                goals = currentState.goals.copy(
                    recommendation = recommendation,
	                    message = if (recommendation == null) {
	                        val missing = currentState.goals.profile.missingRequiredFields(date)
	                        if (missing.isEmpty()) {
	                            "Current goal is already up to date."
	                        } else {
	                            "Missing: ${missing.joinToString(", ")}."
	                        }
                    } else {
                        null
                    },
                )
            )
        }
        writeGoals()
    }

    fun applyRecommendation(date: LocalDate = LocalDate.now()) {
        env.state.update { currentState ->
            val recommendation = currentState.goals.recommendation ?: return@update currentState
            val history = (currentState.goals.history + recommendation.toHistoryEntry(date, currentState.goals.profile))
                .sortedBy { it.effectiveDate }
            currentState.copy(
                goals = currentState.goals.copy(
                    currentTargets = recommendation.targets,
                    history = history,
                    recommendation = null,
                    message = "Applied new goal recommendation.",
                )
            )
        }
        writeGoals()
    }

    fun dismissRecommendation() {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(recommendation = null))
        }
        writeGoals()
    }

    fun deleteHistoryEntry(entry: GoalHistoryEntry) {
        env.state.update { currentState ->
            val remaining = currentState.goals.history.filterNot { it.sameHistoryEntry(entry) }
            val nextTargets = remaining
                .filter { !it.effectiveDate.isAfter(LocalDate.now()) }
                .maxByOrNull { it.effectiveDate }
                ?.targets
                ?: currentState.goals.currentTargets
            currentState.copy(
                goals = currentState.goals.copy(
                    currentTargets = nextTargets,
                    history = remaining,
                    message = "Deleted saved goal.",
                )
            )
        }
        writeGoals()
        env.scope.launch {
            when (val result = env.healthConnectManager.deleteGoalHistoryWeight(entry)) {
                HealthConnectDeleteResult.Success -> Unit
                HealthConnectDeleteResult.HealthConnectUnavailable -> setGoalMessage("Deleted saved goal. Health Connect is unavailable for weight cleanup.")
                HealthConnectDeleteResult.PermissionsMissing -> setGoalMessage("Deleted saved goal. Health Connect weight permissions are missing.")
                is HealthConnectDeleteResult.Failed -> setGoalMessage("Deleted saved goal. Weight cleanup failed: ${result.message}")
            }
        }
    }

    fun refreshFromHealthConnect() {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(message = "Reading Health Connect profile data..."))
        }
        env.scope.launch {
            when (val result = env.healthConnectManager.readGoalProfileSnapshot()) {
                is HealthConnectGoalProfileReadResult.Success -> {
                    env.state.update { currentState ->
                        val profile = GoalCalculator.applyHealthSnapshot(currentState.goals.profile, result.snapshot)
                        val recommendation = GoalCalculator.recommendTargets(
                            profile = profile,
                            existingTargets = currentState.goals.currentTargets,
                            date = LocalDate.now(),
                        )?.onlyIfMeaningfulComparedTo(currentState.goals.activeTargetsFor(LocalDate.now()))
                        currentState.copy(
                            goals = currentState.goals.copy(
                                profile = profile,
                                recommendation = recommendation,
	                                message = if (recommendation == null) {
	                                    val missing = profile.missingRequiredFields(LocalDate.now())
	                                    if (missing.isEmpty()) {
	                                        "Updated unlocked fields from Health Connect. Current goal is up to date."
	                                    } else {
	                                        "Updated unlocked fields. Missing: ${missing.joinToString(", ")}."
	                                    }
	                                } else {
                                    "Updated unlocked fields from Health Connect."
                                },
                            )
                        )
                    }
                    writeGoals()
                }
                HealthConnectGoalProfileReadResult.HealthConnectUnavailable -> {
                    env.state.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect is unavailable."))
                    }
                }
                HealthConnectGoalProfileReadResult.PermissionsMissing -> {
                    env.state.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect profile permissions are missing."))
                    }
                }
                is HealthConnectGoalProfileReadResult.Failed -> {
                    env.state.update { currentState ->
                        currentState.copy(goals = currentState.goals.copy(message = "Health Connect profile read failed: ${result.message}"))
                    }
                }
            }
        }
    }

    private fun writeGoals() {
        env.csvStore.writeGoals(env.context, env.uiState.goals)
        env.launchRoomWrite {
            writeGoals(env.uiState.goals)
        }
    }

    private fun writeManualMeasurementToHealthConnect(field: GoalFieldKey, value: Double?) {
        if (value == null) return
        val heightCm = value.takeIf { field == GoalFieldKey.HeightCm }
        val weightKg = value.takeIf { field == GoalFieldKey.WeightKg }
        if (heightCm == null && weightKg == null) return
        env.scope.launch {
            when (val result = env.healthConnectManager.writeGoalProfileMeasurements(heightCm = heightCm, weightKg = weightKg)) {
                HealthConnectGoalProfileWriteResult.Success -> Unit
                HealthConnectGoalProfileWriteResult.HealthConnectUnavailable -> Unit
                HealthConnectGoalProfileWriteResult.PermissionsMissing -> setGoalMessage("Health Connect profile write permissions are missing.")
                is HealthConnectGoalProfileWriteResult.Failed -> setGoalMessage("Health Connect profile write failed: ${result.message}")
            }
        }
    }

    private fun setGoalMessage(message: String) {
        env.state.update { currentState ->
            currentState.copy(goals = currentState.goals.copy(message = message))
        }
        writeGoals()
    }

    private fun GoalHistoryEntry.sameHistoryEntry(other: GoalHistoryEntry): Boolean {
        return effectiveDate == other.effectiveDate &&
            targets == other.targets &&
            generatedDate == other.generatedDate &&
            weightKg == other.weightKg &&
            leanMassKg == other.leanMassKg &&
            bodyFatPercent == other.bodyFatPercent
    }
}
