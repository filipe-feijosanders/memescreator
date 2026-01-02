package com.example.memeapp.meme_editor.data

import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.domain.MemeExporter
import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import com.example.memeapp.meme_editor.presentation.MemeText
import com.example.memeapp.meme_editor.presentation.util.MemeRenderCalculator
import com.example.memeapp.meme_editor.presentation.util.MemeRenderCalculatorWeb
import com.example.memeapp.meme_editor.presentation.util.ScaledMemeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO

actual class PlatformMemeExporter : MemeExporter {

    // 1. Force Density to 2.0f (Just like Web) to ensure high-quality text export
    private val memeRenderCalculator = MemeRenderCalculatorWeb(density = 2.0f)

    actual override suspend fun exportMeme(
        backgroundImageBytes: ByteArray,
        memeTexts: List<MemeText>,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Starting export...")

            val inputStream = ByteArrayInputStream(backgroundImageBytes)
            val originalImage: BufferedImage = ImageIO.read(inputStream)
                ?: throw IllegalArgumentException("Failed to read input image. Is it a PNG/JPG?")

            // 2. Create the Canvas (RGB for JPEG compatibility)
            val outputImage = BufferedImage(
                originalImage.width,
                originalImage.height,
                BufferedImage.TYPE_INT_RGB
            )
            val g2d = outputImage.createGraphics()

            // 3. Enable Anti-Aliasing (Crucial for smooth text)
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            // 4. Draw Background
            g2d.drawImage(originalImage, 0, 0, null)

            // 5. Calculate Scale Factors
            val scaleFactors = memeRenderCalculator.calculateScaleFactors(
                bitmapWidth = originalImage.width,
                bitmapHeight = originalImage.height,
                templateSize = templateSize
            )

            // 6. Draw Each Text Element
            memeTexts.forEach { memeText ->
                val scaled = memeRenderCalculator.calculateScaledMemeText(
                    memeText, scaleFactors, templateSize
                )
                drawText(g2d, scaled)
            }

            g2d.dispose() // Clean up graphics
            println("DEBUG: Drawing finished.")

            // 7. Save File
            val rawPath = saveToStorageStrategy.getFilePath(fileName)
            val finalFile = if (rawPath.endsWith(".jpg", true) || rawPath.endsWith(".jpeg", true)) {
                File(rawPath)
            } else {
                File("$rawPath.jpg")
            }

            val success = ImageIO.write(outputImage, "jpg", finalFile)

            if (success) {
                Result.success(finalFile.absolutePath)
            } else {
                Result.failure(Exception("ImageIO failed to write image"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun drawText(g2d: Graphics2D, memeText: ScaledMemeText) {
        val originalTransform = g2d.transform

        // 1. Setup Font
        // We ensure a valid size (at least 1px) to prevent crashes
        val fontSize = memeText.scaledFontSizePx.toInt().coerceAtLeast(1)
        val font = Font("Impact", Font.BOLD, fontSize)
        g2d.font = font
        val frc = g2d.fontRenderContext

        // 2. Handle Multi-line Text (Split by \n)
        val lines = memeText.text.split("\n")

        // Calculate Line Height based on the font
        // "Impact" is a tall font, so 1.1x or 1.2x spacing works well
        val metrics = g2d.fontMetrics
        val lineHeight = metrics.height
        val totalTextHeight = lineHeight * lines.size

        // 3. Calculate Box Dimensions (Widest line)
        var maxWidth = 0
        lines.forEach { line ->
            val w = metrics.stringWidth(line)
            if (w > maxWidth) maxWidth = w
        }

        val boxWidth = maxWidth + memeText.textPaddingX * 2
        val boxHeight = totalTextHeight + memeText.textPaddingY * 2

        val centerX = memeText.scaledOffset.x + boxWidth / 2f
        val centerY = memeText.scaledOffset.y + boxHeight / 2f

        // 4. Apply Transformations
        g2d.translate(centerX.toDouble(), centerY.toDouble())
        g2d.scale(memeText.scale.toDouble(), memeText.scale.toDouble())
        g2d.rotate(Math.toRadians(memeText.rotation.toDouble()))

        // 5. Draw Each Line
        // We start drawing from the top-most line relative to vertical center
        val startY = -(totalTextHeight / 2) + (metrics.ascent) - (metrics.descent / 2)

        lines.forEachIndexed { index, line ->
            // Center this specific line horizontally
            val lineWidth = metrics.stringWidth(line)
            val x = -lineWidth / 2.0
            val y = startY + (index * lineHeight).toDouble()

            // Get outline for Stroke effect
            val textLayout = TextLayout(line, font, frc)
            val transform = AffineTransform.getTranslateInstance(x, y)
            val outline: Shape = textLayout.getOutline(transform)

            // Draw Black Stroke
            g2d.color = Color.BLACK
            g2d.stroke = BasicStroke(memeText.strokeWidth)
            g2d.draw(outline)

            // Draw White Fill
            g2d.color = Color.WHITE
            g2d.fill(outline)
        }

        g2d.transform = originalTransform
    }
}