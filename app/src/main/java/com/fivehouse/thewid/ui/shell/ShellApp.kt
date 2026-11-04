package com.fivehouse.thewid.ui.shell

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.PowerManager
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.fivehouse.thewid.R
import com.fivehouse.thewid.data.personal.MAX_PEOPLE
import com.fivehouse.thewid.data.personal.Person
import com.fivehouse.thewid.data.personal.PersonShapes
import com.fivehouse.thewid.data.personal.coarsePosition
import com.fivehouse.thewid.data.personal.messagesPerPerson
import com.fivehouse.thewid.data.settings.AppPreferences
import com.fivehouse.thewid.data.status.platform.NotificationAccess
import com.fivehouse.thewid.data.status.platform.ShapesNotificationListener
import com.fivehouse.thewid.heartpulse.HeartPulse
import com.fivehouse.thewid.heartpulse.PulseTrigger
import com.fivehouse.thewid.ui.catalog.FoundationCatalogRoute
import com.fivehouse.thewid.ui.components.AtmosphereBackground
import com.fivehouse.thewid.ui.screens.AboutScreen
import com.fivehouse.thewid.ui.screens.AccessRequest
import com.fivehouse.thewid.ui.screens.AccessScreen
import com.fivehouse.thewid.ui.screens.AddByHandScreen
import com.fivehouse.thewid.ui.screens.AddedScreen
import com.fivehouse.thewid.ui.screens.BreatheCompleteScreen
import com.fivehouse.thewid.ui.screens.BreatheSessionScreen
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
import com.fivehouse.thewid.ui.theme.Atmosphere
import com.fivehouse.thewid.ui.theme.ShapesTheme
import com.fivehouse.thewid.ui.widgetconfig.WidgetConfigureActivity
import com.fivehouse.thewid.widget.StatusFace
import com.fivehouse.thewid.widget.StatusWidgetKind
import com.fivehouse.thewid.widget.WidgetPinning
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable

@Serializable private data object SplashRoute

@Serializable private data object OnboardingRoute

@Serializable private data object LibraryRoute

@Serializable private data class CategoryRoute(val category: String)

@Serializable private data class CustomizeRoute(val kind: String)

@Serializable private data class PermissionRoute(val kind: String)

@Serializable private data class SetupRoute(val kind: String)

@Serializable private data object BreatheRoute

@Serializable private data object NowPlayingRoute

@Serializable private data object MochiRoute

@Serializable private data object InnerCircleRoute

@Serializable private data object DecideRoute

@Serializable private data object FocusRoute

@Serializable private data class AccessRoute(val request: String)

@Serializable private data object HeartPulseRoute

@Serializable private data class AddedRoute(val kind: String)

@Serializable private data class AddByHandRoute(val kind: String)

@Serializable private data object MyWidgetsRoute

@Serializable private data object HeroPickerRoute

@Serializable private data object SearchRoute

@Serializable private data object SettingsRoute

@Serializable private data object AboutRoute

@Serializable private data object FoundationRoute

private const val FEEDBACK_ADDRESS = ""

private fun kindNamed(name: String) = StatusWidgetKind.valueOf(name)

/** A kind's own screen, or null where its setup screen is the one to open. */
private fun pageFor(kind: StatusWidgetKind, viewModel: ShellViewModel): Any? = when (kind) {
    StatusWidgetKind.Breathe -> BreatheRoute
    StatusWidgetKind.NowPlaying -> NowPlayingRoute
    StatusWidgetKind.Mochi -> MochiRoute
    StatusWidgetKind.Focus -> FocusRoute
    StatusWidgetKind.InnerCircle -> InnerCircleRoute.takeIf { viewModel.people.value.isNotEmpty() }
    StatusWidgetKind.Decide -> DecideRoute.takeIf { viewModel.decision.value.canRoll }
    else -> null
}

