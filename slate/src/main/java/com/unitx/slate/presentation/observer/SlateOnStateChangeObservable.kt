package com.unitx.slate.presentation.observer

class SlateOnStateChangeObservable {
    private val observers = mutableListOf<SlateOnStateChangeObserver>()

    fun addObserver(observer: SlateOnStateChangeObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: SlateOnStateChangeObserver) {
        observers.remove(observer)
    }

    fun removeAllObservers() {
        observers.clear()
    }

    fun notifyStateChanged(state: Int) {
        observers.forEach { it.onStateChanged(state) }
    }

    fun notifySlide(offset: Float) {
        observers.forEach { it.onSlide(offset) }
    }
}