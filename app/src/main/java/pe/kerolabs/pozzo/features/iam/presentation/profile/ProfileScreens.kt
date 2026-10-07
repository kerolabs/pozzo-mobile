package pe.kerolabs.pozzo.features.iam.presentation.profile

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.BuildConfig
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.iam.domain.ThemePreference

/** I1: the member's name and phone, the preferences, the legal documents and the way out. */
@Composable
fun ProfileScreen(onTheme: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ProfileField?>(null) }
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    val openLegal: (String) -> Unit = { page ->
        context.startActivity(Intent(Intent.ACTION_VIEW, (BuildConfig.LANDING_URL + page).toUri()))
    }

    Scaffold { padding ->
        val profile = state.profile
        if (profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (state.isLoading) CircularProgressIndicator()
                else Text(state.errorMessage ?: "No pudimos cargar tu perfil.", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                InitialsAvatar(
                    name = profile.displayName,
                    size = 80.dp,
                    background = MaterialTheme.colorScheme.primaryContainer,
                    content = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(12.dp))
                Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    PhoneNumbers.display(profile.phoneNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
            Text("Mis datos", style = MaterialTheme.typography.titleLarge)
            ProfileRow(Icons.Outlined.Person, "Nombre", profile.displayName, Icons.Outlined.Edit) {
                editing = ProfileField.NAME
            }
            ProfileRow(
                Icons.Outlined.AccountBalanceWallet,
                "Número de Yape o Plin",
                profile.walletNumber?.let(PhoneNumbers::grouped) ?: "Agrégalo para recibir los aportes",
                Icons.Outlined.Edit,
            ) { editing = ProfileField.WALLET }
            ProfileRow(
                Icons.Outlined.Email,
                "Correo de respaldo",
                profile.backupEmail ?: "Opcional, por si pierdes tu celular",
                Icons.Outlined.Edit,
            ) { editing = ProfileField.EMAIL }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(16.dp))
            Text("Preferencias", style = MaterialTheme.typography.titleLarge)
            ProfileRow(
                Icons.Outlined.DarkMode,
                "Tema visual",
                themeLabel(ThemePreference.of(profile.theme)),
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                onTheme,
            )
            ProfileRow(Icons.Outlined.Policy, "Términos y Condiciones", null, Icons.AutoMirrored.Filled.KeyboardArrowRight) {
                openLegal("terms.html")
            }
            ProfileRow(Icons.Outlined.Shield, "Política de privacidad", null, Icons.AutoMirrored.Filled.KeyboardArrowRight) {
                openLegal("privacy.html")
            }
            state.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            PozzoOutlinedButton(text = "Cerrar sesión", onClick = viewModel::signOut, icon = Icons.AutoMirrored.Outlined.Logout)
        }
    }

    val profile = state.profile
    when (editing) {
        ProfileField.NAME -> EditDialog(
            title = "Tu nombre",
            label = "Nombre que ve tu grupo",
            current = profile?.displayName.orEmpty(),
            allowEmpty = false,
            sanitize = { it.take(80) },
            onDismiss = { editing = null },
            onSave = {
                editing = null
                viewModel.rename(it)
            },
        )
        ProfileField.WALLET -> EditDialog(
            title = "Número de Yape o Plin",
            label = "Celular donde recibes transferencias",
            current = profile?.walletNumber.orEmpty(),
            supportingText = "Se propone como destino de los aportes cuando creas una junta.",
            keyboardType = KeyboardType.Phone,
            sanitize = { it.filter(Char::isDigit).take(9) },
            onDismiss = { editing = null },
            onSave = {
                editing = null
                viewModel.saveWalletNumber(it)
            },
        )
        ProfileField.EMAIL -> EditDialog(
            title = "Correo de respaldo",
            label = "Correo",
            current = profile?.backupEmail.orEmpty(),
            supportingText = "Solo lo ves tú. Déjalo vacío para quitarlo.",
            keyboardType = KeyboardType.Email,
            sanitize = { it.trim().take(120) },
            onDismiss = { editing = null },
            onSave = {
                editing = null
                viewModel.saveBackupEmail(it)
            },
        )
        null -> Unit
    }
}

private enum class ProfileField { NAME, WALLET, EMAIL }

@Composable
private fun ProfileRow(icon: ImageVector, title: String, subtitle: String?, trailing: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Icon(trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EditDialog(
    title: String,
    label: String,
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    sanitize: (String) -> String,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    allowEmpty: Boolean = true,
) {
    var value by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            PozzoTextField(
                label = label,
                value = value,
                onValueChange = { value = sanitize(it) },
                supportingText = supportingText,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = allowEmpty || value.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun themeLabel(theme: ThemePreference) = when (theme) {
    ThemePreference.SYSTEM -> "Usar el del sistema"
    ThemePreference.LIGHT -> "Claro"
    ThemePreference.DARK -> "Oscuro"
}

/** I2: three options, applied at once. */
@Composable
fun ThemeScreen(onBack: () -> Unit, viewModel: ThemeViewModel = hiltViewModel()) {
    val current by viewModel.theme.collectAsStateWithLifecycle()
    val error by viewModel.errorMessage.collectAsStateWithLifecycle()
    Scaffold(topBar = { PozzoTopBar(title = "Tema visual", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Elige cómo quieres ver Pozzo. El cambio se aplica al instante.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(
                ThemePreference.SYSTEM to Icons.Outlined.BrightnessAuto,
                ThemePreference.LIGHT to Icons.Outlined.LightMode,
                ThemePreference.DARK to Icons.Outlined.DarkMode,
            ).forEach { (theme, icon) ->
                val selected = theme == current
                Row(
                    Modifier
                        .fillMaxWidth()
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.shapes.large,
                        )
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { viewModel.choose(theme) })
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, contentDescription = null)
                    Spacer(Modifier.width(16.dp))
                    Text(themeLabel(theme), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(
                        if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }
    }
}
