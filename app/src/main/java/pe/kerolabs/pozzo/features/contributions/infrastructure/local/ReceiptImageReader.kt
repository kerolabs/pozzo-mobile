package pe.kerolabs.pozzo.features.contributions.infrastructure.local

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
 * Turns the image of a receipt into a JPEG that is still readable but light to upload: its longest side
 * goes down to 1600 px. A screenshot of several megabytes becomes a few hundred kilobytes.
 */
class ReceiptImageReader @Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        const val LONGEST_SIDE = 1600
        const val QUALITY = 80
    }

    suspend fun read(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val image = decode(uri)
        val longest = maxOf(image.width, image.height)
        val scaled = if (longest > LONGEST_SIDE) {
            val ratio = LONGEST_SIDE.toFloat() / longest
            Bitmap.createScaledBitmap(image, (image.width * ratio).toInt(), (image.height * ratio).toInt(), true)
        } else {
            image
        }
        ByteArrayOutputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)
            output.toByteArray()
        }
    }

    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder applies the rotation the camera wrote in the photo.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri).use { input -> BitmapFactory.decodeStream(input) }
                ?: error("The image could not be read")
        }
}
