package pe.kerolabs.pozzo.features.savingsgroups.presentation.invitation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.ResultBadge

/**
 * C4: the group is ready; the organizer copies or shares its invitation code and link.
 */
@Composable
fun InvitationScreen(groupName: String, code: String, link: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val shortLink = link.removePrefix("https://").removePrefix("http://")
    val colors = MaterialTheme.colorScheme

    fun copy(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        scope.launch { snackbar.showSnackbar("$label copiado") }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResultBadge(icon = Icons.Outlined.Celebration)
            Spacer(Modifier.height(24.dp))
            Text("¡Tu junta está lista!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Comparte el código o el enlace para que tu grupo se una.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                    .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .padding(20.dp),
            ) {
                Text("Código de invitación", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(code, style = MaterialTheme.typography.displayMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { copy("Código", code) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar código", tint = colors.primary)
                    }
                }
                HorizontalDivider(color = colors.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = colors.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(shortLink, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { copy("Enlace", link) }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copiar enlace", tint = colors.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Los integrantes se unen sin que tengas que registrarlos uno por uno.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.weight(1f))
            PozzoPrimaryButton(
                text = "Compartir invitación",
                icon = Icons.Outlined.Share,
                onClick = {
                    val message = "Únete a mi junta \"$groupName\" en Pozzo. Abre $link o ingresa el código $code."
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message)
                    }
                    context.startActivity(Intent.createChooser(send, "Compartir invitación"))
                },
            )
            Spacer(Modifier.height(8.dp))
            PozzoTextButton(text = "Ir a mis juntas", onClick = onDone)
        }
    }
}
