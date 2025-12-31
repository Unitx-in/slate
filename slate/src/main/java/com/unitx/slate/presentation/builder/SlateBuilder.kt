package com.unitx.slate.presentation.builder

import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.unitx.slate.presentation.config.SlateConfig
import com.unitx.slate.presentation.main.Slate
import com.unitx.slate.presentation.observer.StateChangeObserver
import com.unitx.slate.presentation.transition.DefaultStateTransitionStrategy
import com.unitx.slate.presentation.transition.StateTransitionStrategy

class SlateBuilder<T : Slate.ViewBinder>{
    
    companion object {
        private fun <T : Slate.ViewBinder> instance(
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
    private var bottomSheetCallback: BottomSheetCallback? = null
    private val stateChangeObservers = mutableListOf<StateChangeObserver>()

    fun maxWidth(width: Int) = apply {
        config = config.copy(maxWidth = width)
    }

    fun maxHeight(height: Int) = apply {
        config = config.copy(maxHeight = height)
    }

    fun expandedOffset(offset: Int) = apply {
        config = config.copy(expandedOffset = offset)
    }

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

    fun initialState(state: Int) {
        config = config.copy(initialState = state)
    }

    fun addStateTransitionStrategy(strategy: StateTransitionStrategy<T>) = apply {
        this.stateTransitionStrategy = strategy
    }

    fun addBottomSheetCallback(callback: BottomSheetCallback) = apply {
        this.bottomSheetCallback = callback
    }

    fun addStateChangeObserver(stateChangeObserver: StateChangeObserver) = apply {
        stateChangeObservers.add(stateChangeObserver)
    }

    fun onStateChange(callback: (Int) -> Unit) = apply {
        stateChangeObservers.add(object : StateChangeObserver {
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
        onBind: (hostView: View) -> T,
        onBindView: (T) -> Unit,
    ): Slate<T> {
        return instance(
            currentInstance = currentInstance,
            hostView = hostView,
            lifecycleOwner = lifecycleOwner,
            onBackPressedDispatcher = onBackPressedDispatcher,
            bindingListener = object : Slate.BindingListener<T> {
                override fun onBindSheet(hostView: View): T = onBind(hostView)
                override fun onBindView(binder: T) = onBindView(binder)
            }
        ).apply {
            initialize(
                config = config,
                externalStateTransitionStrategy = stateTransitionStrategy,
                externalBottomSheetCallback = bottomSheetCallback,
                fragmentStateChangeObservers = stateChangeObservers
            )
        }
    }
}