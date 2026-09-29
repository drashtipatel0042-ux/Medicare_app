package com.example

import com.example.data.model.MedicineType
import com.example.data.model.RepeatSchedule
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testBmiCalculation() {
        val weightKg = 70.0
        val heightM = 1.75
        val bmi = weightKg / (heightM * heightM)
        val roundedBmi = Math.round(bmi * 10.0) / 10.0
        assertEquals(22.9, roundedBmi, 0.1)
    }

    @Test
    fun testMedicineEnums() {
        assertEquals("Tablet", MedicineType.TABLET.labelEn)
        assertEquals("Every Day", RepeatSchedule.DAILY.labelEn)
    }
}
