package com.unitx.slate.presentation.config

enum class OverlayColorDefault(override val color: Int): OverlayColor{
    Dark(0x99000000.toInt()),
    SemiDark(0x66000000),
    Light(0x33000000),
    LightWhite(0x66FFFFFF)
}