@Composable
fun ShellApp(openRequest: MutableStateFlow<StatusWidgetKind?>, viewModel: ShellViewModel = hiltViewModel()) {
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

    val notificationAccess by NotificationAccess.granted.collectAsStateWithLifecycle()

    // Permission is only ever asked for here, after someone taps a widget that needs it.
    val heartPulseSettings by viewModel.heartPulse.collectAsStateWithLifecycle()
    val heartPulseIsOn = heartPulseSettings.enabled && AccessRequest.Overlay.isGranted(context)

    val toCustomize: (StatusWidgetKind) -> Unit = { kind ->
        when {
            kind.needsNotificationAccess && !notificationAccess -> nav.navigate(PermissionRoute(kind.name))
            kind.needsSetup -> nav.navigate(SetupRoute(kind.name))
            else -> nav.navigate(CustomizeRoute(kind.name))
        }
    }

    // A widget asked to open its screen. Waits for the splash to route so it isn't popped with it.
    val request by openRequest.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val pastSplash = entry?.destination?.let { !it.hasRoute<SplashRoute>() && !it.hasRoute<OnboardingRoute>() } == true
    LaunchedEffect(request, pastSplash) {
        val kind = request
        if (kind != null && pastSplash) {
            openRequest.value = null
            nav.navigate(pageFor(kind, viewModel) ?: SetupRoute(kind.name))
        }
    }

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
                CategoryScreen(
                    category,
                    faces,
                    nav::goBack,
                    toCustomize,
                    heartPulseOn = heartPulseIsOn,
                    onHeartPulseClick = { nav.navigate(HeartPulseRoute) }
                )
            }
        }
        composable<CustomizeRoute> { entry ->
            val kind = kindNamed(entry.toRoute<CustomizeRoute>().kind)
            Hosted(Atmosphere.Moss, prefs) { CustomizeDestination(kind, viewModel, nav) }
        }
        composable<NowPlayingRoute> {
            val track by viewModel.nowPlaying.collectAsStateWithLifecycle()
            Hosted(Atmosphere.Moss, prefs) {
                NowPlayingPage(
                    track = track,
                    extraFaces = listOfNotNull(
                        faces[StatusWidgetKind.Battery]?.let { R.string.widget_battery to it },
                        faces[StatusWidgetKind.Alerts]?.let { R.string.widget_alerts to it }
                    ),
                    onBack = nav::goBack,
                    onPlayPause = viewModel::playPause,
                    onNext = viewModel::nextTrack,
                    onPrevious = viewModel::previousTrack
                )
            }
        }
        composable<MochiRoute> {
            val mochi by viewModel.mochi.collectAsStateWithLifecycle()
            Hosted(Atmosphere.Moss, prefs) {
                MochiPage(
                    face = faces[StatusWidgetKind.Mochi],
                    name = mochi.name,
                    batteryFace = faces[StatusWidgetKind.Battery],
                    onBack = nav::goBack,
                    onEdit = { nav.navigate(SetupRoute(StatusWidgetKind.Mochi.name)) }
                )
            }
        }
        composable<InnerCircleRoute> {
            Hosted(Atmosphere.DuskBloom, prefs) { InnerCirclePageDestination(viewModel, nav) }
        }
        composable<DecideRoute> {
            val decision by viewModel.decision.collectAsStateWithLifecycle()
            Hosted(Atmosphere.Peach, prefs) {
                DecidePage(
                    decision = decision,
                    onRoll = viewModel::rollDecision,
                    onBack = nav::goBack,
                    onEdit = { nav.navigate(SetupRoute(StatusWidgetKind.Decide.name)) }
                )
            }
        }
        composable<FocusRoute> {
            Hosted(Atmosphere.DuskBloom, prefs) { FocusDestination(viewModel, nav) }
        }
        composable<HeartPulseRoute> {
            Hosted(Atmosphere.Night, prefs) { HeartPulseDestination(viewModel, nav) }
        }
        composable<AccessRoute> { backStack ->
            val request = AccessRequest.valueOf(backStack.toRoute<AccessRoute>().request)
            Hosted(Atmosphere.DuskBloom, prefs) { AccessDestination(request, nav) }
        }
        composable<SetupRoute> { backStack ->
            val kind = kindNamed(backStack.toRoute<SetupRoute>().kind)
            val toAdd = {
                nav.navigate(CustomizeRoute(kind.name)) { popUpTo<SetupRoute> { inclusive = true } }
            }
            val default = when (kind) {
                StatusWidgetKind.InnerCircle -> Atmosphere.DuskBloom
                StatusWidgetKind.Decide -> Atmosphere.Peach
                StatusWidgetKind.Time -> Atmosphere.Night
                else -> Atmosphere.Stone
            }
            Hosted(default, prefs) {
                when (kind) {
                    StatusWidgetKind.Week -> {
                        val events by viewModel.events.collectAsStateWithLifecycle()
                        val usingCalendar by viewModel.useCalendar.collectAsStateWithLifecycle()
                        WeekSetupScreen(
                            events,
                            viewModel::setEvents,
                            nav::goBack,
                            toAdd,
                            usingCalendar = usingCalendar
                        )
                    }

                    StatusWidgetKind.Time -> TimeSetupDestination(viewModel, nav)

                    StatusWidgetKind.InnerCircle -> InnerCircleDestination(viewModel, nav, toAdd)

                    StatusWidgetKind.Decide -> {
                        val decision by viewModel.decision.collectAsStateWithLifecycle()
                        DecideSetupScreen(decision, nav::goBack) { saved ->
                            viewModel.setDecision(saved)
                            toAdd()
                        }
                    }

                    else -> {
                        val mochi by viewModel.mochi.collectAsStateWithLifecycle()
                        MochiSetupScreen(mochi, faces[StatusWidgetKind.Mochi]) { saved ->
                            viewModel.setMochi(saved)
                            toAdd()
                        }
                    }
                }
            }
        }
        composable<BreatheRoute> {
            val log by viewModel.breathe.collectAsStateWithLifecycle()
            Hosted(Atmosphere.Mint, prefs) {
                BreatheDestination(
                    sessionsToday = log.sessionsOn(LocalDate.now()),
                    heartPulseOn = heartPulseIsOn && heartPulseSettings.duringBreathe,
                    onBreathChange = { HeartPulse.trigger(context, PulseTrigger.Breathe) },
                    onFinished = viewModel::logBreatheSession,
                    onDone = nav::goBack
                )
            }
        }
        composable<PermissionRoute> { entry ->
            val kind = kindNamed(entry.toRoute<PermissionRoute>().kind)
            Hosted(Atmosphere.DuskBloom, prefs) { PermissionDestination(kind, faces[kind], nav) }
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
                    options = StatusWidgetKind.entries.filter { !it.needsNotificationAccess || notificationAccess },
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
                    onSettingClick = { nav.navigate(SettingsRoute) },
                    onHeartPulseClick = { nav.navigate(HeartPulseRoute) }
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
    var atmosphere by rememberSaveable { mutableStateOf(Atmosphere.CoolMist) }
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
            val asked =
                WidgetPinning.isSupported(context) && WidgetPinning.request(context, kind, shape, glow, atmosphere)
            if (!asked) nav.navigate(AddByHandRoute(kind.name))
        },
        atmosphere = atmosphere,
        onAtmosphereChange = { atmosphere = it },
        secondaryLabel = when (kind) {
            StatusWidgetKind.Breathe -> stringResource(R.string.breathe_start)
            StatusWidgetKind.Time -> stringResource(R.string.customize_open_settings)
            else -> stringResource(R.string.customize_open, stringResource(kind.label))
        },
        onSecondary = when {
            kind == StatusWidgetKind.Time -> ({ nav.navigate(SetupRoute(kind.name)) })
            else -> pageFor(kind, viewModel)?.let { page -> { nav.navigate(page) } }
        }
    )
}

