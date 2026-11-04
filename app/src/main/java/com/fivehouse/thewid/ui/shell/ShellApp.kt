package com.fivehouse.thewid.ui.shell

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.settings.AppPreferences
import com.fivehouse.thewid.ui.catalog.FoundationCatalogRoute
import com.fivehouse.thewid.ui.components.AtmosphereBackground
import com.fivehouse.thewid.ui.screens.AboutScreen
import com.fivehouse.thewid.ui.screens.AddByHandScreen
import com.fivehouse.thewid.ui.screens.AddedScreen
import com.fivehouse.thewid.ui.screens.CategoryScreen
import com.fivehouse.thewid.ui.screens.CustomizeScreen
import com.fivehouse.thewid.ui.screens.HeroPickerScreen
import com.fivehouse.thewid.ui.screens.LibraryScreen
import com.fivehouse.thewid.ui.screens.MyWidgetsScreen
import com.fivehouse.thewid.ui.screens.OnboardingScreen
import com.fivehouse.thewid.ui.screens.PreviewSize
import com.fivehouse.thewid.ui.screens.SearchScreen
import com.fivehouse.thewid.ui.screens.SettingsScreen
import com.fivehouse.thewid.ui.screens.SplashScreen
import com.fivehouse.thewid.ui.shapes.ShapeCatalog
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.ShapesTheme
import com.fivehouse.thewid.ui.widgetconfig.WidgetConfigureActivity
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetPinning
import kotlinx.serialization.Serializable

@Serializable private data object SplashRoute

@Serializable private data object OnboardingRoute

@Serializable private data object LibraryRoute

@Serializable private data class CategoryRoute(val category: String)

@Serializable private data class CustomizeRoute(val kind: String)

@Serializable private data class AddedRoute(val kind: String)

@Serializable private data class AddByHandRoute(val kind: String)

@Serializable private data object MyWidgetsRoute

@Serializable private data object HeroPickerRoute

@Serializable private data object SearchRoute

@Serializable private data object SettingsRoute

@Serializable private data object AboutRoute

@Serializable private data object FoundationRoute

private fun kindNamed(name: String) = StatusWidgetKind.valueOf(name)

@Composable
fun ShellApp(viewModel: ShellViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val loaded by viewModel.preferences.collectAsStateWithLifecycle()
    val prefs = loaded ?: AppPreferences()
    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val placed by viewModel.placed.collectAsStateWithLifecycle()
    val pinned by WidgetPinning.pinned.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(pinned) {
        pinned?.let { kind ->
            WidgetPinning.consumePinned()
            nav.navigate(AddedRoute(kind.name)) { popUpTo(LibraryRoute) }
        }
    }

    val toCustomize: (StatusWidgetKind) -> Unit = { nav.navigate(CustomizeRoute(it.name)) }

    NavHost(nav, startDestination = SplashRoute) {
        composable<SplashRoute> {
            // Route only once the animation has played and preferences have loaded, whichever is last.
            var animationDone by rememberSaveable { mutableStateOf(false) }
            val onboardingDone = loaded?.onboardingDone
            LaunchedEffect(animationDone, onboardingDone) {
                if (animationDone && onboardingDone != null) {
                    val target: Any = if (onboardingDone) LibraryRoute else OnboardingRoute
                    nav.navigate(target) { popUpTo(SplashRoute) { inclusive = true } }
                }
            }
            Hosted(Atmosphere.CoolMist, prefs) { SplashScreen(onFinished = { animationDone = true }) }
        }
        composable<OnboardingRoute> {
            Hosted(Atmosphere.Lilac, prefs) {
                OnboardingScreen(onFinished = {
                    viewModel.finishOnboarding()
                    nav.navigate(LibraryRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
                })
            }
        }
        composable<LibraryRoute> {
            Hosted(Atmosphere.CoolMist, prefs) {
                LibraryScreen(
                    hero = prefs.hero,
                    faces = faces,
                    onHeroClick = { nav.navigate(HeroPickerRoute) },
                    onCategoryClick = { nav.navigate(CategoryRoute(it.name)) },
                    onSearchClick = { nav.navigate(SearchRoute) },
                    onMyWidgetsClick = { nav.navigate(MyWidgetsRoute) },
                    onAddClick = { nav.navigate(CategoryRoute(WidgetCategory.Status.name)) },
                    onSettingsClick = { nav.navigate(SettingsRoute) }
                )
            }
        }
        composable<CategoryRoute> { entry ->
            val category = WidgetCategory.valueOf(entry.toRoute<CategoryRoute>().category)
            Hosted(Atmosphere.Mint, prefs) {
                CategoryScreen(category, faces, nav::goBack, toCustomize)
            }
        }
        composable<CustomizeRoute> { entry ->
            val kind = kindNamed(entry.toRoute<CustomizeRoute>().kind)
            Hosted(Atmosphere.Moss, prefs) { CustomizeDestination(kind, viewModel, nav) }
        }
        composable<AddedRoute> { entry ->
            val kind = kindNamed(entry.toRoute<AddedRoute>().kind)
            Hosted(Atmosphere.Peach, prefs) {
                AddedScreen(
                    kind = kind,
                    face = faces[kind],
                    hero = prefs.hero,
                    heroFace = faces[prefs.hero],
                    onMakeHero = { viewModel.setHero(kind) },
                    onDone = { nav.popBackStack(LibraryRoute, inclusive = false) }
                )
            }
        }
        composable<AddByHandRoute> { entry ->
            val kind = kindNamed(entry.toRoute<AddByHandRoute>().kind)
            Hosted(Atmosphere.Stone, prefs) { AddByHandScreen(kind, faces[kind], nav::goBack) }
        }
        composable<MyWidgetsRoute> {
            LifecycleResumeEffect(Unit) {
                viewModel.refreshPlaced()
                onPauseOrDispose {}
            }
            val list = placed
            Hosted(if (list.isNullOrEmpty()) Atmosphere.Lilac else Atmosphere.Mint, prefs) {
                if (list != null) {
                    MyWidgetsScreen(
                        placed = list,
                        hero = prefs.hero,
                        faces = faces,
                        onEdit = { widget ->
                            context.startActivity(
                                Intent(context, WidgetConfigureActivity::class.java)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)
                            )
                        },
                        onAdd = { nav.navigate(CategoryRoute(WidgetCategory.Status.name)) },
                        onQuickAdd = toCustomize
                    )
                }
            }
        }
        composable<HeroPickerRoute> {
            var selected by rememberSaveable { mutableStateOf(prefs.hero) }
            Hosted(Atmosphere.CoolMist, prefs) {
                HeroPickerScreen(
                    selected = selected,
                    faces = faces,
                    onSelect = { selected = it },
                    onConfirm = {
                        viewModel.setHero(selected)
                        nav.goBack()
                    },
                    onClose = nav::goBack
                )
            }
        }
        composable<SearchRoute> {
            var query by rememberSaveable { mutableStateOf("") }
            Hosted(Atmosphere.CoolMist, prefs) {
                SearchScreen(
                    query = query,
                    faces = faces,
                    recents = prefs.recents,
                    onQueryChange = { query = it },
                    onClose = nav::goBack,
                    onWidgetClick = { kind ->
                        viewModel.addRecent(kind)
                        toCustomize(kind)
                    },
                    onSettingClick = { nav.navigate(SettingsRoute) }
                )
            }
        }
        composable<SettingsRoute> {
            Hosted(Atmosphere.Stone, prefs) { SettingsDestination(prefs, viewModel, nav) }
        }
        composable<AboutRoute> {
            Hosted(Atmosphere.Stone, prefs) { AboutDestination(nav) }
        }
        composable<FoundationRoute> { FoundationCatalogRoute() }
    }
}

