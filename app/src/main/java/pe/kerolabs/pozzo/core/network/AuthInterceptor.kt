package pe.kerolabs.pozzo.core.network

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the bearer token and the language of the messages to every request. A 401 on a protected
 * route means the session expired or was revoked.
 */
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AccessTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder().header("Accept-Language", "es")
        val token = tokenProvider.currentToken()
        // Requesting a verification code is public, so it never carries a token.
        if (token != null && !request.url.encodedPath.contains("/auth/codes")) {
            builder.header("Authorization", "Bearer $token")
        }
        val response = chain.proceed(builder.build())
        // A 401 from an auth route is a wrong code or credentials, not an expired session.
        if (response.code == 401 && token != null && !request.url.encodedPath.contains("/auth/")) {
            tokenProvider.onUnauthorized()
        }
        return response
    }
}
