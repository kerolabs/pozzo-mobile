package pe.kerolabs.pozzo.features.contributions.presentation.register

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import pe.kerolabs.pozzo.core.designsystem.components.FieldLabel
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.OptionChip
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatLongDate
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.contributions.domain.Contribution
import pe.kerolabs.pozzo.features.contributions.domain.ContributionStatus
import pe.kerolabs.pozzo.features.contributions.domain.ReceiptSource
import pe.kerolabs.pozzo.features.contributions.presentation.common.DateField
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.contributions.presentation.common.fieldLabel
import pe.kerolabs.pozzo.features.contributions.presentation.common.inconsistencyLabel

/**
 * F2 to F6: how to pay, the receipt, the data read from it and the result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterContributionScreen(
    onClose: () -> Unit,
    onMyContributions: () -> Unit,
    viewModel: RegisterContributionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { if (!viewModel.back()) onClose() }

    val title = when (state.step) {
        RegisterStep.INSTRUCTIONS -> "Aportar al pozo"
        RegisterStep.CAPTURE -> "Subir comprobante"
        RegisterStep.REVIEW -> "Revisa los datos"
        RegisterStep.RESULT -> ""
    }
    Scaffold(
        topBar = {
            if (state.step != RegisterStep.RESULT) {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            when (state.step) {
                RegisterStep.INSTRUCTIONS -> InstructionsStep(state, onNext = { viewModel.goTo(RegisterStep.CAPTURE) }, onRetry = viewModel::load)
                RegisterStep.CAPTURE -> CaptureStep(state, viewModel)
                RegisterStep.REVIEW -> ReviewStep(state, viewModel)
                RegisterStep.RESULT -> state.result?.let { ResultStep(it, state, onClose, onMyContributions) }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.InstructionsStep(
    state: RegisterUiState,
    onNext: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val cycle = state.cycle
    val period = state.period
    val colors = MaterialTheme.colorScheme
    if (cycle == null || period == null) {
        // Centered in the whole step, not at the top of the scrolling column.
        LoadingOrError(state.errorMessage == null, state.errorMessage, onRetry, Modifier.weight(1f))
        return
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
                .padding(20.dp),
        ) {
            Text("Transfiere ${formatSoles(period.contributionAmount)} a", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                InitialsAvatar(name = cycle.payeeName)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(cycle.payeeName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cabeza de junta · ${cycle.destinationMethod.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Número de destino", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    Text(cycle.destinationPhone.chunked(3).joinToString(" "), style = MaterialTheme.typography.headlineSmall)
                }
                StatusChip(
                    "Copiar",
                    colors.secondaryContainer,
                    colors.onSecondaryContainer,
                    Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Número", cycle.destinationPhone))
                    },
                    Icons.Outlined.ContentCopy,
                )
            }
        }
        listOf(
            "Haz la transferencia desde tu billetera",
            "Guarda el comprobante o tómale una foto",
            "Súbelo aquí y Pozzo lo valida por ti",
        ).forEachIndexed { index, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).background(colors.surfaceContainerHighest, CircleShape), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(12.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        InfoBanner(
            "Pozzo no recibe ni retiene tu dinero. La transferencia va directo a ${cycle.payeeName.substringBefore(' ')}.",
            Icons.Outlined.Info,
        )
    }
    PozzoPrimaryButton(text = "Ya transferí, subir comprobante", onClick = onNext, icon = Icons.Outlined.Upload)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CaptureStep(state: RegisterUiState, viewModel: RegisterContributionViewModel) {
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) photoUri?.let(viewModel::onImageChosen)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onImageChosen)
    }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(colors.inverseSurface, MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isReading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.inverseOnSurface)
                    Spacer(Modifier.height(12.dp))
                    Text("Leyendo el comprobante…", color = colors.inverseOnSurface)
                }
            } else {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = colors.inverseOnSurface, modifier = Modifier.size(96.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Encuadra el comprobante completo: monto, fecha, destinatario y número de operación. Pozzo lo lee en tu teléfono y guarda la imagen para que tú y la cabeza puedan verla después.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
    PozzoPrimaryButton(
        text = "Tomar foto",
        icon = Icons.Outlined.PhotoCamera,
        enabled = !state.isReading,
        onClick = {
            val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
            val file = File(dir, "receipt-${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            camera.launch(uri)
        },
    )
    Spacer(Modifier.height(8.dp))
    PozzoOutlinedButton(
        text = "Elegir de la galería",
        icon = Icons.Outlined.Image,
        enabled = !state.isReading,
        onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
    )
    PozzoTextButton(
        text = "Ingresar los datos a mano",
        onClick = viewModel::enterManually,
        enabled = !state.isReading,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ReviewStep(state: RegisterUiState, viewModel: RegisterContributionViewModel) {
    val form = state.form
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            if (form.readFromImage) "Pozzo leyó estos datos de tu comprobante. Corrige lo que haga falta."
            else "Completa los datos tal como aparecen en tu comprobante.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        PozzoTextButton(
            text = if (form.readFromImage) "Tomar otra foto" else "Subir una foto del comprobante",
            onClick = { viewModel.goTo(RegisterStep.CAPTURE) },
        )
        val period = state.period
        val cycle = state.cycle
        if (period != null && cycle != null && form.amountValue != null) {
            if (state.matchesExpected) {
                Row(
                    Modifier.fillMaxWidth().background(status.successContainer, MaterialTheme.shapes.large).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = status.onSuccessContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Coincide con lo esperado: ${formatSoles(period.contributionAmount)} a ${cycle.payeeName}",
                        color = status.onSuccessContainer,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(status.warningContainer, MaterialTheme.shapes.large).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = status.onWarningContainer)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "No coincide con lo esperado (${formatSoles(period.contributionAmount)} a ${cycle.payeeName} hasta el " +
                            "${formatMediumDate(period.cutoffDate)}). Si lo registras así, la cabeza lo revisará.",
                        color = status.onWarningContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        PozzoTextField(
            label = "Monto",
            value = form.amount,
            onValueChange = viewModel::onAmountChange,
            prefix = "S/",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        DateField("Fecha del pago", form.paidAt, viewModel::onPaidAtChange)
        PozzoTextField(label = "Destinatario", value = form.payeeName, onValueChange = viewModel::onPayeeChange)
        PozzoTextField(
            label = "N.º de operación",
            value = form.operationNumber,
            onValueChange = viewModel::onOperationChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Column {
            FieldLabel("Aplicación")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReceiptSource.entries.forEach { source ->
                    OptionChip(
                        text = when (source) {
                            ReceiptSource.YAPE -> "Yape"
                            ReceiptSource.PLIN -> "Plin"
                            ReceiptSource.BANK -> "Banco"
                        },
                        selected = form.source == source,
                        onClick = { viewModel.onSourceChange(source) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
    }
    Spacer(Modifier.height(12.dp))
    PozzoPrimaryButton(text = "Confirmar aporte", onClick = viewModel::confirm, enabled = form.isComplete, loading = state.isSaving)
    Spacer(Modifier.height(8.dp))

}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ResultStep(
    contribution: Contribution,
    state: RegisterUiState,
    onClose: () -> Unit,
    onMyContributions: () -> Unit,
) {
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    val validated = contribution.status == ContributionStatus.VALIDATED || contribution.status == ContributionStatus.APPROVED
    val period = state.period
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(
            Modifier.size(112.dp).background(if (validated) status.successContainer else status.warningContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (validated) Icons.Filled.CheckCircle else Icons.Outlined.HourglassTop,
                contentDescription = null,
                tint = if (validated) status.onSuccessContainer else status.onWarningContainer,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            if (validated) "¡Aporte validado!" else "Tu aporte está en revisión",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            if (validated) {
                "Tu aporte de ${formatSoles(contribution.amount)} del período ${period?.turnNumber ?: ""} quedó registrado al instante."
            } else {
                "El comprobante no coincide con lo esperado. ${state.cycle?.payeeName?.substringBefore(' ') ?: "La cabeza"} lo revisará y te avisaremos."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.imageNotKept) {
            InfoBanner(
                "Tu aporte quedó registrado, pero no pudimos guardar la imagen del comprobante.",
                Icons.Outlined.Info,
            )
        }
        if (validated) {
            SummaryCard(
                rows = listOfNotNull(
                    "Período" to "${period?.turnNumber} de ${period?.totalTurns}",
                    "Monto" to formatSoles(contribution.amount),
                    contribution.receipt?.let { "Fecha" to formatMediumDate(it.paidAt) },
                    contribution.receipt?.let { "N.º de operación" to it.operationNumber },
                ),
            )
        } else {
            SummaryCard(
                rows = contribution.inconsistencies.flatMap { item ->
                    listOf(
                        "Resultado" to inconsistencyLabel(item.field),
                        "Esperado · ${fieldLabel(item.field)}" to item.expected,
                        "Leído en el comprobante" to item.found,
                    )
                },
            )
        }
    }
    PozzoPrimaryButton(text = if (validated) "Ver el estado del pozo" else "Entendido", onClick = onClose)
    if (validated) {
        PozzoTextButton(
            text = "Ver mis aportes",
            onClick = onMyContributions,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    } else {
        Spacer(Modifier.height(8.dp))
    }
}
