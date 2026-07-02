package com.makstuff.minimalistcaloriecounter.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.AppUiState
import com.makstuff.minimalistcaloriecounter.classes.Archive
import com.makstuff.minimalistcaloriecounter.classes.Combo
import com.makstuff.minimalistcaloriecounter.classes.QuickImportParser
import com.makstuff.minimalistcaloriecounter.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenQuickImportTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun validMealShowsPreviewAndEnablesImport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sample = "100g rice; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g. " +
            "Meal totals; Calories 130, Fat 0.3g, Sat Fat 0.1g, Trans Fat 0g, Cholesterol 0mg, Sodium 1mg, Carbs 28g, Fiber 1g, Sugar 0.1g, Added Sugar 0g, Protein 2.7g."
        val state = baseState(context).copy(
            inputQuickImportText = sample,
            quickImportMeal = QuickImportParser.parse(sample),
        )

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = state,
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onImport = {},
                    onClear = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_preview_totals").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_import_import_button").assertIsEnabled()
    }

    @Test
    fun missingParsedMealDisablesImport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            AppTheme {
                ScreenQuickImport(
                    uiState = baseState(context).copy(inputQuickImportText = "not a meal"),
                    onTextChange = {},
                    onToggleAddDatabase = {},
                    onToggleAddDay = {},
                    onToggleHealthConnect = {},
                    onRefreshDateTime = {},
                    onImport = {},
                    onClear = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("quick_import_import_button").assertIsNotEnabled()
    }

    private fun baseState(context: android.content.Context): AppUiState {
        return AppUiState(
            archive = Archive(context = context),
            day = Combo(context = context),
            currentCombo = Combo(context = context),
        )
    }
}
