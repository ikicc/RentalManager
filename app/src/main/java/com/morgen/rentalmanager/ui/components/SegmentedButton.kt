package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernCorners

/**
 * A modern segmented button row component with sliding animation.
 * 
 * @param options Map of option keys to display labels
 * @param selectedOption The currently selected option key
 * @param onOptionSelected Callback when an option is selected
 * @param modifier Optional modifier for the component
 */
@Composable
fun SegmentedButtonRow(
    options: Map<String, String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val optionsList = options.toList()
    val selectedIndex = optionsList.indexOfFirst { it.first == selectedOption }
    
    // 滑动动画的偏移量
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(
            durationMillis = 300,
            easing = ModernAnimations.PerfectSmoothOut
        ),
        label = "sliding_offset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(ModernCorners.Medium))
            .background(ModernColors.SurfaceVariant)
    ) {
        // 滑动的色块背景
        BoxWithConstraints {
            val itemWidth = maxWidth / optionsList.size
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(itemWidth)
                    .offset(x = itemWidth * animatedOffset)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(ModernCorners.Small))
                    .background(
                        ModernColors.Primary,
                        RoundedCornerShape(ModernCorners.Small)
                    )
            )
        }
        
        // 选项按钮
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            optionsList.forEachIndexed { index, (option, label) ->
                val isSelected = selectedOption == option
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else ModernColors.OnSurfaceVariant,
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = ModernAnimations.PerfectSmoothOut
                    ),
                    label = "textColor_$index"
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}