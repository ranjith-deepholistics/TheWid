package com.fivehouse.thewid.ui.shapes

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlin.math.cos
import kotlin.math.sin

private const val FAN_ROTATION_DEGREES = -45.0

/** Every shape in the M3 Expressive library. Always draw through this, never a hand-made approximation. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
enum class ShapeCatalog(private val source: () -> RoundedPolygon) {
    Circle({ MaterialShapes.Circle }),
    Square({ MaterialShapes.Square }),
    Slanted({ MaterialShapes.Slanted }),
    Arch({ MaterialShapes.Arch }),
    Fan({ MaterialShapes.Fan.pointingDown() }),
    Arrow({ MaterialShapes.Arrow }),
    SemiCircle({ MaterialShapes.SemiCircle }),
    Oval({ MaterialShapes.Oval }),
    Pill({ MaterialShapes.Pill }),
    Triangle({ MaterialShapes.Triangle }),
    Diamond({ MaterialShapes.Diamond }),
    ClamShell({ MaterialShapes.ClamShell }),
    Pentagon({ MaterialShapes.Pentagon }),
    Gem({ MaterialShapes.Gem }),
    Sunny({ MaterialShapes.Sunny }),
    VerySunny({ MaterialShapes.VerySunny }),
    Cookie4Sided({ MaterialShapes.Cookie4Sided }),
    Cookie6Sided({ MaterialShapes.Cookie6Sided }),
    Cookie7Sided({ MaterialShapes.Cookie7Sided }),
    Cookie9Sided({ MaterialShapes.Cookie9Sided }),
    Cookie12Sided({ MaterialShapes.Cookie12Sided }),
    Ghostish({ MaterialShapes.Ghostish }),
    Clover4Leaf({ MaterialShapes.Clover4Leaf }),
    Clover8Leaf({ MaterialShapes.Clover8Leaf }),
    Burst({ MaterialShapes.Burst }),
    SoftBurst({ MaterialShapes.SoftBurst }),
    Boom({ MaterialShapes.Boom }),
    SoftBoom({ MaterialShapes.SoftBoom }),
    Flower({ MaterialShapes.Flower }),
    Puffy({ MaterialShapes.Puffy }),
    PuffyDiamond({ MaterialShapes.PuffyDiamond }),
    PixelCircle({ MaterialShapes.PixelCircle }),
    PixelTriangle({ MaterialShapes.PixelTriangle }),
    Bun({ MaterialShapes.Bun }),
    Heart({ MaterialShapes.Heart });

    val polygon: RoundedPolygon by lazy(source)
}

/** The library Fan turned so its point faces down, as Wi-Fi strength is drawn in the mockups. */
private fun RoundedPolygon.pointingDown(): RoundedPolygon {
    val radians = Math.toRadians(FAN_ROTATION_DEGREES)
    val cosine = cos(radians).toFloat()
    val sine = sin(radians).toFloat()
    return transformed { x, y ->
        val dx = x - 0.5f
        val dy = y - 0.5f
        TransformResult(0.5f + dx * cosine - dy * sine, 0.5f + dx * sine + dy * cosine)
    }.normalized()
}
