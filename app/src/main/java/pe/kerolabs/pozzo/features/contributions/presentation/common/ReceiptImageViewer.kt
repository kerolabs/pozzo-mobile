package pe.kerolabs.pozzo.features.contributions.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.network.ApiException
import pe.kerolabs.pozzo.core.network.userMessage
import pe.kerolabs.pozzo.features.contributions.application.GetReceiptImageUseCase

sealed interface ReceiptImageState {
    data object Loading : ReceiptImageState
    data class Ready(val url: String) : ReceiptImageState
    data class Failed(val message: String) : ReceiptImageState
}

/** Asks for the temporary link to the image of a receipt. */
@HiltViewModel
class ReceiptImageViewModel @Inject constructor(private val getReceiptImage: GetReceiptImageUseCase) : ViewModel() {

    private val _state = MutableStateFlow<ReceiptImageState>(ReceiptImageState.Loading)
    val state: StateFlow<ReceiptImageState> = _state.asStateFlow()

    fun load(contributionId: String) {
        _state.value = ReceiptImageState.Loading
        viewModelScope.launch {
            getReceiptImage(contributionId)
                .onSuccess { url -> _state.value = ReceiptImageState.Ready(url) }
                .onFailure { error ->
                    _state.value = ReceiptImageState.Failed(
                        if (error is ApiException && error.status == 404) {
                            "Este aporte no tiene la imagen del comprobante guardada."
                        } else {
                            error.userMessage()
                        },
                    )
                }
        }
    }
}

/**
 * The image of the receipt of a contribution, full screen. Only the member it counts for and the organizer
 * can open it; the link it uses works for a few minutes.
 */
@Composable
fun ReceiptImageViewer(contributionId: String, onDismiss: () -> Unit) {
    val viewModel: ReceiptImageViewModel = hiltViewModel(key = "receipt-$contributionId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(contributionId) { viewModel.load(contributionId) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
            when (val current = state) {
                ReceiptImageState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
                is ReceiptImageState.Ready -> AsyncImage(
                    model = current.url,
                    contentDescription = "Comprobante",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(top = 56.dp),
                )
                is ReceiptImageState.Failed -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(current.message, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    PozzoOutlinedButton(text = "Reintentar", onClick = { viewModel.load(contributionId) })
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
