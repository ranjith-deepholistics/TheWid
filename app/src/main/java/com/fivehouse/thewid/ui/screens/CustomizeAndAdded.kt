package com.fivehouse.thewid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.FaceValueText
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.LiveDot
import com.fivehouse.thewid.ui.components.OutlineTile
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.components.SegmentedPills
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Ink
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetTint
import com.fivehouse.thewid.widget.toFace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

enum class PreviewSize(val label: Int) {
    One(R.string.size_1x1),
    Two(R.string.size_2x2),
    Wide(R.string.size_4x2),
    Large(R.string.size_4x4)
}

/** Screen 5. */
@Composable
fun CustomizeScreen(
    kind: StatusWidgetKind,
    face: StatusFace?,
    shape: ShapeCatalog?,
    size: PreviewSize,
    glow: Boolean,
    onBack: () -> Unit,
    onShapeChange: (ShapeCatalog) -> Unit,
    onSizeChange: (PreviewSize) -> Unit,
    onGlowChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = Atmosphere.CoolMist,
    onAtmosphereChange: (Atmosphere) -> Unit = {},
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BackButton(onBack, Modifier.align(Alignment.CenterStart))
                Text(stringResource(kind.label), style = MaterialTheme.typography.titleMedium)
            }
            CustomizePreview(kind, face?.inAtmosphere(atmosphere), size)
            if (kind.shapeOptions.isNotEmpty()) {
                Text(
                    stringResource(R.string.customize_shape_named, shape?.name.orEmpty().uppercase()),
                    style = MaterialTheme.typography.labelSmall
                )
                ShapeOptions(kind.shapeOptions, shape, onShapeChange)
            }
            Text(
                stringResource(R.string.customize_atmosphere_named, atmosphere.name.uppercase()),
                style = MaterialTheme.typography.labelSmall
            )
            AtmosphereSwatches(Atmosphere.entries, atmosphere, { if (it != null) onAtmosphereChange(it) })
            SegmentedPills(
                options = PreviewSize.entries,
                selected = size,
                label = { stringResource(it.label) },
                onSelect = onSizeChange,
                modifier = Modifier.fillMaxWidth()
            )
            Text(stringResource(R.string.customize_size_hint), style = MaterialTheme.typography.bodySmall)
            if (kind == StatusWidgetKind.Battery) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.customize_glow), Modifier.weight(1f))
                    Switch(checked = glow, onCheckedChange = onGlowChange)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp))
            Spacer(Modifier.size(4.dp))
            Text(
                stringResource(
                    if (kind.needsNotificationAccess) {
                        R.string.customize_uses_notification_access
                    } else {
                        R.string.customize_no_permission
                    }
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (onSecondary != null && secondaryLabel != null) {
            TextButton(onClick = onSecondary, modifier = Modifier.fillMaxWidth()) { Text(secondaryLabel) }
        }
        PrimaryPill(stringResource(R.string.customize_add), onAdd)
    }
}

private fun StatusFace.inAtmosphere(atmosphere: Atmosphere) =
    if (tint == WidgetTint) copy(tint = atmosphere.gloss) else this

@Composable
private fun CustomizePreview(
    kind: StatusWidgetKind,
    face: StatusFace?,
    size: PreviewSize,
    modifier: Modifier = Modifier
) {
    GlassTile(
        modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Row(
            Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (face?.isLive == true) LiveDot()
            Text(
                "${stringResource(kind.label)} · ${stringResource(size.label)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (face == null) return@GlassTile
        val shapeSize: Dp = if (size == PreviewSize.One) 120.dp else 168.dp
        FaceShape(face, stringResource(kind.label), Modifier.align(Alignment.Center).size(shapeSize))
        if (size != PreviewSize.One) {
            FaceValueText(face, Modifier.align(Alignment.BottomStart), style = Numerals.Large)
        }
    }
}

@Composable
private fun ShapeOptions(
    options: List<ShapeCatalog>,
    chosen: ShapeCatalog?,
    onChoose: (ShapeCatalog) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            val selected = option == chosen
            val shape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(shape)
                    .background(Color.White.copy(alpha = if (selected) 0.25f else 0.1f))
                    .then(if (selected) Modifier.border(1.dp, Color.White, shape) else Modifier)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(option) }),
                contentAlignment = Alignment.Center
            ) {
                GlossyShape(option, option.name, Modifier.size(32.dp))
            }
        }
    }
}