/** Screen 8. Turning on asks for the overlay permission, then finishes turning on when it returns. */
@Composable
private fun HeartPulseDestination(viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    val settings by viewModel.heartPulse.collectAsStateWithLifecycle()
    val notificationAccess by NotificationAccess.granted.collectAsStateWithLifecycle()
    var wantsOn by rememberSaveable { mutableStateOf(false) }
    var canDraw by remember { mutableStateOf(AccessRequest.Overlay.isGranted(context)) }
    val latest by rememberUpdatedState(settings)
    LifecycleResumeEffect(Unit) {
        canDraw = AccessRequest.Overlay.isGranted(context)
        NotificationAccess.refresh(context)
        if (wantsOn) {
            wantsOn = false
            if (canDraw) {
                viewModel.setHeartPulse(latest.copy(enabled = true))
                HeartPulse.trigger(context, PulseTrigger.Test)
            }
        }
        onPauseOrDispose {}
    }
    HeartPulseScreen(
        settings = settings,
        canDrawOverlays = canDraw,
        hasNotificationAccess = notificationAccess,
        onChange = viewModel::setHeartPulse,
        onNotificationToggle = { on ->
            if (on && !notificationAccess) {
                viewModel.setHeartPulse(settings.copy(onNotification = true))
                nav.navigate(PermissionRoute(StatusWidgetKind.Alerts.name))
            } else {
                viewModel.setHeartPulse(settings.copy(onNotification = on))
            }
        },
        onTurnOn = {
            if (canDraw) {
                viewModel.setHeartPulse(settings.copy(enabled = true))
                HeartPulse.trigger(context, PulseTrigger.Test)
            } else {
                wantsOn = true
                nav.navigate(AccessRoute(AccessRequest.Overlay.name))
            }
        },
        onTurnOff = { viewModel.setHeartPulse(settings.copy(enabled = false)) },
        onTest = { HeartPulse.trigger(context, PulseTrigger.Test) },
        onBack = nav::goBack
    )
}

