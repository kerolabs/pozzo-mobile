package pe.kerolabs.pozzo.features.contributions.presentation.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InfoBanner
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTopBar
import pe.kerolabs.pozzo.core.designsystem.components.ResultBadge
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.ReviewDecision
import pe.kerolabs.pozzo.features.contributions.presentation.common.DateField
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.contributions.presentation.common.MemberOption
import pe.kerolabs.pozzo.features.contributions.presentation.common.fieldLabel
import pe.kerolabs.pozzo.features.contributions.presentation.common.inconsistencyLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.rememberMemberPhotos

/** The common frame of the organizer's screens: top bar, scrollable body and actions at the bottom. */
@Composable
private fun OrganizerScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit = {},
    body: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(topBar = { PozzoTopBar(title = title, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                body()
            }
            Column(Modifier.padding(top = 12.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

/** H1: the contributions whose receipt did not match and wait for the organizer. */
@Composable
fun ReviewsScreen(
    onBack: () -> Unit,
    onOpen: (contributionId: String) -> Unit,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(state.cycle?.groupId)
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    OrganizerScaffold(title = "Aportes por revisar", onBack = onBack) {
        val period = state.period
        if (period == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.height(320.dp))
            return@OrganizerScaffold
        }
        Text(
            "Estos comprobantes no coinciden con lo esperado. Revisa cada uno y decide si lo apruebas.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.reviews.isEmpty()) {
            InfoBanner("No hay aportes por revisar en el período ${period.turnNumber}.", Icons.Outlined.TaskAlt)
        }
        state.reviews.forEach { contribution ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
                    .clickable { onOpen(contribution.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InitialsAvatar(name = contribution.memberName, size = 40.dp, photoUrl = photos[contribution.membershipId])
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(contribution.memberName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${formatSoles(contribution.amount)} · " +
                            contribution.inconsistencies.joinToString(", ") { inconsistencyLabel(it.field).lowercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

/** H2: what was expected next to what the receipt says, to approve it or reject it. */
@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    onDecided: (wasLast: Boolean) -> Unit,
    viewModel: ReviewDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(state.cycle?.groupId)
    var note by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(state.decided) { if (state.decided != null) onDecided(state.reviews.size <= 1) }

    val contribution = state.reviews.firstOrNull { it.id == viewModel.contributionId }
    OrganizerScaffold(
        title = "Revisar aporte",
        onBack = onBack,
        actions = {
            if (contribution != null) {
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PozzoOutlinedButton(
                        text = "Rechazar",
                        onClick = { viewModel.decide(ReviewDecision.REJECT, note) },
                        enabled = !state.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                    PozzoPrimaryButton(
                        text = "Aprobar igual",
                        onClick = { viewModel.decide(ReviewDecision.APPROVE, note) },
                        loading = state.isSaving,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
    ) {
        if (contribution == null) {
            if (state.isLoading || state.errorMessage != null) {
                LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.height(320.dp))
            } else {
                InfoBanner("Este aporte ya fue revisado.", Icons.Outlined.Info)
            }
            return@OrganizerScaffold
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = contribution.memberName, photoUrl = photos[contribution.membershipId])
            Spacer(Modifier.width(12.dp))
            Column {
                Text(contribution.memberName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Período ${state.period?.turnNumber} · registrado el ${formatMediumDate(contribution.registeredAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val failed = contribution.inconsistencies.associateBy { it.field }
        val receipt = contribution.receipt
        val cycle = state.cycle
        val period = state.period
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            CheckRow(
                title = fieldLabel("AMOUNT"),
                detail = "${formatSoles(contribution.amount)} · esperado ${period?.let { formatSoles(it.contributionAmount) }.orEmpty()}",
                ok = "AMOUNT" !in failed,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CheckRow(
                title = fieldLabel("PAYEE"),
                detail = "${receipt?.payeeName.orEmpty()} · esperado ${cycle?.payeeName.orEmpty()}",
                ok = "PAYEE" !in failed,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CheckRow(
                title = fieldLabel("DATE"),
                detail = "${receipt?.let { formatMediumDate(it.paidAt) }.orEmpty()} · corte ${period?.let { formatMediumDate(it.cutoffDate) }.orEmpty()}",
                ok = "DATE" !in failed,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            CheckRow(title = "N.º de operación", detail = "${receipt?.operationNumber.orEmpty()} · no está repetido", ok = true)
        }
        PozzoTextField(
            label = "Nota para ${contribution.memberName.substringBefore(' ')} (opcional)",
            value = note,
            onValueChange = { note = it.take(200) },
            singleLine = false,
        )
        InfoBanner(
            "Si lo apruebas, cuenta como aporte validado. Si lo rechazas, ${contribution.memberName.substringBefore(' ')} deberá registrarlo de nuevo.",
            Icons.Outlined.Info,
        )
    }
}

@Composable
private fun CheckRow(title: String, detail: String, ok: Boolean) {
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Outlined.Check else Icons.Outlined.Close,
            contentDescription = null,
            tint = if (ok) status.success else colors.error,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        if (!ok) StatusChip("No coincide", colors.errorContainer, colors.onErrorContainer)
    }
}

/** H3: a contribution the organizer received in cash, for members with or without the app. */
@Composable
fun CashScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: CashViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(state.cycle?.groupId)
    LaunchedEffect(state.done) { if (state.done != null) onDone() }
    val period = state.period
    OrganizerScaffold(
        title = "Registrar efectivo",
        onBack = onBack,
        actions = {
            state.errorMessage?.takeIf { period != null }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PozzoPrimaryButton(
                text = "Registrar aporte",
                onClick = viewModel::register,
                enabled = state.selectedMember != null && state.amountValue != null,
                loading = state.isSaving,
            )
        },
    ) {
        if (period == null) {
            LoadingOrError(state.isLoading, state.errorMessage, onBack, Modifier.height(320.dp))
            return@OrganizerScaffold
        }
        Text("¿De quién lo recibiste?", style = MaterialTheme.typography.titleMedium)
        val pending = period.unsettled
        if (pending.isEmpty()) InfoBanner("Todos ya aportaron en este período.", Icons.Outlined.TaskAlt)
        pending.forEach { member ->
            MemberOption(
                name = member.displayName,
                subtitle = if (member.state == ContributionState.LATE) "Atrasado" else "Pendiente",
                selected = state.selectedMember == member.membershipId,
                onClick = { viewModel.onMemberSelected(member.membershipId) },
                photoUrl = photos[member.membershipId],
            )
        }
        PozzoTextField(
            label = "Monto recibido",
            value = state.amount,
            onValueChange = viewModel::onAmountChange,
            prefix = "S/",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        DateField("Fecha", state.receivedOn, viewModel::onReceivedOnChange)
        InfoBanner("El aporte en efectivo queda validado al registrarlo y figura en el historial.", Icons.Outlined.Info)
    }
}

/** H5: another member covers someone's contribution so the pot can be completed. */
@Composable
fun CoverScreen(onBack: () -> Unit, onDone: () -> Unit, viewModel: CoverViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(state.cycle?.groupId)
    LaunchedEffect(state.done) { if (state.done != null) onDone() }
    val period = state.period
    OrganizerScaffold(
        title = "Cubrir un aporte",
        onBack = onBack,
        actions = {
            state.errorMessage?.takeIf { period != null }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PozzoPrimaryButton(
                text = "Registrar cobertura",
                onClick = viewModel::register,
                enabled = state.selectedMember != null && state.coveredBy != null,
                loading = state.isSaving,
            )
        },
    ) {
        if (period == null) {
            LoadingOrError(state.isLoading, state.errorMessage, onBack, Modifier.height(320.dp))
            return@OrganizerScaffold
        }
        Text("¿Qué aporte se cubre?", style = MaterialTheme.typography.titleMedium)
        val pending = period.unsettled
        if (pending.isEmpty()) InfoBanner("No hay aportes pendientes en este período.", Icons.Outlined.TaskAlt)
        pending.forEach { member ->
            MemberOption(
                name = member.displayName,
                subtitle = if (member.state == ContributionState.LATE) "Atrasado" else "Pendiente",
                selected = state.selectedMember == member.membershipId,
                onClick = { viewModel.onMemberSelected(member.membershipId) },
                photoUrl = photos[member.membershipId],
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("¿Quién lo cubre?", style = MaterialTheme.typography.titleMedium)
        period.members.filter { it.membershipId != state.selectedMember }.forEach { member ->
            MemberOption(
                name = member.displayName,
                subtitle = null,
                isMe = member.isMe,
                selected = state.coveredBy == member.membershipId,
                onClick = { viewModel.onCoveredBySelected(member.membershipId) },
                photoUrl = photos[member.membershipId],
            )
        }
        InfoBanner(
            "La cobertura completa el pozo de ${formatSoles(period.contributionAmount)}. La deuda queda entre ellos y se anota en el historial de cumplimiento.",
            Icons.Outlined.Info,
        )
    }
}

/** H4: the pot is complete and the organizer marks it as delivered to whoever collects the turn. */
@Composable
fun DeliverScreen(
    onBack: () -> Unit,
    onNextPeriod: () -> Unit,
    onCycleClosed: (groupName: String, totalTurns: Int, potAmount: String) -> Unit,
    viewModel: DeliverViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val photos = rememberMemberPhotos(state.cycle?.groupId)
    val period = state.period
    val cycle = state.cycle
    LaunchedEffect(state.delivery) {
        val delivery = state.delivery ?: return@LaunchedEffect
        if (delivery.cycleClosed && cycle != null) {
            onCycleClosed(cycle.groupName, cycle.totalTurns, cycle.potAmount.toPlainString())
        }
    }
    val delivery = state.delivery
    if (delivery != null && !delivery.cycleClosed && period != null) {
        ResultLayout(
            icon = Icons.Filled.CheckCircle,
            title = "Pozo entregado",
            message = "${formatSoles(delivery.deliveredAmount)} a ${period.payoutMemberName}. Ya empezó el período ${delivery.nextTurn}.",
            action = "Ver el nuevo período",
            onAction = onNextPeriod,
        )
        return
    }
    OrganizerScaffold(
        title = "Entregar pozo",
        onBack = onBack,
        actions = {
            state.errorMessage?.takeIf { period != null }?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            PozzoPrimaryButton(
                text = "Marcar como entregado",
                onClick = viewModel::deliver,
                icon = Icons.Outlined.TaskAlt,
                enabled = period != null,
                loading = state.isSaving,
            )
        },
    ) {
        if (period == null || cycle == null) {
            LoadingOrError(state.isLoading, state.errorMessage, onBack, Modifier.height(320.dp))
            return@OrganizerScaffold
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
                .padding(20.dp),
        ) {
            val onBrand = MaterialTheme.colorScheme.onPrimaryContainer
            Text("Período ${period.turnNumber} de ${period.totalTurns} · Pozo completo", style = MaterialTheme.typography.titleSmall, color = onBrand)
            Text(formatSoles(period.collectedAmount), style = MaterialTheme.typography.displayMedium, color = onBrand)
            Text("para ${period.payoutMemberName}", style = MaterialTheme.typography.bodyLarge, color = onBrand)
        }
        SummaryCard(
            rows = listOf(
                "Aportes" to "${period.settledCount} de ${period.membersCount}",
                "Cobra" to period.payoutMemberName,
                "Turno" to "${period.turnNumber} de ${period.totalTurns}",
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                "Transfiere el pozo a ${period.payoutMemberName.substringBefore(' ')} desde tu billetera y luego márcalo como entregado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (period.turnNumber == period.totalTurns) {
            InfoBanner("Es el último turno: al entregarlo se cierra el ciclo de la junta.", Icons.Outlined.Info)
        }
    }
}

/** H6: every member collected their turn and the cycle is closed. */
@Composable
fun CycleClosedScreen(groupName: String, totalTurns: Int, potAmount: String, onDone: () -> Unit) {
    ResultLayout(
        icon = Icons.Outlined.Celebration,
        title = "¡Ciclo completado!",
        message = "Los $totalTurns integrantes de $groupName recibieron su pozo de ${formatSoles(potAmount.toBigDecimal())}. " +
            "El historial de cumplimiento de cada uno ya está actualizado.",
        action = "Volver a mis juntas",
        onAction = onDone,
    )
}

@Composable
private fun ResultLayout(icon: ImageVector, title: String, message: String, action: String, onAction: () -> Unit) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ResultBadge(icon)
                Spacer(Modifier.height(24.dp))
                Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            PozzoPrimaryButton(text = action, onClick = onAction)
            Spacer(Modifier.height(8.dp))
        }
    }
}

