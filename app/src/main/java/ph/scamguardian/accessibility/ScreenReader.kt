package ph.scamguardian.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import ph.scamguardian.core.ScreenNode

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
                    result += ScreenNode(text, bounds.centerX(), containerId)
                }
                // Pushed in reverse so nodes come out in reading order.
                pending.addAll(childrenOf(node).reversed().map { it to id })
            }
        }
        return result
    }

    private fun textOf(node: AccessibilityNodeInfo): String? =
        node.text?.toString()?.takeIf { it.isNotBlank() && !node.isEditable }

    private fun childrenOf(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
        (0 until node.childCount).mapNotNull(node::getChild)
}
