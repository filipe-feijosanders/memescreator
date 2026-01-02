// composeApp/src/desktopMain/kotlin/.../data/CacheStorageStrategy.kt
package com.example.memeapp.meme_editor.data

import com.example.memeapp.meme_editor.domain.SaveToStorageStrategy
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

actual class CacheStorageStrategy : SaveToStorageStrategy {
    actual override fun getFilePath(fileName: String): String {
        var selectedPath: String? = null

        // This helper ensures the code inside runs on the UI thread (EDT),
        // but blocks the current thread (IO) until it's finished.
        SwingUtilities.invokeAndWait {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Save Meme"
            fileChooser.selectedFile = File(fileName)

            val filter = FileNameExtensionFilter("JPEG Images", "jpg", "jpeg")
            fileChooser.fileFilter = filter

            val userSelection = fileChooser.showSaveDialog(null)

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                selectedPath = fileChooser.selectedFile.absolutePath
            }
        }

        return selectedPath ?: throw IllegalStateException("User cancelled save")
    }
}