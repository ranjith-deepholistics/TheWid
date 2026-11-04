package com.fivehouse.thewid.ui.shapes

import org.junit.Assert.assertEquals
import org.junit.Test

class FillLineTest {
    @Test
    fun emptySitsAtBottomAndFullAtTop() {
        assertEquals(110f, fillLineY(top = 10f, bottom = 110f, fill = 0f), 0.001f)
        assertEquals(10f, fillLineY(top = 10f, bottom = 110f, fill = 1f), 0.001f)
    }

    @Test
    fun levelRisesFromTheBottom() {
        assertEquals(42f, fillLineY(top = 10f, bottom = 110f, fill = 0.68f), 0.001f)
    }

    @Test
    fun outOfRangeLevelsAreClamped() {
        assertEquals(110f, fillLineY(top = 10f, bottom = 110f, fill = -0.5f), 0.001f)
        assertEquals(10f, fillLineY(top = 10f, bottom = 110f, fill = 1.5f), 0.001f)
    }
}