/** Asks only when someone taps a feature that needs it. Coming back from settings re-checks. */
@Composable
private fun AccessDestination(request: AccessRequest, nav: NavController) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(PermissionStage.Ask) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        stage = if (granted) PermissionStage.Granted else PermissionStage.Denied
    }
    LifecycleResumeEffect(Unit) {
        if (request.isGranted(context)) {
            stage = PermissionStage.Granted
        } else if (asked && request.permission == null) {
            stage = PermissionStage.Denied
        }
        onPauseOrDispose {}
    }
    val openSystemSettings = {
        asked = true
        context.startActivity(Intent(request.settingsAction, "package:${context.packageName}".toUri()))
    }
    AccessScreen(
        request = request,
        stage = stage,
        onAllow = {
            val permission = request.permission
            if (permission != null) launcher.launch(permission) else openSystemSettings()
        },
        onOpenSettings = openSystemSettings,
        onBack = nav::goBack
    )
}

@Composable
private fun InnerCirclePageDestination(viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    val people by viewModel.people.collectAsStateWithLifecycle()
    val senders by ShapesNotificationListener.senders.collectAsStateWithLifecycle()
    val canSee by NotificationAccess.granted.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val withNumber: (Person, (String) -> Unit) -> Unit = { person, action ->
        if (!AccessRequest.Contacts.isGranted(context)) {
            nav.navigate(AccessRoute(AccessRequest.Contacts.name))
        } else {
            // A contact with no number still gets somewhere useful: their card.
            context.phoneNumberOf(person)?.let(action) ?: context.openContact(person)
        }
    }
    InnerCirclePage(
        people = people,
        messages = messagesPerPerson(people, senders),
        canSeeMessages = canSee,
        selectedIndex = selected,
        onSelect = { selected = it },
        onBack = nav::goBack,
        onEdit = { nav.navigate(SetupRoute(StatusWidgetKind.InnerCircle.name)) },
        onMessage = { person ->
            withNumber(person) { number -> context.startSafely(Intent(Intent.ACTION_SENDTO, "smsto:$number".toUri())) }
        },
        onCall = { person ->
            withNumber(person) { number -> context.startSafely(Intent(Intent.ACTION_DIAL, "tel:$number".toUri())) }
        },
        // Android has no general video call action. The contact card lists the person's video apps.
        onVideo = { person -> context.openContact(person) },
        onTurnOnMessages = { nav.navigate(PermissionRoute(StatusWidgetKind.Alerts.name)) }
    )
}

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Nothing on this device handles it, as with calling on a tablet.
    }
}

private fun Context.openContact(person: Person) = startSafely(Intent(Intent.ACTION_VIEW, person.lookupUri.toUri()))

private fun Context.phoneNumberOf(person: Person): String? = try {
    val id = contentResolver.query(
        person.lookupUri.toUri(),
        arrayOf(ContactsContract.Contacts._ID),
        null,
        null,
        null
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }
    id?.let {
        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(it.toString()),
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }
} catch (_: SecurityException) {
    null
}

