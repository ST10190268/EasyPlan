package com.easyplan

import org.junit.Test
import org.junit.Assert.*

/**
 * Basic unit tests for EasyPlan app
 * Tests core functionality without Android dependencies
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun string_concatenation_works() {
        val result = "Easy" + "Plan"
        assertEquals("EasyPlan", result)
    }

    @Test
    fun list_operations_work() {
        val list = listOf(1, 2, 3, 4, 5)
        assertEquals(5, list.size)
        assertEquals(15, list.sum())
    }

    @Test
    fun percentage_calculation_works() {
        val total = 10
        val completed = 7
        val percentage = (completed * 100f / total)
        assertEquals(70f, percentage, 0.01f)
    }
}