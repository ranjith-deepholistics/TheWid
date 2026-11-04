package com.fivehouse.thewid.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals

/**
 * A permission a feature needs, asked for only after someone taps that feature.
 *
 * @property permission the runtime permission, or null for one granted in system settings.
 */
enum class AccessRequest(
    val permission: String?,
    val shape: ShapeCatalog,
    @param:StringRes val feature: Int,
    @param:StringRes val title: Int,
    @param:StringRes val body: Int,
    @param:StringRes val scope: Int
) {
    Contacts(
        Manifest.permission.READ_CONTACTS,
        ShapeCatalog.Heart,
        R.string.access_contacts_feature,
        R.string.access_contacts_title,
        R.string.access_contacts_body,
        R.string.access_contacts_scope
    ),
    Location(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        ShapeCatalog.Sunny,
        R.string.access_location_feature,
        R.string.access_location_title,
        R.string.access_location_body,
        R.string.access_location_scope
    ),
    Calendar(
        Manifest.permission.READ_CALENDAR,
        ShapeCatalog.Diamond,
        R.string.access_calendar_feature,
        R.string.access_calendar_title,
        R.string.access_calendar_body,
        R.string.access_calendar_scope
    ),
    ExactAlarm(
        null,
        ShapeCatalog.Sunny,
        R.string.access_exact_feature,
        R.string.access_exact_title,
        R.string.access_exact_body,
        R.string.access_exact_scope
    );

    fun isGranted(context: Context): Boolean = if (permission != null) {
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    } else {
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
}

/** The same ask, denied and granted pattern as notification access (screens 6, 11 and 12). */
@Composable
fun AccessScreen(
    request: AccessRequest,
    stage: PermissionStage,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            BackButton(onBack, Modifier.align(Alignment.CenterStart))
            Text(stringResource(request.feature), style = MaterialTheme.typography.titleMedium)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (stage) {
                PermissionStage.Ask -> AskContent(request)
                PermissionStage.Denied -> DeniedContent(request)
                PermissionStage.Granted -> GrantedContent(request)
            }
        }
        when (stage) {
            PermissionStage.Ask -> {
                PrimaryPill(stringResource(R.string.access_allow), onAllow)
                TextButton(onClick = onBack) { Text(stringResource(R.string.permission_not_now)) }
            }

            PermissionStage.Denied -> {
                PrimaryPill(stringResource(R.string.permission_open_again), onOpenSettings)
                TextButton(onClick = onBack) { Text(stringResource(R.string.permission_not_now)) }
            }

            PermissionStage.Granted -> PrimaryPill(stringResource(R.string.setup_continue), onBack)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskContent(request: AccessRequest) {
    GlossyShape(request.shape, null, Modifier.size(180.dp), castShadow = true) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_lock),
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
    Text(
        stringResource(request.title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )
    Text(stringResource(request.body), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        InfoChip(stringResource(request.scope), icon = R.drawable.ic_check)
        InfoChip(stringResource(R.string.permission_chip_local), icon = R.drawable.ic_check)
    }
}

@Composable
private fun DeniedContent(request: AccessRequest) {
    GlossyShape(request.shape, null, Modifier.size(170.dp), surface = ShapeSurface.Dashed) {
        Text("!", style = Numerals.Large)
    }
    Text(
        stringResource(R.string.permission_denied_title, stringResource(request.feature)),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )
    Text(
        stringResource(R.string.access_denied_body),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GrantedContent(request: AccessRequest) {
    GlossyShape(request.shape, null, Modifier.size(180.dp), castShadow = true)
    Text(
        stringResource(R.string.access_granted_title, stringResource(request.feature)),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )
}

@Preview(name = "Ask", widthDp = 390, heightDp = 844)
@Composable
private fun AccessAskPreview() {
    ScreenPreview(Atmosphere.DuskBloom) { AccessScreen(AccessRequest.Contacts, PermissionStage.Ask, {}, {}, {}) }
}

@Preview(name = "Denied", widthDp = 390, heightDp = 844)
@Composable
private fun AccessDeniedPreview() {
    ScreenPreview(Atmosphere.DuskBloom) { AccessScreen(AccessRequest.Location, PermissionStage.Denied, {}, {}, {}) }
}

@Preview(name = "Granted", widthDp = 390, heightDp = 844)
@Composable
private fun AccessGrantedPreview() {
    ScreenPreview(Atmosphere.DuskBloom) { AccessScreen(AccessRequest.Calendar, PermissionStage.Granted, {}, {}, {}) }
}
