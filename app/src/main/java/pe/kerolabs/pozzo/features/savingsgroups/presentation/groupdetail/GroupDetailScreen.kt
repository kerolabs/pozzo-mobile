package pe.kerolabs.pozzo.features.savingsgroups.presentation.groupdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextField
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.SummaryCard
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatMediumDate
import pe.kerolabs.pozzo.core.format.formatSoles
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupDetail
import pe.kerolabs.pozzo.features.savingsgroups.domain.GroupStatus
import pe.kerolabs.pozzo.features.savingsgroups.domain.Invitation
import pe.kerolabs.pozzo.features.savingsgroups.domain.Member
import pe.kerolabs.pozzo.features.savingsgroups.domain.MembershipKind
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.ChecklistItem
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.ReadinessChecklist
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.TurnRow
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.cutoffLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.paymentMethodLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.periodicityTitle
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.shortCutoffLabel
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.turnMethodLabel

/**
 * E1 for the organizer and E9 for a participant: the group with its members, turns and rules, and
 * what is still missing to start it. E2, adding a member without the application, opens as a sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onAssignTurns: (groupId: String, seats: Int) -> Unit,
    onStartGroup: (groupId: String) -> Unit,
    onOpenPot: (groupId: String) -> Unit,
    onShowInvitation: (groupName: String, invitation: Invitation) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menuOpen by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }
    LaunchedEffect(state.invitation) {
        val invitation = state.invitation
        val detail = state.detail
        if (invitation != null && detail != null) {
            viewModel.onInvitationHandled()
            onShowInvitation(detail.group.name, invitation)
        }
    }

    val detail = state.detail
    val group = detail?.group
    val canEdit = group != null && group.isOrganizer && !group.isStarted

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        group?.name.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    if (canEdit) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text("Invitar integrantes") },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.invite()
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            if (canEdit && detail != null && !detail.group.readiness.groupFull && state.tab == GroupDetailTab.MEMBERS) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::openAddMember,
                    icon = { Icon(Icons.Outlined.PersonAddAlt, contentDescription = null) },
                    text = { Text("Agregar", style = MaterialTheme.typography.titleMedium) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        bottomBar = {
            if (detail != null && detail.group.status == GroupStatus.STARTED) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PozzoPrimaryButton(text = "Ver el pozo", onClick = { onOpenPot(viewModel.groupId) })
                }
            } else if (canEdit && detail != null) {
                MainAction(
                    detail = detail,
                    onInvite = viewModel::invite,
                    onAssignTurns = { onAssignTurns(viewModel.groupId, detail.group.seats) },
                    onStart = { onStartGroup(viewModel.groupId) },
                )
            }
        },
    ) { padding ->
        when {
            detail == null && state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            detail == null -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.errorMessage ?: "No pudimos cargar la junta.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                PozzoOutlinedButton(text = "Reintentar", onClick = viewModel::load)
            }
            else -> DetailContent(
                detail = detail,
                tab = state.tab,
                onSelectTab = viewModel::selectTab,
                onRemoveMember = viewModel::remove,
                onChangeTurns = { onAssignTurns(viewModel.groupId, detail.group.seats) },
                canEdit = canEdit,
                contentPadding = padding,
            )
        }
    }

    state.addMember?.let { form ->
        AddMemberSheet(
            form = form,
            onNameChange = viewModel::onNewMemberName,
            onPhoneChange = viewModel::onNewMemberPhone,
            onSave = viewModel::saveNewMember,
            onDismiss = viewModel::closeAddMember,
        )
    }
}

@Composable
private fun DetailContent(
    detail: GroupDetail,
    tab: GroupDetailTab,
    onSelectTab: (GroupDetailTab) -> Unit,
    onRemoveMember: (Member) -> Unit,
    onChangeTurns: () -> Unit,
    canEdit: Boolean,
    contentPadding: PaddingValues,
) {
    val group = detail.group
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (group.status) {
                    GroupStatus.STARTED -> StatusChip("En curso", status.successContainer, status.onSuccessContainer)
                    GroupStatus.CLOSED -> StatusChip("Cerrada", colors.surfaceContainerHighest, colors.onSurfaceVariant)
                    else -> StatusChip("Por iniciar", status.warningContainer, status.onWarningContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${formatSoles(group.contributionAmount)} · ${periodicityTitle(group.periodicity)} · " +
                        shortCutoffLabel(group.periodicity, group.firstContributionDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        if (!group.isStarted) {
            item {
                ReadinessChecklist(
                    title = "Para iniciar la junta",
                    items = listOf(
                        ChecklistItem(
                            group.readiness.groupFull,
                            "Grupo completo",
                            "${detail.activeMembers} de ${group.seats} integrantes",
                        ),
                        ChecklistItem(
                            group.readiness.turnsAssigned,
                            "Turnos definidos",
                            if (group.readiness.turnsAssigned) turnMethodLabel(group.turnMethod)
                            else "Falta elegir cómo se reparte el orden",
                        ),
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        item {
            PrimaryTabRow(selectedTabIndex = tab.ordinal, containerColor = colors.surface) {
                GroupDetailTab.entries.forEach { option ->
                    Tab(
                        selected = tab == option,
                        onClick = { onSelectTab(option) },
                        text = { Text(option.label, style = MaterialTheme.typography.titleMedium) },
                        selectedContentColor = colors.primary,
                        unselectedContentColor = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        when (tab) {
            GroupDetailTab.MEMBERS -> items(detail.members, key = { it.membershipId }) { member ->
                MemberRow(member = member, canRemove = canEdit && !member.isOrganizer, onRemove = { onRemoveMember(member) })
            }
            GroupDetailTab.TURNS -> {
                if (detail.turns.turns.isEmpty()) {
                    item {
                        Text(
                            "Aún no se definen los turnos. La cabeza de junta los asigna cuando el grupo está completo.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                } else {
                    items(detail.turns.turns, key = { it.turnNumber }) { turn ->
                        TurnRow(
                            turnNumber = turn.turnNumber,
                            name = if (turn.isMe) "${turn.displayName} (tú)" else turn.displayName,
                            cutoffDate = turn.cutoffDate,
                            highlighted = turn.isMe,
                        )
                    }
                    if (canEdit) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            PozzoOutlinedButton(text = "Cambiar turnos", onClick = onChangeTurns)
                        }
                    }
                }
            }
            GroupDetailTab.RULES -> item {
                if (!group.isOrganizer) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        StatusChip(
                            "Solo lectura",
                            colors.surfaceContainerHighest,
                            colors.onSurfaceVariant,
                            icon = Icons.Outlined.Visibility,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Las administra la cabeza de la junta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
                SummaryCard(
                    rows = listOfNotNull(
                        "Aporte por integrante" to formatSoles(group.contributionAmount),
                        "Periodicidad" to periodicityTitle(group.periodicity),
                        "Integrantes" to group.seats.toString(),
                        "Pozo por turno" to formatSoles(group.potAmount),
                        "Día de corte" to cutoffLabel(group.periodicity, group.firstContributionDate),
                        "Primer aporte" to formatMediumDate(group.firstContributionDate),
                        group.destination?.let {
                            "Destino" to "${paymentMethodLabel(it.method)} · ${it.phoneNumber.chunked(3).joinToString(" ")}"
                        },
                        "Turnos" to turnMethodLabel(group.turnMethod),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MemberRow(member: Member, canRemove: Boolean, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canRemove) { menuOpen = true }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(name = member.displayName, photoUrl = member.photoUrl)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (member.isMe) "${member.displayName} (tú)" else member.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                member.phoneNumber?.let {
                    Text(
                        "+51 " + it.chunked(3).joinToString(" "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            when {
                member.isOrganizer -> StatusChip("Cabeza", colors.secondaryContainer, colors.onSecondaryContainer)
                member.kind == MembershipKind.APP -> StatusChip("Usa Pozzo", colors.surfaceContainerHighest, colors.onSurfaceVariant)
                else -> StatusChip("Sin la app", status.warningContainer, status.onWarningContainer)
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Quitar de la junta") },
                onClick = {
                    menuOpen = false
                    onRemove()
                },
            )
        }
    }
}

@Composable
private fun MainAction(detail: GroupDetail, onInvite: () -> Unit, onAssignTurns: () -> Unit, onStart: () -> Unit) {
    val readiness = detail.group.readiness
    Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when {
            !readiness.groupFull -> PozzoPrimaryButton(text = "Invitar integrantes", onClick = onInvite)
            !readiness.turnsAssigned -> PozzoPrimaryButton(text = "Asignar turnos", onClick = onAssignTurns)
            else -> PozzoPrimaryButton(text = "Iniciar la junta", onClick = onStart, enabled = readiness.canStart)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberSheet(
    form: AddMemberForm,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Agregar sin la aplicación", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Para quien no instalará Pozzo o aporta en efectivo. Tú registras sus aportes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            PozzoTextField(
                label = "Nombre",
                value = form.name,
                onValueChange = onNameChange,
                placeholder = "Rosa Medina",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            Spacer(Modifier.height(16.dp))
            PozzoTextField(
                label = "Celular (opcional)",
                value = form.phone,
                onValueChange = onPhoneChange,
                prefix = "+51",
                placeholder = "999 000 000",
                errorText = form.errorMessage,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            Spacer(Modifier.height(24.dp))
            PozzoPrimaryButton(text = "Agregar a la junta", onClick = onSave, enabled = form.canSave, loading = form.isSaving)
            PozzoTextButton(text = "Cancelar", onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
