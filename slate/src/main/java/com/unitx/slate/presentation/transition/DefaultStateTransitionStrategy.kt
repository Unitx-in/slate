package com.unitx.slate.presentation.transition

import com.unitx.slate.presentation.main.Slate

internal class DefaultStateTransitionStrategy <T: Slate.ViewBinder>: StateTransitionStrategy<T> {
    override fun onExpanded(slate: Slate<T>) {
        slate.arrowDown()
        slate.blurOverlayVisible()
    }

    override fun onCollapsed(slate: Slate<T>) {
        slate.arrowUp()
        slate.blurOverlayHide()
    }

    override fun onHidden(slate: Slate<T>) {
        slate.blurOverlayHide()
    }

    override fun onSlide(slate: Slate<T>, slideOffset: Float) {
        slate.blurOffSet(slideOffset)
    }
}