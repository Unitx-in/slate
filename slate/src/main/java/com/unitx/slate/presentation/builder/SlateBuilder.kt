package com.unitx.slate.presentation.builder

import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.unitx.slate.presentation.config.SlateConfig
import com.unitx.slate.presentation.main.Slate
import com.unitx.slate.presentation.observer.SlateOnStateChangeObserver
import com.unitx.slate.presentation.transition.DefaultStateTransitionStrategy
import com.unitx.slate.presentation.transition.StateTransitionStrategy

open class SlateBuilder<T : Slate.ViewBinder>{
    
    companion object{
        fun <T : Slate.ViewBinder> instance(
            currentInstance: Slate<T>?,
            hostView: View,
            lifecycleOwner: LifecycleOwner,
            onBackPressedDispatcher: OnBackPressedDispatcher,
            bindingListener: Slate.BindingListener<T>
        ): Slate<T> {
            return currentInstance ?: Slate(
                hostView,
                lifecycleOwner,
                onBackPressedDispatcher,
                bindingListener
            )
        }
    }

    private var config = SlateConfig()
    private var stateTransitionStrategy: StateTransitionStrategy<T> = DefaultStateTransitionStrategy()
    private var externalCallback: BottomSheetCallback? = null
    private val observers = mutableListOf<SlateOnStateChangeObserver>()

    fun peekHeight(height: Int) = apply {
        config = config.copy(peekHeight = height)
    }

    fun fitToContents(value: Boolean) = apply {
        config = config.copy(isFitToContents = value)
    }

    fun hideable(value: Boolean) = apply {
        config = config.copy(isHideable = value)
    }

    fun skipCollapsed(value: Boolean) = apply {
        config = config.copy(skipCollapsed = value)
    }

    fun draggable(value: Boolean) = apply {
        config = config.copy(draggable = value)
    }

    fun halfExpandedRatio(ratio: Float) = apply {
        config = config.copy(halfExpandedRatio = ratio)
    }

    fun stateTransitionStrategy(strategy: StateTransitionStrategy<T>) = apply {
        this.stateTransitionStrategy = strategy
    }

    fun bottomSheetCallback(callback: BottomSheetCallback) = apply {
        this.externalCallback = callback
    }

    fun addObserver(observer: SlateOnStateChangeObserver) = apply {
        observers.add(observer)
    }

    fun onStateChange(callback: (Int) -> Unit) = apply {
        observers.add(object : SlateOnStateChangeObserver {
            override fun onStateChanged(state: Int) {
                callback(state)
            }
        })
    }


    fun build(
        currentInstance: Slate<T>?,
        hostView: View,
        lifecycleOwner: LifecycleOwner,
        onBackPressedDispatcher: OnBackPressedDispatcher,
        bindingListener: Slate.BindingListener<T>
    ): Slate<T> {
        return instance(
            currentInstance = currentInstance,
            hostView = hostView,
            lifecycleOwner = lifecycleOwner,
            onBackPressedDispatcher = onBackPressedDispatcher,
            bindingListener = bindingListener
        ).apply {
            build(
                config = config,
                stateTransitionStrategy = stateTransitionStrategy,
                externalCallback = externalCallback,
                observers = observers
            )
        }
    }
}