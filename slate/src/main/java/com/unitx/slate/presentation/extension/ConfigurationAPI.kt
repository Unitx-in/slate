package com.unitx.slate.presentation.extension

import android.util.Log
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.unitx.slate.presentation.helper.FlagCache
import com.unitx.slate.presentation.main.Slate
import com.unitx.slate.presentation.main.onStateChangeType

/**
 * SlateExtensions.kt
 *
 * Extension properties and functions for the Slate class that provide:
 * - State management and caching via FlagCache
 * - Bottom sheet state queries and transitions
 * - Blur overlay animations
 * - Collapse button arrow state management
 * - Configuration API via default()
 *
 * These extensions enable a fluent, chainable API for working with Slate instances
 * while keeping the core Slate class focused on lifecycle and initialization.
 */

// ═══════════════════════════════════════════════════════════════════════════════
// GLOBAL CACHE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Global cache storing state flags for all Slate instances.
 * Keyed by Slate's unique identifier (hash code).
 *
 * Stores:
 * - isBuilt: Whether build() was called
 * - isConfigured: Whether default() was called
 * - sheetCallback: External BottomSheetCallback if set
 *
 * This cache is cleaned up automatically when release() is called on a Slate instance.
 */
val slateFlagCache = mutableMapOf<String, FlagCache>()

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION PROPERTIES - IDENTIFICATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Unique identifier for each Slate instance based on its hash code.
 * Used as key for the global flag cache.
 */
val Slate<*>.identifier: String
    get() = hashCode().toString()

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION PROPERTIES - LIFECYCLE STATE
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tracks whether the Slate instance has been fully built via build().
 *
 * - Set to true after build() completes successfully
 * - Used to prevent double-initialization
 * - Cleared when release() is called
 *
 * Stored in global slateFlagCache.
 */
var Slate<*>.isBuilt: Boolean
    get() = slateFlagCache[identifier]?.isBuilt ?: false
    set(value) {
        val entry = slateFlagCache.getOrPut(identifier) { FlagCache() }
        entry.isBuilt = value
    }

/**
 * Tracks whether the Slate instance has been configured via default().
 *
 * - Set to true after default() completes
 * - Makes bottom sheet visible when set to true
 * - Prevents reconfiguration if already configured
 * - Cleared when release() is called
 *
 * Stored in global slateFlagCache.
 */
var Slate<*>.isConfigured: Boolean
    get() = slateFlagCache[identifier]?.isConfigured ?: false
    set(value) {
        if (value) bottomSheet.visibility = View.VISIBLE
        val entry = slateFlagCache.getOrPut(identifier) { FlagCache() }
        entry.isConfigured = value
    }


// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION PROPERTIES - CALLBACK MANAGEMENT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Manages the external BottomSheetCallback for observing sheet state changes.
 *
 * Setter behavior:
 * - Automatically removes previous callback if exists
 * - Adds new callback to BottomSheetBehavior
 * - Stores in slateFlagCache for reference
 *
 * This is separate from Slate's internal callback that manages blur and arrows.
 * Use this for custom state change logic in your implementation.
 */
var Slate<*>.sheetCallback: BottomSheetCallback?
    get() = slateFlagCache[identifier]?.sheetCallback
    set(value) {
        val entry = slateFlagCache.getOrPut(identifier) { FlagCache() }
        entry.sheetCallback?.let { bottomSheetBehavior.removeBottomSheetCallback(it) }
        value?.let { bottomSheetBehavior.addBottomSheetCallback(it) }
        entry.sheetCallback = value
    }

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION PROPERTIES - STATE QUERIES
// ═══════════════════════════════════════════════════════════════════════════════

/** Returns true if bottom sheet is fully expanded */
val Slate<*>.isExpanded: Boolean
    get() = bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED

/** Returns true if bottom sheet is collapsed to peek height */
val Slate<*>.isCollapsed: Boolean
    get() = bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED

/** Returns true if bottom sheet is completely hidden */
val Slate<*>.isHidden: Boolean
    get() = bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - VALIDATION
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Validates that Slate has been properly initialized before state changes.
 *
 * Checks:
 * 1. build() was called (isBuilt = true)
 * 2. default() was called (isConfigured = true)
 *
 * @throws IllegalStateException if either check fails
 */
