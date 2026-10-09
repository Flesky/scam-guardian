package ph.scamguardian.accessibility

enum class AppKind { CHAT, FEED }

/** The apps the accessibility service reads. Keep in step with res/xml/scam_accessibility_service.xml. */
object MonitoredApps {
    val chat =
        setOf(
            "com.facebook.orca",
            "com.facebook.mlite",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.viber.voip",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
        )

    val feed =
        setOf(
            "com.facebook.katana",
            "com.facebook.lite",
            "com.instagram.android",
            "com.discord",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.twitter.android",
            "com.google.android.gm",
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
        )

    fun kindOf(packageName: String): AppKind? =
        when (packageName) {
            in chat -> AppKind.CHAT
            in feed -> AppKind.FEED
            else -> null
        }
}
