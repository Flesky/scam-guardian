package ph.scamguardian.accessibility

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import ph.scamguardian.R

/**
 * The warning banner: a card at the top of the screen, drawn over the current app in an accessibility
 * overlay window. It does not take focus and covers only its own area, so the app below stays usable.
 *
 * Use it on the main thread only. [context] must be the accessibility service: only its window manager
 * may add this type of window. Taps and timers are [guarded], so they cannot crash the service.
 */
internal class WarningBanner(
    private val context: Context,
    private val onDismiss: () -> Unit = {},
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val autoHide = Runnable { guarded("hide the banner") { dismiss() } }
    private var view: View? = null

    /** Shows [content] in place of any banner on screen, vibrates once, and hides it after 15 seconds. */
    fun show(
        content: BannerContent,
        onNotScam: () -> Unit,
    ) {
        dismiss()
        val actions =
            BannerActions(
                onClose = { guarded("close the banner") { dismiss() } },
                onNotScam = { guarded("mark a message as not a scam") { onNotScam() } },
                // Opening the evidence gives the user the full 15 seconds again to read it.
                onWhy = { guarded("keep the banner open") { hideLater() } },
            )
        val banner = BannerViews(context).build(content, actions)
        windowManager.addView(banner, windowParams())
        view = banner
        vibrate()
        hideLater()
    }

    fun dismiss() {
        handler.removeCallbacks(autoHide)
        val banner = view ?: return
        view = null
        windowManager.removeView(banner)
        onDismiss()
    }

    private fun hideLater() {
        handler.removeCallbacks(autoHide)
        handler.postDelayed(autoHide, AUTO_HIDE_MS)
    }

    private fun windowParams() =
        WindowManager
            .LayoutParams(
                MATCH_PARENT,
                WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP
                // A window animation, run by the system. An animation inside the view would not run here:
                // some phones pause an app's own animations while it is in the background.
                windowAnimations = R.style.Animation_ScamGuardian_Banner
            }

    private fun vibrate() {
        context
            .getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createOneShot(VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private companion object {
        const val AUTO_HIDE_MS = 15_000L
        const val VIBRATION_MS = 200L
    }
}
