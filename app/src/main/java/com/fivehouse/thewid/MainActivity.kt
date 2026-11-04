package com.fivehouse.thewid

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fivehouse.thewid.data.status.platform.NotificationAccess
import com.fivehouse.thewid.ui.shell.ShellApp
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow

const val EXTRA_OPEN_KIND = "open_kind"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openRequest = MutableStateFlow<StatusWidgetKind?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readOpenRequest(intent)
        setContent {
            ShellApp(openRequest)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readOpenRequest(intent)
    }

    override fun onStart() {
        super.onStart()
        NotificationAccess.refresh(this)
        WidgetRefresh.refreshNow(this)
    }

    /** This activity is exported, so only a known kind is accepted, and it only ever opens our own screen. */
    private fun readOpenRequest(intent: Intent) {
        val name = intent.getStringExtra(EXTRA_OPEN_KIND)
        openRequest.value = StatusWidgetKind.entries.firstOrNull { it.name == name }
    }
}
