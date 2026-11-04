package com.fivehouse.thewid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.FaceValueText
import com.fivehouse.thewid.ui.components.GlassIconButton
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.PlacedWidget
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetLayout
import kotlinx.coroutines.launch

/** Screen 14, or screen 15 when nothing is placed. */
@Composable
fun MyWidgetsScreen(
    placed: List<PlacedWidget>,
    hero: StatusWidgetKind,
    faces: Map<StatusWidgetKind, StatusFace>,
    onEdit: (PlacedWidget) -> Unit,
    onAdd: () -> Unit,
    onQuickAdd: (StatusWidgetKind) -> Unit,
    modifier: Modifier = Modifier
) {
    if (placed.isEmpty()) {
        EmptyWidgets(onQuickAdd, onAdd, modifier)
    } else {
        PlacedList(placed, hero, faces, onEdit, onAdd, modifier)
    }
}

@Composable
private fun PlacedList(
    placed: List<PlacedWidget>,
    hero: StatusWidgetKind,
    faces: Map<StatusWidgetKind, StatusFace>,
    onEdit: (PlacedWidget) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var removing by rememberSaveable { mutableStateOf<StatusWidgetKind?>(null) }
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            pluralStringResource(R.plurals.my_widgets_placed, placed.size, placed.size),
            style = MaterialTheme.typography.bodySmall
        )
        Text(stringResource(R.string.my_widgets_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(placed, key = { it.appWidgetId }) { widget ->
                PlacedRow(widget, widget.kind == hero, faces[widget.kind], onEdit, { removing = widget.kind })
            }
            item(key = "hint") {
                Text(
                    stringResource(R.string.my_widgets_hint),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
        PrimaryPill(stringResource(R.string.my_widgets_add), onAdd)
    }
    removing?.let { kind ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.my_widgets_remove_title, stringResource(kind.label))) },
            text = { Text(stringResource(R.string.my_widgets_remove_body)) },
            confirmButton = {
                TextButton(onClick = { removing = null }) { Text(stringResource(R.string.my_widgets_remove_ok)) }
            }
        )
    }
}

/** Android gives apps no way to remove a launcher's widget, so the swipe explains how instead. */
@Composable
private fun PlacedRow(
    widget: PlacedWidget,
    isHero: Boolean,
    face: StatusFace?,
    onEdit: (PlacedWidget) -> Unit,
    onRemoveRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipe = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onRemoveRequest()
            scope.launch { swipe.reset() }
        },
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(Accent)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(stringResource(R.string.my_widgets_remove), style = MaterialTheme.typography.labelLarge)
            }
        }
    ) {
        GlassTile(
            Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val shown = face?.let { if (widget.shape != null) it.copy(shape = widget.shape) else it }
                if (shown != null) FaceShape(shown, null, Modifier.size(56.dp)) else Spacer(Modifier.size(56.dp))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(widget.kind.label), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoChip(stringResource(widget.layout.sizeLabel))
                        if (isHero) InfoChip(stringResource(R.string.my_widgets_hero), border = Accent)
                    }
                }
                TextButton(onClick = { onEdit(widget) }) { Text(stringResource(R.string.my_widgets_edit)) }
            }
        }
    }
}

private val WidgetLayout.sizeLabel: Int
    get() = when (this) {
        WidgetLayout.ShapeOnly -> R.string.size_1x1
        WidgetLayout.Square -> R.string.size_2x2
        WidgetLayout.Wide -> R.string.size_4x2
        WidgetLayout.Large -> R.string.size_4x4
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyWidgets(onQuickAdd: (StatusWidgetKind) -> Unit, onBrowse: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.empty_eyebrow),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            DashedShape(ShapeCatalog.Cookie7Sided, Alignment.TopStart)
            DashedShape(ShapeCatalog.Clover4Leaf, Alignment.TopEnd)
            DashedShape(ShapeCatalog.PuffyDiamond, Alignment.BottomStart)
            DashedShape(ShapeCatalog.Cookie9Sided, Alignment.BottomEnd)
            GlossyShape(ShapeCatalog.SoftBurst, null, Modifier.size(120.dp))
        }
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_body), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(StatusWidgetKind.Time, StatusWidgetKind.Battery, StatusWidgetKind.Mochi).forEach { kind ->
                TextButton(onClick = { onQuickAdd(kind) }) { Text(stringResource(kind.label)) }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryPill(stringResource(R.string.empty_browse), onBrowse)
    }
}

@Composable
private fun BoxScope.DashedShape(shape: ShapeCatalog, alignment: Alignment) {
    GlossyShape(
        shape,
        null,
        Modifier
            .align(alignment)
            .size(120.dp),
        surface = ShapeSurface.Dashed
    )
}

/** Screen 13. */
@Composable
fun HeroPickerScreen(
    options: List<StatusWidgetKind>,
    selected: StatusWidgetKind,
    faces: Map<StatusWidgetKind, StatusFace>,
    onSelect: (StatusWidgetKind) -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassIconButton(
                R.drawable.ic_close,
                stringResource(R.string.action_close),
                onClose,
                Modifier.align(Alignment.CenterStart)
            )
            Text(stringResource(R.string.hero_eyebrow), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.hero_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { kind ->
                        HeroOption(kind, faces[kind], kind == selected, { onSelect(kind) }, Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryPill(stringResource(R.string.hero_set, stringResource(selected.label)), onConfirm)
    }
}

@Composable
private fun HeroOption(
    kind: StatusWidgetKind,
    face: StatusFace?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.extraLarge
    GlassTile(
        modifier
            .aspectRatio(1.05f)
            .clip(shape)
            .then(if (selected) Modifier.border(1.5.dp, Accent, shape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Column {
            if (face != null) FaceShape(face, null, Modifier.size(64.dp))
            Spacer(Modifier.weight(1f))
            if (face != null) FaceValueText(face)
            Text(stringResource(kind.label), style = MaterialTheme.typography.bodySmall)
        }
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = Color.White)
            }
        }
    }
}

private val PreviewPlaced = listOf(
    PlacedWidget(1, StatusWidgetKind.Time, WidgetLayout.Wide, null, true),
    PlacedWidget(2, StatusWidgetKind.Battery, WidgetLayout.Square, null, true),
    PlacedWidget(3, StatusWidgetKind.Sound, WidgetLayout.ShapeOnly, null, true)
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun MyWidgetsPreview() {
    ScreenPreview(Atmosphere.Mint) {
        MyWidgetsScreen(PreviewPlaced, StatusWidgetKind.Time, PreviewFacesMap, {}, {}, {})
    }
}

@Preview(name = "Empty", widthDp = 390, heightDp = 844)
@Composable
private fun MyWidgetsEmptyPreview() {
    ScreenPreview(Atmosphere.Lilac) {
        MyWidgetsScreen(emptyList(), StatusWidgetKind.Time, PreviewFacesMap, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun HeroPickerPreview() {
    ScreenPreview(Atmosphere.CoolMist) {
        HeroPickerScreen(StatusWidgetKind.entries, StatusWidgetKind.Time, PreviewFacesMap, {}, {}, {})
    }
}
