package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.theme.PozzoThemeExtras

/**
 * Circle with the initials of a member, e.g. "AW" for Anna Weber, or their photo when there is one.
 * The initials stay underneath while the photo loads or if it cannot be loaded.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color? = null,
    content: Color? = null,
    photoUrl: String? = null,
) {
    // Without explicit colors each person keeps the same color everywhere, picked from their name.
    val (bg, fg) = if (background != null && content != null) background to content else avatarColors(name)
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(initialsOf(name), style = MaterialTheme.typography.titleMedium, color = fg)
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de $name",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun avatarColors(name: String): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val status = PozzoThemeExtras.statusColors
    val palette = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
        status.successContainer to status.onSuccessContainer,
        colors.surfaceContainerHighest to colors.onSurfaceVariant,
    )
    return palette[Math.floorMod(name.trim().lowercase().hashCode(), palette.size)]
}

fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
