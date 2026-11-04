package com.fivehouse.thewid.widget

import android.content.Context
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fivehouse.thewid.MainActivity
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.status.BatteryStatus
import com.fivehouse.thewid.data.status.CellularNetwork
import com.fivehouse.thewid.data.status.CellularStatus
import com.fivehouse.thewid.data.status.SoundStatus
import com.fivehouse.thewid.data.status.TimeStatus
import com.fivehouse.thewid.data.status.WifiStatus
import com.fivehouse.thewid.ui.shapes.ShapeBitmapRenderer
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Ink
import java.time.ZonedDateTime
import java.util.Date
import kotlinx.coroutines.flow.first

val WidgetShapeKey = stringPreferencesKey("shape")
val WidgetGlowKey = booleanPreferencesKey("glow")

private val MinTileSide = 110.dp
private val MinWideWidth = 220.dp
private val SquareShape = 68.dp
private val WideShape = 100.dp
private const val SQUARE_NUMERAL_SP = 24f
private const val WIDE_NUMERAL_SP = 44f

/**
 * One subclass per kind, because Glance finds a widget's instances by class. If the kinds shared a
 * class, `updateAll` on one kind would redraw every placed widget as that kind.
 */
sealed class StatusWidget(private val kind: StatusWidgetKind) : GlanceAppWidget() {
    // Exact sends one layout per orientation at its true size. Responsive would send every layout's
    // bitmaps in a single update, which risks Android's roughly 1 MB limit on high-density screens.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val faces = context.statusFaces()
        val label = context.getString(kind.label)
        val saved = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        // Read a first value up front so the widget never composes an empty placeholder.
        val initial = faces.faces(kind, saved.chosenShape(), saved.glow()).first()
        provideContent {
            val prefs = currentState<Preferences>()
            val shape = prefs.chosenShape()
            val glow = prefs.glow()
            val flow = remember(shape, glow) { faces.faces(kind, shape, glow) }
            val face by flow.collectAsState(initial)
            StatusWidgetContent(face, label)
        }
    }
}

class BatteryWidget : StatusWidget(StatusWidgetKind.Battery)

class CellularWidget : StatusWidget(StatusWidgetKind.Cellular)

class WifiWidget : StatusWidget(StatusWidgetKind.Wifi)

class SoundWidget : StatusWidget(StatusWidgetKind.Sound)

class AlarmWidget : StatusWidget(StatusWidgetKind.Alarm)

class TimeWidget : StatusWidget(StatusWidgetKind.Time)

fun Preferences.glow(): Boolean = this[WidgetGlowKey] ?: true

fun Preferences.chosenShape(): ShapeCatalog? =
    this[WidgetShapeKey]?.let { saved -> ShapeCatalog.entries.firstOrNull { it.name == saved } }

enum class WidgetLayout { ShapeOnly, Square, Wide }

/** 1x1 cells get the shape alone, as the spec asks. Launcher cell sizes vary, so this goes by real size. */
fun widgetLayoutFor(size: DpSize): WidgetLayout = when {
    size.width < MinTileSide || size.height < MinTileSide -> WidgetLayout.ShapeOnly
    size.width >= MinWideWidth -> WidgetLayout.Wide
    else -> WidgetLayout.Square
}

@Composable
fun StatusWidgetContent(face: StatusFace, label: String) {
    val open = GlanceModifier.clickable(actionStartActivity<MainActivity>())
    val size = LocalSize.current
    when (widgetLayoutFor(size)) {
        WidgetLayout.Wide -> WideLayout(face, label, open)

        WidgetLayout.Square -> SquareLayout(face, label, open)

        WidgetLayout.ShapeOnly -> ShapeImage(
            face,
            faceDescription(face, label),
            minOf(size.width, size.height),
            open.fillMaxSize()
        )
    }
}

@Composable
private fun SquareLayout(face: StatusFace, label: String, modifier: GlanceModifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_glass))
            .padding(14.dp)
    ) {
        LabelRow(label, face.isLive)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            FaceValueView(face, label, SQUARE_NUMERAL_SP)
            Spacer(GlanceModifier.defaultWeight())
            ShapeImage(face, null, SquareShape, GlanceModifier.size(SquareShape))
        }
    }
}

