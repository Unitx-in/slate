package com.unitx.slate.presentation.radioImg

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.use
import com.unitx.slate.R

class RadioImage @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var isToggled: Boolean = false
        private set

    var drawableOn: Drawable? = null
    var drawableOff: Drawable? = null

    var tintOn: Int? = null
    var tintOff: Int? = null

    var onToggleChanged: ((Boolean) -> Unit)? = null

    private var userClickListener: OnClickListener? = null

    init {
        context.theme.obtainStyledAttributes(attrs,
            R.styleable.RadioImage, 0, 0).use { a ->
            drawableOn = a.getDrawable(R.styleable.RadioImage_drawableOn)
            drawableOff = a.getDrawable(R.styleable.RadioImage_drawableOff)

            if (a.hasValue(R.styleable.RadioImage_tintOn))
                tintOn = a.getColor(R.styleable.RadioImage_tintOn, 0)

            if (a.hasValue(R.styleable.RadioImage_tintOff))
                tintOff = a.getColor(R.styleable.RadioImage_tintOff, 0)

            val initial = a.getBoolean(R.styleable.RadioImage_initialState, false)
            isToggled = initial // directly set internal state without triggering animation
            updateDrawableAndTint() // ← set initial drawable and tint
        }

        super.setOnClickListener {
            toggle()
            userClickListener?.onClick(this)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        userClickListener = l
    }

    fun toggle() {
        setState(!isToggled)
        onToggleChanged?.invoke(isToggled)
    }

    fun setState(toggled: Boolean, animate: Boolean = true) {
        if (isToggled == toggled) return
        isToggled = toggled
        animateStateChange(animate)
    }

    private fun updateDrawableAndTint() {
        setImageDrawable(if (isToggled) drawableOn else drawableOff)

        val tintColor = if (isToggled) tintOn else tintOff
        tintColor?.let {
            setColorFilter(it, PorterDuff.Mode.SRC_IN)
        } ?: clearColorFilter()
    }


    private fun animateStateChange(animate: Boolean) {

        val drawable = if (isToggled) drawableOn else drawableOff
        val tintColor = if (isToggled) tintOn else tintOff

        if (!animate){
            setImageDrawable(drawable)
            return
        }

        drawable?.let {
            crossFadeToDrawable(it)
        }

        tintColor?.let {
            setColorFilter(it, PorterDuff.Mode.SRC_IN)
        } ?: clearColorFilter()
    }

    private fun crossFadeToDrawable(targetDrawable: Drawable) {
        val fadeDuration = 150L
        val current = drawable ?: return setImageDrawable(targetDrawable)

        val transition = TransitionDrawable(arrayOf(current, targetDrawable))
        setImageDrawable(transition)
        transition.isCrossFadeEnabled = true
        transition.startTransition(fadeDuration.toInt())
    }

    fun persistStateAsTag() {
        this.tag = isToggled
    }

    fun restoreStateFromTag() {
        val state = tag as? Boolean ?: false
        setState(state)
    }
}

