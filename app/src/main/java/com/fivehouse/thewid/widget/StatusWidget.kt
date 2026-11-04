package com.fivehouse.thewid.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.text.format.DateFormat
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
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
import androidx.glance.layout.height
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
import com.fivehouse.thewid.ui.shapes.ShapeBitmapRenderer
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.DarkAtmosphereText
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Ink
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

val WidgetShapeKey = stringPreferencesKey("shape")
val WidgetGlowKey = booleanPreferencesKey("glow")
val WidgetAtmosphereKey = stringPreferencesKey("atmosphere")

private val MinTileSide = 110.dp
private val MinWideWidth = 220.dp
private val MinLargeHeight = 220.dp
private val SquareShape = 68.dp
private val WideShape = 100.dp
private val LargeShape = 180.dp
private val StripShape = 20.dp
private val PersonShapeSize = 52.dp
private const val SQUARE_NUMERAL_SP = 24f
private const val WIDE_NUMERAL_SP = 44f
private const val LARGE_NUMERAL_SP = 56f

/** Colours from the widget's chosen atmosphere. */
private data class WidgetLook(val glass: Int, val text: Color, val gloss: GlossTint)

private fun Atmosphere.toLook() = WidgetLook(
    glass = if (isDark) R.drawable.widget_glass_dark else R.drawable.widget_glass,
    text = if (isDark) DarkAtmosphereText else Ink,
    gloss = gloss
)

private val LocalLook = compositionLocalOf { Atmosphere.CoolMist.toLook() }

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
            StatusWidgetContent(kind, face, label, prefs.atmosphere())
        }
    }

    /** What the launcher's widget picker shows, on Android versions that support generated previews. */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val label = context.getString(kind.label)
        provideContent { StatusWidgetContent(kind, sampleFace(kind), label) }
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

class FocusWidget : StatusWidget(StatusWidgetKind.Focus)

fun Preferences.glow(): Boolean = this[WidgetGlowKey] ?: true

fun Preferences.chosenShape(): ShapeCatalog? =
    this[WidgetShapeKey]?.let { saved -> ShapeCatalog.entries.firstOrNull { it.name == saved } }

fun Preferences.atmosphere(): Atmosphere =
    this[WidgetAtmosphereKey]?.let { saved -> Atmosphere.entries.firstOrNull { it.name == saved } }
        ?: Atmosphere.CoolMist

enum class WidgetLayout { ShapeOnly, Square, Wide, Large }

/** 1x1 cells get the shape alone, as the spec asks. Launcher cell sizes vary, so this goes by real size. */
fun widgetLayoutFor(size: DpSize): WidgetLayout = when {
    size.width < MinTileSide || size.height < MinTileSide -> WidgetLayout.ShapeOnly
    size.width >= MinWideWidth && size.height >= MinLargeHeight -> WidgetLayout.Large
    size.width >= MinWideWidth -> WidgetLayout.Wide
    else -> WidgetLayout.Square
}

val OpenKindKey = ActionParameters.Key<String>(EXTRA_OPEN_KIND)

