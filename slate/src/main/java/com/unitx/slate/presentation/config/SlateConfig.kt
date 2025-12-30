package com.unitx.slate.presentation.config

import com.google.android.material.bottomsheet.BottomSheetBehavior

data class SlateConfig(
    val initialState: Int = BottomSheetBehavior.STATE_HIDDEN,
    val peekHeight: Int = 0,
    val isFitToContents: Boolean = true,
    val isHideable: Boolean = true,
    val skipCollapsed: Boolean = false,
    val draggable: Boolean = true,
    val halfExpandedRatio: Float = 0.5f
)