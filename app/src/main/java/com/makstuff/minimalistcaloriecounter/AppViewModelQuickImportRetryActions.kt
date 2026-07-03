package com.makstuff.minimalistcaloriecounter

import android.content.Context
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthWriteResult
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutbox
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

internal class AppViewModelQuickImportRetryActions(
    private val env: AppViewModelEnvironment,
    private val viewModel: AppViewModel,
) {
    fun retry(context: Context, outboxId: String) {
        val item = env.uiState.quickImportOutbox.firstOrNull { it.id == outboxId } ?: return
        if (item.state == QuickImportOutboxState.Synced) return

        if (item.healthPayloads.isEmpty()) {
            env.writeQuickImportOutboxItem(
                context,
                item.copy(
                    state = QuickImportOutboxState.FailedHealthConnect,
                    lastErrorMessage = "This outbox item was created before retry payload storage existed.",
                ),
            )
            return
        }

        env.scope.launch {
            var retrying = QuickImportOutbox.markAttempting(item, LocalDateTime.now())
            env.writeQuickImportOutboxItem(context, retrying)
            try {
                val result = env.healthConnectManager.insertQuickMealNutrition(retrying.healthPayloads)
                retrying = QuickImportOutbox.markResult(retrying, result)
                env.writeQuickImportOutboxItem(context, retrying)
                if (result == QuickImportHealthWriteResult.Success) {
                    env.state.update { currentState ->
                        currentState.copy(
                            healthConnectViewerDate = retrying.intendedDateTime.toLocalDate(),
                            quickImportSuccessMessage = "Health Connect retry succeeded.",
                            quickImportSuccessToken = currentState.quickImportSuccessToken + 1L,
                        )
                    }
                    viewModel.readHealthConnectNutritionMeals()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                env.writeQuickImportOutboxItem(
                    context,
                    retrying.copy(
                        state = QuickImportOutboxState.FailedHealthConnect,
                        lastErrorMessage = e.message ?: "Health Connect retry failed.",
                    ),
                )
            }
        }
    }
}

