package pe.kerolabs.pozzo.features.notifications.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import pe.kerolabs.pozzo.core.designsystem.components.StatusChip
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras
import pe.kerolabs.pozzo.core.format.formatShortDate
import pe.kerolabs.pozzo.features.notifications.domain.Notice
import pe.kerolabs.pozzo.features.notifications.domain.NotificationKind

private val TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("es-PE"))

/**
 * I3 and J3: the automatic reminders of the groups the member organizes, the reminders already stopped
 * because the contribution is settled, and every notice Pozzo sent, grouped by day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticesScreen(onOpen: (deepLink: String) -> Unit, viewModel: NoticesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avisos", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val zone = ZoneId.systemDefault()
        val byDay = state.notices.groupBy { it.sentAt.atZone(zone).toLocalDate() }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(state.reminders, key = { "plan-${it.groupId}" }) { settings ->
                ReminderCard(
                    settings = settings,
                    showGroupName = state.reminders.size > 1,
                    onToggle = { viewModel.setRemindersEnabled(settings.groupId, it) },
                )
            }
            items(state.stopped, key = { "stopped-${it.groupId}" }) { StoppedCard(it, showGroupName = state.stopped.size > 1) }
            state.errorMessage?.let { message ->
                item { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            }
            if (state.notices.isEmpty()) {
                item { EmptyInbox() }
            }
            byDay.forEach { (day, notices) ->
                item(key = "day-$day") {
                    Text(dayLabel(day), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
                }
                items(notices, key = { it.id }) { notice ->
                    NoticeRow(notice, unseen = state.isUnseen(notice), onClick = { notice.deepLink?.let(onOpen) })
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(settings: ReminderSettings, showGroupName: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .background(colors.surfaceContainerLow, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recordatorios automáticos", style = MaterialTheme.typography.titleMedium)
                if (showGroupName) {
                    Text(settings.groupName, style = MaterialTheme.typography.labelLarge, color = colors.primary)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Se envían antes de la fecha de corte a quien aún no aportó.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = settings.plan.enabled, onCheckedChange = onToggle, enabled = !settings.isSaving)
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            settings.plan.offsetsInDays.forEach { days ->
                val (container, content) = if (settings.plan.enabled) colors.secondaryContainer to colors.onSecondaryContainer
                else colors.surfaceContainerHighest to colors.onSurfaceVariant
                StatusChip(offsetLabel(days), container, content)
            }
        }
    }
}

@Composable
private fun StoppedCard(stopped: StoppedReminders, showGroupName: Boolean) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Recordatorios del período ${stopped.turnNumber}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            StatusChip("Detenidos", status.successContainer, status.onSuccessContainer, icon = Icons.Outlined.Check)
        }
        if (showGroupName) Text(stopped.groupName, style = MaterialTheme.typography.labelLarge, color = colors.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu aporte quedó validado. Pozzo ya no te enviará recordatorios de este período.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NoticeRow(notice: Notice, unseen: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    val (icon, container, content) = noticeIcon(notice, colors.errorContainer, colors.onErrorContainer)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (notice.deepLink != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(44.dp).background(container ?: status.warningContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = content ?: status.onWarningContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(notice.title, style = MaterialTheme.typography.titleMedium)
            Text(notice.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(
                TIME.format(notice.sentAt.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (unseen) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.padding(top = 8.dp).size(10.dp).background(colors.primary, CircleShape))
        }
    }
}

@Composable
private fun noticeIcon(notice: Notice, errorContainer: Color, onErrorContainer: Color): Triple<ImageVector, Color?, Color?> {
    val status = PozzoThemeExtras.statusColors
    return when {
        notice.kind == NotificationKind.REMINDER -> Triple(Icons.Outlined.NotificationsActive, null, null)
        notice.deepLink?.endsWith("/reviews") == true -> Triple(Icons.Outlined.ReceiptLong, null, null)
        notice.title.contains("rechaz", ignoreCase = true) ->
            Triple(Icons.Outlined.ErrorOutline, errorContainer, onErrorContainer)
        else -> Triple(Icons.Outlined.CheckCircle, status.successContainer, status.onSuccessContainer)
    }
}

@Composable
private fun EmptyInbox() {
    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Outlined.NotificationsNone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("Aún no tienes avisos", style = MaterialTheme.typography.titleMedium)
        Text(
            "Aquí verás los recordatorios antes del corte y lo que pase en tus juntas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun offsetLabel(days: Int) = when (days) {
    0 -> "El día del corte"
    1 -> "1 día antes"
    else -> "$days días antes"
}

private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Hoy"
        today.minusDays(1) -> "Ayer"
        else -> formatShortDate(day)
    }
}
