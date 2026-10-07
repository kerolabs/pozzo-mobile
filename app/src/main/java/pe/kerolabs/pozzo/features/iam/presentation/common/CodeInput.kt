package pe.kerolabs.pozzo.features.iam.presentation.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.kerolabs.pozzo.core.designsystem.components.PozzoTextButton

/** Codes sent by SMS or email have six digits. */
const val VERIFICATION_CODE_LENGTH = 6

/**
 * The six boxes of a verification code, focused when they appear. The keyboard only offers digits.
 */
@Composable
fun CodeInput(code: String, onCodeChange: (String) -> Unit, isError: Boolean, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(VERIFICATION_CODE_LENGTH)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.focusRequester(focus),
        decorationBox = { CodeBoxes(code, isError) },
    )
}

@Composable
private fun CodeBoxes(code: String, isError: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(VERIFICATION_CODE_LENGTH) { index ->
            val focused = index == code.length
            val borderColor = when {
                isError -> MaterialTheme.colorScheme.error
                focused -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f)
                    .border(if (focused) 2.dp else 1.dp, borderColor, MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center,
            ) {
                Text(code.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

/**
 * "Reenviar código en 0:25" while the backend does not accept a new code, then a button to ask for it.
 */
@Composable
fun ResendCode(secondsToResend: Long, enabled: Boolean, onResend: () -> Unit, modifier: Modifier = Modifier) {
    if (secondsToResend > 0) {
        Text(
            "Reenviar código en %d:%02d".format(secondsToResend / 60, secondsToResend % 60),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        PozzoTextButton(text = "Reenviar código", onClick = onResend, enabled = enabled, modifier = modifier)
    }
}

/**
 * Counts down the seconds until a new code can be requested and reports each second to [onTick].
 */
class ResendCountdown(private val scope: CoroutineScope, private val onTick: (Long) -> Unit) {

    private var job: Job? = null

    fun start(until: Instant) {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val seconds = Duration.between(Instant.now(), until).seconds.coerceAtLeast(0)
                onTick(seconds)
                if (seconds == 0L) break
                delay(1_000)
            }
        }
    }
}
