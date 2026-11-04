package com.fivehouse.thewid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.LiveDot
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.FaceValue
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.toFace

enum class PermissionStage { Ask, Denied, Granted }

private val StatusWidgetKind.permissionShape: ShapeCatalog
    get() = if (this == StatusWidgetKind.Alerts) ShapeCatalog.Clover4Leaf else ShapeCatalog.Cookie12Sided

/** Screens 6 (ask), 11 (denied) and 12 (granted). */
@Composable
fun PermissionScreen(
    kind: StatusWidgetKind,
    stage: PermissionStage,
    face: StatusFace?,
    productName: String,
    listedName: String,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = stringResource(kind.label)
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
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        when (stage) {
            PermissionStage.Ask -> AskContent(kind, productName, Modifier.weight(1f))
            PermissionStage.Denied -> DeniedContent(kind, label, listedName, Modifier.weight(1f))
            PermissionStage.Granted -> GrantedContent(kind, label, face, Modifier.weight(1f))
        }
        when (stage) {
            PermissionStage.Ask -> {
                PrimaryPill(stringResource(R.string.permission_open_settings), onOpenSettings)
                TextButton(onClick = onBack) { Text(stringResource(R.string.permission_not_now)) }
            }

            PermissionStage.Denied -> {
                PrimaryPill(stringResource(R.string.permission_open_again), onOpenSettings)
                TextButton(onClick = onBack) { Text(stringResource(R.string.permission_choose_another)) }
            }

            PermissionStage.Granted -> {
                PrimaryPill(stringResource(R.string.customize_add), onAdd)
                TextButton(onClick = onCustomize) { Text(stringResource(R.string.permission_customize_first)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AskContent(kind: StatusWidgetKind, productName: String, modifier: Modifier = Modifier) {
    val isAlerts = kind == StatusWidgetKind.Alerts
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlossyShape(kind.permissionShape, null, Modifier.size(190.dp), castShadow = true) {
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
            stringResource(
                if (isAlerts) R.string.permission_alerts_title else R.string.permission_now_playing_title,
                productName
            ),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(if (isAlerts) R.string.permission_alerts_body else R.string.permission_now_playing_body),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(
                stringResource(
                    if (isAlerts) R.string.permission_alerts_chip_scope else R.string.permission_now_playing_chip_scope
                ),
                icon = R.drawable.ic_check
            )
            InfoChip(stringResource(R.string.permission_chip_local), icon = R.drawable.ic_check)
            InfoChip(
                stringResource(
                    if (isAlerts) R.string.permission_alerts_chip_also else R.string.permission_now_playing_chip_also
                ),
                icon = R.drawable.ic_check
            )
        }
    }
}

@Composable
private fun DeniedContent(kind: StatusWidgetKind, label: String, listedName: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlossyShape(kind.permissionShape, null, Modifier.size(170.dp), surface = ShapeSurface.Dashed) {
            Text("!", style = Numerals.Large)
        }
        Text(
            stringResource(R.string.permission_denied_title, label),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(
                if (kind == StatusWidgetKind.Alerts) {
                    R.string.permission_alerts_denied_body
                } else {
                    R.string.permission_now_playing_denied_body
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FixStep(1, stringResource(R.string.permission_step1))
                FixStep(2, stringResource(R.string.permission_step2, listedName))
                FixStep(3, stringResource(R.string.permission_step3))
            }
        }
    }
}

@Composable
private fun FixStep(number: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GrantedContent(kind: StatusWidgetKind, label: String, face: StatusFace?, modifier: Modifier = Modifier) {
    val isAlerts = kind == StatusWidgetKind.Alerts
    val count = (face?.value as? FaceValue.Literal)?.text?.toIntOrNull()
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            GlossyShape(ShapeCatalog.Burst, null, Modifier.size(260.dp), tint = GlossTint.Accent, fill = 0.25f)
            if (face != null && !face.needsAccess && face.surface == ShapeSurface.Glossy) {
                FaceShape(face, null, Modifier.size(170.dp)) {
                    if (count != null) Text("$count", style = Numerals.Large)
                }
            } else {
                GlossyShape(kind.permissionShape, null, Modifier.size(170.dp))
            }
        }
        Text(
            stringResource(R.string.permission_granted_title, label),
            style = MaterialTheme.typography.headlineLarge
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LiveDot()
            Text(
                when {
                    !isAlerts -> stringResource(R.string.permission_now_playing_ready)
                    count != null -> pluralStringResource(R.plurals.permission_alerts_counting, count, count)
                    else -> stringResource(R.string.permission_alerts_starting)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(
                if (isAlerts) R.string.permission_alerts_granted_also else R.string.permission_now_playing_granted_also
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PermissionPreview(kind: StatusWidgetKind, stage: PermissionStage, face: StatusFace? = null) {
    ScreenPreview(Atmosphere.DuskBloom) {
        PermissionScreen(kind, stage, face, "Shapes", "TheWid", {}, {}, {}, {})
    }
}

@Preview(name = "Alerts ask", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsAskPreview() = PermissionPreview(StatusWidgetKind.Alerts, PermissionStage.Ask)

@Preview(name = "Alerts denied", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsDeniedPreview() = PermissionPreview(StatusWidgetKind.Alerts, PermissionStage.Denied)

@Preview(name = "Alerts granted", widthDp = 390, heightDp = 844)
@Composable
private fun AlertsGrantedPreview() =
    PermissionPreview(StatusWidgetKind.Alerts, PermissionStage.Granted, AlertsStatus.Count(2).toFace())

@Preview(name = "Now playing ask", widthDp = 390, heightDp = 844)
@Composable
private fun NowPlayingAskPreview() = PermissionPreview(StatusWidgetKind.NowPlaying, PermissionStage.Ask)

@Preview(name = "Now playing denied", widthDp = 390, heightDp = 844)
@Composable
private fun NowPlayingDeniedPreview() = PermissionPreview(StatusWidgetKind.NowPlaying, PermissionStage.Denied)

@Preview(name = "Now playing granted", widthDp = 390, heightDp = 844)
@Composable
private fun NowPlayingGrantedPreview() = PermissionPreview(StatusWidgetKind.NowPlaying, PermissionStage.Granted)
