package com.unitx.slate.presentation.behavior

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.unitx.slate.presentation.config.SlateConfig

internal class SlateBehaviour(private val behavior: BottomSheetBehavior<View>) {

    fun expand() {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun hide() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    fun setState(state: Int) {
        behavior.state = state
    }

    val isExpanded get() = behavior.state == BottomSheetBehavior.STATE_EXPANDED
    val isCollapsed get() = behavior.state == BottomSheetBehavior.STATE_COLLAPSED
    val isHidden get() = behavior.state == BottomSheetBehavior.STATE_HIDDEN
    val currentState get() = behavior.state

    fun configure(config: SlateConfig) {
        behavior.state = config.initialState
        behavior.peekHeight = config.peekHeight
        behavior.isFitToContents = config.isFitToContents
        behavior.isHideable = config.isHideable
        behavior.skipCollapsed = config.skipCollapsed
        behavior.isDraggable = config.draggable
        behavior.halfExpandedRatio = config.halfExpandedRatio
        behavior.maxWidth = config.maxWidth
        behavior.maxHeight = config.maxHeight
        behavior.expandedOffset = config.expandedOffset
    }

    fun addCallback(callback: BottomSheetCallback) {
        behavior.addBottomSheetCallback(callback)
    }

    fun removeCallback(callback: BottomSheetCallback) {
        behavior.removeBottomSheetCallback(callback)
    }
}