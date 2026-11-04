package com.fivehouse.thewid.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.resolve
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GlassIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ShapesTheme.atmosphere.isDark
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isDark) 0.12f else 0.7f))
    ) {
        Icon(painterResource(icon), contentDescription, Modifier.size(20.dp))
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassIconButton(R.drawable.ic_back, stringResource(R.string.action_back), onClick, modifier)
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun PrimaryPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text)
    }
}

/** A small rounded label. Pass [icon] for a leading mark, as on the privacy promises. */
@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, border: Color? = null) {
    val isDark = ShapesTheme.atmosphere.isDark
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = if (isDark) 0.12f else 0.75f),
        contentColor = LocalContentColor.current,
        border = border?.let { BorderStroke(1.dp, it) }
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun LiveDot(modifier: Modifier = Modifier, size: Dp = 6.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Accent)
    )
}

/** A status face drawn in-app, with the same painter as its home-screen widget. */
@Composable
fun FaceShape(
    face: StatusFace,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    castShadow: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    GlossyShape(
        shape = face.shape,
        contentDescription = contentDescription,
        modifier = modifier,
        fill = face.fill,
        surface = face.surface,
        tint = face.tint,
        castShadow = castShadow,
        clock = face.clock,
        art = face.art,
        content = content
    )
}

@Composable
fun FaceValueText(face: StatusFace, modifier: Modifier = Modifier, style: TextStyle = Numerals.Medium) {
    Text(face.value.resolve(LocalContext.current), modifier, style = style)
}

/** One choice from a few, as a pill-shaped track. */
@Composable
fun <T> SegmentedPills(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = ShapesTheme.atmosphere.isDark
    Row(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isDark) 0.1f else 0.5f))
            .padding(4.dp)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                )
            }
        }
    }
}

/** A ring of dots from 12 o'clock, solid up to [progress], with an accent dot at its head. */
@Composable
fun DottedRing(progress: () -> Float, modifier: Modifier = Modifier, dotCount: Int = 64) {
    val ink = LocalContentColor.current
    Canvas(modifier) {
        val radius = size.minDimension / 2 - 8.dp.toPx()
        val done = progress().coerceIn(0f, 1f)
        val dotAt = { fraction: Float ->
            val angle = 2 * PI * fraction - PI / 2
            Offset(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat()
            )
        }
        repeat(dotCount) { index ->
            val fraction = index.toFloat() / dotCount
            drawCircle(
                color = if (fraction <= done) ink else ink.copy(alpha = 0.25f),
                radius = 1.5.dp.toPx(),
                center = dotAt(fraction)
            )
        }
        drawCircle(Accent, radius = 5.dp.toPx(), center = dotAt(done))
    }
}

/** A dashed-tile slot marking somewhere to add. */
@Composable
fun OutlineTile(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = MaterialTheme.shapes.extraLarge
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = LocalContentColor.current,
        border = BorderStroke(1.dp, LocalContentColor.current.copy(alpha = 0.25f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
    }
}

/** For previews: a screen on its atmosphere, as the app shell would host it. */
@Composable
fun ScreenPreview(atmosphere: Atmosphere, content: @Composable BoxScope.() -> Unit) {
    ShapesTheme(atmosphere) {
        AtmosphereBackground(Modifier.fillMaxSize(), content = content)
    }
}

@Preview(widthDp = 320, showBackground = true)
@Composable
private fun SegmentedPillsPreview() {
    ShapesTheme {
        AtmosphereBackground {
            SegmentedPills(
                options = listOf("1×1", "2×2", "4×2"),
                selected = "2×2",
                label = { it },
                onSelect = {},
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Preview(widthDp = 200, heightDp = 200, showBackground = true)
@Composable
private fun DottedRingPreview() {
    ShapesTheme { AtmosphereBackground { DottedRing({ 0.7f }, Modifier.size(200.dp)) } }
}

@Preview(showBackground = true)
@Composable
private fun InfoChipPreview() {
    ShapesTheme {
        AtmosphereBackground {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip("No account", icon = R.drawable.ic_check)
                InfoChip("Hero", border = Accent)
            }
        }
    }
}
