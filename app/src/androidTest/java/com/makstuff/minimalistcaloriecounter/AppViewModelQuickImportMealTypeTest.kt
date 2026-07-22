package com.makstuff.minimalistcaloriecounter

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppViewModelQuickImportMealTypeTest {
    /** Guards the action layer that previously converted early meals into rejected future writes. */
    @Test
    fun manualMealLabelsPreserveTheIntendedTimestamp() {
        val viewModel = AppViewModel(ApplicationProvider.getApplicationContext<Application>())
        val scenarios = listOf(
            LocalDateTime.of(2026, 7, 14, 6, 0) to QuickImportMealType.Breakfast,
            LocalDateTime.of(2026, 7, 14, 11, 0) to QuickImportMealType.Lunch,
            LocalDateTime.of(2026, 7, 14, 4, 0) to QuickImportMealType.Dinner,
        )

        scenarios.forEach { (timestamp, mealType) ->
            viewModel.updateQuickImportDateTime(timestamp)
            viewModel.updateQuickImportMealTypeOverride(mealType)

            assertEquals(timestamp, viewModel.uiState.value.inputQuickImportDateTime)
            assertEquals(mealType, viewModel.uiState.value.quickImportMealType)
        }
    }
}
