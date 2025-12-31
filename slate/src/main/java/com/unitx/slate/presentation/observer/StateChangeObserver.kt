package com.unitx.slate.presentation.observer

interface StateChangeObserver {
    fun onStateChanged(state: Int) {}
    fun onSlide(offset: Float) {}
}