@Composable
private fun WideLayout(face: StatusFace, label: String, modifier: GlanceModifier) {
    Row(
        modifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_glass))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(GlanceModifier.defaultWeight()) {
            LabelRow(label, face.isLive)
            Spacer(GlanceModifier.size(8.dp))
            FaceValueView(face, label, WIDE_NUMERAL_SP)
        }
        ShapeImage(face, null, WideShape, GlanceModifier.size(WideShape))
    }
}

@Composable
private fun LabelRow(label: String, isLive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLive) {
            Image(
                provider = ImageProvider(R.drawable.widget_live_dot),
                contentDescription = LocalContext.current.getString(R.string.widget_live),
                modifier = GlanceModifier.size(6.dp),
                colorFilter = ColorFilter.tint(ColorProvider(Accent))
            )
            Spacer(GlanceModifier.width(6.dp))
        }
        Text(label, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun FaceValueView(face: StatusFace, label: String, sizeSp: Float) {
    val context = LocalContext.current
    when (val value = face.value) {
        FaceValue.LiveClock -> LiveClock(sizeSp)

        else -> {
            val text = value.resolve(context)
            val bitmap = remember(text, sizeSp) {
                val sizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    sizeSp,
                    context.resources.displayMetrics
                )
                NumeralBitmapRenderer.render(context.resources, text, sizePx, Ink.toArgb())
            }
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "$label, $text"
            )
        }
    }
}

/** TextClock keeps running in the launcher, so the digits stay right between 15-minute updates. */
@Composable
private fun LiveClock(sizeSp: Float) {
    val context = LocalContext.current
    val views = remember(sizeSp) {
        RemoteViews(context.packageName, R.layout.widget_clock).apply {
            setTextViewTextSize(R.id.widget_clock, TypedValue.COMPLEX_UNIT_SP, sizeSp)
        }
    }
    AndroidRemoteViews(views)
}

@Composable
private fun ShapeImage(face: StatusFace, description: String?, size: Dp, modifier: GlanceModifier) {
    val resources = LocalContext.current.resources
    val bitmap = remember(face, size) {
        val density = resources.displayMetrics.density
        ShapeBitmapRenderer.render(
            shape = face.shape,
            sizePx = (size.value * density).toInt().coerceAtLeast(1),
            density = density,
            tint = face.tint,
            fill = face.fill,
            surface = face.surface,
            clock = face.clock
        )
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = description,
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun faceDescription(face: StatusFace, label: String): String {
    val context = LocalContext.current
    return when (val value = face.value) {
        FaceValue.LiveClock -> label
        else -> "$label, ${value.resolve(context)}"
    }
}

fun FaceValue.resolve(context: Context): String = when (this) {
    is FaceValue.Literal -> text
    is FaceValue.Resource -> context.getString(id)
    FaceValue.LiveClock -> DateFormat.getTimeFormat(context).format(Date())
}

private val PreviewFaces = listOf(
    BatteryStatus(68, isCharging = true).toFace(),
    CellularStatus.Signal(3, CellularNetwork.FiveG).toFace(),
    WifiStatus.Off.toFace(),
    SoundStatus.Silent.toFace(),
    TimeStatus(ZonedDateTime.parse("2026-11-02T09:42:00+05:30"), isDay = true).toFace()
)

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 120, heightDp = 120)
@Composable
private fun SquareWidgetPreview() {
    Box(GlanceModifier.size(120.dp)) {
        SquareLayout(PreviewFaces[0], "Battery", GlanceModifier)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 120)
@Composable
private fun WideWidgetPreview() {
    Box(GlanceModifier.size(250.dp, 120.dp)) {
        WideLayout(PreviewFaces[1], "Cellular", GlanceModifier)
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 60, heightDp = 60)
@Composable
private fun ShapeOnlyOffPreview() {
    ShapeImage(PreviewFaces[2], null, 60.dp, GlanceModifier.fillMaxSize())
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 60, heightDp = 60)
@Composable
private fun ShapeOnlySilentPreview() {
    ShapeImage(PreviewFaces[3], null, 60.dp, GlanceModifier.fillMaxSize())
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 60, heightDp = 60)
@Composable
private fun ShapeOnlyTimePreview() {
    ShapeImage(PreviewFaces[4], null, 60.dp, GlanceModifier.fillMaxSize())
}