fun Slate<*>.ensureCreatedAndConfigured() {
    if (!isBuilt) throw IllegalStateException("Slate bottom sheet is not created, build() error!")
    if (!isConfigured) throw IllegalStateException("Slate bottom sheet is not configured, config() error")
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - STATE TRANSITIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Sets the bottom sheet to a specific state programmatically.
 *
 * @param state Target state constant from BottomSheetBehavior:
 *              - STATE_EXPANDED
 *              - STATE_COLLAPSED
 *              - STATE_HIDDEN
 *              - STATE_HALF_EXPANDED
 * @return This Slate instance for chaining
 *
 * Note: Posted to main thread to ensure proper timing with layout passes.
 */
fun <T : Slate.ViewBinder> Slate<T>.state(state: Int): Slate<T> {
    binder.rootView.post {
        bottomSheetBehavior.state = state
    }
    return this
}

/**
 * Collapses the bottom sheet to its peek height.
 *
 * @return This Slate instance for chaining
 */
fun <T : Slate.ViewBinder> Slate<T>.collapse(): Slate<T> {
    binder.rootView.post {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }
    return this
}

/**
 * Expands the bottom sheet to full height.
 *
 * Validates that Slate is built and configured before expanding.
 *
 * @return This Slate instance for chaining
 * @throws IllegalStateException if not built or configured
 */
fun <T : Slate.ViewBinder> Slate<T>.expand(): Slate<T> {
    binder.rootView.post {
        ensureCreatedAndConfigured()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
    return this
}

/**
 * Hides the bottom sheet completely.
 *
 * @return This Slate instance for chaining
 */
fun <T : Slate.ViewBinder> Slate<T>.hide(): Slate<T> {
    binder.rootView.post {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    }
    return this
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - COLLAPSE BUTTON ARROWS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Sets collapse button to show 'up' arrow (collapsed state indicator).
 * Called automatically when sheet transitions to collapsed state.
 */
fun Slate<*>.arrowUp() {
    collapseBtn?.setState(toggled = true, animate = false)
}

/**
 * Sets collapse button to show 'down' arrow (expanded state indicator).
 * Called automatically when sheet transitions to expanded state.
 */
fun Slate<*>.arrowDown() {
    collapseBtn?.setState(toggled = false, animate = false)
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - BLUR OVERLAY ANIMATIONS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Updates blur overlay opacity based on sheet slide position.
 *
 * Called during sheet dragging to create smooth fade effect.
 * Maximum opacity is 60% (0.6f) when fully expanded.
 *
 * @param slideOffset Value from -1.0 (hidden) to 1.0 (expanded)
 */
fun Slate<*>.blurOffSet(slideOffset: Float) {
    blurOverlay.apply {
        visibility = View.VISIBLE
        alpha = (slideOffset * 0.6f).coerceAtLeast(0f)
    }
}

/**
 * Hides blur overlay with fade-out animation.
 *
 * - Fades to transparent over 200ms
 * - Disables click interception after animation
 * - Sets visibility to GONE to avoid touch blocking
 *
 * Called when sheet collapses or hides.
 */
fun Slate<*>.blurHide() {
    blurOverlay.animate()
        ?.alpha(0f)
        ?.setDuration(200)
        ?.withEndAction {
            blurOverlay.isClickable = false
            blurOverlay.visibility = View.GONE
        }
        ?.start()
}

/**
 * Shows blur overlay with fade-in animation.
 *
 * - Makes visible immediately
 * - Enables click-to-dismiss
 * - Fades to opaque over 200ms
 *
 * Called when sheet expands.
 */
fun Slate<*>.blurVisible() {
    blurOverlay.apply {
        visibility = View.VISIBLE
        isClickable = true
        animate().alpha(1f).setDuration(200).start()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS - CONFIGURATION API
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Configures bottom sheet behavior properties and finalizes Slate setup.
 *
 * This is the final step after build(). It:
 * - Sets BottomSheetBehavior properties (peek height, draggability, etc.)
 * - Registers external state change callbacks
 * - Makes the bottom sheet visible
 * - Marks Slate as configured
 *
 * Safe to call multiple times - returns immediately if already configured.
 *
 * @param peekHeight Height in pixels when collapsed (0 = fully collapsed)
 * @param isFitToContents If true, sheet fits content; if false, expands to full height
 * @param isHideable If true, sheet can be hidden by dragging down
 * @param skipCollapsed If true, sheet goes directly from expanded to hidden
 * @param draggable If true, user can drag the sheet
 * @param halfExpandedRatio Ratio (0.0-1.0) of screen height for half-expanded state
 * @param bottomSheetCallback Optional external callback for state changes
 * @param onStateChangeCallback Optional Slate-level state change callback
 * @return This Slate instance for chaining
 *
 * Example usage:
 * ```
 * slate.build()
 *     .default(
 *         peekHeight = 200,
 *         isHideable = true,
 *         skipCollapsed = false,
 *         onStateChangeCallback = { state ->
 *             when (state) {
 *                 STATE_EXPANDED -> handleExpanded()
 *                 STATE_HIDDEN -> handleHidden()
 *             }
 *         }
 *     )
 *     .expand()
 * ```
 */
fun <T : Slate.ViewBinder> Slate<T>.default(
    peekHeight: Int = 0,
    isFitToContents: Boolean = true,
    isHideable: Boolean = true,
    skipCollapsed: Boolean = false,
    draggable: Boolean = true,
    halfExpandedRatio: Float = 0.5f,
    bottomSheetCallback: BottomSheetCallback? = null,
    onStateChangeCallback: onStateChangeType? = null
): Slate<T> {
    if (isConfigured) {
        Log.i("Slate", "Slate is already configured. Returning the previous instance.")
        return this
    }

    binder.rootView.post {
        // Configure BottomSheetBehavior properties
        bottomSheetBehavior.peekHeight = peekHeight
        bottomSheetBehavior.isFitToContents = isFitToContents
        bottomSheetBehavior.isHideable = isHideable
        bottomSheetBehavior.skipCollapsed = skipCollapsed
        bottomSheetBehavior.isDraggable = draggable
        bottomSheetBehavior.halfExpandedRatio = halfExpandedRatio

        // Set external BottomSheetCallback via extension property
        sheetCallback = bottomSheetCallback

        // Set Slate-level state change callback (defaults to empty lambda)
        this@default.onStateChangeSlate = onStateChangeCallback ?: {}

        // Mark as configured and make visible
        isConfigured = true
    }
    return this
}