package com.example.memeapp.meme_editor.data

import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.domain.MemeExporter
import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import com.example.memeapp.meme_editor.presentation.MemeText
import com.example.memeapp.meme_editor.presentation.util.MemeRenderCalculator
import com.example.memeapp.meme_editor.presentation.util.MemeRenderCalculatorWeb
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.math.PI
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.dom.CanvasLineJoin
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsArray

// Ensure you have this import for the string conversion
// If it's unresolved, you can remove it and use the helper function below
import kotlin.js.toJsString

actual class PlatformMemeExporter : MemeExporter {

    private val memeRenderCalculator = MemeRenderCalculatorWeb(
        density = 2.0f
    )

    actual override suspend fun exportMeme(
        backgroundImageBytes: ByteArray,
        memeTexts: List<MemeText>,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String
    ): Result<String> {
        return try {
            println("WEB_EXPORT: Starting export...")
            val img = createImageFromBytes(backgroundImageBytes)
            println("WEB_EXPORT: Image loaded. Size: ${img.width}x${img.height}")

            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.width = img.width
            canvas.height = img.height
            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

            // 1. Draw Background
            ctx.drawImage(img, 0.0, 0.0)

            // 2. Calculate Scales
            // Warning: If templateSize is 0, this will fail.
            if (templateSize.width == 0 || templateSize.height == 0) {
                println("WEB_EXPORT ERROR: Template size is 0!")
                return Result.failure(Exception("Invalid template size"))
            }

            val scaleFactors = memeRenderCalculator.calculateScaleFactors(
                bitmapWidth = img.width,
                bitmapHeight = img.height,
                templateSize = templateSize
            )

            ctx.textAlign = CanvasTextAlign.CENTER
            ctx.textBaseline = CanvasTextBaseline.MIDDLE
            ctx.lineJoin = CanvasLineJoin.ROUND

            // 3. Draw Texts
            memeTexts.forEach { memeText ->
                val scaled = memeRenderCalculator.calculateScaledMemeText(
                    memeText, scaleFactors, templateSize
                )

                ctx.save()

                // Construct Font String (e.g., "40px Impact, Arial")
                val fontSize = scaled.scaledFontSizePx.toInt().coerceAtLeast(1)
                ctx.font = "${fontSize}px Impact, Arial"

                println("WEB_EXPORT: Drawing text '${memeText.text}' at size ${fontSize}px")

                // Handle Multiline Text (Split by \n)
                val lines = scaled.text.split("\n")
                val lineHeight = fontSize * 1.2 // 1.2 is a standard line-height multiplier

                // Calculate Box Dimensions for centering
                // We estimate width based on the longest line
                var maxWidth = 0.0
                lines.forEach { line ->
                    val w = ctx.measureText(line).width
                    if (w > maxWidth) maxWidth = w
                }

                val totalTextHeight = lineHeight * lines.size
                val boxWidth = maxWidth + scaled.textPaddingX * 2
                val boxHeight = totalTextHeight + scaled.textPaddingY * 2

                val centerX = scaled.scaledOffset.x + boxWidth / 2.0
                val centerY = scaled.scaledOffset.y + boxHeight / 2.0

                println("WEB_EXPORT: Coords: ($centerX, $centerY)")

                // Transforms
                ctx.translate(centerX, centerY)
                ctx.scale(scaled.scale.toDouble(), scaled.scale.toDouble())
                ctx.rotate(scaled.rotation * (PI / 180.0))

                // Draw Style
                ctx.lineWidth = scaled.strokeWidth.toDouble()
                ctx.strokeStyle = "black".toJsAny()
                ctx.fillStyle = "white".toJsAny()

                // Draw Each Line
                // We start drawing from the top-most line relative to the vertical center
                val startY = -(totalTextHeight / 2.0) + (lineHeight / 2.0)

                lines.forEachIndexed { index, line ->
                    val yOffset = startY + (index * lineHeight)
                    ctx.strokeText(line, 0.0, yOffset)
                    ctx.fillText(line, 0.0, yOffset)
                }

                ctx.restore()
            }

            val dataUrl = canvas.toDataURL("image/jpeg")
            triggerDownload(dataUrl, fileName)

            Result.success("Downloaded")
        } catch (e: Exception) {
            println("WEB_EXPORT ERROR: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun createImageFromBytes(bytes: ByteArray): HTMLImageElement = suspendCoroutine { cont ->
        val img = document.createElement("img") as HTMLImageElement
        val uint8Array = Uint8Array(bytes.size)
        for (i in bytes.indices) {
            uint8Array[i] = bytes[i]
        }
        val parts = JsArray<JsAny?>()
        parts[0] = uint8Array

        val blob = Blob(parts)
        val url = URL.createObjectURL(blob)

        img.onload = {
            URL.revokeObjectURL(url)
            cont.resume(img)
        }
        img.onerror = { _, _, _, _, _ ->
            cont.resumeWithException(Exception("Failed to load image"))
            null
        }
        img.src = url
    }

    private fun triggerDownload(dataUrl: String, fileName: String) {
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = dataUrl
        val finalName = if (fileName.endsWith(".jpg", true)) fileName else "$fileName.jpg"
        anchor.download = finalName
        document.body?.appendChild(anchor)
        anchor.click()
        document.body?.removeChild(anchor)
    }

    // --- Helper Extensions for Strict Wasm Types ---

    private val CanvasTextAlign.Companion.CENTER: CanvasTextAlign
        get() = "center".toJsAny().unsafeCast()

    private val CanvasTextBaseline.Companion.MIDDLE: CanvasTextBaseline
        get() = "middle".toJsAny().unsafeCast()

    private val CanvasLineJoin.Companion.ROUND: CanvasLineJoin
        get() = "round".toJsAny().unsafeCast()

    // Converts String to the 'JsAny' type required by color properties
    private fun String.toJsAny(): JsAny = this.toJsString().unsafeCast()
}