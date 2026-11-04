package com.fivehouse.thewid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fivehouse.thewid.data.status.platform.NotificationAccess
import com.fivehouse.thewid.ui.shell.ShellApp
import com.fivehouse.thewid.widget.WidgetRefresh
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShellApp()
        }
    }

    override fun onStart() {
        super.onStart()
        NotificationAccess.refresh(this)
        WidgetRefresh.refreshNow(this)
    }
}