/** Ticks once a second while open. The state itself lives on disk, so it keeps running when closed. */
@Composable
private fun FocusDestination(viewModel: ShellViewModel, nav: NavController) {
    val stored by viewModel.focus.collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    val current = stored.settledAt(now)
    FocusPage(
        state = current,
        nowMs = now,
        onBack = nav::goBack,
        onToggle = {
            val at = System.currentTimeMillis()
            now = at
            viewModel.updateFocus { state ->
                val settled = state.settledAt(at)
                if (settled.isRunning) settled.paused(at) else settled.started(at)
            }
        },
        onReset = { viewModel.updateFocus { it.settledAt(System.currentTimeMillis()).restarted() } },
        onSkip = { viewModel.updateFocus { it.settledAt(System.currentTimeMillis()).next() } }
    )
}

@Composable
private fun TimeSetupDestination(viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    val settings by viewModel.timeSettings.collectAsStateWithLifecycle()
    var wantsSunset by rememberSaveable { mutableStateOf(false) }
    var wantsExact by rememberSaveable { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val latest by rememberUpdatedState(settings)
    LifecycleResumeEffect(Unit) {
        if (wantsExact) {
            wantsExact = false
            if (AccessRequest.ExactAlarm.isGranted(context)) viewModel.setTimeSettings(latest.copy(exactMinute = true))
        }
        refresh++
        onPauseOrDispose {}
    }
    // Finding a position is asynchronous, so it runs here rather than inside the resume check.
    LaunchedEffect(refresh, wantsSunset) {
        if (wantsSunset && AccessRequest.Location.isGranted(context)) {
            wantsSunset = false
            val position = context.coarsePosition()
            viewModel.setTimeSettings(
                latest.copy(switchAtSunset = true, latitude = position?.first, longitude = position?.second)
            )
        }
    }
    TimeSetupScreen(
        settings = settings,
        locationMissing = settings.switchAtSunset && settings.latitude == null,
        onChange = viewModel::setTimeSettings,
        onSunsetToggle = { on ->
            if (!on) {
                viewModel.setTimeSettings(settings.copy(switchAtSunset = false))
            } else {
                wantsSunset = true
                if (!AccessRequest.Location.isGranted(context)) {
                    nav.navigate(AccessRoute(AccessRequest.Location.name))
                }
            }
        },
        onExactToggle = { on ->
            when {
                !on -> viewModel.setTimeSettings(settings.copy(exactMinute = false))

                AccessRequest.ExactAlarm.isGranted(context) ->
                    viewModel.setTimeSettings(settings.copy(exactMinute = true))

                else -> {
                    wantsExact = true
                    nav.navigate(AccessRoute(AccessRequest.ExactAlarm.name))
                }
            }
        },
        onBack = nav::goBack,
        onSave = {
            nav.navigate(CustomizeRoute(StatusWidgetKind.Time.name)) { popUpTo<SetupRoute> { inclusive = true } }
        }
    )
}

/** Picks through the system's contact picker, which needs no contacts permission. */
@Composable
private fun InnerCircleDestination(viewModel: ShellViewModel, nav: NavController, onSaved: () -> Unit) {
    val context = LocalContext.current
    val people by viewModel.people.collectAsStateWithLifecycle()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        val name = uri?.let(context::displayNameOf) ?: return@rememberLauncherForActivityResult
        val shape = PersonShapes[people.size % PersonShapes.size]
        viewModel.setPeople((people + Person(uri.toString(), name, shape)).take(MAX_PEOPLE))
    }
    InnerCircleSetupScreen(
        people = people,
        onAddPerson = { pick.launch(null) },
        onChange = viewModel::setPeople,
        onBack = nav::goBack,
        onSave = onSaved
    )
}

private fun Context.displayNameOf(uri: Uri): String? = try {
    contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
} catch (_: SecurityException) {
    null // The picker's one-time read grant didn't cover this provider.
}

