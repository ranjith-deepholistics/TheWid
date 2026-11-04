# Shapes — Build Spec (Version 1 · "Frost")

Android widget app where every widget is a **Material 3 Expressive shape** whose form carries the data.
Designs are in `/design` (exported PNGs, numbered 1–26 plus the concept shots). Treat them as the visual source of truth.

---

## 0. How to work (read first)

- Build in the **phases below, one phase per PR/session**. Stop after each phase, summarise, and wait for review.
- Kotlin + Jetpack Compose only. Follow the repo's Compose best-practices skill/conventions if present (Route + stateless Screen, hoisted state, previews for every state).
- Use the **real `MaterialShapes`** from `androidx.compose.material3` (M3 Expressive) and `androidx.graphics.shapes` (`RoundedPolygon`, `Morph`). The design mockups used hand-drawn approximations for some shapes — always use the real library shape named in this spec.
- No network calls, no analytics, no account. Everything stays on device.
- Ask before adding any dependency not listed here.

---

## 1. Stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 (Expressive APIs) |
| Shapes | `MaterialShapes.*`, `RoundedPolygon`, `Morph`, `toPath()` |
| Frosted glass (in app) | Haze library now; swap to official Compose backdrop blur when available — wrap behind one `Modifier.frostedGlass()` so the swap is one file |
| Widgets | Jetpack Glance (`GlanceAppWidget`) |
| Background work | WorkManager (periodic refresh) |
| Storage | DataStore (preferences + small proto/JSON for widget configs) |
| DI | Hilt (or Koin if already in repo) |
| Min SDK | 34 (Android 14) · Target latest stable |

---

## 2. Design tokens

**Fonts:** Roboto Flex (UI, weights 200–500) · Doto (dot-matrix numerals for every hero number/value).

**Core colours**
- Ink `#142033` · Secondary text `#3A4A60` · Accent (live / selected only) `#FF7A3D`
- Glass tile: white @ 35–45% alpha, 1px white @ 70–80% border, radius 32–40dp, blur 24dp
- Pills/buttons: fully rounded; primary = ink fill + white text

**Atmospheres** (screen backgrounds, vertical gradients)
| Name | Stops |
|---|---|
| Cool Mist | `#B7CBE2 → #CFDDEC → #EEF3F8` |
| Dusk Bloom | `#F8F6F7 → #F6ECF0 → #F0CFDD → #EBB3CA` |
| Moss (dark) | `#1A261E → #22362A → #3A5A43` (text `#EEF3EE`) |
| Lilac | `#F6F4F9 → #EAE3F4 → #D9CDEE` |
| Mint | `#F4F8F6 → #E6F2EC → #C9E4D6` |
| Stone | `#E9E3DA → #F2EEE8 → #FAF8F5` |
| Peach | `#FBF6F2 → #F8EADF → #F2CDB4` |
| Night (dark) | `#121A30 → #1A2645 → #2B3A66` |

**Glossy shape fill:** radial gradient, centre at 35%/28%, white → light tint → darker tint of the atmosphere. Optional soft ellipse shadow under hero shapes.

**Shape rules (apply everywhere)**
1. **Fill = level** (clip the fill from the bottom by %)
2. **Outline (dashed) = off / unavailable**
3. **Spikier = more activity** (Clover → Burst → Boom)
4. **Morph = state change** (animate with `Morph`, spring motion)

---

## 3. Widgets (data source · shape · permission)

