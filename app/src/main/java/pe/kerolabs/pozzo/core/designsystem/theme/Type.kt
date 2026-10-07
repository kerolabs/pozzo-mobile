package pe.kerolabs.pozzo.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pe.kerolabs.pozzo.R

private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

// Plus Jakarta Sans (SIL Open Font License), the only family of the style guide, as a variable font.
val PozzoFontFamily: FontFamily = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

private fun style(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = PozzoFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

// Sizes and line heights read from the text styles of the design file.
val PozzoTypography = Typography(
    displayLarge = style(FontWeight.ExtraBold, 56, 64),
    displayMedium = style(FontWeight.ExtraBold, 40, 48),
    displaySmall = style(FontWeight.Bold, 36, 45),
    headlineLarge = style(FontWeight.Bold, 32, 40),
    headlineMedium = style(FontWeight.Bold, 28, 36),
    headlineSmall = style(FontWeight.Bold, 24, 32),
    titleLarge = style(FontWeight.SemiBold, 22, 28),
    titleMedium = style(FontWeight.SemiBold, 16, 24),
    titleSmall = style(FontWeight.SemiBold, 14, 20),
    bodyLarge = style(FontWeight.Normal, 16, 24),
    bodyMedium = style(FontWeight.Normal, 14, 20),
    bodySmall = style(FontWeight.Normal, 12, 16),
    labelLarge = style(FontWeight.SemiBold, 14, 20),
    labelMedium = style(FontWeight.SemiBold, 12, 16),
    labelSmall = style(FontWeight.Medium, 11, 16),
)
