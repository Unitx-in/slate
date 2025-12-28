package com.unitx.slate.presentation.transition

import com.unitx.slate.presentation.main.Slate

interface StateTransitionStrategy <T: Slate.ViewBinder>{
    fun onExpanded(slate: Slate<T>)
    fun onCollapsed(slate: Slate<T>)
    fun onHidden(slate: Slate<T>)
    fun onSlide(slate: Slate<T>, slideOffset: Float)
}