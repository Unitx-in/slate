package com.unitx.slatesheet

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.unitx.slate.presentation.main.Slate
import com.unitx.slate.presentation.builder.SlateBuilder
import com.unitx.slate.presentation.utilExtension.inflateBinder
import com.unitx.slatesheet.databinding.ActivityMainBinding
import com.unitx.slatesheet.databinding.SlateTesterCreateTextBinding

class MainActivity : AppCompatActivity() {

    private var _binding : ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var _slateAddCategory : Slate<BinderAddCategory>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        handleClicks()
    }

    private fun handleClicks() {
        binding.testBtn.post {
            binding.testBtn.setOnClickListener {
                _slateAddCategory?.expand() ?: setUpAddCategorySlate()
            }
        }
    }

    private fun setUpAddCategorySlate() {
        _slateAddCategory = SlateBuilder<BinderAddCategory>()
            .onStateChange { state->
                when (state) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        // Handle expanded
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        // Handle hidden
                    }
                }
            }
            .build(
                currentInstance = _slateAddCategory,
                hostView = binding.main,
                lifecycleOwner = this,
                onBackPressedDispatcher = onBackPressedDispatcher,
                onBind = { hostView-> hostView.inflateBinder<SlateTesterCreateTextBinding, BinderAddCategory> { BinderAddCategory(it) }},
                onBindView = { binder->
                    binder.bind(
                        object : BinderAddCategory.OnBinderAddCategoryClickListener {
                            override fun onSave(categoryName: String) {

                            }
                        }
                    )
                },
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        _slateAddCategory = null
    }

}