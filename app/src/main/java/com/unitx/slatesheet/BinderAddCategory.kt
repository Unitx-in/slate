package com.unitx.slatesheet

import android.view.View
import androidx.core.content.ContextCompat
import com.unitx.slate.presentation.utilExtension.appendClickListener
import com.unitx.slate.presentation.utilExtension.inflateBinder
import com.unitx.slate.presentation.main.Slate
import com.unitx.slatesheet.databinding.SlateTesterCreateTextBinding

class BinderAddCategory(
    private val onBinderAddCategoryClickListener: OnBinderAddCategoryClickListener
) : Slate.BindingListener<BinderAddCategory.ViewBinder> {

    interface OnBinderAddCategoryClickListener{
        fun onSave(categoryName: String)
    }

    class ViewBinder(private val sheetBinding: SlateTesterCreateTextBinding) : Slate.ViewBinder(sheetBinding.root) {
        fun bind(onBinderAddCategoryClickListener: OnBinderAddCategoryClickListener) {
            sheetBinding.apply {
                setCollapseBtn = null
                setSaveBtn = bCtIvSave

                bCtTvName.text = ContextCompat.getString(rootView.context, R.string.title_category)
                bCtEtName.hint = ContextCompat.getString(rootView.context, R.string.enter_a_text)
                bCtTvDescription.text = ContextCompat.getString(rootView.context, R.string.hint_category_error)


                bCtIvSave.appendClickListener({
                    onBinderAddCategoryClickListener.onSave(bCtEtName.text.toString())
                    bCtEtName.text?.clear()
                })
            }
        }
    }

    override fun onBindSheet(hostView: View) =
        hostView.inflateBinder<SlateTesterCreateTextBinding, ViewBinder> { ViewBinder(it) }

    override fun onBindView(binder: ViewBinder) {
        binder.bind(onBinderAddCategoryClickListener)
    }
}