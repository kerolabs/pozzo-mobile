package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Text field with the label above it, as in the design file, and an optional help or error text.
 */
@Composable
fun PozzoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder?.let { { Text(it) } },
            prefix = prefix?.let { { Text("$it  ") } },
            isError = errorText != null,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            enabled = enabled,
            visualTransformation = visualTransformation,
            shape = MaterialTheme.shapes.large,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        val help = errorText ?: supportingText
        if (help != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = help,
                style = MaterialTheme.typography.bodySmall,
                color = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
