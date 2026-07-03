package com.makstuff.minimalistcaloriecounter.persistence.room

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makstuff.minimalistcaloriecounter.classes.GoalProfile
import com.makstuff.minimalistcaloriecounter.classes.GoalSex
import com.makstuff.minimalistcaloriecounter.classes.Goals
import com.makstuff.minimalistcaloriecounter.classes.MacroTargets
import com.makstuff.minimalistcaloriecounter.classes.QuickImportHealthPayload
import com.makstuff.minimalistcaloriecounter.classes.QuickImportMealType
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxItem
import com.makstuff.minimalistcaloriecounter.classes.QuickImportOutboxState
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppRoomStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: AppRoomStore

    @Before
    fun setUp() {
        context.deleteDatabase(AppRoomStore.DATABASE_NAME)
        store = AppRoomStore(context)
    }

    @After
    fun tearDown() {
        store.close()
        context.deleteDatabase(AppRoomStore.DATABASE_NAME)
    }

    @Test
    fun roundTripsGoalsAndOutbox() = runBlocking {
        val goals = Goals(
            profile = GoalProfile(
                birthday = LocalDate.of(1985, 1, 2),
                sex = GoalSex.Male,
            ),
            currentTargets = MacroTargets(calories = 2200.0, protein = 180.0, carbs = 210.0, fat = 70.0, fiber = 35.0),
        )
        val outbox = QuickImportOutboxItem(
            id = "room-test",
            createdAt = LocalDateTime.of(2026, 7, 3, 12, 1),
            intendedDateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
            mealType = QuickImportMealType.Lunch,
            sourceTextHash = "hash",
            mealSummary = "1 foods, 500 kcal",
            foodCount = 1,
            state = QuickImportOutboxState.PendingHealthConnect,
            attemptCount = 0,
            lastAttemptAt = null,
            lastErrorMessage = null,
            healthPayloads = listOf(
                QuickImportHealthPayload(
                    dateTime = LocalDateTime.of(2026, 7, 3, 12, 0),
                    mealType = QuickImportMealType.Lunch.healthConnectValue,
                    energy = 500.0,
                    energyFromFat = 90.0,
                    totalCarbohydrate = 55.0,
                    sugar = 4.0,
                    protein = 35.0,
                    totalFat = 10.0,
                    saturatedFat = 2.0,
                    dietaryFiber = 8.0,
                    name = "Lunch bowl",
                    clientRecordId = "mcc-add-meal-room-test-0",
                )
            ),
        )

        store.writeGoals(goals)
        store.writeQuickImportOutboxItem(outbox)

        assertEquals(goals, store.readGoals())
        assertEquals(listOf(outbox), store.readQuickImportOutbox())
    }
}
