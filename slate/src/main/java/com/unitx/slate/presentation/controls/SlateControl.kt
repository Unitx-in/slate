package com.unitx.slate.presentation.controls

interface SlateControl {
    fun attach(onHide: () -> Unit)
    fun detach()
}