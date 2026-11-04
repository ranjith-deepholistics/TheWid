package com.fivehouse.thewid.ui.widgetconfig

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.screens.AtmosphereSwatches
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeBitmapRenderer
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetAtmosphereKey
import com.fivehouse.thewid.widget.WidgetShapeKey
import com.fivehouse.thewid.widget.WidgetTint
import com.fivehouse.thewid.widget.atmosphere
import com.fivehouse.thewid.widget.resolve
import com.fivehouse.thewid.widget.statusFaces
import com.fivehouse.thewid.widget.toFace
import kotlinx.coroutines.launch

/** Screen 10: quick setup when a widget is added from the launcher's picker, or reconfigured. */
class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Backing out must not leave a half-added widget behind, so cancelled is the default result.
        setResult(RESULT_CANCELED)

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val provider = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider
        val kind = provider
            ?.takeIf { it.packageName == packageName }
            ?.let { StatusWidgetKind.forReceiverClass(it.className) }
        if (kind == null) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ShapesTheme(Atmosphere.CoolMist) {
                WidgetConfigureRoute(
                    kind = kind,
                    appWidgetId = appWidgetId,
                    onDone = {
                        setResult(
                            RESULT_OK,
                            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        )
                        finish()
                    },
                    onDismiss = ::finish
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigureRoute(kind: StatusWidgetKind, appWidgetId: Int, onDone: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glanceId = remember(appWidgetId) { GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId) }
    var chosen by rememberSaveable { mutableStateOf(kind.shapeOptions.firstOrNull()) }
    var atmosphere by rememberSaveable { mutableStateOf(Atmosphere.CoolMist) }

    LaunchedEffect(glanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        val saved = prefs[WidgetShapeKey]?.let { name -> ShapeCatalog.entries.firstOrNull { it.name == name } }
        if (saved != null && saved in kind.shapeOptions) chosen = saved
        atmosphere = prefs.atmosphere()
    }

    val face by produceState<StatusFace?>(initialValue = null, chosen) {
        context.statusFaces().faces(kind, chosen).collect { value = it }
    }

    WidgetConfigureSheet(
        kind = kind,
        face = face,
        chosen = chosen,
        valueText = face?.value?.resolve(context).orEmpty(),
        onShapeChoose = { chosen = it },
        atmosphere = atmosphere,
        onAtmosphereChange = { atmosphere = it },
        onDone = {
            scope.launch {
                updateAppWidgetState(context, glanceId) { prefs ->
                    chosen?.let { prefs[WidgetShapeKey] = it.name }
                    prefs[WidgetAtmosphereKey] = atmosphere.name
                }
                kind.newWidget().update(context, glanceId)
                onDone()
            }
        },
        onDismiss = onDismiss
    )
}

@Composable
fun WidgetConfigureSheet(
    kind: StatusWidgetKind,
    face: StatusFace?,
    chosen: ShapeCatalog?,
    valueText: String,
    onShapeChoose: (ShapeCatalog) -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    atmosphere: Atmosphere = Atmosphere.CoolMist,
    onAtmosphereChange: (Atmosphere) -> Unit = {}
) {
    val label = stringResource(kind.label)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // Taps on the sheet itself mustn't reach the dismissing scrim behind it.
                .clickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
            color = ShapesTheme.atmosphere.stops.last(),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.configure_eyebrow), style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.configure_title, label), style = MaterialTheme.typography.headlineSmall)
                PreviewPanel(
                    label,
                    face?.let { if (it.tint == WidgetTint) it.copy(tint = atmosphere.gloss) else it },
                    valueText
                )
                if (kind.shapeOptions.isNotEmpty()) {
                    Text(stringResource(R.string.configure_shape), style = MaterialTheme.typography.labelSmall)
                    ShapePicker(kind.shapeOptions, chosen, onShapeChoose)
                }
                AtmosphereSwatches(Atmosphere.entries, atmosphere, { if (it != null) onAtmosphereChange(it) })
                Text(
                    stringResource(R.string.configure_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text(stringResource(R.string.configure_done))
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(label: String, face: StatusFace?, valueText: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(ShapesTheme.atmosphere.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassTile(Modifier.size(150.dp)) {
            Text(label, Modifier.align(Alignment.TopStart), style = MaterialTheme.typography.titleSmall)
            Text(valueText, Modifier.align(Alignment.BottomStart), style = Numerals.Medium)
            if (face != null) {
                FaceImage(face, Modifier.align(Alignment.BottomEnd).size(68.dp))
            }
        }
    }
}

@Composable
private fun FaceImage(face: StatusFace, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val bitmap = remember(face, density) {
        ShapeBitmapRenderer.render(
            shape = face.shape,
            sizePx = with(density) { 68.dp.roundToPx() },
            density = density.density,
            tint = face.tint,
            fill = face.fill,
            surface = face.surface,
            clock = face.clock,
            art = face.art,
            expression = face.expression
        ).asImageBitmap()
    }
    Image(bitmap, contentDescription = null, modifier = modifier)
}

@Composable
private fun ShapePicker(
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
                    .background(Color.White)
                    .then(
                        if (selected) {
                            Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), shape)
                        } else {
                            Modifier
                        }
                    )
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(option) }),
                contentAlignment = Alignment.Center
            ) {
                GlossyShape(
                    shape = option,
                    contentDescription = option.name,
                    modifier = Modifier.size(32.dp),
                    tint = if (selected) {
                        GlossTint(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ShapesTheme.atmosphere.gloss
                    }
                )
            }
        }
    }
}

@Preview(name = "Picker", widthDp = 390, heightDp = 700)
@Composable
private fun WidgetConfigureBatteryPreview() {
    ShapesTheme {
        WidgetConfigureSheet(
            kind = StatusWidgetKind.Battery,
            face = BatteryStatus(68, isCharging = true).toFace(),
            chosen = ShapeCatalog.SoftBurst,
            valueText = "68%",
            onShapeChoose = {},
            onDone = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "No picker", widthDp = 390, heightDp = 700)
@Composable
private fun WidgetConfigureSoundPreview() {
    ShapesTheme {
        WidgetConfigureSheet(
            kind = StatusWidgetKind.Sound,
            face = SoundStatus.Silent.toFace(),
            chosen = null,
            valueText = "MUTE",
            onShapeChoose = {},
            onDone = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Loading", widthDp = 390, heightDp = 700)
@Composable
private fun WidgetConfigureLoadingPreview() {
    ShapesTheme {
        WidgetConfigureSheet(
            kind = StatusWidgetKind.Time,
            face = null,
            chosen = null,
            valueText = "",
            onShapeChoose = {},
            onDone = {},
            onDismiss = {}
        )
    }
}
