package ph.scamguardian.ui.main

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import ph.scamguardian.R

/**
 * The mascot's two sound effects. They are kept decoded in memory, so each one starts together with
 * its move. They play as interface sounds: the phone's silent and vibrate modes mute them.
 */
internal class MascotSounds(
    context: Context,
) {
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val assemble = pool.load(context, R.raw.mascot_on, 1)
    private val disassemble = pool.load(context, R.raw.mascot_off, 1)

    /** Plays the sound of [move], cutting off the other one. Silent until the sounds have loaded. */
    fun play(move: MascotMove) {
        val sound =
            when (move) {
                MascotMove.ASSEMBLE -> assemble
                MascotMove.DISASSEMBLE -> disassemble
            }
        pool.play(sound, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

/** The mascot's sounds for as long as the caller is on screen; none in a preview. */
@Composable
internal fun rememberMascotSounds(): MascotSounds? {
    if (LocalInspectionMode.current) return null
    val context = LocalContext.current.applicationContext
    val sounds = remember(context) { MascotSounds(context) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    return sounds
}
