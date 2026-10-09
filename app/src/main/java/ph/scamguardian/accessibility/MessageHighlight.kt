package ph.scamguardian.accessibility

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import ph.scamguardian.core.ScreenRect
import kotlin.math.roundToInt

/**
 * An outline drawn around the message a warning is about, in an accessibility overlay window that
 * lets every touch through. Use it on the main thread only; [context] must be the accessibility service.
 */
internal class MessageHighlight(
    private val context: Context,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null

    /** Outlines [place] in [color], in place of any outline on screen. */
    fun show(
        place: ScreenRect,
        color: Int,
    ) {
        dismiss()
        val outline =
            View(context).apply {
                background =
                    GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(STROKE_DP), color)
                        cornerRadius = dp(CORNER_DP).toFloat()
                    }
            }
        windowManager.addView(outline, windowParams(place))
        view = outline
    }

    fun dismiss() {
        val outline = view ?: return
        view = null
        windowManager.removeView(outline)
    }

    // Placed in screen coordinates, a little larger than the text so the outline does not touch it.
    private fun windowParams(place: ScreenRect): WindowManager.LayoutParams {
        val margin = dp(MARGIN_DP)
        return WindowManager
            .LayoutParams(
                place.right - place.left + 2 * margin,
                place.bottom - place.top + 2 * margin,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = place.left - margin
                y = place.top - margin
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private companion object {
        const val STROKE_DP = 3
        const val CORNER_DP = 12
        const val MARGIN_DP = 4
    }
}
