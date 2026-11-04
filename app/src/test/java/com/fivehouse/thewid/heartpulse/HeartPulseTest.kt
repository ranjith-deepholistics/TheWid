package com.fivehouse.thewid.heartpulse

import com.fivehouse.thewid.data.personal.HeartPulseSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartPulseTest {
    private val on = HeartPulseSettings(enabled = true)

    private fun allowed(
        settings: HeartPulseSettings = on,
        trigger: PulseTrigger = PulseTrigger.Charging,
        canDraw: Boolean = true,
        interactive: Boolean = true
    ) = shouldPulse(settings, trigger, canDraw, interactive)

    @Test
    fun neverBeatsWithoutPermissionOrWithTheScreenOff() {
        assertFalse(allowed(canDraw = false))
        assertFalse(allowed(interactive = false))
        assertFalse(allowed(trigger = PulseTrigger.Test, canDraw = false))
    }

    @Test
    fun offMeansOffExceptAnExplicitTest() {
        assertFalse(allowed(HeartPulseSettings(enabled = false)))
        assertTrue(allowed(HeartPulseSettings(enabled = false), PulseTrigger.Test))
    }

    @Test
    fun eachTriggerFollowsItsOwnSwitch() {
        assertTrue(allowed(trigger = PulseTrigger.Notification))
        assertFalse(allowed(on.copy(onNotification = false), PulseTrigger.Notification))
        assertTrue(allowed(trigger = PulseTrigger.Charging))
        assertFalse(allowed(on.copy(onCharging = false), PulseTrigger.Charging))
        assertTrue(allowed(trigger = PulseTrigger.Breathe))
        assertFalse(allowed(on.copy(duringBreathe = false), PulseTrigger.Breathe))
    }

    @Test
    fun everyHourIsOffByDefaultLikeTheMockup() {
        assertFalse(allowed(trigger = PulseTrigger.Hour))
        assertTrue(allowed(on.copy(everyHour = true), PulseTrigger.Hour))
    }

    @Test
    fun theWindowCentresOnTheCamera() {
        assertEquals(400 to -150, pulseWindowOrigin(centerX = 600, centerY = 50, sizePx = 400))
    }
}
