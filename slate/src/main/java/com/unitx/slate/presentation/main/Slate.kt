package com.unitx.slate.presentation.main

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.unitx.slate.presentation.config.SlateConfig
import com.unitx.slate.presentation.controls.impl.AddNewButtonControl
import com.unitx.slate.presentation.controls.impl.CollapseButtonControl
import com.unitx.slate.presentation.controls.impl.SaveButtonControl
import com.unitx.slate.presentation.controls.SlateControlComposite
import com.unitx.slate.presentation.behavior.SlateBehaviour
import com.unitx.slate.presentation.config.OverlayColor
import com.unitx.slate.presentation.utilsSingleton.Overlay
import com.unitx.slate.presentation.utilsSingleton.SlatePositioning
import com.unitx.slate.presentation.observer.SlateOnStateChangeObservable
import com.unitx.slate.presentation.observer.SlateOnStateChangeObserver
import com.unitx.slate.presentation.radioImg.RadioImage
import com.unitx.slate.presentation.transition.DefaultStateTransitionStrategy
import com.unitx.slate.presentation.transition.StateTransitionStrategy

class Slate<T : Slate.ViewBinder>(
    private val hostView: View,
    private val lifecycleOwner: LifecycleOwner,
    private val onBackPressedDispatcher: OnBackPressedDispatcher,
    private val bindingListener: BindingListener<T>
){

    companion object {
        enum class Tags(val content: String, val metaData: Map<String, Any> = mapOf()) {
            BlurAccessibility("Dismiss Bottom Sheet!")
        }

        val initializationTracker = mutableMapOf<String, Boolean>()
    }

    interface BindingListener<T : ViewBinder> {
        fun onBindSheet(hostView: View): T
        fun onBindView(binder: T)
    }

    open class ViewBinder(val rootView: View) {
        var setSaveBtn: ImageView? = null
        var setCollapseBtn: RadioImage? = null
        var setAddNewBtn: View? = null
        var setOverlayColor: OverlayColor = OverlayColor.Light
        var onStateChangedFromBinder: ((Int) -> Unit)? = null
    }

    private var _container: ViewGroup? = null
    private val container get() = _container ?: error("Container not yet initialized. Call build() first.")

    private var _blurOverlay: View? = null
    private val blurOverlay get() = _blurOverlay ?: error("Blur overlay not yet initialized. Call build() first.")

    private var _binder: T? = null
    val binder get() = _binder ?: error("Binder not yet initialized. Call build() first.")

    private var _slateBehaviour: SlateBehaviour? = null
    private val slateBehaviour get() = _slateBehaviour ?: error("BottomSheetFacade not initialized.")

    private val bottomSheet get() = binder.rootView
    private val bottomSheetBehavior get() = BottomSheetBehavior.from(bottomSheet)
    private val collapseBtn get() = binder.setCollapseBtn
    private val onStateChangedFromBinder get() = binder.onStateChangedFromBinder
    private val overlayColor get() = binder.setOverlayColor

    private lateinit var lifecycleObserver: DefaultLifecycleObserver
    private lateinit var backPressedCallback: OnBackPressedCallback
    private var internalBottomSheetBehaviorCallback: BottomSheetCallback? = null

    private val controlComposite = SlateControlComposite()
    private var onStateChangeObservable = SlateOnStateChangeObservable()
    private var stateTransitionStrategy: StateTransitionStrategy<T> = DefaultStateTransitionStrategy()

    private val identifier: String
        get() = hashCode().toString()

    private var isInit: Boolean
        get() = initializationTracker[identifier] ?: false
        set(value) {
            if (value) bottomSheet.visibility = View.VISIBLE
            initializationTracker[identifier] = value
        }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MAIN LEVEL
    // ═══════════════════════════════════════════════════════════════════════════════

    internal fun initialize(
        config: SlateConfig,
        stateTransitionStrategy: StateTransitionStrategy<T>,
        externalCallback: BottomSheetCallback?,
        observers: List<SlateOnStateChangeObserver>
        ): Slate<T>
    {
        if (isInit) {
            Log.i("Slate", "Slate is already built. Returning the previous instance instead of rebuilding!")
            return this
        }

        observers.forEach{ doAddObserver(it) }
        doAddObserver(object :SlateOnStateChangeObserver{
            override fun onStateChanged(state: Int) {
                onStateChangedFromBinder?.invoke(state)
            }
        })
        this.stateTransitionStrategy = stateTransitionStrategy

        bindCore()
        bindViews()
        bindUserContent()
        bindInternalContent()
        bindConfig(
            config = config,
            externalCallback = externalCallback
        )
        bindSystemLevelCallbacks()

        isInit = true
        return this
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INIT LEVEL BIND FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun bindCore() {
        _container = hostView as ViewGroup
        _binder = bindingListener.onBindSheet(hostView)
        _blurOverlay = Overlay.createOverlay(
            context = bottomSheet.context,
            overlayColor = overlayColor,
        ) {
            if (isExpanded) hide()
        }
        _slateBehaviour = SlateBehaviour(BottomSheetBehavior.from(bottomSheet))
    }

    private fun bindViews() {
        (bottomSheet.parent as? ViewGroup)?.removeView(bottomSheet)
        (blurOverlay.parent as? ViewGroup)?.removeView(blurOverlay)

        // For the not configured view visibility blink!
        bottomSheet.visibility = View.INVISIBLE

        container.addView(bottomSheet)
        container.addView(blurOverlay)
        bottomSheet.bringToFront()
    }

    private fun bindUserContent() {
        bindingListener.onBindView(binder)
    }

    private fun bindInternalContent() {
        registerInternalControls()
        registerInternalStateChangeCallback()
    }

    private fun bindConfig(config: SlateConfig, externalCallback: BottomSheetCallback?) {
        binder.rootView.post {
            slateBehaviour.configure(config)
            externalCallback?.let { slateBehaviour.addCallback(it) }

            val bottomSheetPaddingBottom = SlatePositioning.adjustBottomSheetPositioning(
                bottomSheet = bottomSheet,
                container = container
            )
            SlatePositioning.handleKeyboardPositioning(
                bottomSheet = bottomSheet,
                bottomSheetPaddingBottom = bottomSheetPaddingBottom
            )
        }
    }

    private fun bindSystemLevelCallbacks() {
        binder.rootView.post {
            backPressedCallback = createBackPressedCallbackObject()
            lifecycleObserver = createOwnerLifecycleObserverObject()

            registerBackPressedCallback()
            registerOwnerLifecycleAwareness()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SETUP LEVEL REGISTER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerInternalControls() {
        controlComposite
            .add(SaveButtonControl(binder.setSaveBtn))
            .add(AddNewButtonControl(binder.setAddNewBtn))
            .add(
                CollapseButtonControl(
                    collapseBtn = binder.setCollapseBtn,
                    isCollapsible = !bottomSheetBehavior.skipCollapsed,
                    onCollapse = { collapse() },
                    onExpand = { expand() },
                ))
            .attach { hide() }
    }

    private fun registerInternalStateChangeCallback() {
        internalBottomSheetBehaviorCallback?.let { bottomSheetBehavior.removeBottomSheetCallback(it) }
        internalBottomSheetBehaviorCallback = createBottomSheetCallback()
        internalBottomSheetBehaviorCallback?.let { bottomSheetBehavior.addBottomSheetCallback(it) }
    }

    private fun registerBackPressedCallback() {
        onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback)
    }

    private fun registerOwnerLifecycleAwareness() {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BUILD LEVEL CREATE FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun createBackPressedCallbackObject() = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
                hide()
            } else {
                // If the sheet is already hidden, temporarily disable this callback
                // and let the default back press behavior (or next callback in chain) proceed.
                isEnabled = false
                binder.rootView.post { isEnabled = true } // Re-enable for future presses
                onBackPressedDispatcher.onBackPressed() // Delegate to the next callback
            }
        }
    }

    private fun createOwnerLifecycleObserverObject() = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            release()
        }
    }

    private fun createBottomSheetCallback() = object : BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_EXPANDED -> {
                    stateTransitionStrategy.onExpanded(this@Slate)
                    doNotifyStateChanged(BottomSheetBehavior.STATE_EXPANDED)
                }

                BottomSheetBehavior.STATE_COLLAPSED -> {
                    stateTransitionStrategy.onCollapsed(this@Slate)
                    doNotifyStateChanged(BottomSheetBehavior.STATE_COLLAPSED)
                }

                BottomSheetBehavior.STATE_HIDDEN -> {
                    stateTransitionStrategy.onHidden(this@Slate)
                    doNotifyStateChanged(BottomSheetBehavior.STATE_HIDDEN)
                }

                BottomSheetBehavior.STATE_DRAGGING,
                BottomSheetBehavior.STATE_SETTLING,
                BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                }
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            stateTransitionStrategy.onSlide(this@Slate, slideOffset)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SIMPLE HELPER LEVEL DO FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun doEnsureBuilt() {
        if (!isInit) throw IllegalStateException("Slate bottom sheet is not created, build() error!")
    }

    private fun doAddObserver(observer: SlateOnStateChangeObserver) {
        onStateChangeObservable.addObserver(observer)
    }

    private fun doNotifyStateChanged(state: Int) {
        onStateChangeObservable.notifyStateChanged(state)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════════

    val isExpanded get() = slateBehaviour.isExpanded
    val isCollapsed get() = slateBehaviour.isCollapsed
    val isHidden get() = slateBehaviour.isHidden

    fun expand(): Slate<T> {
        doEnsureBuilt()
        binder.rootView.post {
            slateBehaviour.expand()
        }
        return this
    }

    fun collapse(): Slate<T> {
        binder.rootView.post {
            slateBehaviour.collapse()
        }
        return this
    }

    fun hide(): Slate<T> {
        binder.rootView.post {
            slateBehaviour.hide()
        }
        return this
    }

    fun setState(state: Int): Slate<T> {
        binder.rootView.post {
            slateBehaviour.setState(state)
        }
        return this
    }

    fun blurOffSet(slideOffset: Float) {
        blurOverlay.apply {
            visibility = View.VISIBLE
            alpha = (slideOffset * 0.6f).coerceAtLeast(0f)
        }
    }

    fun arrowDown() {
        collapseBtn?.setState(toggled = false, animate = false)
    }

    fun arrowUp() {
        collapseBtn?.setState(toggled = true, animate = false)
    }

    fun blurHide() {
        blurOverlay.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                blurOverlay.isClickable = false
                blurOverlay.visibility = View.GONE
            }
            ?.start()
    }

    fun blurVisible() {
        blurOverlay.apply {
            visibility = View.VISIBLE
            isClickable = true
            animate().alpha(1f).setDuration(200).start()
        }
    }

    fun release() {
        if (!isInit) return

        // 1. Detach views from container
        if (bottomSheet.parent === _container) {
            _blurOverlay?.let { _container?.removeView(it) }
            _container?.removeView(bottomSheet)
        }

        // 2. Unregister system callbacks
        backPressedCallback.remove()
        lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)

        // 3. Remove internal bottom sheet callback
        internalBottomSheetBehaviorCallback?.let { bottomSheetBehavior.removeBottomSheetCallback(it) }
        internalBottomSheetBehaviorCallback = null

        // Cleanup controls and observers
        controlComposite.detach()
        onStateChangeObservable.removeAllObservers()

        // 5. Nullify references for garbage collection
        _blurOverlay = null
        _container = null
        _binder = null
        _slateBehaviour = null
        internalBottomSheetBehaviorCallback = null
        isInit = false

        initializationTracker.remove(identifier)
    }
}