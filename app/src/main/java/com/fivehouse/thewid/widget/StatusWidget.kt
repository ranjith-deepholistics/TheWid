package com.fivehouse.thewid.widget

import android.content.Context
import android.content.Intent
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
import androidx.core.net.toUri
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
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import com.fivehouse.thewid.EXTRA_OPEN_KIND
import com.fivehouse.thewid.MainActivity
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.MAX_PEOPLE
import com.fivehouse.thewid.data.personal.Person
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
private val StripShape = 20.dp
private val PersonShapeSize = 52.dp
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
            StatusWidgetContent(kind, face, label)
        }
    }
}

class BatteryWidget : StatusWidget(StatusWidgetKind.Battery)

class CellularWidget : StatusWidget(StatusWidgetKind.Cellular)

class WifiWidget : StatusWidget(StatusWidgetKind.Wifi)

class SoundWidget : StatusWidget(StatusWidgetKind.Sound)

class AlarmWidget : StatusWidget(StatusWidgetKind.Alarm)

class TimeWidget : StatusWidget(StatusWidgetKind.Time)

class AlertsWidget : StatusWidget(StatusWidgetKind.Alerts)

class NowPlayingWidget : StatusWidget(StatusWidgetKind.NowPlaying)

class WeekWidget : StatusWidget(StatusWidgetKind.Week)

class InnerCircleWidget : StatusWidget(StatusWidgetKind.InnerCircle)

class DecideWidget : StatusWidget(StatusWidgetKind.Decide)

class MochiWidget : StatusWidget(StatusWidgetKind.Mochi)

class BreatheWidget : StatusWidget(StatusWidgetKind.Breathe)

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

val OpenKindKey = ActionParameters.Key<String>(EXTRA_OPEN_KIND)

@Composable
fun StatusWidgetContent(kind: StatusWidgetKind, face: StatusFace, label: String) {
    val open = GlanceModifier.clickable(
        when {
            face.rollable -> actionRunCallback<RollDecisionAction>()

            kind.needsSetup || kind == StatusWidgetKind.Breathe ->
                actionStartActivity<MainActivity>(actionParametersOf(OpenKindKey to kind.name))

            else -> actionStartActivity<MainActivity>()
        }
    )
    val size = LocalSize.current
    val people = face.people
    if (people != null) {
        PeopleLayout(people, label, size, open)
        return
    }
    when (widgetLayoutFor(size)) {
        WidgetLayout.Wide -> WideLayout(face, label, open)

        WidgetLayout.Square -> SquareLayout(face, label, open)

        WidgetLayout.ShapeOnly -> ShapeWithValue(
            face,
            label,
            faceDescription(face, label),
            minOf(size.width, size.height),
            SQUARE_NUMERAL_SP,
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
        LabelRow(face, label)
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            if (!face.valueInside) FaceValueView(face, label, SQUARE_NUMERAL_SP)
            Spacer(GlanceModifier.defaultWeight())
            ShapeWithValue(face, label, null, SquareShape, SQUARE_NUMERAL_SP, GlanceModifier.size(SquareShape))
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
            LabelRow(face, label)
            if (!face.valueInside) {
                Spacer(GlanceModifier.size(8.dp))
                FaceValueView(face, label, WIDE_NUMERAL_SP)
            }
        }
        if (face.strip != null) {
            WeekStrip(face.strip)
        } else {
            ShapeWithValue(face, label, null, WideShape, WIDE_NUMERAL_SP, GlanceModifier.size(WideShape))
        }
    }
}

/** The current week as one small shape per day. */
@Composable
private fun WeekStrip(strip: List<ShapeCatalog>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        strip.forEach { shape ->
            ShapeImage(StatusFace(shape, FaceValue.Literal("")), null, StripShape, GlanceModifier.size(StripShape))
        }
    }
}

/** Each person opens their own contact card. Square and wide show up to four, a 1x1 just the first. */
@Composable
private fun PeopleLayout(people: List<Person>, label: String, size: DpSize, open: GlanceModifier) {
    val layout = widgetLayoutFor(size)
    if (layout == WidgetLayout.ShapeOnly) {
        PersonMark(people.first(), 0, minOf(size.width, size.height), open.fillMaxSize())
        return
    }
    Column(
        open
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_glass))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.size(6.dp))
        val perRow = if (layout == WidgetLayout.Wide) MAX_PEOPLE else 2
        people.chunked(perRow).forEachIndexed { rowIndex, row ->
            Row {
                row.forEachIndexed { index, person ->
                    PersonMark(
                        person,
                        rowIndex * perRow + index,
                        PersonShapeSize,
                        GlanceModifier
                            .size(PersonShapeSize)
                            .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, person.lookupUri.toUri())))
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonMark(person: Person, index: Int, size: Dp, modifier: GlanceModifier) {
    val face = StatusFace(person.shape, FaceValue.Literal(""), tint = PersonTints[index % PersonTints.size])
    Box(modifier, contentAlignment = Alignment.Center) {
        ShapeImage(face, person.name, size, GlanceModifier.fillMaxSize())
        Text(person.initial, style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Normal))
    }
}

/** Shows the track title in place of the widget's name when there is one. */
@Composable
private fun LabelRow(face: StatusFace, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (face.isLive) {
            Image(
                provider = ImageProvider(R.drawable.widget_live_dot),
                contentDescription = LocalContext.current.getString(R.string.widget_live),
                modifier = GlanceModifier.size(6.dp),
                colorFilter = ColorFilter.tint(ColorProvider(Accent))
            )
            Spacer(GlanceModifier.width(6.dp))
        }
        if (face.needsAccess) {
            Image(
                provider = ImageProvider(R.drawable.ic_lock),
                contentDescription = LocalContext.current.getString(R.string.widget_needs_access),
                modifier = GlanceModifier.size(14.dp),
                colorFilter = ColorFilter.tint(ColorProvider(Accent))
            )
            Spacer(GlanceModifier.width(4.dp))
        }
        Text(
            face.caption ?: label,
            style = TextStyle(color = ColorProvider(Ink), fontWeight = FontWeight.Medium),
            maxLines = 1
        )
    }
}

/** The shape, with its value on top for faces that keep the number inside, such as Alerts. */
@Composable
private fun ShapeWithValue(
    face: StatusFace,
    label: String,
    description: String?,
    size: Dp,
    numeralSp: Float,
    modifier: GlanceModifier
) {
    if (!face.valueInside) {
        ShapeImage(face, description, size, modifier)
        return
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        ShapeImage(face, description, size, GlanceModifier.fillMaxSize())
        FaceValueView(face, label, numeralSp)
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
            clock = face.clock,
            art = face.art,
            expression = face.expression
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
    is FaceValue.Plural -> context.resources.getQuantityString(id, count, count)
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
