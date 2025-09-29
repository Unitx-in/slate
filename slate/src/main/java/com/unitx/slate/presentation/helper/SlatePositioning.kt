package com.unitx.slate.presentation.helper

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * SlatePositioning handles bottom sheet positioning adjustments for:
 * - Edge-to-edge layouts where containers exceed screen height
 * - Keyboard (IME) appearance and dismissal
 * - Navigation bar insets
 *
 * This ensures bottom sheets remain properly positioned and visible
 * in various layout configurations and system UI states.
 */
object SlatePositioning {

    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Adjusts bottom sheet positioning when host container extends beyond visible screen.
     *
     * Handles edge-to-edge layouts where CoordinatorLayout or similar containers
     * can be larger than actual screen height, causing bottom sheets to be
     * positioned incorrectly.
     *
     * How it works:
     * 1. Compares container height vs screen height
     * 2. If container is taller, retrieves navigation bar height
     * 3. Adds navigation bar height to bottom padding to compensate
     *
     * @param bottomSheet The bottom sheet view to adjust
     * @param container The host ViewGroup containing the bottom sheet
     * @return The calculated bottom padding value for use in keyboard handling
     *
     * Example scenario:
     * - Screen height: 2400px
     * - Container height: 2500px (edge-to-edge with nav bar drawn behind)
     * - Nav bar: 100px
     * - Result: Adds 100px bottom padding to position sheet correctly
     */
    fun adjustBottomSheetPositioning(bottomSheet: View, container: ViewGroup): Int {
        val screenHeight = bottomSheet.context.resources.displayMetrics.heightPixels
        val hostViewHeight = container.height
        var bottomPadding = bottomSheet.paddingBottom

        // Check if host view extends beyond screen (common with edge-to-edge + CoordinatorLayout)
        if (hostViewHeight > screenHeight) {
            // Get the navigation bar height to adjust positioning
            ViewCompat.getRootWindowInsets(container)?.let { insets ->
                val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                // Adjust bottom sheet position to account for the oversized container
                val layoutParams = bottomSheet.layoutParams as ViewGroup.MarginLayoutParams
                val currentBottomMargin = layoutParams.bottomMargin
                bottomPadding = currentBottomMargin + navBarHeight
                bottomSheet.updatePadding(bottom = bottomPadding)
            }
        }

        return bottomPadding
    }

    /**
     * Handles dynamic bottom sheet positioning when keyboard (IME) appears/disappears.
     *
     * Sets up a window insets listener that:
     * - Adds padding when keyboard appears to keep content visible
     * - Restores original padding when keyboard disappears
     *
     * This prevents keyboard from covering bottom sheet content and ensures
     * smooth transitions during keyboard show/hide animations.
     *
     * @param bottomSheet The bottom sheet view to adjust for keyboard
     * @param bottomSheetPaddingBottom The base padding to restore when keyboard is hidden
     *        (typically the value returned from adjustBottomSheetPositioning)
     *
     * Behavior:
     * - Keyboard visible: Sets padding to keyboard height
     * - Keyboard hidden: Restores original padding
     * - Handles edge cases with coerceAtLeast(0) to prevent negative padding
     */
    fun handleKeyboardPositioning(bottomSheet: View, bottomSheetPaddingBottom: Int) {
        // Handle keyboard insets
        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
            val isVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (isVisible) {
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                val extraPadding = imeInsets.bottom.coerceAtLeast(0)
                view.updatePadding(bottom = extraPadding)
            } else {
                view.updatePadding(bottom = bottomSheetPaddingBottom)
            }
            insets
        }
    }
}