package com.fivehouse.thewid.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.ui.components.Eyebrow
import com.fivehouse.thewid.ui.components.FaceShape
import com.fivehouse.thewid.ui.components.FaceValueText
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.PrimaryPill
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.components.ShapeChip
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeSurface
import com.fivehouse.thewid.ui.shell.CatalogWidget
import com.fivehouse.thewid.ui.shell.WidgetCategory
import com.fivehouse.thewid.ui.shell.matching
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.resolve

enum class SearchSetting(@param:StringRes val title: Int, @param:StringRes val body: Int) {
    Background(R.string.settings_background, R.string.settings_background_search_body),
    Theme(R.string.settings_theme, R.string.settings_theme_search_body),
    Atmosphere(R.string.settings_atmosphere, R.string.settings_atmosphere_search_body),
    Hero(R.string.settings_hero, R.string.settings_hero_search_body)
}

/** Screen 25. */
@Composable
fun SearchScreen(
    query: String,
    faces: Map<StatusWidgetKind, StatusFace>,
    recents: List<StatusWidgetKind>,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    onWidgetClick: (StatusWidgetKind) -> Unit,
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
    onHeartPulseClick: () -> Unit = {}
) {
    val resources = LocalResources.current
    val widgets = remember(query, resources) { CatalogWidget.entries.matching(query) { resources.getString(it.label) } }
    val settings = remember(query, resources) {
        SearchSetting.entries.matching(query) { resources.getString(it.title) }
    }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SearchField(query, onQueryChange, onClose)
        if (widgets.isNotEmpty()) {
            Eyebrow(stringResource(R.string.search_widgets))
            widgets.forEach { widget ->
                WidgetResult(widget, query, widget.kind?.let { faces[it] }, onWidgetClick, onHeartPulseClick)
            }
        }
        if (settings.isNotEmpty()) {
            Eyebrow(stringResource(R.string.search_settings))
            settings.forEach { setting -> SettingResult(setting, query, onSettingClick) }
        }
        if (query.isNotBlank() && widgets.isEmpty() && settings.isEmpty()) {
            Text(stringResource(R.string.search_no_results), style = MaterialTheme.typography.bodyMedium)
        }
        if (recents.isNotEmpty()) {
            Eyebrow(stringResource(R.string.search_recent))
            Recents(recents, faces, onWidgetClick)
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val ink = LocalContentColor.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.6f))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_search), null, Modifier.size(18.dp))
        Spacer(Modifier.size(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(stringResource(R.string.search_hint), color = ink.copy(alpha = 0.6f))
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = ink),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
        }
        IconButton(onClose) {
            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_close), Modifier.size(18.dp))
        }
    }
}

private fun highlighted(text: String, query: String): AnnotatedString {
    val start = text.indexOf(query.trim(), ignoreCase = true)
    if (start < 0 || query.isBlank()) return AnnotatedString(text)
    val end = start + query.trim().length
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(start, end)) }
        append(text.substring(end))
    }
}

@Composable
private fun WidgetResult(
    widget: CatalogWidget,
    query: String,
    face: StatusFace?,
    onClick: (StatusWidgetKind) -> Unit,
    onHeartPulseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kind = widget.kind
    val context = LocalContext.current
    val category = WidgetCategory.entries.first { widget in it.widgets }
    GlassTile(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable { if (kind != null) onClick(kind) else onHeartPulseClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (face != null) {
                FaceShape(face, null, Modifier.size(56.dp))
            } else {
                GlossyShape(widget.shape, null, Modifier.size(56.dp), surface = ShapeSurface.Dashed)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(highlighted(stringResource(widget.label), query), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.search_result_subtitle,
                        stringResource(category.title),
                        face?.value?.resolve(context) ?: stringResource(R.string.heart_pulse_search)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (face != null) FaceValueText(face)
        }
    }
}

@Composable
private fun SettingResult(setting: SearchSetting, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassTile(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(highlighted(stringResource(setting.title), query), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(setting.body), style = MaterialTheme.typography.bodySmall)
            }
            Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Recents(
    recents: List<StatusWidgetKind>,
    faces: Map<StatusWidgetKind, StatusFace>,
    onClick: (StatusWidgetKind) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        recents.forEach { kind ->
            ShapeChip(faces[kind], stringResource(kind.label), { onClick(kind) })
        }
    }
}

/** Screen 16. */
@Composable
fun AddByHandScreen(kind: StatusWidgetKind, face: StatusFace?, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(kind.label)
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.by_hand_eyebrow), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.by_hand_title), style = MaterialTheme.typography.headlineLarge)
        Step("1", stringResource(R.string.by_hand_step1))
        Step("2", stringResource(R.string.by_hand_step2))
        Step("3", stringResource(R.string.by_hand_step3, stringResource(R.string.app_name), label)) {
            if (face != null) FaceShape(face, null, Modifier.size(48.dp))
        }
        Spacer(Modifier.weight(1f))
        PrimaryPill(stringResource(R.string.by_hand_ok), onDone)
    }
}

@Composable
private fun Step(number: String, text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    GlassTile(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(number, style = Numerals.Large)
            Spacer(Modifier.size(12.dp))
            Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            trailing()
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SearchResultsPreview() {
    ScreenPreview(Atmosphere.CoolMist) {
        SearchScreen("ba", PreviewFacesMap, listOf(StatusWidgetKind.Time, StatusWidgetKind.Wifi), {}, {}, {}, {})
    }
}

@Preview(name = "No results", widthDp = 390, heightDp = 400)
@Composable
private fun SearchEmptyPreview() {
    ScreenPreview(Atmosphere.CoolMist) {
        SearchScreen("zzz", PreviewFacesMap, emptyList(), {}, {}, {}, {})
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AddByHandPreview() {
    ScreenPreview(Atmosphere.Stone) {
        AddByHandScreen(StatusWidgetKind.Battery, PreviewFacesMap[StatusWidgetKind.Battery], {})
    }
}
