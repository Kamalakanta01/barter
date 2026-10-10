package dev.barter.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.barter.core.designsystem.theme.tileBackgroundLightness
import dev.barter.core.designsystem.theme.tileForegroundLightness

/**
 * The prototype's `.tile`: a coloured block showing a title's first letter, with the hue derived
 * from the title so the same item always gets the same colour.
 *
 * The caller owns the size — cards use `fillMaxWidth().aspectRatio(4f / 3f)`, the dialogs a square.
 *
 * Purely decorative: every use sits next to the title it was built from, so screen readers skip it
 * rather than announcing a stray letter.
 */
@Composable
fun LetterTile(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    cornerRadius: Dp = 12.dp,
) {
    val backgroundLightness = tileBackgroundLightness
    val foregroundLightness = tileForegroundLightness
    // Worked out once per title, not on every redraw while a list scrolls.
    val (background, foreground) = remember(text, backgroundLightness, foregroundLightness) {
        val hue = ((text.sumOf(Char::code) * 7) % 360).toFloat()
        Color.hsl(hue, 0.45f, backgroundLightness) to Color.hsl(hue, 0.50f, foregroundLightness)
    }
    val letter = remember(text) { text.take(1).uppercase() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