@Composable
fun StatusWidgetContent(
    kind: StatusWidgetKind,
    face: StatusFace,
    label: String,
    atmosphere: Atmosphere = Atmosphere.CoolMist
) {
    val open = GlanceModifier.clickable(
        when {
            face.rollable -> actionRunCallback<RollDecisionAction>()

            kind.opensOwnScreen ->
                actionStartActivity<MainActivity>(actionParametersOf(OpenKindKey to kind.name))

            else -> actionStartActivity<MainActivity>()
        }
    )
    val size = LocalSize.current
    CompositionLocalProvider(LocalLook provides atmosphere.toLook()) {
        val people = face.people
        if (people != null) {
            PeopleLayout(people, label, size, open)
        } else {
            when (widgetLayoutFor(size)) {
                WidgetLayout.Large -> LargeLayout(face, label, open)

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
    }
}

@Composable
private fun SquareLayout(face: StatusFace, label: String, modifier: GlanceModifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(ImageProvider(LocalLook.current.glass))
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
            .background(ImageProvider(LocalLook.current.glass))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(GlanceModifier.defaultWeight()) {
            LabelRow(face, label)
            if (!face.valueInside) {
                Spacer(GlanceModifier.size(8.dp))
                FaceValueView(face, label, WIDE_NUMERAL_SP)
            }
            Details(face)
        }
        if (face.strip != null) {
            WeekStrip(face.strip)
        } else {
            ShapeWithValue(face, label, null, WideShape, WIDE_NUMERAL_SP, GlanceModifier.size(WideShape))
        }
    }
}

/** 4x4 and up: the shape as the hero, with the value beneath it. */
@Composable
private fun LargeLayout(face: StatusFace, label: String, modifier: GlanceModifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(ImageProvider(LocalLook.current.glass))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LabelRow(face, label)
        Spacer(GlanceModifier.defaultWeight())
        ShapeWithValue(face, label, null, LargeShape, LARGE_NUMERAL_SP, GlanceModifier.size(LargeShape))
        Spacer(GlanceModifier.defaultWeight())
        if (!face.valueInside) FaceValueView(face, label, WIDE_NUMERAL_SP)
        Details(face)
        if (face.strip != null) WeekStrip(face.strip)
    }
}

/** The day track and footnote, where a face has them. */
@Composable
private fun Details(face: StatusFace) {
    val context = LocalContext.current
    val look = LocalLook.current
    face.track?.let { progress ->
        Spacer(GlanceModifier.height(6.dp))
        val bitmap = remember(progress, look) {
            val density = context.resources.displayMetrics.density
            dayTrackBitmap(progress, (TRACK_WIDTH_DP * density).toInt(), density, look.text.toArgb())
        }
        Image(ImageProvider(bitmap), contentDescription = null)
    }
    face.footnote?.let { note ->
        Spacer(GlanceModifier.height(2.dp))
        Text(note.resolve(context), style = TextStyle(color = ColorProvider(look.text)), maxLines = 1)
    }
}

private const val TRACK_WIDTH_DP = 120
private const val TRACK_DOTS = 24

private fun dayTrackBitmap(progress: Float, widthPx: Int, density: Float, color: Int): Bitmap {
    val radius = density * 1.25f
    val bitmap = createBitmap(widthPx.coerceAtLeast(1), (radius * 2).toInt().coerceAtLeast(1) + 1)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    repeat(TRACK_DOTS) { index ->
        val fraction = index.toFloat() / (TRACK_DOTS - 1)
        paint.color = if (fraction <= progress) Accent.toArgb() else color and 0x40FFFFFF
        canvas.drawCircle(radius + fraction * (widthPx - 2 * radius), radius, radius, paint)
    }
    return bitmap
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
    val look = LocalLook.current
    if (layout == WidgetLayout.ShapeOnly) {
        PersonMark(people.first(), 0, minOf(size.width, size.height), open.fillMaxSize())
        return
    }
    Column(
        open
            .fillMaxSize()
            .background(ImageProvider(look.glass))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = TextStyle(color = ColorProvider(look.text), fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.size(6.dp))
        val perRow = if (layout == WidgetLayout.Square) 2 else MAX_PEOPLE
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
    val look = LocalLook.current
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
            style = TextStyle(color = ColorProvider(look.text), fontWeight = FontWeight.Medium),
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
        FaceValueView(face, label, numeralSp, Ink)
    }
}

@Composable
private fun FaceValueView(face: StatusFace, label: String, sizeSp: Float, color: Color = LocalLook.current.text) {
    val context = LocalContext.current
    when (val value = face.value) {
        FaceValue.LiveClock -> LiveText(R.layout.widget_clock, R.id.widget_clock, sizeSp, color)

        FaceValue.LiveClockSeconds -> LiveText(R.layout.widget_clock_seconds, R.id.widget_clock, sizeSp, color)

        is FaceValue.Countdown -> Countdown(value, sizeSp, color)

        else -> {
            val text = value.resolve(context)
            val bitmap = remember(text, sizeSp, color) {
                val sizePx = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    sizeSp,
                    context.resources.displayMetrics
                )
                NumeralBitmapRenderer.render(context.resources, text, sizePx, color.toArgb())
            }
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "$label, $text"
            )
        }
    }
}

