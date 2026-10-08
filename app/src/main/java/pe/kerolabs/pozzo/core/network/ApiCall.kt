package pe.kerolabs.pozzo.core.network

import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

/**
 * An error answered by the Pozzo backend: the code tells the cases apart (e.g. INVALID_VERIFICATION_CODE)
 * and the message is already in Spanish.
 */
class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val details: String? = null,
) : Exception(message)

/**
 * The shape of the error body the backend sends; every field is optional because a proxy or the
 * server itself may answer with something else.
 */
private data class ErrorBody(val code: String?, val message: String?, val details: String?)

private val gson = Gson()

/**
 * Runs a Retrofit call and turns the answer into a [Result]: the body on success, an [ApiException]
 * with the backend's code and message on an error, or a readable message when there is no connection.
 */
suspend fun <T> apiCall(call: suspend () -> Response<T>): Result<T> = try {
    val response = call()
    if (response.isSuccessful) {
        // Endpoints that answer 204 have no body, so Unit stands in for it.
        @Suppress("UNCHECKED_CAST")
        Result.success(response.body() ?: Unit as T)
    } else {
        val error = response.errorBody()?.string()?.let {
            runCatching { gson.fromJson(it, ErrorBody::class.java) }.getOrNull()
        }
        Result.failure(
            ApiException(
                status = response.code(),
                code = error?.code ?: "HTTP_${response.code()}",
                message = error?.message ?: "Ocurrió un error. Inténtalo de nuevo.",
                details = error?.details,
            ),
        )
    }
} catch (e: IOException) {
    Result.failure(
        ApiException(0, "NETWORK_ERROR", "No pudimos conectarnos. Revisa tu conexión e inténtalo de nuevo."),
    )
}

/**
 * The message to show for a failure.
 */
fun Throwable.userMessage(): String = when (this) {
    is ApiException -> details?.takeIf { code == "VALIDATION_ERROR" } ?: message
    else -> message ?: "Ocurrió un error. Inténtalo de nuevo."
}
