package ph.scamguardian.accessibility

enum class AppKind { CHAT, FEED }

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

    private val feedNames =
        mapOf(
            "com.facebook.katana" to "Facebook",
            "com.facebook.lite" to "Facebook Lite",
            "com.instagram.android" to "Instagram",
            "com.discord" to "Discord",
            "com.zhiliaoapp.musically" to "TikTok",
            "com.ss.android.ugc.trill" to "TikTok",
            "com.twitter.android" to "X",
            "com.google.android.gm" to "Gmail",
            "com.android.chrome" to "Chrome",
            "com.sec.android.app.sbrowser" to "Samsung Internet",
            "org.mozilla.firefox" to "Firefox",
        )

    val chat: Set<String> = chatNames.keys

    val feed: Set<String> = feedNames.keys

    fun kindOf(packageName: String): AppKind? =
        when (packageName) {
            in chat -> AppKind.CHAT
            in feed -> AppKind.FEED
            else -> null
        }

    /** The name shown in the history, for example "Messenger". An unknown app keeps its package name. */
    fun nameOf(packageName: String): String = chatNames[packageName] ?: feedNames[packageName] ?: packageName
}
