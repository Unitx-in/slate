package com.unitx.slate.presentation.helper

import com.google.android.material.bottomsheet.BottomSheetBehavior

data class FlagCache(
    var isBuilt: Boolean = false,
    var isConfigured: Boolean = false,
    var sheetCallback: BottomSheetBehavior.BottomSheetCallback? = null
)