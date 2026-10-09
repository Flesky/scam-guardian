package ph.scamguardian.accessibility

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import ph.scamguardian.R
import ph.scamguardian.core.Severity
import ph.scamguardian.theme.color
import ph.scamguardian.theme.icon
import kotlin.math.roundToInt

/** What one warning banner shows. */
internal data class BannerContent(
    val severity: Severity,
    val title: String,
    /** The message in the selected language, with the brand filled in. */
    val message: String,
    /** Where the brand is in [message], or null when there is none. */
    val brandRange: IntRange?,
    val evidence: String,
)

/** What the taps on a banner do. */
internal class BannerActions(
    val onClose: () -> Unit,
    val onNotScam: () -> Unit,
    /** Called after "Why?" opened or closed the evidence line. */
    val onWhy: () -> Unit,
)

/**
 * Builds the views of the warning banner: a card in the warning color with white text.
 *
 * These are plain views, not Compose: the banner window belongs to a service, which has none of the
 * owners (lifecycle, saved state) that Compose needs.
 */
internal class BannerViews(
    private val context: Context,
) {
    fun build(
        content: BannerContent,
        actions: BannerActions,
    ): View {
        val card =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background =
                    GradientDrawable().apply {
                        setColor(content.severity.color.toArgb())
                        cornerRadius = dp(CORNER_DP).toFloat()
                    }
                setPadding(dp(PADDING_DP), dp(PADDING_DP), dp(PADDING_DP), dp(SPACE_DP))
            }
        val evidence = text(content.evidence).apply { isVisible = false }
        card.addView(header(content))
        card.addView(text(messageText(content)), below())
        card.addView(whyLink(evidence, actions.onWhy), LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        card.addView(evidence)
        card.addView(buttons(actions))
        return FrameLayout(context).apply {
            addView(card, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            keepClearOfSystemBars(this)
        }
    }

    private fun header(content: BannerContent): View {
        val icon =
            ImageView(context).apply {
                setImageResource(content.severity.icon)
                setColorFilter(Color.WHITE)
            }
        val title = text(content.title, size = TITLE_SP, bold = true).apply { setPadding(dp(SPACE_DP), 0, 0, 0) }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icon, LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP)))
            addView(title)
        }
    }

    // The message with the brand name in bold.
    private fun messageText(content: BannerContent): CharSequence {
        val range = content.brandRange ?: return content.message
        return SpannableString(content.message).apply {
            setSpan(StyleSpan(Typeface.BOLD), range.first, range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    // "Why?" opens and closes the evidence line.
    private fun whyLink(
        evidence: View,
        onWhy: () -> Unit,
    ): View =
        text(context.getString(R.string.banner_why)).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            minHeight = dp(TOUCH_DP)
            minWidth = dp(TOUCH_DP)
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener {
                evidence.isVisible = !evidence.isVisible
                onWhy()
            }
        }

    private fun buttons(actions: BannerActions): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(button(R.string.banner_close, actions.onClose))
            addView(button(R.string.banner_not_scam, actions.onNotScam))
        }

    private fun text(
        text: CharSequence,
        size: Float = TEXT_SP,
        bold: Boolean = false,
    ) = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    // A flat button: white bold text on the card, no background of its own.
    private fun button(
        label: Int,
        onClick: () -> Unit,
    ) = Button(context).apply {
        text = context.getString(label)
        isAllCaps = false
        background = null
        stateListAnimator = null
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SP)
        minHeight = dp(TOUCH_DP)
        minimumHeight = dp(TOUCH_DP)
        minWidth = dp(TOUCH_DP)
        minimumWidth = dp(TOUCH_DP)
        setPadding(dp(PADDING_DP), 0, dp(PADDING_DP), 0)
        setOnClickListener { onClick() }
    }

    private fun below() = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(SPACE_DP) }

    // A margin around the card, plus the status bar and cutout when the window reaches under them.
    private fun keepClearOfSystemBars(root: View) {
        val margin = dp(SPACE_DP)
        root.setPadding(margin, margin, margin, margin)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(margin + bars.left, margin + bars.top, margin + bars.right, margin)
            insets
        }
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).roundToInt()

    private companion object {
        // Large text for older users: at least 18 sp, and 48 dp touch targets.
        const val TEXT_SP = 18f
        const val TITLE_SP = 20f
        const val TOUCH_DP = 48
        const val ICON_DP = 32
        const val PADDING_DP = 16
        const val SPACE_DP = 8
        const val CORNER_DP = 12
    }
}
