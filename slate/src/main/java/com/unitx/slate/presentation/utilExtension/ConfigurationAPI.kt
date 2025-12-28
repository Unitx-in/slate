package com.unitx.slate.presentation.utilExtension


import com.unitx.slate.presentation.main.Slate

val slateFlagCache = mutableMapOf<String, Boolean>()

val Slate<*>.identifier: String
    get() = hashCode().toString()

var Slate<*>.isBuilt: Boolean
    get() = slateFlagCache[identifier] ?: false
    set(value) {
        slateFlagCache[identifier] = value
    }