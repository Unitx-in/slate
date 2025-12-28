package com.unitx.slate.presentation.utilExtension

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.unitx.slate.presentation.builder.SlateBuilder
import com.unitx.slate.presentation.main.Slate

/**
 * Inflates a ViewBinding and converts it to a Slate ViewBinder using reflection.
 *
 * @param viewBinding The ViewBinding type to inflate
 * @param slateViewBinder The ViewBinder subclass to create
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
inline fun <reified viewBinding : ViewBinding, slateViewBinder : Slate.ViewBinder> View.inflateBinder(
    crossinline binderProvider: (viewBinding) -> slateViewBinder
): slateViewBinder {
    // Reflectively call ViewBinding's static inflate method
    val binding = viewBinding::class.java.getMethod(
        "inflate",
        LayoutInflater::class.java,
        ViewGroup::class.java,
        Boolean::class.javaPrimitiveType
    ).invoke(null, LayoutInflater.from(context), this as ViewGroup, false) as viewBinding

    return binderProvider(binding)
}

inline fun <reified T : Slate.ViewBinder> Fragment.SlateCreator(
    currentInstance: Slate<T>?,
    noinline onBind: (hostView: View) -> T,
    noinline onBindView: (T) -> Unit
): Slate<T> {
    return SlateBuilder<T>().build(
        currentInstance = currentInstance,
        hostView = requireView(),
        lifecycleOwner = viewLifecycleOwner,
        onBackPressedDispatcher = requireActivity().onBackPressedDispatcher,
        bindingListener = object : Slate.BindingListener<T> {
            override fun onBindSheet(hostView: View): T = onBind(hostView)
            override fun onBindView(binder: T) = onBindView(binder)
        }
    )
}