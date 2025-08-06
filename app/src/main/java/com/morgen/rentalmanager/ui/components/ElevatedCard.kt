package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernElevation

/**
 * A card with animated elevation effects when pressed.
 * 
 * @param modifier The modifier to apply to the component
 * @param shape The shape of the card
 * @param onClick The callback to invoke when the card is clicked
 * @param defaultElevation The default elevation of the card
 * @param pressedElevation The elevation of the card when pressed
 * @param content The content to display inside the card
 */
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(ModernCorners.Medium),
    onClick: (() -> Unit)? = null,
    defaultElevation: Dp = ModernElevation.Level1,
    pressedElevation: Dp = ModernElevation.Level3,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) pressedElevation else defaultElevation,
        animationSpec = tween(
            durationMillis = ModernAnimations.FAST_DURATION,
            easing = ModernAnimations.FastOutSlowIn
        ),
        label = "cardElevation"
    )
    
    Card(
        modifier = if (onClick != null) {
            modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
        } else {
            modifier
        },
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.SurfaceContainer // 修复：使用主题表面容器色，自动适配深色模式
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Box(content = content)
    }
}