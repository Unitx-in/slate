package com.unitx.slate.presentation.observer

interface SlateOnStateChangeObserver {
    fun onStateChanged(state: Int) {}
    fun onSlide(offset: Float) {}
}