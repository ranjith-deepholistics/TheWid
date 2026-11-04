package com.fivehouse.thewid.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shell.WidgetCategory
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.toFace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShellScreensTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun onboardingAdvancesThroughItsPagesThenFinishes() {
        var finished = false
        rule.setContent { ScreenPreview(Atmosphere.Lilac) { OnboardingScreen(onFinished = { finished = true }) } }
        rule.onNodeWithContentDescription("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Start").performClick()
        assertTrue(finished)
    }

    @Test
    fun categoryOpensBuiltWidgets() {
        var opened: StatusWidgetKind? = null
        rule.setContent {
            ScreenPreview(Atmosphere.Mint) {
                CategoryScreen(WidgetCategory.Status, PreviewFacesMap, {}, { opened = it })
            }
        }
        rule.onNodeWithText("Battery").performClick()
        assertEquals(StatusWidgetKind.Battery, opened)
    }

    @Test
    fun emptyMyWidgetsOffersQuickAdds() {
        var added: StatusWidgetKind? = null
        rule.setContent {
            ScreenPreview(Atmosphere.Lilac) {
                MyWidgetsScreen(emptyList(), StatusWidgetKind.Time, PreviewFacesMap, {}, {}, { added = it })
            }
        }
        rule.onNodeWithText("Nothing placed yet").assertIsDisplayed()
        rule.onNodeWithText("Sound").performClick()
        assertEquals(StatusWidgetKind.Sound, added)
    }

    private fun showPermission(stage: PermissionStage, onSettings: () -> Unit = {}, onAdd: () -> Unit = {}) {
        rule.setContent {
            ScreenPreview(Atmosphere.DuskBloom) {
                PermissionScreen(
                    StatusWidgetKind.Alerts,
                    stage,
                    AlertsStatus.Count(2).toFace(),
                    "Shapes",
                    "TheWid",
                    onSettings,
                    {},
                    onAdd,
                    {}
                )
            }
        }
    }

    @Test
    fun permissionAskOpensSettings() {
        var opened = false
        showPermission(PermissionStage.Ask, onSettings = { opened = true })
        rule.onNodeWithText("Open settings").performClick()
        assertTrue(opened)
    }

    @Test
    fun permissionDeniedExplainsTheStepsWithTheNameInTheList() {
        showPermission(PermissionStage.Denied)
        rule.onNodeWithText("Find TheWid in the list").assertIsDisplayed()
    }

    @Test
    fun permissionGrantedShowsTheCountAndAdds() {
        var added = false
        showPermission(PermissionStage.Granted, onAdd = { added = true })
        rule.onNodeWithText("Counting 2 new right now").assertIsDisplayed()
        rule.onNodeWithText("Add to home screen").performClick()
        assertTrue(added)
    }

    @Test
    fun searchFindsWidgetsAndSaysWhenNothingMatches() {
        rule.setContent {
            ScreenPreview(Atmosphere.CoolMist) {
                SearchScreen("batt", PreviewFacesMap, emptyList(), {}, {}, {}, {})
            }
        }
        rule.onNodeWithText("Status · 68%").assertIsDisplayed()
    }
}
