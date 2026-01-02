package com.example.memeapp.meme_editor.data

import androidx.compose.ui.unit.IntSize
import com.example.memeapp.meme_editor.domain.MemeExporter
import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import com.example.memeapp.meme_editor.presentation.MemeText
import com.example.memeapp.meme_editor.presentation.util.MemeRenderCalculator
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

    // You might need to adjust density if the text looks too small/big on desktop
    private val memeRenderCalculator = MemeRenderCalculator(density = 2.0f)

    actual override suspend fun exportMeme(
        backgroundImageBytes: ByteArray,
        memeTexts: List<MemeText>,
        templateSize: IntSize,
        saveToStorageStrategy: SaveToStorageStrategy,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("DEBUG: Starting export...")

            // 1. Read Image
            val inputStream = ByteArrayInputStream(backgroundImageBytes)
            val originalImage: BufferedImage = ImageIO.read(inputStream)
                ?: throw IllegalArgumentException("Failed to read input image. Is it a PNG/JPG?")
            println("DEBUG: Image read successfully. Size: ${originalImage.width}x${originalImage.height}")

            // 2. Create Canvas
            val outputImage = BufferedImage(
                originalImage.width,
                originalImage.height,
                BufferedImage.TYPE_INT_RGB
            )
            val g2d = outputImage.createGraphics()

            // ... (Your drawing settings and drawImage code here) ...
            g2d.drawImage(originalImage, 0, 0, null)

            // ... (Your text rendering loop here) ...
            // Just ensure you dispose at the end:
            g2d.dispose()
            println("DEBUG: Drawing finished.")

            // 3. Get Path from User
            val rawPath = saveToStorageStrategy.getFilePath(fileName)
            println("DEBUG: User selected path: $rawPath")

            // 4. Ensure Extension
            // If user typed "my_meme" without .jpg, we fix it here.
            val finalFile = if (rawPath.endsWith(".jpg", ignoreCase = true) || rawPath.endsWith(".jpeg", ignoreCase = true)) {
                File(rawPath)
            } else {
                File("$rawPath.jpg")
            }

            // 5. Write to Disk
            val success = ImageIO.write(outputImage, "jpg", finalFile)

            if (success) {
                println("DEBUG: Saved successfully to: ${finalFile.absolutePath}")
                Result.success(finalFile.absolutePath)
            } else {
                println("ERROR: ImageIO.write returned false. No writer found for 'jpg'?")
                Result.failure(Exception("ImageIO failed to write image"))
            }

        } catch (e: Exception) {
            println("ERROR: Exception during export: ${e.message}")
            e.printStackTrace() // This prints the full error stack to the console
            Result.failure(e)
        }
    }

    private fun drawText(g2d: Graphics2D, memeText: com.example.memeapp.meme_editor.presentation.util.ScaledMemeText) {
        // Save original transform to restore later
        val originalTransform = g2d.transform

        // Create Font (Using Impact or fallback to SansSerif)
        val font = Font("Impact", Font.BOLD, memeText.scaledFontSizePx.toInt())
        g2d.font = font

        // Get text metrics to center it
        val fontMetrics = g2d.fontMetrics
        val textWidth = fontMetrics.stringWidth(memeText.text)
        val textHeight = fontMetrics.ascent - fontMetrics.descent

        // Calculate Position
        val boxWidth = textWidth + memeText.textPaddingX * 2
        val boxHeight = textHeight + memeText.textPaddingY * 2
        val centerX = memeText.scaledOffset.x + boxWidth / 2f
        val centerY = memeText.scaledOffset.y + boxHeight / 2f

        // Apply Transformations (Translate -> Rotate -> Scale)
        g2d.translate(centerX.toDouble(), centerY.toDouble())
        g2d.scale(memeText.scale.toDouble(), memeText.scale.toDouble())
        g2d.rotate(Math.toRadians(memeText.rotation.toDouble()))

        // Center text in the new coordinate system
        val x = -textWidth / 2f
        val y = (textHeight / 2f)

        // Create a Shape of the text for outlining
        val fontRenderContext = g2d.fontRenderContext
        val textLayout = TextLayout(memeText.text, font, fontRenderContext)
        val transform = AffineTransform.getTranslateInstance(x.toDouble(), y.toDouble())
        val outline: Shape = textLayout.getOutline(transform)

        // Draw Stroke (Black Outline)
        g2d.color = Color.BLACK
        g2d.stroke = BasicStroke(memeText.strokeWidth)
        g2d.draw(outline)

        // Draw Fill (White Inside)
        g2d.color = Color.WHITE
        g2d.fill(outline)

        // Restore original position for the next text
        g2d.transform = originalTransform
    }
}