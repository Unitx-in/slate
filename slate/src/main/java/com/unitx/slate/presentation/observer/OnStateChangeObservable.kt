package com.unitx.slate.presentation.observer

internal class OnStateChangeObservable {
    private val observers = mutableListOf<StateChangeObserver>()

    fun addObserver(observer: StateChangeObserver) {
        observers.add(observer)
    }

    fun removeObserver(observer: StateChangeObserver) {
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