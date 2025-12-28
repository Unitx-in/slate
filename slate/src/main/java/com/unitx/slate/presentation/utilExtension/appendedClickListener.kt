package com.unitx.slate.presentation.utilExtension

import android.view.View
import com.unitx.slate.R

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


@Suppress("UNCHECKED_CAST")
fun View.removeClickListeners(vararg listeners: (View) -> Unit) {
    val existingListeners =
        getTag(R.id.appended_click_listeners) as? MutableList<(View) -> Unit>
            ?: return

    existingListeners.removeAll(listeners.toSet())

    // Optional: clean up if empty
    if (existingListeners.isEmpty()) {
        setOnClickListener(null)
        setTag(R.id.appended_click_listeners, null)
    }
}

@Suppress("UNCHECKED_CAST")
fun View.clearAppendedClickListeners() {
    val existingListeners =
        getTag(R.id.appended_click_listeners) as? MutableList<(View) -> Unit>
            ?: return

    existingListeners.clear()
    setOnClickListener(null)
    setTag(R.id.appended_click_listeners, null)
}

