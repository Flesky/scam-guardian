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

private const val DARKEN = 0.65f

private fun filled(
    color: Int,
    cornerPx: Int,
) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = cornerPx.toFloat()
}

private fun darker(color: Int): Int =
    Color.rgb(
        (Color.red(color) * DARKEN).roundToInt(),
        (Color.green(color) * DARKEN).roundToInt(),
        (Color.blue(color) * DARKEN).roundToInt(),
    )

/**
 * Builds the views of the warning banner: a compact card in the warning color with white text and two
 * solid buttons.
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
        val cardColor = content.severity.color.toArgb()
        val card =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = filled(cardColor, dp(CARD_CORNER_DP))
                setPadding(dp(PADDING_DP), dp(SPACE_DP), dp(PADDING_DP), dp(PADDING_DP))
            }
        val evidence = text(content.evidence, size = SMALL_SP).apply { isVisible = false }
        card.addView(header(content, evidence, actions.onWhy))
        card.addView(text(messageText(content)))
        card.addView(evidence)
        card.addView(
            buttons(actions, cardColor),
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(SPACE_DP) },
        )
        return FrameLayout(context).apply {
            addView(card, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            keepClearOfSystemBars(this)
        }
    }

    // One line: the icon, the title, and "Why?" at the end.
    private fun header(
        content: BannerContent,
        evidence: View,
        onWhy: () -> Unit,
    ): View {
        val icon =
            ImageView(context).apply {
                setImageResource(content.severity.icon)
                setColorFilter(Color.WHITE)
            }
        val title = text(content.title, bold = true).apply { setPadding(dp(SPACE_DP), 0, 0, 0) }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(icon, LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP)))
            addView(title, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(whyLink(evidence, onWhy))
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
        text(context.getString(R.string.banner_why), size = SMALL_SP).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
            minHeight = dp(BUTTON_DP)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(PADDING_DP), 0, 0, 0)
            setOnClickListener {
                evidence.isVisible = !evidence.isVisible
                onWhy()
            }
        }

    // Two solid buttons at the end of the card: "Not a scam" in a darker shade, "Close" in white.
    private fun buttons(
        actions: BannerActions,
        cardColor: Int,
    ): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            val gap = LinearLayout.LayoutParams(WRAP_CONTENT, dp(BUTTON_DP)).apply { marginEnd = dp(SPACE_DP) }
            addView(button(R.string.banner_not_scam, darker(cardColor), Color.WHITE, actions.onNotScam), gap)
            addView(
                button(R.string.banner_close, Color.WHITE, cardColor, actions.onClose),
                LinearLayout.LayoutParams(WRAP_CONTENT, dp(BUTTON_DP)),
            )
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

    // A plain text view with a solid rounded fill; no platform button styling.
    private fun button(
        label: Int,
        fill: Int,
        textColor: Int,
        onClick: () -> Unit,
    ) = TextView(context).apply {
        text = context.getString(label)
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        background = filled(fill, dp(BUTTON_CORNER_DP))
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, SMALL_SP)
        minWidth = dp(BUTTON_MIN_WIDTH_DP)
        setPadding(dp(PADDING_DP), 0, dp(PADDING_DP), 0)
        setOnClickListener { onClick() }
    }

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
        const val TEXT_SP = 16f
        const val SMALL_SP = 14f
        const val ICON_DP = 20
        const val PADDING_DP = 12
        const val SPACE_DP = 8
        const val CARD_CORNER_DP = 10
        const val BUTTON_DP = 36
        const val BUTTON_MIN_WIDTH_DP = 72
        const val BUTTON_CORNER_DP = 8
    }
}
