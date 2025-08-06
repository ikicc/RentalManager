package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.morgen.rentalmanager.ui.theme.ModernColors

/**
 * 脉冲加载动画
 * 用于显示加载状态
 */
@Composable
fun PulseLoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = ModernColors.Primary,
    size: Dp = 40.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loading")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
    }
}

/**
 * 成功动画
 * 用于显示操作成功状态
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50),
    size: Dp = 40.dp,
    isVisible: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "success_scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isVisible) 0f else -180f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "success_rotation"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

/**
 * 错误动画
 * 用于显示操作失败状态
 */
@Composable
fun ErrorAnimation(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFF44336),
    size: Dp = 40.dp,
    isVisible: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 400f
        ),
        label = "error_scale"
    )
    
    val shake by animateFloatAsState(
        targetValue = if (isVisible) 0f else 10f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "error_shake"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .offset(x = shake.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Error",
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

/**
 * 标准加载动画
 * 用于显示加载状态
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    color: Color = ModernColors.Primary,
    size: Dp = 40.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            )
        ),
        label = "loading_rotation"
    )
    
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = color,
        strokeWidth = 4.dp
    )
}