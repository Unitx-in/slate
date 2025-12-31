package com.unitx.slate.presentation.utilsSingleton

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.unitx.slate.presentation.config.OverlayColor
import com.unitx.slate.presentation.main.Slate.Companion.Tags
import com.unitx.slate.presentation.config.OverlayColorDefault


internal object Overlay{
    fun createOverlay(
        context: Context,
        overlayColor: OverlayColor,
        onClick: () -> Unit
    ) = View(context).apply {
        setBackgroundColor(overlayColor.color)
        alpha = 0f
        visibility = View.GONE
        isClickable = true
        contentDescription = Tags.BlurAccessibility.content
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        setOnClickListener { onClick() }
    }
}