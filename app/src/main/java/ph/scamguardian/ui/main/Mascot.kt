package ph.scamguardian.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import ph.scamguardian.theme.MascotColors
import ph.scamguardian.theme.mascotColors

private val MascotWidth = 200.dp

// The mascot is drawn in its own units and scaled to its size on screen.
private const val UNITS_WIDE = 200f
private const val UNITS_HIGH = 206f
private val Middle = Offset(100f, 105f)

// Everything below is the left half, as x, y pairs. The right half is drawn mirrored.
private val PlatePoints =
    mapOf(
        Plate.CROWN to floatArrayOf(97f, 9f, 47f, 31f, 47f, 41f, 16f, 56f, 34f, 63f, 93f, 77f),
        Plate.CHEEK to floatArrayOf(16f, 56f, 19f, 120f, 91.6f, 150f, 93f, 110f, 82f, 87f, 36f, 73f, 34f, 63f),
        Plate.JAW to floatArrayOf(19f, 120f, 32f, 152f, 90f, 194f, 91.6f, 150f),
    )
private val EyePoints = floatArrayOf(43f, 87f, 66f, 111f, 69f, 156f, 43f, 122f)

// The shell under the armor, from its top middle to its bottom middle.
private val ShellPoints = floatArrayOf(100f, 24f, 58f, 42f, 36f, 60f, 40f, 122f, 54f, 152f, 96f, 190f, 100f, 190f)

// The armor (plates and eyes) is drawn slightly narrower than its numbers say: every point is this much
// closer to the middle. The shell keeps its width.
private const val ARMOR_WIDTH = 0.94f

// A thin outline in the plate's own color hides the hairline between two plates that touch.
private val Seam = Stroke(width = 0.8f, join = StrokeJoin.Round)
private val RingStroke = Stroke(width = 3f)
private const val RING_RADIUS = 70f
private const val SCAN_LINE_WIDTH = 3f

private class MascotShapes {
    val plates: Map<Plate, Path> = PlatePoints.mapValues { polygon(it.value, ARMOR_WIDTH) }
    val eye: Path = polygon(EyePoints, ARMOR_WIDTH)
    val shell: Path = polygon(ShellPoints + mirrored(ShellPoints))
}

// The same outline on the right half, walked backwards so the two halves join into one shape.
private fun mirrored(points: FloatArray): FloatArray {
    val result = FloatArray(points.size)
    for (i in points.indices step 2) {
        val from = points.size - 2 - i
        result[i] = UNITS_WIDE - points[from]
        result[i + 1] = points[from + 1]
    }
    return result
}

// [width] squeezes the shape towards the middle of the mascot: 1 leaves it as it is.
private fun polygon(
    points: FloatArray,
    width: Float = 1f,
): Path {
    fun x(i: Int) = Middle.x + (points[i] - Middle.x) * width

    val path = Path()
    path.moveTo(x(0), points[1])
    for (i in 2 until points.size step 2) path.lineTo(x(i), points[i + 1])
    path.close()
    return path
}

/**
 * The app's mascot, a sentinel. A plain gray shell while protection is off; in its solid
 * armor, with lit eyes, while it is on. The armor locks on and falls off when [isOn] changes, and
 * [onMove] is called as each of these moves starts. While it stays on, a scanner line sweeps the eyes.
 *
 * Flat fills only: no gradients, and no part ever stretches.
 */
@Composable
internal fun Mascot(
    isOn: Boolean,
    onMove: (MascotMove) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = mascotColors(isSystemInDarkTheme())
    val shapes = remember { MascotShapes() }
    val currentOnMove by rememberUpdatedState(onMove)
    val move = if (isOn) MascotMove.ASSEMBLE else MascotMove.DISASSEMBLE
    // Starts at the end of its move: the mascot only moves when the state changes in front of the user.
    var shown by remember { mutableStateOf(move) }
    val clock = remember { Animatable(move.durationMs) }
    val scan = remember { Animatable(0f) }
    LaunchedEffect(move) {
        if (move != shown) {
            shown = move
            currentOnMove(move)
            clock.snapTo(0f)
            clock.animateTo(move.durationMs, tween(move.durationMs.toInt(), easing = LinearEasing))
        }
        if (move == MascotMove.ASSEMBLE) {
            scan.snapTo(0f)
            scan.animateTo(1f, infiniteRepeatable(tween(SCAN_PERIOD_MS, easing = LinearEasing)))
        }
    }
    Canvas(modifier.size(MascotWidth, MascotWidth * (UNITS_HIGH / UNITS_WIDE))) {
        val ms = clock.value
        // The scanner only runs once the armor is on and still.
        val scanLine = if (shown == MascotMove.ASSEMBLE && ms >= shown.durationMs) scanLine(scan.value) else null
        scale(size.width / UNITS_WIDE, pivot = Offset.Zero) { drawMascot(shapes, colors, shown, ms, scanLine) }
    }
}

private fun DrawScope.drawMascot(
    shapes: MascotShapes,
    colors: MascotColors,
    move: MascotMove,
    ms: Float,
    scanLine: ScanLine?,
) {
    translate(top = move.jolt(ms)) {
        drawPath(shapes.shell, colors.shell)
        drawHalf(shapes, colors, move, ms, scanLine)
        scale(scaleX = -1f, scaleY = 1f, pivot = Middle) { drawHalf(shapes, colors, move, ms, scanLine) }
    }
    val ring = move.ring(ms)
    if (ring.alpha > 0f) {
        drawCircle(colors.eye, RING_RADIUS * ring.scale, Middle, alpha = ring.alpha, style = RingStroke)
    }
}

private fun DrawScope.drawHalf(
    shapes: MascotShapes,
    colors: MascotColors,
    move: MascotMove,
    ms: Float,
    scanLine: ScanLine?,
) {
    for ((plate, path) in shapes.plates) {
        val pose = move.plate(plate, ms)
        if (pose.alpha > 0f) {
            withTransform({
                translate(pose.dx, pose.dy)
                rotate(pose.degrees, path.getBounds().center)
            }) {
                drawPath(path, colors.armor, alpha = pose.alpha)
                drawPath(path, colors.armor, alpha = pose.alpha, style = Seam)
            }
        }
    }
    val eye = move.eye(ms)
    if (eye.alpha > 0f) {
        drawPath(shapes.eye, lerp(colors.eye, Color.White, eye.flash))
    }
    if (scanLine != null && scanLine.alpha > 0f) {
        clipPath(shapes.eye) {
            val start = Offset(0f, scanLine.y)
            drawLine(Color.White, start, Offset(Middle.x, scanLine.y), SCAN_LINE_WIDTH, alpha = scanLine.alpha)
        }
    }
}
