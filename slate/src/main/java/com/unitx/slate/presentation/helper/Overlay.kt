package com.unitx.slate.presentation.helper

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.unitx.slate.presentation.main.Slate.Companion.Tags

/**
 * Overlay creates and configures the blur/dimming overlay view that appears
 * behind bottom sheets in Slate.
 *
 * The overlay provides:
 * - Visual backdrop for bottom sheets (dimming effect)
 * - Click-to-dismiss functionality
 * - Accessibility support
 * - Smooth fade animations via alpha transitions
 */
object Overlay {

    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Creates a configured overlay view for bottom sheets.
     *
     * The overlay is created with:
     * - Full screen dimensions (MATCH_PARENT)
     * - Initial hidden state (alpha = 0f, visibility = GONE)
     * - Click listener for dismissal
     * - Accessibility support for screen readers
     * - Configurable background color (light/dark theme)
     *
     * The overlay's visibility and alpha are managed by Slate's internal
     * state callbacks during sheet expand/collapse/hide transitions.
     *
     * @param context The context for creating the view
     * @param overlayColor The background color configuration (Light/Dark theme)
     * @param onClick Callback invoked when overlay is clicked (typically hides sheet)
     * @return Fully configured overlay View ready to be added to container
     *
     * Usage in Slate:
     * ```
     * _blurOverlay = Overlay.createOverlay(
     *     context = bottomSheet.context,
     *     overlayColor = overlayColor,
     * ) {
     *     if (isExpanded) hide() // Hide sheet when overlay clicked
     * }
     * ```
     */
    fun createOverlay(
        context: Context,
        overlayColor: OverlayColor,
        onClick: () -> Unit
    ) = View(context).apply {
        // Set background color based on configuration (light/dark theme)
        setBackgroundColor(overlayColor.color)

        // Start fully transparent - animated to visible by Slate
        alpha = 0f

        // Start hidden - shown when sheet expands
        visibility = View.GONE

        // Enable clicks for dismissal functionality
        isClickable = true

        // Accessibility description for screen readers
        contentDescription = Tags.BlurAccessibility.content

        // Fill entire parent container
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Trigger provided callback on click (typically dismisses sheet)
        setOnClickListener { onClick() }
    }
}