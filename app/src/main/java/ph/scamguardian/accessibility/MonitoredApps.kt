package ph.scamguardian.accessibility

enum class AppKind { CHAT, BROWSER }

/** The apps the accessibility service reads. Keep in step with res/xml/scam_accessibility_service.xml. */
object MonitoredApps {
    private val chatNames =
        mapOf(
            "com.facebook.orca" to "Messenger",
            "com.facebook.mlite" to "Messenger Lite",
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "com.viber.voip" to "Viber",
            "org.telegram.messenger" to "Telegram",
            "com.google.android.apps.messaging" to "Messages",
            "com.samsung.android.messaging" to "Messages",
        )

    private val browserNames =
        mapOf(
            "com.android.chrome" to "Chrome",
            "com.sec.android.app.sbrowser" to "Samsung Internet",
            "org.mozilla.firefox" to "Firefox",
        )

    val chat: Set<String> = chatNames.keys

    val browser: Set<String> = browserNames.keys

    fun kindOf(packageName: String): AppKind? =
        when (packageName) {
            in chat -> AppKind.CHAT
            in browser -> AppKind.BROWSER
            else -> null
        }

    /** The name shown in the history, for example "Messenger". An unknown app keeps its package name. */
    fun nameOf(packageName: String): String = chatNames[packageName] ?: browserNames[packageName] ?: packageName
}
