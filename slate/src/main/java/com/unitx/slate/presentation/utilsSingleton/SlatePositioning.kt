package com.unitx.slate.presentation.utilsSingleton

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

internal object SlatePositioning {
    fun adjustBottomSheetPositioning(bottomSheet: View, container: ViewGroup): Int {
        val screenHeight = bottomSheet.context.resources.displayMetrics.heightPixels
        val hostViewHeight = container.height
        var bottomPadding = bottomSheet.paddingBottom

        if (hostViewHeight > screenHeight) {
            ViewCompat.getRootWindowInsets(container)?.let { insets ->
                val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val layoutParams = bottomSheet.layoutParams as ViewGroup.MarginLayoutParams
                val currentBottomMargin = layoutParams.bottomMargin
                bottomPadding = currentBottomMargin + navBarHeight
                bottomSheet.updatePadding(bottom = bottomPadding)
            }
        }

        return bottomPadding
    }

    fun handleKeyboardPositioning(bottomSheet: View, bottomSheetPaddingBottom: Int) {
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