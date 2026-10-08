package pe.kerolabs.pozzo.features.contributions.infrastructure.ocr

import android.content.Context
import android.util.Log
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import pe.kerolabs.pozzo.BuildConfig
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import pe.kerolabs.pozzo.features.contributions.domain.ReadReceipt
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptReader

/**
 * Reads the receipt with ML Kit Text Recognition on the device, as the architecture decided: the
 * image is never uploaded, only the four confirmed fields reach the backend.
 */
class MlKitReceiptReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReceiptReader {

    override suspend fun read(imageUri: Uri): Result<ReadReceipt> = runCatching {
        val image = InputImage.fromFilePath(context, imageUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val text = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { continuation.resume(Result.success(it.text)) }
                    .addOnFailureListener { continuation.resume(Result.failure(it)) }
            }.getOrThrow()
            if (BuildConfig.DEBUG) Log.d("ReceiptReader", text)
            ReceiptTextParser.parse(text)
        } finally {
            recognizer.close()
        }
    }
}
