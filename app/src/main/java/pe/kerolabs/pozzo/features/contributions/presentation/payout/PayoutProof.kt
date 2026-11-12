package pe.kerolabs.pozzo.features.contributions.presentation.payout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.network.ApiException
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.AttachPayoutProofUseCase
import pe.kerolabs.pozzo.features.contributions.application.GetPayoutProofUseCase

data class AttachPayoutProofUiState(
    val isUploading: Boolean = false,
    val attached: Boolean = false,
    val errorMessage: String? = null,
)

/** US36: the organizer keeps the image of the transfer with which a delivered pot was paid. */
@HiltViewModel
class AttachPayoutProofViewModel @Inject constructor(private val attachPayoutProof: AttachPayoutProofUseCase) : ViewModel() {

    private val _state = MutableStateFlow(AttachPayoutProofUiState())
    val state: StateFlow<AttachPayoutProofUiState> = _state.asStateFlow()

    fun attach(periodId: String, imageUri: Uri) {
        if (_state.value.isUploading) return
        viewModelScope.launch {
            _state.update { it.copy(isUploading = true, errorMessage = null) }
            attachPayoutProof(periodId, imageUri)
                .onSuccess { _state.update { it.copy(isUploading = false, attached = true) } }
                .onFailure { error -> _state.update { it.copy(isUploading = false, errorMessage = error.userMessage()) } }
        }
    }
}

/**
 * Lets the organizer pick the screenshot of the transfer of a delivered pot from the gallery; every member of
 * the cycle can see it afterwards.
 */
@Composable
fun AttachPayoutProofButton(periodId: String, modifier: Modifier = Modifier) {
    val viewModel: AttachPayoutProofViewModel = hiltViewModel(key = "payout-proof-$periodId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.attach(periodId, it) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.attached) {
            InfoBanner("Adjuntaste la transferencia. Los integrantes ya pueden verla.", Icons.Outlined.TaskAlt)
        } else {
            PozzoOutlinedButton(
                text = if (state.isUploading) "Subiendo la transferencia…" else "Adjuntar la transferencia",
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !state.isUploading,
                icon = Icons.Outlined.AddPhotoAlternate,
            )
        }
        state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
    }
}

sealed interface PayoutProofState {
    data object Loading : PayoutProofState
    data class Ready(val url: String) : PayoutProofState
    data class Failed(val message: String) : PayoutProofState
}

/** Asks for the temporary link to the image of the transfer of a pot. */
@HiltViewModel
class PayoutProofViewModel @Inject constructor(private val getPayoutProof: GetPayoutProofUseCase) : ViewModel() {

    private val _state = MutableStateFlow<PayoutProofState>(PayoutProofState.Loading)
    val state: StateFlow<PayoutProofState> = _state.asStateFlow()

    fun load(periodId: String) {
        _state.value = PayoutProofState.Loading
        viewModelScope.launch {
            getPayoutProof(periodId)
                .onSuccess { url -> _state.value = PayoutProofState.Ready(url) }
                .onFailure { error ->
                    _state.value = PayoutProofState.Failed(
                        if (error is ApiException && error.status == 404) "Este período no tiene la transferencia adjunta."
                        else error.userMessage(),
                    )
                }
        }
    }
}

/** The image of the transfer of a delivered pot, full screen; the link it uses works for a few minutes. */
@Composable
fun PayoutProofViewer(periodId: String, onDismiss: () -> Unit) {
    val viewModel: PayoutProofViewModel = hiltViewModel(key = "payout-proof-viewer-$periodId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(periodId) { viewModel.load(periodId) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
            when (val current = state) {
                PayoutProofState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                is PayoutProofState.Ready -> AsyncImage(
                    model = current.url,
                    contentDescription = "Transferencia del pozo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                )
                is PayoutProofState.Failed -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(current.message, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    PozzoOutlinedButton(text = "Reintentar", onClick = { viewModel.load(periodId) })
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
