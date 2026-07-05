package com.makstuff.minimalistcaloriecounter.ui.screens

import com.makstuff.minimalistcaloriecounter.classes.GoalMeasurement
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalImperialUnitsTest {
    @Test
    fun heightConversionsRoundBetweenCentimetersAndFeetInches() {
        assertEquals(ImperialHeight(5, 11), 180.0.toImperialHeight())
        assertEquals(182.88, ImperialHeight(6, 0).toCentimeters(), 0.01)
    }

    @Test
    fun weightConversionsRoundBetweenKilogramsAndPounds() {
        assertEquals(209, 95.0.toImperialPounds())
        assertEquals(95.25, 210.toKilograms(), 0.01)
    }

    @Test
    fun goalMeasurementsRenderImperialLabelsOrRequired() {
        assertEquals("5' 11\"", GoalMeasurement(180.0).heightImperialLabel())
        assertEquals("209 lb", GoalMeasurement(95.0).weightImperialLabel())
        assertEquals("Required", GoalMeasurement().heightImperialLabel())
        assertEquals("Required", GoalMeasurement().weightImperialLabel())
    }
}