/** A TextClock keeps running in the launcher, so the digits stay right between 15-minute updates. */
@Composable
private fun LiveText(layout: Int, id: Int, sizeSp: Float, color: Color) {
    val context = LocalContext.current
    val views = remember(layout, sizeSp, color) {
        RemoteViews(context.packageName, layout).apply {
            setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(id, color.toArgb())
        }
    }
    AndroidRemoteViews(views)
}

/** A Chronometer counts down in the launcher with no updates from the app. */
@Composable
private fun Countdown(value: FaceValue.Countdown, sizeSp: Float, color: Color) {
    val context = LocalContext.current
    val views = remember(value, sizeSp, color) {
        RemoteViews(context.packageName, R.layout.widget_countdown).apply {
            setTextViewTextSize(R.id.widget_countdown, TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(R.id.widget_countdown, color.toArgb())
            setChronometerCountDown(R.id.widget_countdown, true)
            setChronometer(
                R.id.widget_countdown,
                SystemClock.elapsedRealtime() + value.leftMs,
                null,
                value.isRunning
            )
        }
    }
    AndroidRemoteViews(views)
}

@Composable
private fun ShapeImage(face: StatusFace, description: String?, size: Dp, modifier: GlanceModifier) {
    val resources = LocalContext.current.resources
    val look = LocalLook.current
    val bitmap = remember(face, size, look) {
        val density = resources.displayMetrics.density
        ShapeBitmapRenderer.render(
            shape = face.shape,
            sizePx = (size.value * density).toInt().coerceAtLeast(1),
            density = density,
            tint = if (face.tint == WidgetTint) look.gloss else face.tint,
            fill = face.fill,
            surface = face.surface,
            outlineColor = look.text,
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
    return "$label, ${face.value.resolve(context)}"
}

fun FaceValue.resolve(context: Context): String = when (this) {
    is FaceValue.Literal -> text

    is FaceValue.Resource -> context.getString(id)

    is FaceValue.Plural -> context.resources.getQuantityString(id, count, count)

    is FaceValue.Format -> context.getString(id, *args.toTypedArray())

    is FaceValue.Countdown -> formatCountdown(leftMs)

    FaceValue.LiveClock -> DateFormat.getTimeFormat(context).format(Date())

    FaceValue.LiveClockSeconds -> DateFormat.getTimeFormat(context).format(Date())
}

/** Minutes and seconds, rounding up so a timer shows 25:00 at its very start. */
fun formatCountdown(leftMs: Long): String {
    val seconds = (leftMs.coerceAtLeast(0) + 999) / 1_000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 120, heightDp = 120)
@Composable
private fun SquareWidgetPreview() {
    Box(GlanceModifier.size(120.dp)) { SquareLayout(sampleFace(StatusWidgetKind.Battery), "Battery", GlanceModifier) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 120)
@Composable
private fun WideWidgetPreview() {
    Box(GlanceModifier.size(250.dp, 120.dp)) { WideLayout(sampleFace(StatusWidgetKind.Time), "Time", GlanceModifier) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 250, heightDp = 250)
@Composable
private fun LargeWidgetPreview() {
    Box(GlanceModifier.size(250.dp)) { LargeLayout(sampleFace(StatusWidgetKind.Mochi), "Mochi", GlanceModifier) }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 60, heightDp = 60)
@Composable
private fun ShapeOnlyPreview() {
    ShapeImage(sampleFace(StatusWidgetKind.Sound), null, 60.dp, GlanceModifier.fillMaxSize())
}
