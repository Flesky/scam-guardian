package ph.scamguardian.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import ph.scamguardian.core.ScreenBlock
import ph.scamguardian.core.ScreenBlocks
import ph.scamguardian.core.ScreenNode
import ph.scamguardian.core.ScreenRect

/** What is on screen in a watched app. */
class Screen(
    val packageName: String,
    val blocks: List<ScreenBlock>,
)

/** Reads the visible, non-editable text nodes of a window. */
object ScreenReader {
    private const val MAX_NODES = 3000
    private const val NO_CONTAINER = -1

    fun textNodes(root: AccessibilityNodeInfo): List<ScreenNode> {
        val result = mutableListOf<ScreenNode>()
        val bounds = Rect()
        val pending = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        pending.add(root to NO_CONTAINER)
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_NODES) {
            val (node, containerId) = pending.removeLast()
            val id = visited++
            // A hidden node hides everything under it.
            if (node.isVisibleToUser) {
                textOf(node)?.let { text ->
                    node.getBoundsInScreen(bounds)
                    val place = ScreenRect(bounds.left, bounds.top, bounds.right, bounds.bottom)
                    result += ScreenNode(text, bounds.centerX(), containerId, place)
                }
                // Pushed in reverse so nodes come out in reading order.
                pending.addAll(childrenOf(node).reversed().map { it to id })
            }
        }
        return result
    }

    /** The blocks of text worth checking in the window under [root], or null when its app is not watched. */
    fun read(root: AccessibilityNodeInfo): Screen? {
        val packageName = root.packageName?.toString().orEmpty()
        val blocks =
            when (MonitoredApps.kindOf(packageName)) {
                AppKind.CHAT -> {
                    // Measured against the app's own window, which is not the whole screen in split-screen.
                    val window = Rect().also(root::getBoundsInScreen)
                    ScreenBlocks.chat(textNodes(root), windowLeft = window.left, windowWidth = window.width())
                }

                AppKind.BROWSER -> {
                    ScreenBlocks.page(textNodes(root))
                }

                null -> {
                    null
                }
            }
        return blocks?.let { Screen(packageName, it) }
    }

    private fun textOf(node: AccessibilityNodeInfo): String? =
        node.text?.toString()?.takeIf { it.isNotBlank() && !node.isEditable }

    private fun childrenOf(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        (0 until node.childCount).mapNotNull(node::getChild)
}
