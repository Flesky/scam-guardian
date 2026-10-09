package ph.scamguardian.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MonitoredAppsTest {
    @Test
    fun serviceConfig_listsExactlyTheMonitoredApps() {
        val config = File("src/main/res/xml/scam_accessibility_service.xml").readText()
        val packages = checkNotNull(Regex("""android:packageNames="([^"]+)"""").find(config)).groupValues[1].split(',')

        assertEquals((MonitoredApps.chat + MonitoredApps.browser).sorted(), packages.sorted())
        assertEquals(packages.size, packages.toSet().size)
    }

    @Test
    fun serviceConfig_readsWindowContentAndIsNotAnAccessibilityTool() {
        val config = File("src/main/res/xml/scam_accessibility_service.xml").readText()

        assertTrue("typeWindowContentChanged|typeWindowStateChanged|typeViewScrolled" in config)
        assertTrue("""android:canRetrieveWindowContent="true"""" in config)
        assertTrue("""android:isAccessibilityTool="false"""" in config)
    }

    @Test
    fun kindOf_sortsAppsIntoChatAndBrowser() {
        assertEquals(AppKind.CHAT, MonitoredApps.kindOf("com.facebook.orca"))
        assertEquals(AppKind.CHAT, MonitoredApps.kindOf("com.samsung.android.messaging"))
        assertEquals(AppKind.BROWSER, MonitoredApps.kindOf("com.android.chrome"))
        assertEquals(AppKind.BROWSER, MonitoredApps.kindOf("org.mozilla.firefox"))
        // Social and mail apps are not read.
        listOf("com.facebook.katana", "com.instagram.android", "com.google.android.gm", "com.discord")
            .forEach { assertNull(it, MonitoredApps.kindOf(it)) }
        assertNull(MonitoredApps.kindOf("ph.scamguardian"))
        assertTrue(MonitoredApps.chat.intersect(MonitoredApps.browser).isEmpty())
    }

    @Test
    fun nameOf_givesTheAppNameForEveryMonitoredApp() {
        assertEquals("Messenger", MonitoredApps.nameOf("com.facebook.orca"))
        assertEquals("Chrome", MonitoredApps.nameOf("com.android.chrome"))
        assertEquals("ph.scamguardian", MonitoredApps.nameOf("ph.scamguardian"))
        (MonitoredApps.chat + MonitoredApps.browser).forEach { assertTrue(it, MonitoredApps.nameOf(it) != it) }
    }

    @Test
    fun scamGuardianItself_isNeverRead() {
        val config = File("src/main/res/xml/scam_accessibility_service.xml").readText()

        assertNull(MonitoredApps.kindOf("ph.scamguardian"))
        assertTrue("ph.scamguardian" !in config)
    }
}
