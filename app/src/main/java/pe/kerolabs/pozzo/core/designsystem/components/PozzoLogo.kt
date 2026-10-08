package pe.kerolabs.pozzo.core.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.kerolabs.pozzo.R

/**
 * The Pozzo logo: two interlocked rings, terracotta and gold, for the members and the turns that go
 * round. With [showName] it is the full logo; [size] is the height of the mark. Over a dark theme it
 * uses the variant with the name in the dark primary color.
 */
@Composable
fun PozzoLogo(modifier: Modifier = Modifier, size: Dp = 40.dp, showName: Boolean = true) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    if (showName) {
        Image(
            painter = painterResource(if (dark) R.drawable.pozzo_logo_dark else R.drawable.pozzo_logo_light),
            contentDescription = "Pozzo",
            modifier = modifier.height(size),
        )
    } else {
        Image(
            painter = painterResource(if (dark) R.drawable.pozzo_mark_dark else R.drawable.pozzo_mark_light),
            contentDescription = "Pozzo",
            modifier = modifier.size(size),
        )
    }
}