| Widget | Data source | Shape(s) | Permission |
|---|---|---|---|
| Battery | `BatteryManager` / sticky battery intent | SoftBurst, fill = %, accent while charging | none |
| Cellular | `TelephonyManager.getSignalStrength()` | Pentagon, fill = bars | none |
| Wi-Fi | `ConnectivityManager` / `NetworkCapabilities` (strength only, no SSID) | Fan; dashed outline when off | none |
| Sound | `AudioManager.ringerMode` | Circle (ring) / Cookie12Sided (vibrate) / Ghostish (silent) | none |
| Alarm | `AlarmManager.getNextAlarmClock()` | SemiCircle (sunrise) | none |
| Time (hero) | system clock | Sunny (day) / Cookie12Sided (night) + Arrow hand + Diamond minute dot | none |
| Alerts | `NotificationListenerService` (count only) | Clover4Leaf, number inside | Notification access |
| Now playing | `MediaSessionManager.getActiveSessions(listenerComponent)` | Cookie12Sided album art; Square when paused | same Notification access |
| Week | user-entered dates (DataStore) | Diamond deadline · Burst busy · Heart birthday · Arrow trip · Circle free | none |
| Inner Circle | system contact picker (`PickContact`) | Heart / Flower / Clover4Leaf / Clover8Leaf per person | none |
| Decide | user options | one shape per option | none |
| Mochi (pet) | derived from status above | Puffy (happy), Ghostish (sleepy, low battery), SoftBoom (excited, charging) | none |
| Breathe | in-app session | Circle ↔ Flower morph, SoftBurst on finish | none |
| Heart Pulse | overlay around camera cutout (`DisplayCutout`) | Heart rings | Display over other apps |

**Widget sizes:** 1×1 (shape only), 2×2, 4×2, 4×4 where it makes sense.

---

## 4. Glance widget rules

- Glance **cannot blur** or draw `RoundedPolygon` directly. Render each shape to a `Bitmap` (`RoundedPolygon.toPath()` on an Android `Canvas`, with the glossy gradient + fill clip) and show it with `Image`.
- Fake the frost: white 35–45% background + light border + rounded corners.
- No continuous animation on the home screen. A morph = a new state on the next update.
- Updates: WorkManager periodic (15 min), plus immediate update when the app is opened and from any change broadcasts you can legally receive on the target API (check Android's implicit-broadcast restrictions; register at runtime where needed).
- Every widget has a **configure activity** (screen 10) and uses `requestPinAppWidget` from the app; check `isRequestPinAppWidgetSupported()` and show screen 16 (add by hand) when false.

---

## 5. Screens (match `/design` numbering)

1 Splash · 2–3 / 23–24 Onboarding (3 pages) · 3 Library (bento categories) · 4 Category list · 5 Customize (no permission) · 6 Permission ask · 11 Permission denied · 12 Permission granted · 7 Added confirmation · 8 Heart Pulse setup · 9 Settings · 10 Launcher configure sheet · 13 Hero picker · 14 My widgets (swipe to remove) · 15 Empty state · 16 Add by hand · 17 Time setup · 18 Week setup · 19 Inner Circle setup · 20 Decide setup · 21 Mochi setup · 22 Breathe complete (+ Breathe session from concept shot) · 25 Search · 26 About & privacy.

Navigation: Compose Navigation with type-safe routes. Floating pill nav bar on Library.

---

## 6. Build phases

1. **Foundation** — theme (tokens above), fonts, `ShapeCatalog` (enum → `MaterialShapes`), `GlossyShape` composable (fill %, outline, dashed), `GlassTile`, `frostedGlass()` modifier, `ShapeBitmapRenderer` for Glance. Preview screen showing every shape + every rule.
2. **Status data layer** — repositories for battery, cellular, Wi-Fi, sound, alarm, time as `Flow`s. Unit tests with fakes.
3. **Status widgets** — Glance widgets for Battery, Cellular, Wi-Fi, Sound, Alarm, Time in 1×1 / 2×2 / 4×2; configure activity; WorkManager refresh.
4. **App shell** — Splash, Onboarding, Library, Category, Customize, Added, My widgets, Empty, Hero picker, Add by hand, Search, Settings, About.
5. **Permissioned widgets** — NotificationListener (Alerts count, Now playing), permission ask/denied/granted flows.
6. **Personal widgets** — Week, Inner Circle, Decide, Mochi, Breathe (+ setup screens).
7. **Heart Pulse** — overlay service drawn around the camera cutout, triggers, permission flow.
8. **Polish** — morph animations, dark atmospheres, accessibility (content descriptions, 48dp targets, contrast), screenshot tests.

## 7. Definition of done (per phase)

- Builds, no lint errors, previews for every screen state.
- Matches the corresponding design PNGs closely (spacing, type, colours, shapes).
- No permission requested before the user taps a widget that needs it.
- Short summary of what was built + anything that differs from the design and why.
