package com.fivehouse.thewid

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.fivehouse.thewid.data.personal.DayKind
import com.fivehouse.thewid.data.personal.Decision
import com.fivehouse.thewid.data.personal.DecisionOption
import com.fivehouse.thewid.data.personal.FocusState
import com.fivehouse.thewid.data.personal.HeartPulseSettings
import com.fivehouse.thewid.data.personal.MochiMood
import com.fivehouse.thewid.data.personal.MochiSettings
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.TimeSettings
import com.fivehouse.thewid.data.personal.WeekEvent
import com.fivehouse.thewid.data.settings.ThemeChoice
import com.fivehouse.thewid.data.status.AlertsStatus
import com.fivehouse.thewid.data.status.NowPlayingStatus
import com.fivehouse.thewid.ui.components.ScreenPreview
import com.fivehouse.thewid.ui.screens.AboutScreen
import com.fivehouse.thewid.ui.screens.AccessRequest
import com.fivehouse.thewid.ui.screens.AccessScreen
import com.fivehouse.thewid.ui.screens.AddByHandScreen
import com.fivehouse.thewid.ui.screens.AddedScreen
import com.fivehouse.thewid.ui.screens.BreatheCompleteScreen
import com.fivehouse.thewid.ui.screens.CategoryScreen
import com.fivehouse.thewid.ui.screens.CustomizeScreen
import com.fivehouse.thewid.ui.screens.DecidePage
import com.fivehouse.thewid.ui.screens.DecideSetupScreen
import com.fivehouse.thewid.ui.screens.FocusPage
import com.fivehouse.thewid.ui.screens.HeartPulseScreen
import com.fivehouse.thewid.ui.screens.HeroPickerScreen
import com.fivehouse.thewid.ui.screens.InnerCirclePage
import com.fivehouse.thewid.ui.screens.InnerCircleSetupScreen
import com.fivehouse.thewid.ui.screens.LibraryScreen
import com.fivehouse.thewid.ui.screens.MochiPage
import com.fivehouse.thewid.ui.screens.MochiSetupScreen
import com.fivehouse.thewid.ui.screens.MyWidgetsScreen
import com.fivehouse.thewid.ui.screens.NowPlayingPage
import com.fivehouse.thewid.ui.screens.OnboardingScreen
import com.fivehouse.thewid.ui.screens.PermissionScreen
import com.fivehouse.thewid.ui.screens.PermissionStage
import com.fivehouse.thewid.ui.screens.PreviewSize
import com.fivehouse.thewid.ui.screens.SearchScreen
import com.fivehouse.thewid.ui.screens.SettingsScreen
import com.fivehouse.thewid.ui.screens.SplashScreen
import com.fivehouse.thewid.ui.screens.TimeSetupScreen
import com.fivehouse.thewid.ui.screens.WeekSetupScreen
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.shell.WidgetCategory
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.widget.PlacedWidget
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetLayout
import com.fivehouse.thewid.widget.sampleFace
import com.fivehouse.thewid.widget.toFace
import java.time.LocalDate

