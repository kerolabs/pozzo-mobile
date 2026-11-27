package pe.kerolabs.pozzo.features.compliancehistory.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.kerolabs.pozzo.core.designsystem.components.InitialsAvatar
import pe.kerolabs.pozzo.core.designsystem.components.PozzoOutlinedButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.components.TabLabel
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatShortDate
import pe.kerolabs.pozzo.features.compliancehistory.domain.ComplianceSummary
import pe.kerolabs.pozzo.features.compliancehistory.domain.MemberCompliance
import pe.kerolabs.pozzo.features.contributions.domain.ContributionState
import pe.kerolabs.pozzo.features.contributions.domain.Cycle
import pe.kerolabs.pozzo.features.contributions.domain.Period
import pe.kerolabs.pozzo.features.contributions.domain.PeriodState
import pe.kerolabs.pozzo.features.contributions.presentation.common.LoadingOrError
import pe.kerolabs.pozzo.features.savingsgroups.presentation.common.rememberMemberPhotos

/**
 * Root of the History destination. In a started group the member sees who contributed in each period (K1),
 * choosing between the groups they organize and the ones they take part in; the organizer also sees how
 * every member complies (K2). Without started groups, the member sees their own history (G3).
 */
@Composable
fun HistoryScreen(
    onMyHistory: () -> Unit,
    onGroupContributions: (groupId: String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when {
        state.isResolving -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.hasGroups -> GroupHistory(state, viewModel, onMyHistory)
        else -> MyHistoryScreen(onBack = null, onGroupContributions = onGroupContributions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupHistory(state: HistoryUiState, viewModel: HistoryViewModel, onMyHistory: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var choosing by remember { mutableStateOf(false) }
    var choosingRole by remember { mutableStateOf(false) }
    val showsMembers = state.showsMembers && tab == 1
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    Box {
                        IconButton(onClick = { choosingRole = true }) {
                            Icon(Icons.Outlined.Person, contentDescription = "Ver juntas como cabeza o participante")
                        }
                        DropdownMenu(expanded = choosingRole, onDismissRequest = { choosingRole = false }) {
                            HistoryRole.entries.forEach { role ->
                                val count = state.groupsOf(role).size
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (count) {
                                                0 -> "${role.label} (sin juntas)"
                                                1 -> "${role.label} · 1 junta"
                                                else -> "${role.label} · $count juntas"
                                            },
                                        )
                                    },
                                    leadingIcon = {
                                        if (role == state.role) Icon(Icons.Outlined.Check, contentDescription = "Elegido")
                                    },
                                    enabled = count > 0,
                                    onClick = {
                                        choosingRole = false
                                        if (role != state.role) {
                                            tab = 0
                                            viewModel.selectRole(role)
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            // The member's own history, with the link to share it (G3), is a tap away from any group.
            Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                PozzoOutlinedButton(text = "Ver y compartir mi historial", onClick = onMyHistory, icon = Icons.Outlined.IosShare)
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text(
                "Como ${state.role.label.lowercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.selectedGroup?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        groupSubtitle(state.cycle, state.periods.lastOrNull()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.groups.size > 1) {
                    Box {
                        OutlinedIconButton(onClick = { choosing = true }) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Elegir otra junta")
                        }
                        DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                            state.groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(if (group.isClosed) "${group.name} (cerrada)" else group.name) },
                                    onClick = {
                                        choosing = false
                                        viewModel.select(group.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // A participant reads the contributions of the group, but only the organizer reads each member's compliance.
            if (state.showsMembers) {
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { TabLabel("Aportes") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { TabLabel("Integrantes") })
                }
            }
            val cycle = state.cycle
            if (cycle == null) {
                LoadingOrError(state.isLoading, state.errorMessage, viewModel::load)
                return@Column
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                val photos = rememberMemberPhotos(state.selectedGroupId)
                if (showsMembers) MembersCompliance(state.members, photos) else ContributionsMatrix(cycle, state.periods, photos)
            }
        }
    }
}

private fun groupSubtitle(cycle: Cycle?, current: Period?): String {
    if (cycle == null) return ""
    if (!cycle.isActive) return "Ciclo completado · ${cycle.totalTurns} períodos"
    if (current == null) return "Período ${cycle.currentTurn} de ${cycle.totalTurns}"
    return "Período ${current.turnNumber} de ${current.totalTurns} · Corte ${formatShortDate(current.cutoffDate)}"
}

/** K1: members in rows and periods in columns, with the state of each contribution. */
@Composable
private fun ContributionsMatrix(cycle: Cycle, periods: List<Period>, photos: Map<String, String>) {
    val colors = MaterialTheme.colorScheme
    val members = periods.lastOrNull()?.members.orEmpty()
    val byTurn = periods.associateBy { it.turnNumber }
    val currentTurn = if (cycle.isActive) cycle.currentTurn else null
    val cell = 32.dp
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .padding(12.dp),
    ) {
        Row {
            // Names stay fixed; the periods scroll sideways when there are many.
            Column {
                Spacer(Modifier.height(cell))
                members.forEach { member ->
                    Row(Modifier.height(cell + 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(name = member.displayName, size = 28.dp, photoUrl = photos[member.membershipId])
                        Spacer(Modifier.width(8.dp))
                        Text(
                            member.displayName.substringBefore(' '),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.width(72.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                (1..cycle.totalTurns).forEach { turn ->
                    val highlighted = turn == currentTurn
                    Column(
                        Modifier
                            .width(cell)
                            .background(if (highlighted) colors.secondaryContainer else Color.Transparent, MaterialTheme.shapes.small),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.height(cell), contentAlignment = Alignment.Center) {
                            Text("$turn", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                        }
                        members.forEach { member ->
                            val state = byTurn[turn]?.members?.firstOrNull { it.membershipId == member.membershipId }?.state
                            Box(Modifier.height(cell + 8.dp), contentAlignment = Alignment.Center) { StateMark(state) }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    // The legend wraps instead of running off the edge of narrow screens.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Legend(ContributionState.VALIDATED, "Validado")
        Legend(ContributionState.IN_REVIEW, "Revisión")
        Legend(ContributionState.PENDING, "Pendiente")
        Legend(ContributionState.LATE, "Atrasado")
        Legend(ContributionState.COVERED, "Cubierto")
    }
    val current = periods.lastOrNull()
    if (current != null && current.state != PeriodState.DELIVERED) {
        Spacer(Modifier.height(16.dp))
        Row {
            Figure("Validados", "${current.settledCount} de ${current.membersCount}", Modifier.weight(1f))
            Figure("Por revisar", "${current.inReviewCount}", Modifier.weight(1f))
            Figure("Pendientes", "${current.unsettled.size}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StateMark(state: ContributionState?) {
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    when (state) {
        null -> Box(Modifier.size(6.dp).background(colors.outlineVariant, CircleShape))
        ContributionState.VALIDATED -> Icon(Icons.Filled.CheckCircle, "Validado", tint = status.success, modifier = Modifier.size(20.dp))
        ContributionState.IN_REVIEW -> Icon(Icons.Outlined.HourglassTop, "En revisión", tint = status.onWarningContainer, modifier = Modifier.size(20.dp))
        ContributionState.PENDING -> Icon(Icons.Outlined.Schedule, "Pendiente", tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        ContributionState.LATE -> Icon(Icons.Outlined.WarningAmber, "Atrasado", tint = colors.error, modifier = Modifier.size(20.dp))
        ContributionState.COVERED -> Icon(Icons.Outlined.VolunteerActivism, "Cubierto", tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Legend(state: ContributionState, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateMark(state)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Figure(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

/** K2: every member with their contributions on time, across all their groups. */
@Composable
private fun MembersCompliance(members: List<MemberCompliance>, photos: Map<String, String>) {
    val colors = MaterialTheme.colorScheme
    members.forEach { member ->
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = member.displayName, size = 44.dp, photoUrl = photos[member.membershipId])
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(member.displayName, style = MaterialTheme.typography.titleMedium)
                val summary = member.summary
                val detail = when {
                    !member.usesApp || summary == null -> "Sin la app · lo registras tú"
                    member.isOrganizer -> "Cabeza · ${onTimeLabel(summary)}"
                    else -> onTimeLabel(summary)
                }
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            val summary = member.summary
            if (!member.usesApp || summary == null) {
                StatusChip("Manual", colors.surfaceContainerHighest, colors.onSurfaceVariant)
            } else {
                PunctualityChip(summary)
            }
        }
    }
}

private fun onTimeLabel(summary: ComplianceSummary) =
    if (summary.contributions == 0) "Aún sin aportes" else "${summary.onTime} de ${summary.contributions} puntuales"

@Composable
private fun PunctualityChip(summary: ComplianceSummary) {
    val status = PozzoThemeExtras.statusColors
    val colors = MaterialTheme.colorScheme
    val punctuality = summary.punctuality
    when {
        punctuality == null -> StatusChip("Nuevo", colors.surfaceContainerHighest, colors.onSurfaceVariant)
        punctuality >= 90 -> StatusChip("$punctuality %", status.successContainer, status.onSuccessContainer)
        punctuality >= 75 -> StatusChip("$punctuality %", status.warningContainer, status.onWarningContainer)
        else -> StatusChip("$punctuality %", colors.errorContainer, colors.onErrorContainer)
    }
}

/**
 * G3: the share of contributions made on time across every group, the detail by group and a link to
 * share it. [onBack] is null when it is the root of the History destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHistoryScreen(
    onBack: (() -> Unit)?,
    onGroupContributions: (groupId: String) -> Unit,
    viewModel: MyHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }
    LaunchedEffect(state.shareUrl) {
        val url = state.shareUrl ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Mi historial de cumplimiento en Pozzo: $url")
        }
        context.startActivity(Intent.createChooser(send, "Compartir mi historial"))
        viewModel.onShared()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi historial", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        val history = state.history
        if (history == null) {
            LoadingOrError(state.isLoading, state.errorMessage, viewModel::load, Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryHero(history.summary)
            Text("Por junta", style = MaterialTheme.typography.titleLarge)
            if (history.groups.isEmpty()) {
                Text(
                    "Aún no hay aportes registrados. Tu historial se arma con los aportes de tus juntas en curso.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            history.groups.forEachIndexed { index, group ->
                Row(
                    Modifier.fillMaxWidth().clickable { onGroupContributions(group.groupId) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                if (group.completed) MaterialTheme.colorScheme.surfaceContainerHighest
                                else MaterialTheme.colorScheme.secondaryContainer,
                                CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Outlined.Groups, contentDescription = null) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.groupName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${if (group.completed) "Cerrada" else "En curso"} · ${group.onTime} de ${group.contributions} puntuales",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ver mis aportes")
                }
                if (index < history.groups.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Text(
                "Resumen generado por Pozzo a partir de los aportes registrados en tus juntas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            PozzoPrimaryButton(
                text = "Compartir mi historial",
                onClick = viewModel::share,
                icon = Icons.Outlined.IosShare,
                loading = state.isSharing,
                enabled = history.summary.contributions > 0,
            )
        }
    }
}

@Composable
private fun SummaryHero(summary: ComplianceSummary) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.secondaryContainer, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Text("Aportes puntuales", style = MaterialTheme.typography.titleSmall, color = colors.onSecondaryContainer)
        Text(
            summary.punctuality?.let { "$it %" } ?: "Nuevo",
            style = MaterialTheme.typography.displayMedium,
            color = colors.onSecondaryContainer,
        )
        Text(
            if (summary.contributions == 0) "Todavía no tienes aportes registrados"
            else "${summary.onTime} de ${summary.contributions} aportes en todas tus juntas",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSecondaryContainer,
        )
        if (summary.contributions > 0) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("${summary.onTime} puntuales", status.successContainer, status.onSuccessContainer)
                if (summary.late > 0) {
                    StatusChip(
                        if (summary.late == 1) "1 atrasado" else "${summary.late} atrasados",
                        colors.surfaceContainerLowest,
                        colors.onSurface,
                    )
                }
                if (summary.covered > 0) {
                    StatusChip(
                        if (summary.covered == 1) "1 cubierto" else "${summary.covered} cubiertos",
                        colors.surfaceContainerLowest,
                        colors.onSurface,
                    )
                }
            }
        }
    }
}
