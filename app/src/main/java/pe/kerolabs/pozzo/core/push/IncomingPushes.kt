package pe.kerolabs.pozzo.core.push

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The deep links of the pushes that arrive while the application is open, e.g. "pozzo://groups/{id}/detail".
 * The screen on view listens to them to refresh what changed, such as a member who just joined.
 */
@Singleton
class IncomingPushes @Inject constructor() {

    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val deepLinks: SharedFlow<String> = _deepLinks.asSharedFlow()

    fun onPush(deepLink: String) {
        _deepLinks.tryEmit(deepLink)
    }
}