private val Today = LocalDate.of(2026, 11, 2)
private val Faces = StatusWidgetKind.entries.associateWith(::sampleFace)
private val People = listOf(
    Person("a", "Partner", ShapeCatalog.Heart),
    Person("b", "Mom", ShapeCatalog.Flower),
    Person("c", "Sam", ShapeCatalog.Clover4Leaf)
)
private val Options = listOf(
    DecisionOption("Dosa", ShapeCatalog.Cookie6Sided),
    DecisionOption("Biryani", ShapeCatalog.Clover4Leaf),
    DecisionOption("Pizza", ShapeCatalog.Diamond),
    DecisionOption("Salad", ShapeCatalog.Sunny)
)

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Splash01() = ScreenPreview(Atmosphere.CoolMist) { SplashScreen({}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Onboarding02() = ScreenPreview(Atmosphere.Lilac) { OnboardingScreen({}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Library03() = ScreenPreview(Atmosphere.CoolMist) {
    LibraryScreen(StatusWidgetKind.Time, Faces, {}, {}, {}, {}, {}, {}, today = Today)
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 900)
@Composable
fun Category04() = ScreenPreview(Atmosphere.Mint) { CategoryScreen(WidgetCategory.Status, Faces, {}, {}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Customize05() = ScreenPreview(Atmosphere.Moss) {
    CustomizeScreen(
        StatusWidgetKind.Battery,
        Faces[StatusWidgetKind.Battery],
        ShapeCatalog.SoftBurst,
        PreviewSize.Two,
        true,
        {},
        {},
        {},
        {},
        {}
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun PermissionAsk06() = ScreenPreview(Atmosphere.DuskBloom) {
    PermissionScreen(StatusWidgetKind.Alerts, PermissionStage.Ask, null, "Shapes", "TheWid", {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Added07() = ScreenPreview(Atmosphere.Peach) {
    AddedScreen(
        StatusWidgetKind.Battery,
        Faces[StatusWidgetKind.Battery],
        StatusWidgetKind.Time,
        Faces[StatusWidgetKind.Time],
        {},
        {},
        today = Today
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun HeartPulse08() = ScreenPreview(Atmosphere.Night) {
    HeartPulseScreen(HeartPulseSettings(), false, true, {}, {}, {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Settings09() = ScreenPreview(Atmosphere.Stone) {
    SettingsScreen(
        hero = StatusWidgetKind.Time,
        theme = ThemeChoice.Auto,
        atmosphere = Atmosphere.CoolMist,
        notificationsAllowed = true,
        overlayAllowed = false,
        calendarOn = false,
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

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun PermissionDenied11() = ScreenPreview(Atmosphere.DuskBloom) {
    PermissionScreen(StatusWidgetKind.Alerts, PermissionStage.Denied, null, "Shapes", "TheWid", {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun PermissionGranted12() = ScreenPreview(Atmosphere.DuskBloom) {
    PermissionScreen(
        StatusWidgetKind.Alerts,
        PermissionStage.Granted,
        AlertsStatus.Count(2).toFace(),
        "Shapes",
        "TheWid",
        {},
        {},
        {},
        {}
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Hero13() = ScreenPreview(Atmosphere.CoolMist) {
    HeroPickerScreen(StatusWidgetKind.entries.take(6), StatusWidgetKind.Time, Faces, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun MyWidgets14() = ScreenPreview(Atmosphere.Mint) {
    MyWidgetsScreen(
        listOf(
            PlacedWidget(1, StatusWidgetKind.Time, WidgetLayout.Wide, null, true),
            PlacedWidget(2, StatusWidgetKind.Battery, WidgetLayout.Square, null, true),
            PlacedWidget(3, StatusWidgetKind.Sound, WidgetLayout.Square, null, true),
            PlacedWidget(4, StatusWidgetKind.Alerts, WidgetLayout.ShapeOnly, null, true)
        ),
        StatusWidgetKind.Time,
        Faces,
        {},
        {},
        {}
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Empty15() = ScreenPreview(Atmosphere.Lilac) {
    MyWidgetsScreen(emptyList(), StatusWidgetKind.Time, Faces, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun AddByHand16() = ScreenPreview(Atmosphere.Stone) {
    AddByHandScreen(StatusWidgetKind.Battery, Faces[StatusWidgetKind.Battery], {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun TimeSetup17() = ScreenPreview(Atmosphere.Night) { TimeSetupScreen(TimeSettings(), false, {}, {}, {}, {}, {}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun WeekSetup18() = ScreenPreview(Atmosphere.Stone) {
    WeekSetupScreen(
        listOf(
            WeekEvent("1", LocalDate.of(2026, 11, 5).toEpochDay(), "Your event", DayKind.Deadline),
            WeekEvent("2", LocalDate.of(2026, 11, 6).toEpochDay(), "Busy day", DayKind.Busy),
            WeekEvent("3", LocalDate.of(2026, 11, 14).toEpochDay(), "Their name", DayKind.Birthday),
            WeekEvent("4", LocalDate.of(2026, 11, 21).toEpochDay(), "Where to", DayKind.Trip)
        ),
        {},
        {},
        {}
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun CircleSetup19() = ScreenPreview(Atmosphere.DuskBloom) { InnerCircleSetupScreen(People, {}, {}, {}, {}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun DecideSetup20() = ScreenPreview(Atmosphere.Peach) {
    DecideSetupScreen(Decision("Where to eat tonight?", Options), {}) {}
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun MochiSetup21() = ScreenPreview(Atmosphere.Stone) { MochiSetupScreen(MochiSettings(), null) {} }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun BreatheDone22() = ScreenPreview(Atmosphere.Mint) { BreatheCompleteScreen(3, {}, {}, heartPulseOn = true) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Search25() = ScreenPreview(Atmosphere.CoolMist) {
    SearchScreen("batt", Faces, listOf(StatusWidgetKind.Time, StatusWidgetKind.Wifi), {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun About26() = ScreenPreview(Atmosphere.Stone) { AboutScreen("1.0", null, {}, {}, {}, {}) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Focus28() = ScreenPreview(Atmosphere.DuskBloom) {
    FocusPage(FocusState(session = 3).started(0), 42_000, {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun NowPlaying33() = ScreenPreview(Atmosphere.Moss) {
    NowPlayingPage(
        NowPlayingStatus.Track("Track name", "Artist", true, null, 161_000, 240_000),
        listOf(com.fivehouse.thewid.R.string.widget_battery to Faces.getValue(StatusWidgetKind.Battery)),
        {},
        {},
        {},
        {}
    )
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 1000)
@Composable
fun Mochi34() = ScreenPreview(Atmosphere.Moss) {
    MochiPage(MochiMood.Excited.toFace("Mochi"), "Mochi", Faces[StatusWidgetKind.Battery], {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Circle36() = ScreenPreview(Atmosphere.DuskBloom) {
    InnerCirclePage(People, mapOf(People[0] to 2), true, 0, {}, {}, {}, {}, {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Decide37() = ScreenPreview(Atmosphere.Peach) {
    DecidePage(Decision("Where to eat tonight?", Options, lastIndex = 1), {}, {}, {})
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun AccessAsk() = ScreenPreview(Atmosphere.DuskBloom) {
    AccessScreen(AccessRequest.Contacts, PermissionStage.Ask, {}, {}, {})
}

@PreviewTest
@Preview(name = "Library in dark theme", widthDp = 390, heightDp = 844)
@Composable
fun LibraryDark() = ScreenPreview(Atmosphere.Night) {
    LibraryScreen(StatusWidgetKind.Time, Faces, {}, {}, {}, {}, {}, {}, today = Today)
}

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Onboarding23() = ScreenPreview(Atmosphere.Lilac) { OnboardingScreen({}, initialPage = 1) }

@PreviewTest
@Preview(widthDp = 390, heightDp = 844)
@Composable
fun Onboarding24() = ScreenPreview(Atmosphere.Lilac) { OnboardingScreen({}, initialPage = 2) }

@PreviewTest
@Preview(name = "Large text", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun LibraryLargeText() = ScreenPreview(Atmosphere.CoolMist) {
    LibraryScreen(StatusWidgetKind.Time, Faces, {}, {}, {}, {}, {}, {}, today = Today)
}

@PreviewTest
@Preview(name = "Large text", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun SettingsLargeText() = Settings09()

@PreviewTest
@Preview(name = "Large text", widthDp = 390, heightDp = 844, fontScale = 1.5f)
@Composable
fun PermissionLargeText() = PermissionAsk06()

@PreviewTest
@Preview(name = "Dark", widthDp = 390, heightDp = 844)
@Composable
fun SettingsDark() = ScreenPreview(Atmosphere.Night) {
    SettingsScreen(
        hero = StatusWidgetKind.Time,
        theme = ThemeChoice.Dark,
        atmosphere = null,
        notificationsAllowed = false,
        overlayAllowed = true,
        calendarOn = true,
        backgroundAllowed = false,
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
