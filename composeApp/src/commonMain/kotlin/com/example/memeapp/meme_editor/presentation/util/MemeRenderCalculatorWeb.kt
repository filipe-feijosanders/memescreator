package com.example.memeapp.meme_editor.presentation.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.presentation.MemeText
import kotlin.math.roundToInt

class MemeRenderCalculatorWeb(
    private val density: Float
) {
    companion object {
        private const val TEXT_PADDING_DP = 8f
        private const val STROKE_WIDTH_DP = 3f
    }

    fun calculateScaleFactors(
        bitmapWidth: Int,
        bitmapHeight: Int,
        templateSize: IntSize
    ): ScaleFactors {
        val scaleX = if(templateSize.width > 0) bitmapWidth.toFloat() / templateSize.width else 1f
        val scaleY = if(templateSize.height > 0) bitmapHeight.toFloat() / templateSize.height else 1f

        val bitmapScale = (scaleX + scaleY) / 2

        return ScaleFactors(
            scaleX = scaleX,
            scaleY = scaleY,
            bitmapScale = bitmapScale
        )
    }

    fun calculateScaledMemeText(
        memeText: MemeText,
        scaleFactors: ScaleFactors,
        templateSize: IntSize
    ): ScaledMemeText {
        val (scaleX, scaleY, bitmapScale) = scaleFactors

        val scaledOffset = Offset(
            x = (memeText.offsetRatioX * templateSize.width) * scaleX,
            y = (memeText.offsetRatioY * templateSize.height) * scaleY
        )

        val textPaddingPx = TEXT_PADDING_DP * density
        val textPaddingBitmapX = textPaddingPx * scaleX
        val textPaddingBitmapY = textPaddingPx * scaleY

        // This is a TextUnit (e.g., 40.sp)
        val scaledFontSize = memeText.fontSize * bitmapScale

        val strokeWidth = STROKE_WIDTH_DP * density * scaleX

        val paddingDp = TEXT_PADDING_DP * 2
        val paddingPx = paddingDp * density
        val constraintWidth = ((templateSize.width / memeText.scale) * scaleX - paddingPx * scaleX)
            .roundToInt()
            .coerceAtLeast(1)

        return ScaledMemeText(
            text = memeText.text,
            scaledOffset = scaledOffset,
            // --- FIX APPLIED HERE ---
            scaledFontSizePx = if (scaledFontSize.isSp) {
                scaledFontSize.value * density
            } else {
                scaledFontSize.value
            },
            // ------------------------
            strokeWidth = strokeWidth,
            constraintWidth = constraintWidth,
            textPaddingX = textPaddingBitmapX,
            textPaddingY = textPaddingBitmapY,
            rotation = memeText.rotation,
            scale = memeText.scale,
            originalText = memeText
        )
    }
}