/** Screens 32 and 22. The session is logged once, when it finishes. */
@Composable
private fun BreatheDestination(
    sessionsToday: Int,
    heartPulseOn: Boolean,
    onBreathChange: () -> Unit,
    onFinished: () -> Unit,
    onDone: () -> Unit
) {
    var run by rememberSaveable { mutableIntStateOf(0) }
    var complete by rememberSaveable { mutableStateOf(false) }
    if (complete) {
        BreatheCompleteScreen(
            sessionsToday,
            onAgain = {
                run++
                complete = false
            },
            onDone = onDone,
            heartPulseOn = heartPulseOn
        )
    } else {
        key(run) {
            BreatheSessionScreen(
                onBack = onDone,
                onFinished = {
                    onFinished()
                    complete = true
                },
                heartPulseOn = heartPulseOn,
                onBreathChange = onBreathChange
            )
        }
    }
}

/** Screens 6, 11 and 12. Returning from system settings decides whether it was granted or still off. */
@Composable
private fun PermissionDestination(kind: StatusWidgetKind, face: StatusFace?, nav: NavController) {
    val context = LocalContext.current
    var stage by rememberSaveable { mutableStateOf(PermissionStage.Ask) }
    var openedSettings by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        NotificationAccess.refresh(context)
        if (NotificationAccess.granted.value) {
            stage = PermissionStage.Granted
        } else if (openedSettings) {
            stage = PermissionStage.Denied
        }
        onPauseOrDispose {}
    }
    PermissionScreen(
        kind = kind,
        stage = stage,
        face = face,
        productName = stringResource(R.string.product_name),
        listedName = stringResource(R.string.app_name),
        onOpenSettings = {
            openedSettings = true
            context.openNotificationAccessSettings()
        },
        onBack = nav::goBack,
        onAdd = {
            val asked = WidgetPinning.isSupported(context) && WidgetPinning.request(context, kind, null, true)
            if (!asked) nav.navigate(AddByHandRoute(kind.name))
        },
        onCustomize = {
            nav.navigate(CustomizeRoute(kind.name)) { popUpTo<PermissionRoute> { inclusive = true } }
        }
    )
}

private fun Context.openNotificationAccessSettings() {
    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

@Composable
private fun SettingsDestination(prefs: AppPreferences, viewModel: ShellViewModel, nav: NavController) {
    val context = LocalContext.current
    val notificationsAllowed by NotificationAccess.granted.collectAsStateWithLifecycle()
    val useCalendar by viewModel.useCalendar.collectAsStateWithLifecycle()
    var wantsCalendar by rememberSaveable { mutableStateOf(false) }
    var overlayAllowed by remember { mutableStateOf(AccessRequest.Overlay.isGranted(context)) }
    var backgroundAllowed by remember { mutableStateOf(context.isBackgroundAllowed()) }
    LifecycleResumeEffect(Unit) {
        NotificationAccess.refresh(context)
        backgroundAllowed = context.isBackgroundAllowed()
        overlayAllowed = AccessRequest.Overlay.isGranted(context)
        if (wantsCalendar) {
            wantsCalendar = false
            if (AccessRequest.Calendar.isGranted(context)) viewModel.setUseCalendar(true)
        }
        onPauseOrDispose {}
    }
    SettingsScreen(
        hero = prefs.hero,
        theme = prefs.theme,
        atmosphere = prefs.atmosphere,
        notificationsAllowed = notificationsAllowed,
        overlayAllowed = overlayAllowed,
        calendarOn = useCalendar,
        backgroundAllowed = backgroundAllowed,
        version = context.versionName(),
        showFoundation = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        onBack = nav::goBack,
        onThemeChange = viewModel::setTheme,
        onAtmosphereChange = viewModel::setAtmosphere,
        onNotificationsClick = {
            if (notificationsAllowed) {
                context.openNotificationAccessSettings()
            } else {
                nav.navigate(PermissionRoute(StatusWidgetKind.Alerts.name))
            }
        },
        onOverlayClick = { nav.navigate(HeartPulseRoute) },
        onCalendarClick = {
            when {
                useCalendar -> viewModel.setUseCalendar(false)

                AccessRequest.Calendar.isGranted(context) -> viewModel.setUseCalendar(true)

                else -> {
                    wantsCalendar = true
                    nav.navigate(AccessRoute(AccessRequest.Calendar.name))
                }
            }
        },
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
        onRateClick = { context.openPlayListing() },
        onFeedbackClick = FEEDBACK_ADDRESS.takeIf { it.isNotBlank() }?.let { address ->
            { context.startSafely(Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri())) }
        }
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
