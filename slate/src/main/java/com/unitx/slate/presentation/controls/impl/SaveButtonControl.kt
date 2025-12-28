package com.unitx.slate.presentation.controls.impl

import android.widget.ImageView
import com.unitx.slate.presentation.controls.SlateControl
import com.unitx.slate.presentation.utilExtension.appendClickListener
import com.unitx.slate.presentation.utilExtension.clearAppendedClickListeners

class SaveButtonControl(private val saveBtn: ImageView?) : SlateControl {
    override fun attach(onHide: () -> Unit) {
        saveBtn?.appendClickListener ({ onHide() } )
    }

    override fun detach() {
        saveBtn?.clearAppendedClickListeners()
    }
}