package com.unitx.slate.presentation.extension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.unitx.slate.presentation.main.Slate

/**
 * Inflates a ViewBinding and converts it to a Slate ViewBinder using reflection.
 *
 * @param B The ViewBinding type to inflate
 * @param VB The ViewBinder subclass to create
 * @param binderProvider Lambda that creates ViewBinder from inflated binding
 * @return Custom ViewBinder instance
 *
 * Example:
 * ```
 * override fun onBindSheet(hostView: View): MyBottomSheetBinder {
 *     return hostView.inflateBinder { binding: BottomSheetLayoutBinding ->
 *         MyBottomSheetBinder(binding)
 *     }
 * }
 * ```
 */
inline fun <reified B : ViewBinding, VB : Slate.ViewBinder> View.inflateBinder(
    crossinline binderProvider: (B) -> VB
): VB {
    // Reflectively call ViewBinding's static inflate method
    val binding = B::class.java.getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.javaPrimitiveType
    ).invoke(null, LayoutInflater.from(context), this as ViewGroup, false) as B

    return binderProvider(binding)
}