package com.fivehouse.thewid.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.FaceValueText
import com.fivehouse.thewid.ui.components.GlassIconButton
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.LiveDot
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.shell.CatalogWidget
import com.fivehouse.thewid.ui.shell.WidgetCategory
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Ink
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.sampleFace
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Screen 3. */
@Composable
fun LibraryScreen(
    hero: StatusWidgetKind,
    faces: Map<StatusWidgetKind, StatusFace>,
    onHeroClick: () -> Unit,
    onCategoryClick: (WidgetCategory) -> Unit,
    onSearchClick: () -> Unit,
    onMyWidgetsClick: () -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LibraryHeader(onSearchClick)
            HeroTile(hero, faces[hero], onHeroClick)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text(stringResource(R.string.library_categories), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    pluralStringResource(
                        R.plurals.library_widget_count,
                        CatalogWidget.entries.size,
                        CatalogWidget.entries.size
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            CategoryBento(faces, onCategoryClick)
        }
        FloatingNav(
            onMyWidgetsClick,
            onAddClick,
            onSettingsClick,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun LibraryHeader(onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.bodySmall
            )
            Text(stringResource(R.string.product_name), style = MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.weight(1f))
        GlassIconButton(R.drawable.ic_search, stringResource(R.string.library_search), onSearchClick)
    }
}

@Composable
private fun HeroTile(hero: StatusWidgetKind, face: StatusFace?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassTile(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveDot()
                    Text(
                        stringResource(R.string.library_hero_label, stringResource(hero.label)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (face != null) FaceValueText(face, style = Numerals.Large)
                Text(stringResource(R.string.library_hero_change), style = MaterialTheme.typography.bodySmall)
            }
            if (face != null) FaceShape(face, null, Modifier.size(108.dp))
        }
    }
}

@Composable
private fun CategoryBento(
    faces: Map<StatusWidgetKind, StatusFace>,
    onClick: (WidgetCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryTile(WidgetCategory.Status, onClick, Modifier.weight(1f)) {
                StatusSamples(faces)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(WidgetCategory.Time, onClick) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        faces[StatusWidgetKind.Time]?.let { FaceShape(it, null, Modifier.size(48.dp)) }
                        GlossyShape(ShapeCatalog.Diamond, null, Modifier.size(40.dp))
                    }
                }
                CategoryTile(WidgetCategory.Companion, onClick) {
                    GlossyShape(ShapeCatalog.Puffy, null, Modifier.size(56.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(WidgetCategory.People, onClick, Modifier.weight(1f)) {
                GlossyShape(ShapeCatalog.Heart, null, Modifier.size(44.dp))
            }
            CategoryTile(WidgetCategory.Fun, onClick, Modifier.weight(1f)) {
                GlossyShape(ShapeCatalog.Cookie9Sided, null, Modifier.size(44.dp))
            }
        }
    }
}

@Composable
private fun StatusSamples(faces: Map<StatusWidgetKind, StatusFace>, modifier: Modifier = Modifier) {
    val samples =
        listOf(StatusWidgetKind.Battery, StatusWidgetKind.Wifi, StatusWidgetKind.Sound, StatusWidgetKind.Cellular)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        samples.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { kind ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        faces[kind]?.let { face ->
                            FaceShape(face, null, Modifier.size(52.dp))
                            FaceValueText(face, style = Numerals.Small)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    category: WidgetCategory,
    onClick: (WidgetCategory) -> Unit,
    modifier: Modifier = Modifier,
    sample: @Composable () -> Unit
) {
    GlassTile(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { onClick(category) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(stringResource(category.title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("${category.widgets.size}", style = MaterialTheme.typography.bodyMedium)
            }
            sample()
        }
    }
}

@Composable
private fun FloatingNav(
    onMyWidgetsClick: () -> Unit,
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(CircleShape)
            .background(Ink)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavIcon(R.drawable.ic_grid, R.string.nav_my_widgets, onMyWidgetsClick)
        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Accent)
        ) {
            Icon(
                painterResource(R.drawable.ic_plus),
                stringResource(R.string.nav_add),
                tint = MaterialTheme.colorScheme.onTertiary
            )
        }
        NavIcon(R.drawable.ic_tune, R.string.nav_settings, onSettingsClick)
    }
}

@Composable
private fun NavIcon(icon: Int, @StringRes label: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick, modifier.size(48.dp)) {
        Icon(painterResource(icon), stringResource(label), tint = Color.White)
    }
}

/** Screen 4. */
@Composable
fun CategoryScreen(
    category: WidgetCategory,
    faces: Map<StatusWidgetKind, StatusFace>,
    onBack: () -> Unit,
    onWidgetClick: (StatusWidgetKind) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            LiveDot()
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.category_live_now), style = MaterialTheme.typography.bodySmall)
        }
        Text(stringResource(category.title), style = MaterialTheme.typography.headlineLarge)
        Text(
            pluralStringResource(
                R.plurals.category_count,
                category.widgets.size,
                category.widgets.size,
                stringResource(category.subtitle)
            ),
            style = MaterialTheme.typography.bodyMedium
        )
        category.widgets.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { widget ->
                    CatalogTile(widget, widget.kind?.let { faces[it] }, onWidgetClick, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CatalogTile(
    widget: CatalogWidget,
    face: StatusFace?,
    onClick: (StatusWidgetKind) -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = widget.kind
    val clickable = if (kind != null) {
        Modifier.clickable(role = Role.Button) { onClick(kind) }
    } else {
        Modifier
    }
    GlassTile(
        modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraLarge)
            .then(clickable)
    ) {
        Column(Modifier.fillMaxHeight()) {
            if (face != null) {
                FaceShape(face, null, Modifier.size(64.dp))
            } else {
                GlossyShape(widget.shape, null, Modifier.size(64.dp), surface = ShapeSurface.Dashed)
            }
            Spacer(Modifier.weight(1f))
            if (face != null) {
                FaceValueText(face)
            } else {
                Text(stringResource(R.string.soon), style = MaterialTheme.typography.labelMedium)
            }
            Text(stringResource(widget.label), style = MaterialTheme.typography.bodySmall)
        }
        if (widget.needsPermission && (face == null || face.needsAccess)) {
            Icon(
                painterResource(R.drawable.ic_lock),
                null,
                Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp),
                tint = Accent
            )
        }
    }
}

internal val PreviewFacesMap: Map<StatusWidgetKind, StatusFace> = StatusWidgetKind.entries.associateWith(::sampleFace)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LibraryPreview() {
    ScreenPreview(Atmosphere.CoolMist) {
        LibraryScreen(StatusWidgetKind.Time, PreviewFacesMap, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Status loading", widthDp = 390, heightDp = 844)
@Composable
private fun LibraryLoadingPreview() {
    ScreenPreview(Atmosphere.CoolMist) {
        LibraryScreen(StatusWidgetKind.Time, emptyMap(), {}, {}, {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun CategoryStatusPreview() {
    ScreenPreview(Atmosphere.Mint) { CategoryScreen(WidgetCategory.Status, PreviewFacesMap, {}, {}) }
}

@Preview(name = "Nothing built yet", widthDp = 390, heightDp = 500)
@Composable
private fun CategoryPeoplePreview() {
    ScreenPreview(Atmosphere.Mint) { CategoryScreen(WidgetCategory.People, PreviewFacesMap, {}, {}) }
}
