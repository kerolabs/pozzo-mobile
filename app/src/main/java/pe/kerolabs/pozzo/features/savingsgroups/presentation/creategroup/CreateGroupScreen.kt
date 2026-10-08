package pe.kerolabs.pozzo.features.savingsgroups.presentation.creategroup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import pe.kerolabs.pozzo.core.designsystem.components.FieldLabel
import pe.kerolabs.pozzo.core.designsystem.components.NumberStepper
import pe.kerolabs.pozzo.core.designsystem.components.OptionChip
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.StepProgress
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.format.formatLongDate
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.iam.domain.PhoneNumbers
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.NewGroup
import pe.kerolabs.pozzo.features.savingsgroups.domain.PaymentMethod
import pe.kerolabs.pozzo.features.savingsgroups.domain.Periodicity
import pe.kerolabs.pozzo.features.savingsgroups.domain.SavingsGroup
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.cutoffLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.paymentMethodLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.periodicityTitle

/**
 * C1, C2 and C3: the rules, the dates and destination, and the summary of a new group. Opened for a group
 * that has not started, the same steps edit it and [onSaved] runs once the changes are saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onClose: () -> Unit,
    onCreated: (SavingsGroup, Invitation) -> Unit,
    onSaved: () -> Unit = {},
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.onSavedHandled()
            onSaved()
        }
    }

    LaunchedEffect(state.created) {
        state.created?.let { (group, invitation) ->
            viewModel.onCreatedHandled()
            onCreated(group, invitation)
        }
    }
    BackHandler { if (!viewModel.back()) onClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isEditing) "Editar junta" else "Nueva junta", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { if (!viewModel.back()) onClose() }) {
                        if (state.step == CreateGroupStep.RULES) {
                            Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (state.isPreparing) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            StepProgress(current = state.step.number, total = CreateGroupStep.entries.size, label = state.step.label)
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (state.step) {
                    CreateGroupStep.RULES -> RulesStep(state, viewModel)
                    CreateGroupStep.DATES -> DatesStep(state, viewModel)
                    CreateGroupStep.SUMMARY -> SummaryStep(state)
                }
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(12.dp))
            when (state.step) {
                CreateGroupStep.RULES ->
                    PozzoPrimaryButton(text = "Continuar", onClick = viewModel::next, enabled = state.rulesValid)
                CreateGroupStep.DATES ->
                    PozzoPrimaryButton(text = "Continuar", onClick = viewModel::next, enabled = state.datesValid)
                CreateGroupStep.SUMMARY -> PozzoPrimaryButton(
                    text = if (state.isEditing) "Guardar cambios" else "Crear junta",
                    onClick = viewModel::submit,
                    loading = state.isLoading,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RulesStep(state: CreateGroupUiState, viewModel: CreateGroupViewModel) {
    PozzoTextField(
        label = "Nombre de la junta",
        value = state.name,
        onValueChange = viewModel::onNameChange,
        placeholder = "Junta del barrio",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
    )
    PozzoTextField(
        label = "Aporte por integrante",
        value = state.amount,
        onValueChange = viewModel::onAmountChange,
        prefix = "S/",
        placeholder = "300",
        supportingText = "Monto fijo que cada integrante entrega en cada período.",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
    Column {
        FieldLabel("Periodicidad")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Periodicity.entries.forEach { option ->
                OptionChip(
                    text = periodicityTitle(option),
                    selected = state.periodicity == option,
                    onClick = { viewModel.onPeriodicityChange(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    Column {
        FieldLabel("Número de integrantes")
        Spacer(Modifier.height(8.dp))
        NumberStepper(
            value = state.seats,
            onValueChange = viewModel::onSeatsChange,
            range = state.minSeats..NewGroup.MAX_SEATS,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "El ciclo dura ${state.seats} períodos y cada integrante cobra una vez. " +
                if (state.minSeats > NewGroup.MIN_SEATS) {
                    "No puede ser menos que los ${state.minSeats} integrantes que ya tiene la junta."
                } else {
                    "Entre ${NewGroup.MIN_SEATS} y ${NewGroup.MAX_SEATS} integrantes."
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatesStep(state: CreateGroupUiState, viewModel: CreateGroupViewModel) {
    var pickingDate by remember { mutableStateOf(false) }

    Column {
        FieldLabel("Primer aporte")
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedTextField(
                value = formatLongDate(state.firstContributionDate),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) },
            )
            // The read-only field does not take clicks itself, so a transparent layer opens the calendar.
            Box(Modifier.matchParentSize().clickable { pickingDate = true })
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Fecha límite del primer período. Corte: ${cutoffLabel(state.periodicity, state.firstContributionDate).lowercase()}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column {
        FieldLabel("Destino de los aportes")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { method ->
                OptionChip(
                    text = paymentMethodLabel(method),
                    selected = state.paymentMethod == method,
                    onClick = { viewModel.onPaymentMethodChange(method) },
                    modifier = Modifier.width(112.dp),
                )
            }
        }
    }
    PozzoTextField(
        label = "Número del destino",
        value = state.destinationPhone,
        onValueChange = viewModel::onDestinationPhoneChange,
        prefix = "+51",
        placeholder = "999 000 123",
        supportingText = "Pozzo valida cada comprobante contra este número. No recibe ni retiene dinero.",
        errorText = if (state.destinationPhone.length == 9 && !PhoneNumbers.isValid(state.destinationPhone)) {
            "Ingresa un celular de nueve dígitos que empiece con 9."
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
    )

    if (pickingDate) {
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.firstContributionDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !utcTimeMillis.toUtcDate().isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { viewModel.onFirstContributionDateChange(it.toUtcDate()) }
                    pickingDate = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SummaryStep(state: CreateGroupUiState) {
    val amount = state.contributionAmount ?: return
    SummaryCard(
        rows = listOf(
            "Nombre" to state.name.trim(),
            "Aporte" to formatSoles(amount),
            "Periodicidad" to periodicityTitle(state.periodicity),
            "Integrantes" to state.seats.toString(),
            "Día de corte" to cutoffLabel(state.periodicity, state.firstContributionDate),
            "Primer aporte" to formatMediumDate(state.firstContributionDate),
            "Destino" to "${paymentMethodLabel(state.paymentMethod)} · ${state.destinationPhone.chunked(3).joinToString(" ")}",
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
        Text("Pozo por turno", style = MaterialTheme.typography.titleSmall, color = onBrand)
        Text(formatSoles(state.potAmount ?: amount), style = MaterialTheme.typography.displayMedium, color = onBrand)
        Text(
            "${formatSoles(amount)} × ${state.seats} integrantes",
            style = MaterialTheme.typography.bodyLarge,
            color = onBrand,
        )
    }
    if (state.clearsTurns) {
        Text(
            "Cambiaste el número de integrantes: los turnos asignados se borran y tendrás que asignarlos de nuevo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    Text(
        "Podrás ajustar las reglas hasta que inicies la junta.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

private fun Long.toUtcDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
