package ph.scamguardian.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ph.scamguardian.ScamGuardianApp
import ph.scamguardian.core.BannerLimiter
import ph.scamguardian.core.HistoryEntry
import ph.scamguardian.core.ScamWarning
import ph.scamguardian.core.ScreenBlock
import ph.scamguardian.settings.DemoPreferences
import ph.scamguardian.settings.LanguagePreferences
import ph.scamguardian.theme.color
import java.util.UUID

/**
 * Tells the user about a warning: the banner, its history entry, and what "Not a scam" does.
 * An app gets at most one banner every 30 seconds. Use it on the main thread only.
 */
internal class WarningNotifier(
    service: AccessibilityService,
    private val scope: CoroutineScope,
    /** Called on the main thread when the banner has gone, so a warning that waited can be shown. */
    private val onBannerGone: () -> Unit = {},
) {
    private val app = service.application as ScamGuardianApp
    private val languages = LanguagePreferences(service)
    private val highlight = MessageHighlight(service)

    // The outline goes away with the banner.
    private val banner =
        WarningBanner(
            service,
            onDismiss = {
                guarded("remove the outline") { highlight.dismiss() }
                onBannerGone()
            },
        )
    private val limiter = BannerLimiter()
    private val demo = DemoPreferences(service)

    // History writes run one at a time and in order, off the main thread.
    private val files = Dispatchers.IO.limitedParallelism(1)

    /** False while [packageName] had a banner less than 30 seconds ago. Demo mode has no such wait. */
    fun mayShow(packageName: String): Boolean =
        limiter.isOpen(packageName, SystemClock.elapsedRealtime(), unlimited = demo.enabled)

    /** How long until [packageName] may show a banner again; 0 when it may show one now. */
    fun waitMs(packageName: String): Long =
        limiter.remainingMs(packageName, SystemClock.elapsedRealtime(), unlimited = demo.enabled)

    /**
     * Shows the banner for [warning], found in the text [block] of the app [packageName], and adds the
     * history entry. Returns false, and does nothing, while another banner is on screen (only one is
     * shown at a time) or when this app may not show a banner yet.
     */
    fun show(
        packageName: String,
        block: ScreenBlock,
        warning: ScamWarning,
    ): Boolean {
        val allowed =
            !banner.isShowing && limiter.tryShow(packageName, SystemClock.elapsedRealtime(), unlimited = demo.enabled)
        if (!allowed) return false
        // Read each time: the user may have changed the language since the last banner.
        val language = languages.language
        val catalog = app.engine.warnings
        val entry =
            HistoryEntry(
                id = UUID.randomUUID().toString(),
                type = warning.type,
                severity = warning.severity,
                brand = warning.brand,
                app = MonitoredApps.nameOf(packageName),
                timeMs = System.currentTimeMillis(),
                text = block.text,
            )
        val content =
            BannerContent(
                severity = warning.severity,
                title = catalog.title(warning.type),
                message = catalog.message(warning.type, language, warning.brand),
                brandRange = catalog.brandRange(warning.type, language, warning.brand),
            )
        banner.show(content, onNotScam = { markNotScam(block.text, entry.id) })
        block.bounds?.let { highlight.show(it, warning.severity.color.toArgb()) }
        scope.launch(files) { guarded("save the history entry") { app.history.add(entry) } }
        return true
    }

    fun dismiss() = banner.dismiss()

    /** The screen scrolled or changed window: the outline no longer sits on the message. */
    fun hideHighlight() = highlight.dismiss()

    // "Not a scam": the banner closes, the history entry is marked, and the text becomes a saved safe anchor.
    private fun markNotScam(
        block: String,
        entryId: String,
    ) {
        banner.dismiss()
        scope.launch(files) { guarded("mark the history entry") { app.history.markNotScam(entryId) } }
        scope.launch { guarded("save a safe message") { app.engine.markNotScam(block) } }
    }
}
