package com.unitx.slate.presentation.controls.impl

import android.view.View
import com.unitx.slate.presentation.controls.SlateControl
import com.unitx.slate.presentation.utilExtension.appendClickListener

class AddNewButtonControl(private val addNewBtn: View?) : SlateControl {
    override fun attach(onHide: () -> Unit) {
        addNewBtn?.appendClickListener ({ onHide() } )
    }

    override fun detach() {

    }
}