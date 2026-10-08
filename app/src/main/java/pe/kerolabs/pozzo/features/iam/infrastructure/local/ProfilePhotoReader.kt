package pe.kerolabs.pozzo.features.iam.infrastructure.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns a picked or taken photo into a small square JPEG before it is uploaded: centered, 512 px a side.
 * A phone photo of several megabytes becomes about 50 KB.
 */
class ProfilePhotoReader @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val SIDE = 512
        const val QUALITY = 85
    }

    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val original = decode(uri)
        val side = minOf(original.width, original.height)
        val square = Bitmap.createBitmap(original, (original.width - side) / 2, (original.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, SIDE, SIDE, true)
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)
            output.toByteArray()
        }
    }

    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies the rotation the camera wrote in the photo, and reduces it while decoding.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val shortest = minOf(info.size.width, info.size.height)
                if (shortest > SIDE * 2) {
                    val ratio = shortest / (SIDE * 2f)
                    decoder.setTargetSize((info.size.width / ratio).toInt(), (info.size.height / ratio).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = 4 }
            context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input, null, options) }
                ?: error("The image could not be read")
        }
}
