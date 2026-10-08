package pe.kerolabs.pozzo.features.iam.presentation.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import pe.kerolabs.pozzo.core.designsystem.components.PozzoLogo
import pe.kerolabs.pozzo.core.designsystem.components.PozzoPrimaryButton
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton
import pe.kerolabs.pozzo.core.designsystem.theme.Gold
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoTheme
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras

/**
 * Initial landing screen for unauthenticated users (A1).
 *
 * Introduces the Pozzo platform value proposition and offers primary onboarding actions:
 * phone number authentication or direct entry using an invitation code.
 *
 * @param onContinueWithPhone Callback invoked when the user selects phone authentication.
 * @param onHaveInvitationCode Callback invoked when the user indicates having a group invitation code.
 */
@Composable
fun WelcomeScreen(onContinueWithPhone: () -> Unit, onHaveInvitationCode: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        PozzoLogo()
        Spacer(Modifier.height(24.dp))
        GroupCircle(modifier = Modifier.fillMaxWidth(0.85f).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(32.dp))
        Text("Tu junta, sin cuaderno ni capturas", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Pozzo valida los aportes con el comprobante y avisa a quien falta. No maneja tu dinero.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        PozzoPrimaryButton(text = "Continuar con mi celular", onClick = onContinueWithPhone, icon = Icons.Outlined.Phone)
        Spacer(Modifier.height(8.dp))
        PozzoTextButton(
            text = "Tengo un código de invitación",
            onClick = onHaveInvitationCode,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "Al continuar aceptas los Términos y Condiciones",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Illustrative graphic composable displaying group members arranged in a circular formation
 * around a central pot, joined by a dashed rotation ring.
 *
 * @param modifier Modifier applied to the outer layout container.
 */
@Composable
private fun GroupCircle(modifier: Modifier = Modifier) {
    val members = listOf("RM", "JS", "CV", "SG", "LP", "MQ", "DR", "AW")
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    val palette = listOf(
        colors.secondaryContainer to colors.onSecondaryContainer,
        Gold to colors.onTertiaryContainer,
        colors.primaryContainer to colors.onPrimaryContainer,
        status.successContainer to status.onSuccessContainer,
        colors.surfaceContainerHighest to colors.onSurfaceVariant,
        colors.secondaryContainer to colors.onSecondaryContainer,
        Gold to colors.onTertiaryContainer,
        colors.primaryContainer to colors.onPrimaryContainer,
    )
    BoxWithConstraints(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val radius = maxWidth * 0.4f
        val bubble = maxWidth * 0.13f
        val ringColor = colors.outlineVariant
        Canvas(modifier = Modifier.fillMaxSize(0.8f)) {
            drawCircle(
                color = ringColor,
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 18f))),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(0.42f).background(colors.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Savings,
                contentDescription = null,
                tint = colors.surfaceContainer,
                modifier = Modifier.fillMaxSize(0.45f),
            )
        }
        members.forEachIndexed { index, initials ->
            val angle = Math.toRadians(-90.0 + index * 360.0 / members.size)
            val (bg, fg) = palette[index]
            Box(
                modifier = Modifier
                    .offset(x = radius * cos(angle).toFloat(), y = radius * sin(angle).toFloat())
                    .size(bubble)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    PozzoTheme { WelcomeScreen({}, {}) }
}
