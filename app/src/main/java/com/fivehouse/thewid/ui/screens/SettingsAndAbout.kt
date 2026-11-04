package com.fivehouse.thewid.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.settings.ThemeChoice
import com.fivehouse.thewid.ui.components.BackButton
import com.fivehouse.thewid.ui.components.Eyebrow
import com.fivehouse.thewid.ui.components.GlassTile
import com.fivehouse.thewid.ui.components.InfoChip
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.components.SegmentedPills
import com.fivehouse.thewid.ui.shapes.GlossyShape
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Accent
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.Numerals
import com.fivehouse.thewid.widget.StatusWidgetKind

private val AtmosphereChoices = listOf(Atmosphere.CoolMist, Atmosphere.DuskBloom, Atmosphere.Mint, Atmosphere.Moss)

@StringRes
private fun ThemeChoice.label(): Int = when (this) {
    ThemeChoice.Light -> R.string.settings_theme_light
    ThemeChoice.Dark -> R.string.settings_theme_dark
    ThemeChoice.Auto -> R.string.settings_theme_auto
}

/** Screen 9. */
@Composable
fun SettingsScreen(
    hero: StatusWidgetKind,
    theme: ThemeChoice,
    atmosphere: Atmosphere?,
    notificationsAllowed: Boolean,
    overlayAllowed: Boolean,
    calendarOn: Boolean,
    backgroundAllowed: Boolean,
    version: String,
    showFoundation: Boolean,
    onBack: () -> Unit,
    onThemeChange: (ThemeChoice) -> Unit,
    onAtmosphereChange: (Atmosphere?) -> Unit,
    onNotificationsClick: () -> Unit,
    onOverlayClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onHeroClick: () -> Unit,
    onAboutClick: () -> Unit,
    onFoundationClick: () -> Unit,
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BackButton(onBack)
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineLarge)
        }
        Eyebrow(stringResource(R.string.settings_access))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccessCard(
                ShapeCatalog.Clover4Leaf,
                R.string.settings_notifications,
                R.string.settings_notifications_body,
                stringResource(if (notificationsAllowed) R.string.settings_on else R.string.settings_turn_on),
                !notificationsAllowed,
                onNotificationsClick,
                Modifier.weight(1f)
            )
            AccessCard(
                ShapeCatalog.Heart,
                R.string.settings_overlay,
                R.string.settings_overlay_body,
                stringResource(if (overlayAllowed) R.string.settings_on else R.string.settings_turn_on),
                !overlayAllowed,
                onOverlayClick,
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccessCard(
                ShapeCatalog.Diamond,
                R.string.settings_calendar,
                R.string.settings_calendar_body,
                stringResource(if (calendarOn) R.string.settings_on else R.string.settings_turn_on),
                !calendarOn,
                onCalendarClick,
                Modifier.weight(1f)
            )
            AccessCard(
                ShapeCatalog.Cookie12Sided,
                R.string.settings_background,
                R.string.settings_background_body,
                stringResource(if (backgroundAllowed) R.string.settings_on else R.string.settings_turn_on),
                !backgroundAllowed,
                onBackgroundClick,
                Modifier.weight(1f)
            )
        }
        Eyebrow(stringResource(R.string.settings_look))
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_theme), Modifier.weight(1f))
                    SegmentedPills(
                        options = ThemeChoice.entries,
                        selected = theme,
                        label = { stringResource(it.label()) },
                        onSelect = onThemeChange,
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_atmosphere), Modifier.weight(1f))
                    AtmosphereSwatches(AtmosphereChoices, atmosphere, onAtmosphereChange)
                }
            }
        }
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingRow(R.string.settings_hero, stringResource(hero.label), onHeroClick)
                SettingRow(R.string.settings_refresh, stringResource(R.string.settings_refresh_value), null)
                if (showFoundation) SettingRow(R.string.settings_foundation, "", onFoundationClick)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onAboutClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(14.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.settings_footer, version), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AccessCard(
    shape: ShapeCatalog,
    @StringRes title: Int,
    @StringRes body: Int,
    status: String,
    needsAction: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    GlassTile(
        modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                GlossyShape(shape, null, Modifier.size(40.dp))
                Spacer(Modifier.weight(1f))
                InfoChip(status, border = if (needsAction) Accent else null)
            }
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun AtmosphereSwatches(
    choices: List<Atmosphere>,
    selected: Atmosphere?,
    onChange: (Atmosphere?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        choices.forEach { choice ->
            val isSelected = choice == selected
            // Tapping the chosen swatch clears it, returning every screen to its designed atmosphere.
            Box(
                Modifier
                    .size(48.dp)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onChange(if (isSelected) null else choice) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(choice.background)
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun SettingRow(@StringRes title: Int, value: String, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(title), Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Screen 26. */
@Composable
fun AboutScreen(
    version: String,
    licences: String?,
    onBack: () -> Unit,
    onLicencesClick: () -> Unit,
    onLicencesDismiss: () -> Unit,
    onRateClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFeedbackClick: (() -> Unit)? = null
) {
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackButton(onBack)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            GlossyShape(ShapeCatalog.Cookie9Sided, null, Modifier.size(120.dp), castShadow = true)
            Text(stringResource(R.string.product_name), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.about_version, version), style = Numerals.Small)
        }
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    R.string.about_no_account,
                    R.string.about_nothing_sent,
                    R.string.about_no_ads
                ).forEach { promise ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(16.dp))
                        Text(stringResource(promise), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        GlassTile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LinkRow(R.string.about_licences, onLicencesClick)
                LinkRow(R.string.about_rate, onRateClick)
                if (onFeedbackClick != null) LinkRow(R.string.about_feedback, onFeedbackClick)
            }
        }
        Spacer(Modifier.weight(1f))
        Eyebrow(stringResource(R.string.product_made_with), Modifier.align(Alignment.CenterHorizontally))
    }
    if (licences != null) LicencesDialog(licences, onLicencesDismiss)
}

@Composable
private fun LinkRow(@StringRes title: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(title), Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_external), null, Modifier.size(16.dp))
    }
}

