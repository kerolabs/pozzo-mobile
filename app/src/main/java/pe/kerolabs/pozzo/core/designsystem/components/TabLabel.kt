package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

/**
 * The label of a tab. It stays on one line and shrinks when the phone uses a large font or a narrow
 * screen, instead of breaking a word such as "Integrantes" in two.
 */
@Composable
fun TabLabel(text: String) {
    val style = MaterialTheme.typography.titleMedium
    Text(
        text = text,
        style = style,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = style.fontSize),
    )
}
