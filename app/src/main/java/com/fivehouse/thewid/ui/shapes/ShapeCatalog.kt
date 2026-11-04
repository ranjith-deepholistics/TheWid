package com.fivehouse.thewid.ui.shapes

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.graphics.shapes.RoundedPolygon

/** Every shape in the M3 Expressive library. Always draw through this, never a hand-made approximation. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
enum class ShapeCatalog(private val source: () -> RoundedPolygon) {
    Circle({ MaterialShapes.Circle }),
    Square({ MaterialShapes.Square }),
    Slanted({ MaterialShapes.Slanted }),
    Arch({ MaterialShapes.Arch }),
    Fan({ MaterialShapes.Fan }),
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

    val polygon: RoundedPolygon get() = source()
}
