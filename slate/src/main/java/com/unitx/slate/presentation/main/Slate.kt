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
import com.unitx.slate.presentation.extension.blurHide
import com.unitx.slate.presentation.extension.blurOffSet
import com.unitx.slate.presentation.extension.blurVisible
import com.unitx.slate.presentation.extension.appendClickListener
import com.unitx.slate.presentation.extension.arrowDown
import com.unitx.slate.presentation.extension.arrowUp
import com.unitx.slate.presentation.extension.collapse
import com.unitx.slate.presentation.extension.expand
import com.unitx.slate.presentation.extension.slateFlagCache
import com.unitx.slate.presentation.extension.hide
import com.unitx.slate.presentation.extension.identifier
import com.unitx.slate.presentation.extension.isBuilt
import com.unitx.slate.presentation.extension.isExpanded
import com.unitx.slate.presentation.extension.sheetCallback
import com.unitx.slate.presentation.helper.OverlayColor
import com.unitx.slate.presentation.helper.Overlay
import com.unitx.slate.presentation.helper.SlatePositioning
import com.unitx.slate.presentation.radioImg.RadioImage

typealias onStateChangeType = (Int) -> Unit

/**
 * Slate is a lifecycle-aware bottom sheet manager that handles:
 * - Bottom sheet behavior and state management
 * - Blur overlay coordination
 * - Back press handling
 * - Automatic cleanup on lifecycle destruction
 *
 * @param T The type of ViewBinder used for custom sheet content
 * @param hostView The parent container where the sheet will be attached
 * @param lifecycleOwner The lifecycle owner for automatic cleanup
 * @param onBackPressedDispatcher Dispatcher for handling back press events
 * @param bindingListener Listener for binding custom sheet content
 */