/** Screen 7: the home screen as it now looks, confirming the placement. */
@Composable
fun AddedScreen(
    kind: StatusWidgetKind,
    face: StatusFace?,
    hero: StatusWidgetKind,
    heroFace: StatusFace?,
    onMakeHero: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.added_title), style = MaterialTheme.typography.headlineLarge)
            if (hero != kind) {
                GlassTile(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(hero.label), style = MaterialTheme.typography.bodySmall)
                            heroFace?.let { FaceValueText(it, style = Numerals.Large) }
                        }
                        heroFace?.let { FaceShape(it, null, Modifier.size(96.dp)) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NewWidgetTile(kind, face, Modifier.weight(1f))
                OutlineTile(
                    onDone,
                    Modifier
                        .weight(1f)
                        .height(170.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_plus), stringResource(R.string.nav_add))
                }
            }
        }
        AddedSnack(
            hero == kind,
            onMakeHero,
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun NewWidgetTile(kind: StatusWidgetKind, face: StatusFace?, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraLarge
    GlassTile(
        modifier
            .height(170.dp)
            .border(1.5.dp, Accent, shape)
    ) {
        Text(stringResource(kind.label), Modifier.align(Alignment.TopStart), style = MaterialTheme.typography.bodySmall)
        Text(
            stringResource(R.string.added_new),
            Modifier
                .align(Alignment.TopEnd)
                .clip(CircleShape)
                .background(Ink)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
        if (face != null) {
            FaceValueText(face, Modifier.align(Alignment.BottomStart))
            FaceShape(face, null, Modifier.align(Alignment.BottomEnd).size(68.dp))
        }
    }
}

@Composable
private fun AddedSnack(isHero: Boolean, onMakeHero: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Ink)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(20.dp), tint = Accent)
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.added_snack_title),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.added_snack_body),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (!isHero) {
            FilledTonalButton(onClick = onMakeHero) { Text(stringResource(R.string.added_make_hero)) }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CustomizeBatteryPreview() {
    ScreenPreview(Atmosphere.Moss) {
        CustomizeScreen(
            StatusWidgetKind.Battery,
            BatteryStatus(68, isCharging = true).toFace(),
            ShapeCatalog.SoftBurst,
            PreviewSize.Two,
            glow = true,
            onBack = {},
            onShapeChange = {},
            onSizeChange = {},
            onGlowChange = {},
            onAdd = {}
        )
    }
}

@Preview(name = "No shape choice", widthDp = 390, heightDp = 844)
@Composable
private fun CustomizeTimePreview() {
    ScreenPreview(Atmosphere.Moss) {
        CustomizeScreen(
            StatusWidgetKind.Time,
            PreviewFacesMap[StatusWidgetKind.Time],
            null,
            PreviewSize.One,
            glow = true,
            onBack = {},
            onShapeChange = {},
            onSizeChange = {},
            onGlowChange = {},
            onAdd = {}
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AddedPreview() {
    ScreenPreview(Atmosphere.Peach) {
        AddedScreen(
            StatusWidgetKind.Battery,
            PreviewFacesMap[StatusWidgetKind.Battery],
            StatusWidgetKind.Time,
            PreviewFacesMap[StatusWidgetKind.Time],
            {},
            {}
        )
    }
}

@Preview(name = "Already the hero", widthDp = 390, heightDp = 844)
@Composable
private fun AddedAsHeroPreview() {
    ScreenPreview(Atmosphere.Peach) {
        AddedScreen(
            StatusWidgetKind.Time,
            PreviewFacesMap[StatusWidgetKind.Time],
            StatusWidgetKind.Time,
            PreviewFacesMap[StatusWidgetKind.Time],
            {},
            {}
        )
    }
}
