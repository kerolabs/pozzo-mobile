package pe.kerolabs.pozzo.features.savingsgroups.presentation.joingroup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.savingsgroups.domain.InvitationCodes

/** Shows the dash of an invitation code (JB-7K4M) without making the member type it. */
private object InvitationCodeTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val shown = InvitationCodes.display(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset > 2) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = if (offset > 2) offset - 1 else offset
        }
        return TransformedText(AnnotatedString(shown), mapping)
    }
}

/**
 * D1: the invitation code shared by the organizer.
 */
@Composable
fun JoinCodeScreen(
    onBack: () -> Unit,
    onGroupFound: (code: String) -> Unit,
    viewModel: JoinCodeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.foundCode) {
        state.foundCode?.let {
            viewModel.onFoundHandled()
            onGroupFound(it)
        }
    }

    Scaffold(topBar = { PozzoTopBar(title = "Unirme a una junta", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("Ingresa el código de invitación", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Lo encuentras en el mensaje que te compartió tu cabeza de junta. También puedes abrir el enlace directamente.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            PozzoTextField(
                label = "Código de invitación",
                value = state.code,
                visualTransformation = InvitationCodeTransformation,
                onValueChange = viewModel::onCodeChange,
                placeholder = "JB-7K4M",
                supportingText = "Son ${InvitationCodes.LENGTH} caracteres.",
                errorText = state.errorMessage,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                ),
            )
            Spacer(Modifier.weight(1f))
            PozzoPrimaryButton(
                text = "Buscar junta",
                onClick = viewModel::search,
                enabled = state.canSearch,
                loading = state.isLoading,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
