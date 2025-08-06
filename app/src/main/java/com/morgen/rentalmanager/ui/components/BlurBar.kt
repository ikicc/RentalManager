package com.morgen.rentalmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay

/**
 * Modern frosted glass top bar using Compose-native blur effects.
 * Provides a translucent background with blur-like appearance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedBlurTopBar(
    height: Dp = 56.dp,
    blurRadius: Float = 16f,
    overlayColor: ComposeColor = MaterialTheme.colorScheme.surface, // 使用主题表面色，自动适配深色模式
    title: String,
    onMenuClick: () -> Unit = {},
    menuExpanded: Boolean = false,
    onDismissMenu: () -> Unit = {},
    menuContent: @Composable ColumnScope.() -> Unit = {}
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height + statusBarPadding)
    ) {
        // Frosted glass background with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor)
        )

        // Foreground content (status bar spacer + top app bar row)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarPadding))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title, 
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface // 使用主题文字色，自动适配深色模式
                )
                
                // Menu button with dropdown and animations
                Box(contentAlignment = Alignment.TopEnd) {
                    // 动画状态
                    var iconScale by remember { mutableStateOf(1f) }
                    var iconRotation by remember { mutableStateOf(0f) }
                    
                    // 动画效果
                    val animatedScale by animateFloatAsState(
                        targetValue = iconScale,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        ),
                        label = "menu_icon_scale"
                    )
                    
                    val animatedRotation by animateFloatAsState(
                        targetValue = iconRotation,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        ),
                        label = "menu_icon_rotation"
                    )
                    
                    // 菜单展开状态变化时的动画效果
                    LaunchedEffect(menuExpanded) {
                        if (menuExpanded) {
                            // 展开时旋转180度
                            iconRotation = 180f
                            iconScale = 0.8f
                            delay(100)
                            iconScale = 1f
                        } else {
                            // 收起时恢复原位
                            iconRotation = 0f
                            iconScale = 0.8f
                            delay(100)
                            iconScale = 1f
                        }
                    }
                    
                    // 菜单按钮 - 添加动画
                    IconButton(
                        onClick = onMenuClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (menuExpanded) 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                            else 
                                ComposeColor.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Default.MoreVert, 
                            contentDescription = "菜单",
                            tint = MaterialTheme.colorScheme.onSurface, // 使用主题文字色，自动适配深色模式
                            modifier = Modifier
                                .scale(animatedScale)
                                .rotate(animatedRotation)
                        )
                    }
                    
                    // 菜单下拉 - 修复：使用标准DropdownMenu确保DropdownMenuItem正常工作
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = onDismissMenu,
                        modifier = Modifier
                            // 菜单背景自动适配深色模式，使用主题表面色
                            .background(MaterialTheme.colorScheme.surface) // 使用背景修饰符设置颜色
                            .zIndex(100f) // 确保菜单在最上层
                    ) {
                        menuContent()
                    }
                }
            }
        }
    }
}