class Slate<T : Slate.ViewBinder>(
    private val hostView: View,
    private val lifecycleOwner: LifecycleOwner,
    private val onBackPressedDispatcher: OnBackPressedDispatcher,
    private val bindingListener: BindingListener<T>
) {

    // ═══════════════════════════════════════════════════════════════════════════════
    // COMPANION OBJECT
    // ═══════════════════════════════════════════════════════════════════════════════

    companion object {
        /** Tags for accessibility or identification purposes. */
        enum class Tags(val content: String, val metaData: Map<String, Any> = mapOf()) {
            BlurAccessibility("Dismiss Bottom Sheet!")
        }

        /**
         * Returns existing Slate instance or creates a new one if null.
         * Useful for implementing singleton pattern in fragments/activities.
         */
        fun <T : ViewBinder> singleInstance(
            currentInstance: Slate<T>?,
            hostView: View,
            lifecycleOwner: LifecycleOwner,
            onBackPressedDispatcher: OnBackPressedDispatcher,
            bindingListener: BindingListener<T>
        ): Slate<T> {
            return currentInstance ?: Slate(
                hostView,
                lifecycleOwner,
                onBackPressedDispatcher,
                bindingListener
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INTERFACES & OPEN CLASSES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Listener interface for binding custom sheet content and configuration.
     */
    interface BindingListener<T : ViewBinder> {
        /** Inflate and return the ViewBinder for the bottom sheet */
        fun onBindSheet(hostView: View): T

        /** Bind data and configure views in the sheet */
        fun onBindView(binder: T)
    }

    /**
     * Base class for custom sheet view holders.
     * Extend this to define your sheet's UI components and configuration.
     */
    open class ViewBinder(val rootView: View) {
        /** Optional save button that triggers sheet hide on click */
        var setSaveBtn: ImageView? = null

        /** Optional collapse/expand button with toggle state */
        var setCollapseBtn: RadioImage? = null

        /** Optional "add new" button that triggers sheet hide on click */
        var setAddNewBtn: View? = null

        /** Overlay background color theme (Light/Dark) */
        var setOverlayColor: OverlayColor = OverlayColor.Light

        /** Optional callback for sheet state changes from binder level */
        var onStateChangedCallbackReusable: onStateChangeType? = null
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PROPERTIES - CORE COMPONENTS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** The host container where sheet and overlay are attached */
    private var _container: ViewGroup? = null
    val container get() = _container ?: error("Container not yet initialized. Call build() first.")

    /** The blur overlay view positioned behind the sheet */
    private var _blurOverlay: View? = null
    val blurOverlay get() = _blurOverlay ?: error("Blur overlay not yet initialized. Call build() first.")

    /** The ViewBinder instance holding sheet configuration and views */
    private var _binder: T? = null
    val binder get() = _binder ?: error("Binder not yet initialized. Call build() first.")

    // ═══════════════════════════════════════════════════════════════════════════════
    // PROPERTIES - DERIVED & CONVENIENCE
    // ═══════════════════════════════════════════════════════════════════════════════

    /** The bottom sheet root view */
    val bottomSheet get() = binder.rootView

    /** The BottomSheetBehavior controlling the sheet */
    val bottomSheetBehavior get() = BottomSheetBehavior.from(bottomSheet)

    /** Optional save button from binder */
    private val saveBtn get() = binder.setSaveBtn

    /** Optional collapse button from binder */
    val collapseBtn get() = binder.setCollapseBtn

    /** Optional add new button from binder */
    private val addNewBtn get() = binder.setAddNewBtn

    /** Optional state change callback from binder */
    private val onStateChangeBinder get() = binder.onStateChangedCallbackReusable

    /** Slate-level state change callback */
    var onStateChangeSlate: onStateChangeType = {}

    /** Background overlay color configuration */
    private val overlayColor get() = binder.setOverlayColor

    // ═══════════════════════════════════════════════════════════════════════════════
    // PROPERTIES - LIFECYCLE & CALLBACKS
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Observes lifecycle for automatic cleanup */
    private lateinit var lifecycleObserver: DefaultLifecycleObserver

    /** Handles back press to hide sheet */
    private lateinit var backPressedCallback: OnBackPressedCallback

    /** Internal callback managing blur and arrow states */
    private var internalBottomSheetBehaviorCallback: BottomSheetCallback? = null

    // ═══════════════════════════════════════════════════════════════════════════════
    // PUBLIC API - LIFECYCLE METHODS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Builds and initializes the Slate instance for the first time.
     * Call this once after creating a Slate object. Safe to call multiple times.
     *
     * Sets up:
     * - Core variables (container, binder, blur overlay)
     * - View attachment and content binding
     * - System callbacks (back press, lifecycle observer)
     *
     * @return This Slate instance for chaining
     * @throws IllegalStateException if build fails
     */
    fun build(): Slate<T> {
        if (isBuilt) {
            Log.i("Slate", "Slate is already built. Returning the previous instance instead of rebuilding!")
            return this
        }

        initCoreVariables()
        bindCore()
        initSystemLevelCallbacks()

        isBuilt = true
        return this
    }

    /**
     * Releases all resources held by the Slate instance.
     * MUST be called when Slate is no longer needed (e.g., Fragment's onDestroyView).
     *
     * Performs cleanup:
     * 1. Detaches views from container
     * 2. Unregisters system callbacks (back press, lifecycle)
     * 3. Removes bottom sheet callbacks
     * 4. Nullifies references for garbage collection
     * 5. Clears cache entries
     *
     * Prevents memory leaks and allows clean re-initialization.
     */
    fun release() {
        if (!isBuilt) return

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

        // 4. Remove external sheet callback (fail-safe)
        sheetCallback?.let { bottomSheetBehavior.removeBottomSheetCallback(it) }

        // 5. Nullify references for garbage collection
        _blurOverlay = null
        _container = null
        _binder = null

        // 6. Clear cache entry
        slateFlagCache.remove(identifier)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Initializes container, binder, and blur overlay */
    private fun initCoreVariables() {
        _container = hostView as ViewGroup
        _binder = bindingListener.onBindSheet(hostView)
        _blurOverlay = Overlay.createOverlay(
            context = bottomSheet.context,
            overlayColor = overlayColor,
        ) {
            if (isExpanded) hide()
        }
    }

    /** Executes view binding, content binding, and configuration */
    private fun bindCore() {
        bindViews()
        bindContent()
        bindConfig()
    }

    /** Initializes and registers back press and lifecycle callbacks */
    private fun initSystemLevelCallbacks() {
        binder.rootView.post {
            backPressedCallback = createBackPressedCallback()
            lifecycleObserver = createOwnerLifecycleObserver()

            registerBackPressedCallback()
            registerOwnerLifecycleAwareness()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - VIEW BINDING
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Attaches bottom sheet and blur overlay to container */
    private fun bindViews() {
        (bottomSheet.parent as? ViewGroup)?.removeView(bottomSheet)
        (blurOverlay.parent as? ViewGroup)?.removeView(blurOverlay)

        // For the not configured view visibility blink!
        bottomSheet.visibility = View.INVISIBLE

        container.addView(bottomSheet)
        container.addView(blurOverlay)
        bottomSheet.bringToFront()
    }

    /** Delegates content binding and initializes UI controls */
    private fun bindContent() {
        bindingListener.onBindView(binder)
        initCommonButtons()
        setUpInternalStateChangedCallback()
    }

    /** Configures bottom sheet positioning and keyboard handling */
    private fun bindConfig() {
        binder.rootView.post {
            sheetCallback?.let { bottomSheetBehavior.addBottomSheetCallback(it) }
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

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - BUTTON INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Sets up click listeners for save, add new, and collapse buttons */
    private fun initCommonButtons() {
        addNewBtn?.appendClickListener({ hide() })
        saveBtn?.appendClickListener({ hide() })
        collapseBtn?.setOnClickListener { btn ->
            val isCollapsible = !bottomSheetBehavior.skipCollapsed
            if ((btn as RadioImage).isToggled && isCollapsible) collapse()
            else if (btn.isToggled && !isCollapsible) hide()
            else expand()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Manages blur visibility, arrow states, and state change callbacks */
    private fun setUpInternalStateChangedCallback() {
        internalBottomSheetBehaviorCallback?.let { bottomSheetBehavior.removeBottomSheetCallback(it) }
        internalBottomSheetBehaviorCallback = object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val runStateChangeCallback = fun(state: Int) {
                    onStateChangeBinder?.invoke(state)
                    onStateChangeSlate.invoke(state)
                }

                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        arrowDown()
                        blurVisible()
                        runStateChangeCallback(BottomSheetBehavior.STATE_EXPANDED)
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        arrowUp()
                        blurHide()
                        runStateChangeCallback(BottomSheetBehavior.STATE_COLLAPSED)
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        blurHide()
                        runStateChangeCallback(BottomSheetBehavior.STATE_HIDDEN)
                    }
                    BottomSheetBehavior.STATE_DRAGGING,
                    BottomSheetBehavior.STATE_SETTLING,
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                blurOffSet(slideOffset)
            }
        }
        internalBottomSheetBehaviorCallback?.let { bottomSheetBehavior.addBottomSheetCallback(it) }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - CALLBACK FACTORIES
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Creates back press callback that hides sheet or delegates to next handler */
    private fun createBackPressedCallback() = object : OnBackPressedCallback(true) {
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

    /** Creates lifecycle observer that auto-releases Slate on destroy */
    private fun createOwnerLifecycleObserver() = object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            release()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRIVATE - CALLBACK REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Registers back press callback with dispatcher */
    private fun registerBackPressedCallback() {
        onBackPressedDispatcher.addCallback(lifecycleOwner, backPressedCallback)
    }

    /** Registers lifecycle observer for automatic cleanup */
    private fun registerOwnerLifecycleAwareness() {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
    }
}