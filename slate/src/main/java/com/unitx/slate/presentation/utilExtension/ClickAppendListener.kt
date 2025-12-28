package com.unitx.slate.presentation.utilExtension

import android.view.View
import com.unitx.slate.R

/**
 * Allows multiple click listeners to be registered on a single View without
 * overwriting previous listeners.
 *
 * Unlike the standard `setOnClickListener()` which replaces any existing listener,
 * this extension accumulates all listeners and invokes them sequentially when clicked.
 *
 * This is particularly useful in Slate for adding common button behaviors
 * (like hiding the sheet) while preserving custom click handlers defined
 * in ViewBinder implementations.
 *
 * How it works:
 * 1. Stores listeners in View's tag storage (R.id.appended_click_listeners)
 * 2. First call sets up the actual OnClickListener that invokes all stored listeners
 * 3. Subsequent calls add new listeners to the existing list
 * 4. All listeners execute in the order they were added
 *
 * @param newListeners One or more click listener lambdas to append
 *
 * Example usage in Slate:
 * ```
 * // First append - sets up the infrastructure
 * saveBtn?.appendClickListener({ hide() })
 *
 * // Later in custom code - adds without removing hide() behavior
 * saveBtn?.appendClickListener({ viewModel.saveData() })
 *
 * // When clicked: both hide() and saveData() execute
 * ```
 *
 * Thread safety: Not thread-safe. Should only be called from the main/UI thread.
 */
@Suppress("UNCHECKED_CAST")
fun View.appendClickListener(vararg newListeners: (View) -> Unit) {
    // Retrieve existing listeners from view tag, or create new list if first time
    val existingListeners = getTag(R.id.appended_click_listeners) as? MutableList<(View) -> Unit>
        ?: mutableListOf<(View) -> Unit>().also {
            // First time setup: store list in tag and set the actual click listener
            setTag(R.id.appended_click_listeners, it)

            // The actual OnClickListener invokes all accumulated listeners
            setOnClickListener { view ->
                it.forEach { listener -> listener(view) }
            }
        }

    // Add new listener(s) to the accumulated list
    existingListeners.addAll(newListeners)
}