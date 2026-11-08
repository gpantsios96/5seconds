package app.anasa.pause

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * The breathing shape. [progress] is 0 when fully exhaled (small, soft "cookie" shape) and 1 when
 * fully inhaled (large, perfectly round). Shapes are built with androidx.graphics.shapes and morphed.
 */
@Composable
fun BreathingCircle(
    progress: Float,
    color: Color,
    haloColor: Color,
    modifier: Modifier = Modifier,
) {
    val morph = remember {
        val cookie = RoundedPolygon.star(
            numVerticesPerRadius = 9,
            innerRadius = 0.82f,
            rounding = CornerRounding(radius = 0.3f, smoothing = 0.6f),
        )
        val round = RoundedPolygon.circle(numVertices = 9)
        Morph(cookie, round)
    }
    // A slow idle rotation keeps the shape alive between breaths.
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2f
            val scale = 0.5f + 0.5f * progress
            val r = maxRadius * scale

            // Halo rings that trail the main shape.
            drawCircle(haloColor.copy(alpha = 0.18f * progress), radius = r * 1.12f)
            drawCircle(
                haloColor.copy(alpha = 0.35f),
                radius = maxRadius * 0.98f,
                style = Stroke(width = 2.dp.toPx()),
            )

            val path = morph.toPath(progress).asComposePath()
            val m = Matrix().apply {
                translate(center.x, center.y)
                scale(r, r)
            }
            path.transform(m)
            rotate(spin) { drawPath(path, color) }
        }
    }
}
