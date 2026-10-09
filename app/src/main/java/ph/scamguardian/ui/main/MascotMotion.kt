package ph.scamguardian.ui.main

import androidx.compose.animation.core.CubicBezierEasing

// The mascot is a machine: every part is rigid. Plates shoot in on straight rails and stop dead, the
// whole body jolts when they lock, and the eyes switch on and off. Released plates drop and tumble
// as solid pieces. Nothing stretches or bounces.
// All distances are in the mascot's own units: it is drawn 200 units wide.
private const val LOCK_MS = 70f
private const val LOCK_FADE = 0.4f
private const val FALL_MS = 420f
private const val FALL_DISTANCE = 230f
private const val FALL_FADE_START = 0.75f

// The eyes boot like a sensor: a white blink, dark, then on for good while the white cools down.
private const val EYES_BLINK_AT_MS = 160f
private const val EYES_DARK_AT_MS = 190f
private const val EYES_ON_AT_MS = 215f
private const val EYES_COOL_MS = 150f

private const val JOLT_AT_MS = 140f
private const val JOLT_MS = 120f
private const val JOLT_DROP = 3f

private const val RING_MS = 420f
private const val RING_START_SCALE = 0.5f
private const val RING_ALPHA = 0.8f

// While the mascot stands guard a line sweeps down its eyes, then waits for the rest of the period.
internal const val SCAN_PERIOD_MS = 3600
private const val SCAN_TOP = 84f
private const val SCAN_BOTTOM = 158f
private const val SCAN_SWEEP = 0.3f
private const val SCAN_FADE = 0.04f
private const val SCAN_ALPHA = 0.9f

// A plate speeds up all the way, so it is at full speed when it stops. A released plate falls as if dropped.
private val RailEasing = CubicBezierEasing(0.5f, 0f, 1f, 1f)
private val FallEasing = CubicBezierEasing(0.55f, 0f, 0.9f, 0.45f)

/** The mascot's two moves. Each one ends in a resting look: armored, or bare. */
internal enum class MascotMove(
    val durationMs: Float,
) {
    /** The armor plates lock onto the shell, then the eyes switch on. */
    ASSEMBLE(durationMs = 640f),

    /** The eyes switch off, then the plates release and fall off the shell. */
    DISASSEMBLE(durationMs = 600f),
}

/**
 * An armor plate on the left half of the mascot. The right half is its mirror image.
 *
 * It locks on along a straight rail that starts [out] units to the side and [down] units below its
 * place on the shell. When released it drops, drifting [fallOut] units to the side and turning
 * [fallTurn] degrees away from the middle.
 */
internal enum class Plate(
    val out: Float,
    val down: Float,
    val lockAtMs: Float,
    val fallOut: Float,
    val fallTurn: Float,
    val fallAtMs: Float,
) {
    CHEEK(out = 120f, down = 0f, lockAtMs = 0f, fallOut = 50f, fallTurn = -25f, fallAtMs = 130f),
    JAW(out = 60f, down = 110f, lockAtMs = 35f, fallOut = 20f, fallTurn = 20f, fallAtMs = 170f),
    CROWN(out = 60f, down = -90f, lockAtMs = 70f, fallOut = 30f, fallTurn = 35f, fallAtMs = 90f),
}

/** Where a left plate is, relative to its place on the shell. */
internal data class PlatePose(
    val dx: Float,
    val dy: Float,
    val degrees: Float,
    val alpha: Float,
)

/** [flash] is how white the eye is: it switches on white and cools down to its color. */
internal data class EyeLook(
    val alpha: Float,
    val flash: Float,
)

/** The ring that spreads from the mascot when the eyes switch on. */
internal data class Ring(
    val scale: Float,
    val alpha: Float,
)

/** The scanner line across the lit eyes: how far down it is, and how visible. */
internal data class ScanLine(
    val y: Float,
    val alpha: Float,
)

/** The scanner line at [cycle], from 0 at the start of a period to 1 at its end. */
internal fun scanLine(cycle: Float): ScanLine {
    val fade = progress(cycle, 0f, SCAN_FADE) * (1f - progress(cycle, SCAN_SWEEP, SCAN_FADE))
    return ScanLine(
        y = SCAN_TOP + (SCAN_BOTTOM - SCAN_TOP) * progress(cycle, 0f, SCAN_SWEEP),
        alpha = SCAN_ALPHA * fade,
    )
}

internal fun MascotMove.plate(
    plate: Plate,
    ms: Float,
): PlatePose =
    when (this) {
        MascotMove.ASSEMBLE -> {
            val progress = progress(ms, plate.lockAtMs, LOCK_MS)
            val away = 1f - RailEasing.transform(progress)
            PlatePose(
                -plate.out * away,
                plate.down * away,
                degrees = 0f,
                alpha = (progress / LOCK_FADE).coerceAtMost(1f),
            )
        }

        MascotMove.DISASSEMBLE -> {
            val progress = progress(ms, plate.fallAtMs, FALL_MS)
            val fallen = FallEasing.transform(progress)
            val alpha = ((1f - progress) / (1f - FALL_FADE_START)).coerceAtMost(1f)
            PlatePose(-plate.fallOut * fallen, FALL_DISTANCE * fallen, -plate.fallTurn * fallen, alpha)
        }
    }

internal fun MascotMove.eye(ms: Float): EyeLook =
    when (this) {
        MascotMove.ASSEMBLE -> {
            val lit = ms >= EYES_ON_AT_MS || (ms >= EYES_BLINK_AT_MS && ms < EYES_DARK_AT_MS)
            EyeLook(alpha = if (lit) 1f else 0f, flash = 1f - progress(ms, EYES_ON_AT_MS, EYES_COOL_MS))
        }

        MascotMove.DISASSEMBLE -> {
            EyeLook(alpha = 0f, flash = 0f)
        }
    }

/** How far the whole mascot is knocked down when the last plates lock. It drops at once and comes back. */
internal fun MascotMove.jolt(ms: Float): Float =
    when (this) {
        MascotMove.ASSEMBLE -> if (ms >= JOLT_AT_MS) JOLT_DROP * (1f - progress(ms, JOLT_AT_MS, JOLT_MS)) else 0f
        MascotMove.DISASSEMBLE -> 0f
    }

internal fun MascotMove.ring(ms: Float): Ring =
    when (this) {
        MascotMove.ASSEMBLE -> {
            val progress = progress(ms, EYES_ON_AT_MS, RING_MS)
            Ring(RING_START_SCALE + progress, alpha = if (progress > 0f) RING_ALPHA * (1f - progress) else 0f)
        }

        MascotMove.DISASSEMBLE -> {
            Ring(scale = 1f, alpha = 0f)
        }
    }

private fun progress(
    ms: Float,
    delayMs: Float,
    durationMs: Float,
): Float = ((ms - delayMs) / durationMs).coerceIn(0f, 1f)
