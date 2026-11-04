package com.fivehouse.thewid.ui.shell

import com.fivehouse.thewid.widget.StatusWidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCatalogTest {
    @Test
    fun everyWidgetIsInACategory() {
        val categorised = WidgetCategory.entries.flatMap { it.widgets }.toSet()
        assertEquals(CatalogWidget.entries.toSet(), categorised)
    }

    @Test
    fun everyBuiltKindIsInTheCatalog() {
        assertEquals(StatusWidgetKind.entries.toSet(), CatalogWidget.entries.mapNotNull { it.kind }.toSet())
    }

    @Test
    fun searchIgnoresCaseAndTrimsAndBlankFindsNothing() {
        val names = listOf("Battery", "Background updates", "Sound")
        assertEquals(listOf("Battery", "Background updates"), names.matching(" BA ") { it })
        assertEquals(emptyList<String>(), names.matching("   ") { it })
        assertEquals(emptyList<String>(), names.matching("zzz") { it })
    }

    @Test
    fun eachKindHasItsOwnWidgetClassSoUpdateAllStaysWithinAKind() {
        val classes = StatusWidgetKind.entries.map { it.newWidget()::class }
        assertEquals(classes.size, classes.toSet().size)
        assertTrue(classes.isNotEmpty())
    }
}
