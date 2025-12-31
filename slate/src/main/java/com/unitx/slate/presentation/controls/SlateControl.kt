package com.unitx.slate.presentation.controls

internal interface SlateControl {
    fun attach(onHide: () -> Unit)
    fun detach()
}