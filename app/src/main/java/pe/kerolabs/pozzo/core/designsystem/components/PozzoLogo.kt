package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.core.designsystem.theme.Gold
import pe.kerolabs.pozzo.core.designsystem.theme.SurfaceContainerLight

/**
 * The Pozzo logo: a terracotta rounded square with a cream ring (the turns) and a gold coin (the pot).
 */
@Composable
fun PozzoLogo(modifier: Modifier = Modifier, size: Dp = 36.dp, showName: Boolean = true) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size * 0.6f)) {
                drawCircle(color = SurfaceContainerLight, style = Stroke(width = this.size.minDimension * 0.12f))
                drawCircle(color = Gold, radius = this.size.minDimension * 0.22f)
            }
        }
        if (showName) {
            Text(
                "Pozzo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
            )
        }
    }
}
