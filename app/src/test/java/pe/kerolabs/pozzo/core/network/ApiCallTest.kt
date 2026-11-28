package pe.kerolabs.pozzo.core.network

import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** How every call to the services turns an answer, an error of the backend or a lost connection into a result. */
class ApiCallTest {

    private val json = "application/json".toMediaType()

    @Test
    fun `a successful answer gives its body`() = runTest {
        val result = apiCall { Response.success("ok") }

        assertEquals("ok", result.getOrThrow())
    }

    @Test
    fun `an error of the backend keeps its code and its message`() = runTest {
        val body = """{"code":"BID_BELOW_MINIMUM","message":"La oferta mínima es S/ 10."}""".toResponseBody(json)

        val error = apiCall<String> { Response.error(422, body) }.exceptionOrNull() as ApiException

        assertEquals(422, error.status)
        assertEquals("BID_BELOW_MINIMUM", error.code)
        assertEquals("La oferta mínima es S/ 10.", error.userMessage())
    }

    @Test
    fun `an answer that is not the backend's shows a generic message`() = runTest {
        val body = "<html>Bad Gateway</html>".toResponseBody("text/html".toMediaType())

        val error = apiCall<String> { Response.error(502, body) }.exceptionOrNull() as ApiException

        assertEquals("HTTP_502", error.code)
        assertEquals("Ocurrió un error. Inténtalo de nuevo.", error.userMessage())
    }

    @Test
    fun `a validation error shows which field failed`() = runTest {
        val body = """{"code":"VALIDATION_ERROR","message":"Datos inválidos","details":"amount: debe ser mayor que 0"}"""
            .toResponseBody(json)

        val error = apiCall<String> { Response.error(400, body) }.exceptionOrNull()!!

        assertEquals("amount: debe ser mayor que 0", error.userMessage())
    }

    @Test
    fun `a lost connection becomes a message that asks to try again`() = runTest {
        val error = apiCall<String> { throw IOException("timeout") }.exceptionOrNull() as ApiException

        assertEquals("NETWORK_ERROR", error.code)
        assertTrue(error.userMessage().contains("inténtalo de nuevo"))
    }
}
