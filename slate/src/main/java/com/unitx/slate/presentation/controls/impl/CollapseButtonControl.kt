package com.unitx.slate.presentation.controls.impl

import com.unitx.slate.presentation.controls.SlateControl
import com.unitx.slate.presentation.radioImg.RadioImage
import com.unitx.slate.presentation.utilExtension.appendClickListener
import com.unitx.slate.presentation.utilExtension.clearAppendedClickListeners

internal class CollapseButtonControl(
    private val collapseBtn: RadioImage?,
    private val isCollapsible: Boolean,
    private val onCollapse: () -> Unit,
    private val onExpand: () -> Unit,
) : SlateControl {
    override fun attach(onHide: () -> Unit) {
        collapseBtn?.appendClickListener(
            { btn ->
                when {
                    (btn as RadioImage).isToggled && isCollapsible -> onCollapse()
                    btn.isToggled && !isCollapsible -> onHide()
                    else -> onExpand()
                }
            }
        )
    }

    override fun detach() {
        collapseBtn?.clearAppendedClickListeners()
    }
}