package com.unitx.slate.presentation.controls

class SlateControlComposite : SlateControl {
    private val controls = mutableListOf<SlateControl>()

    fun add(control: SlateControl) = apply { controls.add(control) }

    override fun attach(onHide: () -> Unit) {
        controls.forEach { it.attach(onHide) }
    }

    override fun detach() {
        controls.forEach { it.detach() }
    }
}