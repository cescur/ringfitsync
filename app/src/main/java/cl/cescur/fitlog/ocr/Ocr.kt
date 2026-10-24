package cl.cescur.fitlog.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import cl.cescur.fitlog.parser.OcrLine
import kotlinx.coroutines.tasks.await

/** Reconoce el texto de la imagen en el propio móvil y devuelve cada línea con su posición. */
suspend fun readImage(context: Context, uri: Uri): List<OcrLine> {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val result = recognizer.process(InputImage.fromFilePath(context, uri)).await()
        return result.textBlocks.flatMap { block ->
            block.lines.map { line ->
                val box = line.boundingBox
                OcrLine(line.text, box?.top, box?.bottom, box?.left, box?.right)
            }
        }
    } finally {
        recognizer.close()
    }
}
