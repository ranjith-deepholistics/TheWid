package com.fivehouse.thewid.ui.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.AtmosphereBackground
import com.fivehouse.thewid.ui.components.FrostedGlassState
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.frostedGlassSource
import com.fivehouse.thewid.ui.components.rememberFrostedGlassState
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeBitmapRenderer
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.GlossTint
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.ui.theme.ShapesTheme

private const val GRID_COLUMNS = 4
private val SpikeProgression = listOf(ShapeCatalog.Clover4Leaf, ShapeCatalog.Burst, ShapeCatalog.Boom)

@Composable
fun FoundationCatalogRoute(modifier: Modifier = Modifier) {
    var atmosphere by rememberSaveable { mutableStateOf(Atmosphere.CoolMist) }
    var fill by rememberSaveable { mutableFloatStateOf(0.68f) }
    var morphed by rememberSaveable { mutableStateOf(false) }

    ShapesTheme(atmosphere) {
        FoundationCatalogScreen(
            fill = fill,
            morphed = morphed,
            onAtmosphereChange = { atmosphere = it },
            onFillChange = { fill = it },
            onMorphClick = { morphed = !morphed },
            modifier = modifier
        )
    }
}

/** Phase 1's review surface: every shape, rule and token. Reads the atmosphere from [ShapesTheme]. */
@Composable
fun FoundationCatalogScreen(
    fill: Float,
    morphed: Boolean,
    onAtmosphereChange: (Atmosphere) -> Unit,
    onFillChange: (Float) -> Unit,
    onMorphClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = rememberFrostedGlassState()
    AtmosphereBackground(modifier.fillMaxSize(), glass = glass) {
        LazyColumn(
            contentPadding = WindowInsets.safeDrawing.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "title") {
                Text(
                    stringResource(R.string.catalog_title),
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.displaySmall
                )
            }
            item(key = "atmospheres") { AtmospherePicker(onAtmosphereChange) }
            section("type", R.string.catalog_type) { TypeSpecimen() }
            section("fill", R.string.catalog_rule_fill) { FillRule(fill, onFillChange) }
            section("outline", R.string.catalog_rule_outline) { OutlineRule() }
            section("spike", R.string.catalog_rule_spike) { SpikeRule() }
            section("morph", R.string.catalog_rule_morph) { MorphRule(morphed, onMorphClick) }
            section("glass", R.string.catalog_glass) { GlassSample(glass) }
            section("bitmap", R.string.catalog_bitmap) { BitmapParity(fill) }
            section("pills", R.string.catalog_pills) { PillSample() }
            item(key = "shapes_title") {
                SectionTitle(R.string.catalog_all_shapes, Modifier.padding(horizontal = 20.dp))
            }
            items(
                items = ShapeCatalog.entries.chunked(GRID_COLUMNS),
                key = { row -> row.first().name }
            ) { row ->
                ShapeGridRow(row, Modifier.padding(horizontal = 20.dp))
            }
        }
    }
}

private fun LazyListScope.section(key: String, @StringRes title: Int, content: @Composable () -> Unit) {
    item(key = key) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(stringResource(title), modifier, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun AtmospherePicker(onAtmosphereChange: (Atmosphere) -> Unit, modifier: Modifier = Modifier) {
    val selected = ShapesTheme.atmosphere
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(Atmosphere.entries, key = { it.name }) { atmosphere ->
            FilterChip(
                selected = atmosphere == selected,
                onClick = { onAtmosphereChange(atmosphere) },
                label = { Text(atmosphere.name) }
            )
        }
    }
}

@Composable
private fun TypeSpecimen(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("09:42", style = Numerals.Hero)
        Text("68%  5G  7:20", style = Numerals.Medium)
        listOf(200, 300, 400, 500).forEach { weight ->
            Text(
                stringResource(R.string.catalog_flex_weight, weight),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight(weight))
            )
        }
    }
}

@Composable
private fun FillRule(fill: Float, onFillChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlossyShape(
                shape = ShapeCatalog.SoftBurst,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                fill = fill,
                tint = GlossTint.Accent,
                castShadow = true
            )
            Text("${(fill * 100).toInt()}%", style = Numerals.Large)
        }
        Slider(value = fill, onValueChange = onFillChange)
    }
}

@Composable
private fun OutlineRule(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ShapeSurface.entries.forEach { surface ->
            LabelledShape(surface.name) {
                GlossyShape(ShapeCatalog.Fan, null, Modifier.size(80.dp), surface = surface)
            }
        }
    }
}

@Composable
private fun SpikeRule(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SpikeProgression.forEach { shape ->
            LabelledShape(shape.name) {
                GlossyShape(shape, null, Modifier.size(80.dp))
            }
        }
    }
}

@Composable
private fun MorphRule(morphed: Boolean, onMorphClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        GlossyShape(
            shape = if (morphed) ShapeCatalog.Flower else ShapeCatalog.Circle,
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Button(onClick = onMorphClick) { Text(stringResource(R.string.catalog_morph_action)) }
    }
}

@Composable
private fun GlassSample(glass: FrostedGlassState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        GlossyShape(
            shape = ShapeCatalog.SoftBurst,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .frostedGlassSource(glass),
            tint = GlossTint.Accent
        )
        GlassTile(
            modifier = Modifier
                .padding(start = 56.dp, top = 40.dp)
                .fillMaxWidth()
                .height(128.dp),
            glass = glass
        ) {
            Text("68%", Modifier.align(Alignment.BottomStart), style = Numerals.Large)
        }
    }
}

@Composable
private fun BitmapParity(fill: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val tint = ShapesTheme.atmosphere.gloss
    val bitmap = remember(fill, tint, density) {
        ShapeBitmapRenderer.render(
            shape = ShapeCatalog.SoftBurst,
            sizePx = with(density) { 96.dp.roundToPx() },
            density = density.density,
            tint = tint,
            fill = fill
        ).asImageBitmap()
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LabelledShape(stringResource(R.string.catalog_bitmap_compose)) {
            GlossyShape(ShapeCatalog.SoftBurst, null, Modifier.size(96.dp), fill = fill)
        }
        LabelledShape(stringResource(R.string.catalog_bitmap_glance)) {
            Image(bitmap, contentDescription = null, Modifier.size(96.dp))
        }
    }
}

@Composable
private fun PillSample(modifier: Modifier = Modifier) {
    Button(onClick = {}, modifier = modifier.fillMaxWidth()) {
        Text(stringResource(R.string.catalog_pill_label))
    }
}

@Composable
private fun ShapeGridRow(row: List<ShapeCatalog>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { shape ->
            LabelledShape(shape.name, Modifier.weight(1f)) {
                GlossyShape(shape, shape.name, Modifier.size(64.dp))
            }
        }
        repeat(GRID_COLUMNS - row.size) { Box(Modifier.weight(1f)) }
    }
}

@Composable
private fun LabelledShape(label: String, modifier: Modifier = Modifier, shape: @Composable () -> Unit) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        shape()
        Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

private class AtmosphereProvider : PreviewParameterProvider<Atmosphere> {
    override val values = Atmosphere.entries.asSequence()
}

@Preview(widthDp = 390, heightDp = 2600)
@Composable
private fun FoundationCatalogScreenPreview(@PreviewParameter(AtmosphereProvider::class) atmosphere: Atmosphere) {
    ShapesTheme(atmosphere) {
        FoundationCatalogScreen(
            fill = 0.68f,
            morphed = false,
            onAtmosphereChange = {},
            onFillChange = {},
            onMorphClick = {}
        )
    }
}
