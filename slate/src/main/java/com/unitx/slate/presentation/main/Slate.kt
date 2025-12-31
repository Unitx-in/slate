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
import com.unitx.slate.presentation.config.OverlayColorDefault
import com.unitx.slate.presentation.utilsSingleton.Overlay
import com.unitx.slate.presentation.utilsSingleton.SlatePositioning
import com.unitx.slate.presentation.observer.OnStateChangeObservable
import com.unitx.slate.presentation.observer.StateChangeObserver
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
        var setOverlayColor: OverlayColor = OverlayColorDefault.Light
        var onStateChangedFromBinder: ((Int) -> Unit)? = null
    }

    private var _container: ViewGroup? = null
    private val container get() = _container ?: error("Container not yet initialized. Call build() first.")

    private var _blurOverlay: View? = null
    private val blurOverlay get() = _blurOverlay ?: error("Blur overlay not yet initialized. Call build() first.")

    private var _binder: T? = null
    private val binder get() = _binder ?: error("Binder not yet initialized. Call build() first.")

    private var _slateBehaviour: SlateBehaviour? = null
    private val slateBehaviour get() = _slateBehaviour ?: error("BottomSheetFacade not initialized.")

    private var _internalBottomSheetCallback: BottomSheetCallback? = null
    private val internalCallback get() = _internalBottomSheetCallback ?: error("Internal Callback not initialized")

    private var _externalBottomSheetCallback: BottomSheetCallback? = null

    private val bottomSheet get() = binder.rootView
    private val collapseBtn get() = binder.setCollapseBtn
    private val onStateChangedFromBinder get() = binder.onStateChangedFromBinder
    private val overlayColor get() = binder.setOverlayColor

    private lateinit var lifecycleObserver: DefaultLifecycleObserver
    private lateinit var backPressedCallback: OnBackPressedCallback

    private val controlComposite = SlateControlComposite()
    private var onStateChangeObservable = OnStateChangeObservable()
    private var stateTransitionStrategy: StateTransitionStrategy<T> = DefaultStateTransitionStrategy()

    private val identifier: String
        get() = hashCode().toString()

    private var isInit: Boolean
        get() = initializationTracker[identifier] ?: false
        set(value) {
            initializationTracker[identifier] = value
        }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MAIN LEVEL
    // ═══════════════════════════════════════════════════════════════════════════════

    internal fun initialize(
        config: SlateConfig,
        externalStateTransitionStrategy: StateTransitionStrategy<T>,
        externalBottomSheetCallback: BottomSheetCallback?,
        fragmentStateChangeObservers: List<StateChangeObserver>
        ): Slate<T>
    {
        if (isInit) {
            Log.i("Slate", "Slate is already built. Returning the previous instance instead of rebuilding!")
            return this
        }

        bindCore(
            externalStateTransitionStrategy = externalStateTransitionStrategy,
            fragmentStateChangeObservers = fragmentStateChangeObservers,
            externalBottomSheetCallback = externalBottomSheetCallback
        )
        bindViews()
        bindUserContent()
        bindInternalContent()
        bindConfig(config)
        bindSystemLevelCallbacks()
        isInit = true
        return this
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INIT LEVEL BIND FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun bindCore(
        externalStateTransitionStrategy: StateTransitionStrategy<T>,
        fragmentStateChangeObservers: List<StateChangeObserver>,
        externalBottomSheetCallback: BottomSheetCallback?
    ) {
        _container = hostView as ViewGroup
        _binder = bindingListener.onBindSheet(hostView)
        _blurOverlay = Overlay.createOverlay(
            context = bottomSheet.context,
            overlayColor = overlayColor,
        ) {
            if (isExpanded) hide()
        }
        _slateBehaviour = SlateBehaviour(BottomSheetBehavior.from(bottomSheet))

        registerStateTransitionStrategy(externalStateTransitionStrategy)
        registerStateChangeObservers(fragmentStateChangeObservers)
        _internalBottomSheetCallback = createBottomSheetCallback()
        _externalBottomSheetCallback = externalBottomSheetCallback
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

    private fun bindConfig(config: SlateConfig) {
        bottomSheet.post {
            slateBehaviour.configure(config)
            _externalBottomSheetCallback?.let { slateBehaviour.addCallback(it) }

            val bottomSheetPaddingBottom = SlatePositioning.adjustBottomSheetPositioning(
                bottomSheet = bottomSheet,
                container = container
            )
            SlatePositioning.handleKeyboardPositioning(
                bottomSheet = bottomSheet,
                bottomSheetPaddingBottom = bottomSheetPaddingBottom
            )
            bottomSheet.visibility = View.VISIBLE
        }
    }

    private fun bindSystemLevelCallbacks() {
        bottomSheet.post {
            backPressedCallback = createBackPressedCallbackObject()
            lifecycleObserver = createOwnerLifecycleObserverObject()

            registerBackPressedCallback()
            registerOwnerLifecycleAwareness()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SETUP LEVEL REGISTER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun registerStateChangeObservers(fragmentStateChangeObservers: List<StateChangeObserver>) {
        val binderStateChangeObserver = onStateChangedFromBinder?.let { observer->
            object : StateChangeObserver {
                override fun onStateChanged(state: Int) {
                    observer(state)
                }
            }
        }

        onStateChangeObservable.apply {
            fragmentStateChangeObservers.forEach{
                addObserver(it)
            }
            binderStateChangeObserver?.let {
                addObserver(it)
            }
        }
    }

    private fun registerInternalControls() {
        controlComposite
            .add(SaveButtonControl(binder.setSaveBtn))
            .add(AddNewButtonControl(binder.setAddNewBtn))
            .add(
                CollapseButtonControl(
                    collapseBtn = binder.setCollapseBtn,
                    isCollapsible = !slateBehaviour.isSkipCollapsed,
                    onCollapse = { collapse() },
                    onExpand = { expand() },
                ))
            .attach { hide() }
    }

    private fun registerInternalStateChangeCallback() {
        internalCallback.let { slateBehaviour.removeCallback(it) }
        internalCallback.let { slateBehaviour.addCallback(it) }
    }

    private fun registerBackPressedCallback() {
        onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback)
    }

    private fun registerOwnerLifecycleAwareness() {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }

    private fun registerStateTransitionStrategy(strategy: StateTransitionStrategy<T>) {
        stateTransitionStrategy = strategy
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BUILD LEVEL CREATE FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun createBackPressedCallbackObject() = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (slateBehaviour.currentState != BottomSheetBehavior.STATE_HIDDEN) {
                hide()
            } else {
                // If the sheet is already hidden, temporarily disable this callback
                // and let the default back press behavior (or next callback in chain) proceed.
                isEnabled = false
                bottomSheet.post { isEnabled = true } // Re-enable for future presses
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
        bottomSheet.post {
            slateBehaviour.expand()
        }
        return this
    }

    fun collapse(): Slate<T> {
        bottomSheet.post {
            slateBehaviour.collapse()
        }
        return this
    }

    fun hide(): Slate<T> {
        bottomSheet.post {
            slateBehaviour.hide()
        }
        return this
    }

    fun setState(state: Int): Slate<T> {
        bottomSheet.post {
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

    fun blurOverlayHide() {
        blurOverlay.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                blurOverlay.isClickable = false
                blurOverlay.visibility = View.GONE
            }
            ?.start()
    }

    fun blurOverlayVisible() {
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
        slateBehaviour.removeCallback(internalCallback)
        _externalBottomSheetCallback?.let { slateBehaviour.removeCallback(it) }

        // Cleanup controls and observers
        controlComposite.detach()
        onStateChangeObservable.removeAllObservers()

        // 5. Nullify references for garbage collection
        _blurOverlay = null
        _container = null
        _binder = null
        _slateBehaviour = null
        _internalBottomSheetCallback = null
        _externalBottomSheetCallback = null
        isInit = false

        initializationTracker.remove(identifier)
    }
}