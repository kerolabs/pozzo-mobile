package pe.kerolabs.pozzo.core.network

/**
 * Gives the network layer the session token without depending on how Identity & Access stores it.
 */
interface AccessTokenProvider {

    /**
     * The token of the open session, or null when nobody is signed in.
     */
    fun currentToken(): String?

    /**
     * Called when the backend rejects the token, so the session can be closed.
     */
    fun onUnauthorized()
}