private fun NavController.goBack() {
    popBackStack()
}

/** Applies the screen's atmosphere and matches the status bar icons to it. */
@Composable
private fun Hosted(screenDefault: Atmosphere, prefs: AppPreferences, content: @Composable () -> Unit) {
    val atmosphere = prefs.atmosphereFor(screenDefault, isSystemInDarkTheme())
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !atmosphere.isDark
        }
    }
    ShapesTheme(atmosphere) {
        AtmosphereBackground(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun CustomizeDestination(kind: StatusWidgetKind, viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    var shapeName by rememberSaveable(kind) { mutableStateOf(kind.shapeOptions.firstOrNull()?.name) }
    var size by rememberSaveable { mutableStateOf(PreviewSize.Two) }
    var glow by rememberSaveable { mutableStateOf(true) }
    val shape = shapeName?.let(ShapeCatalog::valueOf)
    val faceFlow = remember(kind, shape, glow) { viewModel.face(kind, shape, glow) }
    val face by faceFlow.collectAsStateWithLifecycle(null)
    CustomizeScreen(
        kind = kind,
        face = face,
        shape = shape,
        size = size,
        glow = glow,
        onBack = nav::goBack,
        onShapeChange = { shapeName = it.name },
        onSizeChange = { size = it },
        onGlowChange = { glow = it },
        onAdd = {
            val asked = WidgetPinning.isSupported(context) && WidgetPinning.request(context, kind, shape, glow)
            if (!asked) nav.navigate(AddByHandRoute(kind.name))
        }
    )
}

@Composable
private fun SettingsDestination(prefs: AppPreferences, viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    var backgroundAllowed by remember { mutableStateOf(context.isBackgroundAllowed()) }
    LifecycleResumeEffect(Unit) {
        backgroundAllowed = context.isBackgroundAllowed()
        onPauseOrDispose {}
    }
    SettingsScreen(
        hero = prefs.hero,
        theme = prefs.theme,
        atmosphere = prefs.atmosphere,
        backgroundAllowed = backgroundAllowed,
        version = context.versionName(),
        showFoundation = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        onBack = nav::goBack,
        onThemeChange = viewModel::setTheme,
        onAtmosphereChange = viewModel::setAtmosphere,
        onBackgroundClick = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        onHeroClick = { nav.navigate(HeroPickerRoute) },
        onAboutClick = { nav.navigate(AboutRoute) },
        onFoundationClick = { nav.navigate(FoundationRoute) }
    )
}

@Composable
private fun AboutDestination(nav: NavController) {
    val context = LocalContext.current
    var licences by rememberSaveable { mutableStateOf<String?>(null) }
    val libraries = stringResource(R.string.about_licences_libraries)
    AboutScreen(
        version = context.versionName(),
        licences = licences,
        onBack = nav::goBack,
        onLicencesClick = { licences = context.readLicences(libraries) },
        onLicencesDismiss = { licences = null },
        onRateClick = { context.openPlayListing() }
    )
}

private fun Context.isBackgroundAllowed(): Boolean =
    getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

private fun Context.versionName(): String = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

private fun Context.readLicences(libraries: String): String {
    val fonts = listOf("doto-OFL.txt", "roboto-flex-OFL.txt").joinToString("\n\n") { file ->
        assets.open("licenses/$file").bufferedReader().use { it.readText() }
    }
    return "$libraries\n\n$fonts"
}

private fun Context.openPlayListing() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
    } catch (_: ActivityNotFoundException) {
        startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri())
        )
    }
}
