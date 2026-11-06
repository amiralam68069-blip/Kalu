package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

data class ScreenElementInfo(
    val text: String,
    val contentDescription: String = "",
    val hintText: String = "",
    val className: String,
    val isClickable: Boolean,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean,
    val isScrollable: Boolean = false,
    val isChecked: Boolean = false,
    val viewId: String,
    val bounds: Rect = Rect(),
    val centerX: Int = 0,
    val centerY: Int = 0
)

class AmancipherAccessibilityService : AccessibilityService() {

    private var lastEventScanTimestamp = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        _lastGestureStatus.value = "Service connected & monitoring active window"
        inspectScreen()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotEmpty()) {
            _lastActivePackage.value = pkg
        }

        val now = System.currentTimeMillis()
        val eventType = event.eventType
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && now - lastEventScanTimestamp > 750L)
        ) {
            lastEventScanTimestamp = now
            inspectScreen()
        }
    }

    override fun onInterrupt() {
        _lastGestureStatus.value = "Accessibility service interrupted"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isConnected.value = false
            _lastGestureStatus.value = "Service disconnected"
        }
    }

    /**
     * Traverses the active window hierarchy and extracts all readable and interactive UI nodes
     * along with their exact screen coordinates.
     */
    fun inspectScreen(): List<ScreenElementInfo> {
        val elements = mutableListOf<ScreenElementInfo>()
        val root = rootInActiveWindow
        if (root != null) {
            root.packageName?.toString()?.let { pkg ->
                if (pkg.isNotEmpty()) _lastActivePackage.value = pkg
            }
            traverseNode(root, elements)
        } else {
            // Fallback to interactive windows list if rootInActiveWindow is null
            try {
                windows?.forEach { win ->
                    win.root?.let { winRoot ->
                        traverseNode(winRoot, elements)
                    }
                }
            } catch (_: Exception) {}
        }

        _lastInspectedElements.value = elements
        _lastScreenSummaryText.value = buildScreenSummary(elements)
        return elements
    }

    /**
     * Reads all visible text and interactive elements on the current screen as a structured summary.
     */
    fun readScreenContent(): String {
        val elements = inspectScreen()
        return buildScreenSummary(elements)
    }

    private fun buildScreenSummary(elements: List<ScreenElementInfo>): String {
        if (elements.isEmpty()) {
            return "Package: ${_lastActivePackage.value} | No readable nodes found in active window."
        }
        val texts = elements.mapNotNull { el ->
            val label = el.text.ifBlank { el.contentDescription.ifBlank { el.hintText } }
            if (label.isNotBlank()) label else null
        }.distinct()

        val clickables = elements.filter { it.isClickable && it.text.isNotBlank() }
            .map { "${it.text} (${it.centerX},${it.centerY})" }
            .distinct()
            .take(8)

        val editables = elements.filter { it.isEditable }
            .map { it.hintText.ifBlank { it.text.ifBlank { "Input Field" } } }

        return buildString {
            append("Active App: ${_lastActivePackage.value}\n")
            append("Nodes: ${elements.size} | Visible Text: ${texts.take(12).joinToString(" • ")}\n")
            if (clickables.isNotEmpty()) {
                append("Clickable Buttons: ${clickables.joinToString(", ")}\n")
            }
            if (editables.isNotEmpty()) {
                append("Editable Inputs: ${editables.joinToString(", ")}")
            }
        }.trim()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, list: MutableList<ScreenElementInfo>) {
        if (node == null || list.size >= 85) return
        if (!node.isVisibleToUser) return

        val rawText = node.text?.toString()?.trim() ?: ""
        val desc = node.contentDescription?.toString()?.trim() ?: ""
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            node.hintText?.toString()?.trim() ?: ""
        } else ""
        val displayLabel = when {
            rawText.isNotEmpty() -> rawText
            desc.isNotEmpty() -> desc
            hint.isNotEmpty() -> hint
            node.isEditable -> "[Editable Field]"
            else -> ""
        }

        val className = node.className?.toString()?.substringAfterLast('.') ?: "View"
        val viewId = node.viewIdResourceName?.substringAfterLast('/') ?: ""

        val rect = Rect()
        node.getBoundsInScreen(rect)

        if (displayLabel.isNotEmpty() || node.isEditable || (node.isClickable && viewId.isNotEmpty())) {
            list.add(
                ScreenElementInfo(
                    text = displayLabel.ifEmpty { "[$viewId]" },
                    contentDescription = desc,
                    hintText = hint,
                    className = className,
                    isClickable = node.isClickable,
                    isLongClickable = node.isLongClickable,
                    isEditable = node.isEditable,
                    isScrollable = node.isScrollable,
                    isChecked = node.isChecked,
                    viewId = viewId,
                    bounds = Rect(rect),
                    centerX = rect.centerX(),
                    centerY = rect.centerY()
                )
            )
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), list)
        }
    }

    // =========================================================================
    // NODE-BASED & COORDINATE-BASED CLICK / LONG-CLICK / TEXT INPUT
    // =========================================================================

    fun clickByText(query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return false
        val root = rootInActiveWindow ?: return false

        // 1. Try native Accessibility findAccessibilityNodeInfosByText
        val matchedNodes = root.findAccessibilityNodeInfosByText(cleanQuery)
        if (!matchedNodes.isNullOrEmpty()) {
            for (node in matchedNodes) {
                if (performClickOrGestureTap(node)) {
                    _lastGestureStatus.value = "Clicked '$cleanQuery'"
                    return true
                }
            }
        }

        // 2. Recursive case-insensitive search across text, contentDescription, hint, and viewId
        val clicked = searchAndClickRecursive(root, cleanQuery.lowercase(), longClick = false)
        _lastGestureStatus.value = if (clicked) "Clicked '$cleanQuery'" else "Could not find '$cleanQuery'"
        return clicked
    }

    fun longClickByText(query: String): Boolean {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        val clicked = searchAndClickRecursive(root, cleanQuery.lowercase(), longClick = true)
        _lastGestureStatus.value = if (clicked) "Long-pressed '$cleanQuery'" else "Could not find '$cleanQuery'"
        return clicked
    }

    private fun searchAndClickRecursive(
        node: AccessibilityNodeInfo?,
        targetLower: String,
        longClick: Boolean
    ): Boolean {
        if (node == null || !node.isVisibleToUser) return false
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.substringAfterLast('/')?.lowercase() ?: ""

        if (text.contains(targetLower) || desc.contains(targetLower) || viewId == targetLower) {
            val ok = if (longClick) performLongClickOrGesture(node) else performClickOrGestureTap(node)
            if (ok) return true
        }
        for (i in 0 until node.childCount) {
            if (searchAndClickRecursive(node.getChild(i), targetLower, longClick)) return true
        }
        return false
    }

    private fun performClickOrGestureTap(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    return true
                }
            }
            current = current.parent
            depth++
        }
        // Fallback: dispatch physical coordinate tap inside the node's screen bounds
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty && rect.centerX() > 0 && rect.centerY() > 0) {
            return performTap(rect.exactCenterX(), rect.exactCenterY())
        }
        return false
    }

    private fun performLongClickOrGesture(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isLongClickable) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                    return true
                }
            }
            current = current.parent
            depth++
        }
        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (!rect.isEmpty && rect.centerX() > 0 && rect.centerY() > 0) {
            return performLongPress(rect.exactCenterX(), rect.exactCenterY())
        }
        return false
    }

    fun inputText(textToType: String, targetFieldHint: String? = null): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. If a specific field hint/label was requested, search for that editable node first
        if (!targetFieldHint.isNullOrBlank()) {
            if (findAndSetEditableText(root, textToType, targetFieldHint.lowercase())) {
                _lastGestureStatus.value = "Typed '$textToType' into '$targetFieldHint'"
                return true
            }
        }

        // 2. Check currently focused input field
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (ok) {
                _lastGestureStatus.value = "Typed '$textToType' into focused input"
                return true
            }
        }

        // 3. Fallback to first visible editable node on screen
        val ok = setFirstEditableNodeText(root, textToType)
        _lastGestureStatus.value = if (ok) "Typed '$textToType'" else "No editable field found"
        return ok
    }

    private fun findAndSetEditableText(
        node: AccessibilityNodeInfo?,
        textToType: String,
        hintLower: String
    ): Boolean {
        if (node == null || !node.isVisibleToUser) return false
        if (node.isEditable) {
            val text = node.text?.toString()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.toString()?.lowercase() ?: ""
            } else ""
            if (text.contains(hintLower) || desc.contains(hintLower) || hint.contains(hintLower)) {
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
                }
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
        for (i in 0 until node.childCount) {
            if (findAndSetEditableText(node.getChild(i), textToType, hintLower)) return true
        }
        return false
    }

    private fun setFirstEditableNodeText(node: AccessibilityNodeInfo?, textToType: String): Boolean {
        if (node == null || !node.isVisibleToUser) return false
        if (node.isEditable) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToType)
            }
            return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        for (i in 0 until node.childCount) {
            if (setFirstEditableNodeText(node.getChild(i), textToType)) return true
        }
        return false
    }

    // =========================================================================
    // GESTURE DISPATCH ENGINE (TAP, DOUBLE TAP, LONG PRESS, SWIPE, PINCH)
    // =========================================================================

    /**
     * Performs a single tap gesture at the given screen coordinates (x, y).
     */
    fun performTap(
        x: Float,
        y: Float,
        durationMs: Long = 60L,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val safeX = x.coerceAtLeast(1f)
        val safeY = y.coerceAtLeast(1f)
        val path = Path().apply { moveTo(safeX, safeY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(20L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        _lastGestureStatus.value = "Tap at (${safeX.toInt()}, ${safeY.toInt()})"
        return dispatchGestureWithCallback(gesture, onComplete)
    }

    /**
     * Performs a rapid double-tap gesture at the given screen coordinates (x, y).
     */
    fun performDoubleTap(
        x: Float,
        y: Float,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val safeX = x.coerceAtLeast(1f)
        val safeY = y.coerceAtLeast(1f)
        val tapPath1 = Path().apply { moveTo(safeX, safeY) }
        val tapPath2 = Path().apply { moveTo(safeX, safeY) }

        val stroke1 = GestureDescription.StrokeDescription(tapPath1, 0L, 45L)
        val stroke2 = GestureDescription.StrokeDescription(tapPath2, 105L, 45L)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()
        _lastGestureStatus.value = "Double-tap at (${safeX.toInt()}, ${safeY.toInt()})"
        return dispatchGestureWithCallback(gesture, onComplete)
    }

    /**
     * Performs a long-press gesture at the given screen coordinates (x, y).
     */
    fun performLongPress(
        x: Float,
        y: Float,
        durationMs: Long = 650L,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val safeX = x.coerceAtLeast(1f)
        val safeY = y.coerceAtLeast(1f)
        val path = Path().apply { moveTo(safeX, safeY) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(500L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        _lastGestureStatus.value = "Long-press at (${safeX.toInt()}, ${safeY.toInt()})"
        return dispatchGestureWithCallback(gesture, onComplete)
    }

    /**
     * Performs a directional swipe gesture (UP, DOWN, LEFT, RIGHT) across the screen.
     * Note:
     * - Swipe UP moves the finger from bottom to top (scrolling content downward/forward).
     * - Swipe DOWN moves the finger from top to bottom (scrolling content upward/backward).
     * - Swipe LEFT moves the finger from right to left (next page/tab).
     * - Swipe RIGHT moves the finger from left to right (previous page/back gesture).
     */
    fun performSwipe(
        direction: SwipeDirection,
        distanceRatio: Float = 0.55f,
        durationMs: Long = 320L,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val dm = resources.displayMetrics
        val width = dm.widthPixels.toFloat().coerceAtLeast(300f)
        val height = dm.heightPixels.toFloat().coerceAtLeast(500f)
        val ratio = distanceRatio.coerceIn(0.2f, 0.85f)

        val midX = width / 2f
        val midY = height / 2f
        val halfSpanX = (width * ratio) / 2f
        val halfSpanY = (height * ratio) / 2f

        val (startX, startY, endX, endY) = when (direction) {
            SwipeDirection.UP -> listOf(midX, midY + halfSpanY, midX, midY - halfSpanY)
            SwipeDirection.DOWN -> listOf(midX, midY - halfSpanY, midX, midY + halfSpanY)
            SwipeDirection.LEFT -> listOf(midX + halfSpanX, midY, midX - halfSpanX, midY)
            SwipeDirection.RIGHT -> listOf(midX - halfSpanX, midY, midX + halfSpanX, midY)
        }

        _lastGestureStatus.value = "Swipe ${direction.name}"
        return performCustomDrag(startX, startY, endX, endY, durationMs, onComplete)
    }

    /**
     * Performs a smooth linear drag/swipe from (startX, startY) to (endX, endY).
     */
    fun performCustomDrag(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 350L,
        onComplete: ((Boolean) -> Unit)? = null
    ): Boolean {
        val path = Path().apply {
            moveTo(startX.coerceAtLeast(1f), startY.coerceAtLeast(1f))
            lineTo(endX.coerceAtLeast(1f), endY.coerceAtLeast(1f))
        }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceAtLeast(100L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGestureWithCallback(gesture, onComplete)
    }

    /**
     * Performs a 2-finger pinch zoom-in or zoom-out gesture around the center of the screen.
     */
    fun performPinchZoom(zoomIn: Boolean, durationMs: Long = 400L): Boolean {
        val dm = resources.displayMetrics
        val cx = dm.widthPixels / 2f
        val cy = dm.heightPixels / 2f
        val nearOffset = dm.widthPixels * 0.08f
        val farOffset = dm.widthPixels * 0.32f

        val path1 = Path()
        val path2 = Path()
        if (zoomIn) {
            path1.moveTo(cx - nearOffset, cy - nearOffset)
            path1.lineTo(cx - farOffset, cy - farOffset)
            path2.moveTo(cx + nearOffset, cy + nearOffset)
            path2.lineTo(cx + farOffset, cy + farOffset)
        } else {
            path1.moveTo(cx - farOffset, cy - farOffset)
            path1.lineTo(cx - nearOffset, cy - nearOffset)
            path2.moveTo(cx + farOffset, cy + farOffset)
            path2.lineTo(cx + nearOffset, cy + nearOffset)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0L, durationMs))
            .addStroke(GestureDescription.StrokeDescription(path2, 0L, durationMs))
            .build()
        _lastGestureStatus.value = if (zoomIn) "Pinch Zoom In" else "Pinch Zoom Out"
        return dispatchGestureWithCallback(gesture, null)
    }

    /**
     * Scrolls the active scrollable container or falls back to a physical swipe gesture.
     */
    fun scrollScreen(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow
        if (root != null && scrollNodeRecursive(root, forward)) {
            _lastGestureStatus.value = if (forward) "Scrolled down (node)" else "Scrolled up (node)"
            return true
        }
        return performSwipe(if (forward) SwipeDirection.UP else SwipeDirection.DOWN)
    }

    private fun scrollNodeRecursive(node: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        if (node == null || !node.isVisibleToUser) return false
        if (node.isScrollable) {
            val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            if (node.performAction(action)) return true
        }
        for (i in 0 until node.childCount) {
            if (scrollNodeRecursive(node.getChild(i), forward)) return true
        }
        return false
    }

    private fun dispatchGestureWithCallback(
        gesture: GestureDescription,
        onComplete: ((Boolean) -> Unit)?
    ): Boolean {
        return try {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        onComplete?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        onComplete?.invoke(false)
                    }
                },
                null
            )
        } catch (e: Exception) {
            onComplete?.invoke(false)
            false
        }
    }

    // =========================================================================
    // GLOBAL NAVIGATION & SYSTEM ACTIONS
    // =========================================================================

    fun navigateBack(): Boolean {
        _lastGestureStatus.value = "Global Action: Back"
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun navigateHome(): Boolean {
        _lastGestureStatus.value = "Global Action: Home"
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun openRecents(): Boolean {
        _lastGestureStatus.value = "Global Action: Recents"
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications(): Boolean {
        _lastGestureStatus.value = "Global Action: Notifications"
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings(): Boolean {
        _lastGestureStatus.value = "Global Action: Quick Settings"
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun takeScreenSnapshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            _lastGestureStatus.value = "Global Action: Screenshot"
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    companion object {
        @Volatile
        var instance: AmancipherAccessibilityService? = null
            private set

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        private val _lastActivePackage = MutableStateFlow("com.aistudio.nova.assistant")
        val lastActivePackage: StateFlow<String> = _lastActivePackage.asStateFlow()

        private val _lastInspectedElements = MutableStateFlow<List<ScreenElementInfo>>(emptyList())
        val lastInspectedElements: StateFlow<List<ScreenElementInfo>> = _lastInspectedElements.asStateFlow()

        private val _lastScreenSummaryText = MutableStateFlow("")
        val lastScreenSummaryText: StateFlow<String> = _lastScreenSummaryText.asStateFlow()

        private val _lastGestureStatus = MutableStateFlow("Standby")
        val lastGestureStatus: StateFlow<String> = _lastGestureStatus.asStateFlow()
    }
}
