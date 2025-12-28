package com.unitx.slate.presentation.config

data class SlateConfig(
    val peekHeight: Int = 0,
    val isFitToContents: Boolean = true,
    val isHideable: Boolean = true,
    val skipCollapsed: Boolean = false,
    val draggable: Boolean = true,
    val halfExpandedRatio: Float = 0.5f
)