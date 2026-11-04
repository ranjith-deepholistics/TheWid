package com.fivehouse.thewid.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** Tapping Decide on the home screen rolls it there, without opening the app. */
class RollDecisionAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.personalRepository().rollDecision()
        DecideWidget().updateAll(context)
    }
}