@Composable
private fun LicencesDialog(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.about_licences), fontWeight = FontWeight.Normal) },
        text = {
            Text(
                text,
                Modifier.verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.about_licences_ok)) } }
    )
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SettingsPreview() {
    ScreenPreview(Atmosphere.Stone) {
        SettingsScreen(
            StatusWidgetKind.Time,
            ThemeChoice.Auto,
            Atmosphere.CoolMist,
            notificationsAllowed = false,
            overlayAllowed = false,
            calendarOn = false,
            backgroundAllowed = false,
            version = "1.0",
            showFoundation = true,
            onBack = {},
            onThemeChange = {},
            onAtmosphereChange = {},
            onNotificationsClick = {},
            onOverlayClick = {},
            onCalendarClick = {},
            onBackgroundClick = {},
            onHeroClick = {},
            onAboutClick = {},
            onFoundationClick = {}
        )
    }
}

@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
private fun SettingsDarkPreview() {
    ScreenPreview(Atmosphere.Night) {
        SettingsScreen(
            StatusWidgetKind.Battery,
            ThemeChoice.Dark,
            null,
            notificationsAllowed = true,
            overlayAllowed = true,
            calendarOn = true,
            backgroundAllowed = true,
            version = "1.0",
            showFoundation = false,
            onBack = {},
            onThemeChange = {},
            onAtmosphereChange = {},
            onNotificationsClick = {},
            onOverlayClick = {},
            onCalendarClick = {},
            onBackgroundClick = {},
            onHeroClick = {},
            onAboutClick = {},
            onFoundationClick = {}
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AboutPreview() {
    ScreenPreview(Atmosphere.Stone) { AboutScreen("1.0", null, {}, {}, {}, {}) }
}
