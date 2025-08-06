package com.morgen.rentalmanager.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import android.graphics.Shader
import com.morgen.rentalmanager.ui.theme.*

// 动画参数调优为帧率对齐（120fps）
private val OPEN_DURATION = 166 // 20帧 @120fps
private val CLOSE_DURATION = 133 // 16帧 @120fps
private val ITEM_STAGGER_DELAY = 16 // 2帧 @120fps
private val SCALE_DURATION = 166 // 20帧 @120fps

// 平滑精细的动画曲线 - 帧率对齐优化
private val UltraSmoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
private val SmoothInOutEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
private val BounceEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * A floating action button menu item.
 */
data class FabMenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * A floating action button menu with blur effects and animations.
 * 所有菜单项从主FAB位置弹出，收回时也回到主FAB位置
 * 
 * @param items The menu items to display
 * @param modifier The modifier to apply to the component
 */
@Composable
fun FloatingActionButtonMenu(
    items: List<FabMenuItem>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // 主FAB旋转动画 - 使用优化的缓动函数，确保帧率对齐
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(
            durationMillis = OPEN_DURATION,
            easing = UltraSmoothEasing
        ),
        label = "fabRotation"
    )
    
    // 主FAB缩放动画 - 使用优化的弹簧参数
    val fabScale by animateFloatAsState(
        targetValue = if (expanded) 1.1f else 1f,
        animationSpec = tween( // 改用tween而非spring，确保稳定帧率
            durationMillis = SCALE_DURATION,
            easing = UltraSmoothEasing
        ),
        label = "fabScale"
    )
    
    // 背景模糊透明度动画
    val blurAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (expanded) OPEN_DURATION else CLOSE_DURATION,
            easing = UltraSmoothEasing
        ),
        label = "blurAlpha"
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // 纯高斯模糊覆盖层 - 使用animateFloatAsState替代AnimatedVisibility以获得更平滑的效果
        if (blurAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(blurAlpha)
                    .blur(ModernBlur.Light) 
                    .clickable { expanded = false }
            )
        }
        
        // 主FAB按钮 - 改进的弹性动画
        val fabButton = @Composable {
            FloatingActionButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(ModernSpacing.Medium)
                    .scale(fabScale),
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (expanded) "Close Menu" else "Open Menu",
                    modifier = Modifier
                        .rotate(rotation)
                        .size(ModernIconSize.Medium)
                )
            }
        }
        
        // 将菜单项放置在FAB上方
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = ModernSpacing.Medium, end = ModernSpacing.Medium)
        ) {
            // 菜单项容器 - 从下往上排列菜单项
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom // 从底部开始排列
            ) {
                // 占位符，与主FAB大小相同，保持菜单项与FAB对齐
                Spacer(modifier = Modifier.size(56.dp))
                
                // 反转项目列表，从下往上排列
                items.asReversed().forEachIndexed { reversedIndex, item ->
                    val index = items.size - reversedIndex - 1  // 计算原始索引用于动画延迟
                    
                    // 高级的动画参数 - 帧率对齐优化
                    val openDelay = index * ITEM_STAGGER_DELAY
                    val closeDelay = (items.size - index - 1) * ITEM_STAGGER_DELAY
                    
                    // 初始位置和目标位置计算
                    val yOffset = -70 * (reversedIndex + 1)
                    
                    // 使用动画合成 - Y轴位置动画
                    val animatedY by animateFloatAsState(
                        targetValue = if (expanded) yOffset.toFloat() else 0f,
                        animationSpec = if (expanded) {
                            // 弹出时使用tween动画替代spring，确保帧率稳定
                            tween(
                                durationMillis = OPEN_DURATION,
                                delayMillis = openDelay,
                                easing = UltraSmoothEasing
                            )
                        } else {
                            // 收回时使用标准缓动，确保回到+号中间位置
                            tween(
                                durationMillis = CLOSE_DURATION,
                                delayMillis = closeDelay,
                                easing = UltraSmoothEasing
                            )
                        },
                        label = "itemY$index"
                    )
                    
                    // 缩放动画 - 从FAB尺寸过渡到菜单项尺寸
                    val scale by animateFloatAsState(
                        targetValue = if (expanded) 1f else 0.7f,
                        animationSpec = tween(
                            durationMillis = if (expanded) SCALE_DURATION else CLOSE_DURATION,
                            delayMillis = if (expanded) openDelay else closeDelay,
                            easing = UltraSmoothEasing
                        ),
                        label = "itemScale$index"
                    )
                    
                    // 透明度动画
                    val alpha by animateFloatAsState(
                        targetValue = if (expanded) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = if (expanded) OPEN_DURATION else CLOSE_DURATION,
                            delayMillis = if (expanded) openDelay else closeDelay,
                            easing = UltraSmoothEasing
                        ),
                        label = "itemAlpha$index"
                    )
                    
                    if (reversedIndex > 0) {
                        Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                    }
                    
                    // 菜单项行 - 使用graphicsLayer替代多个修饰符以获得更好的性能和动画效果
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = animatedY
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                transformOrigin = TransformOrigin(0.5f, 0.5f) // 从中心点缩放，而不是底部中心
                                // 启用硬件加速，仅限于API 31+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    try {
                                        renderEffect = RenderEffect.createBlurEffect(
                                            0.01f, 0.01f, Shader.TileMode.DECAL
                                        ).asComposeRenderEffect()
                                    } catch (e: Exception) {
                                        // 忽略异常，在某些设备上可能不支持
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable {
                                    item.onClick()
                                    expanded = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
                        ) {
                            // 文本标签
                            Surface(
                                modifier = Modifier.wrapContentSize(),
                                shape = RoundedCornerShape(ModernCorners.Medium),
                                color = Color.White,
                                shadowElevation = ModernElevation.Level2
                            ) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = ModernColors.OnSurface,
                                    modifier = Modifier.padding(
                                        horizontal = ModernSpacing.Medium,
                                        vertical = ModernSpacing.Small
                                    )
                                )
                            }
                            
                            // 按钮
                            FloatingActionButton(
                                onClick = {
                                    item.onClick()
                                    expanded = false
                                },
                                shape = CircleShape,
                                containerColor = Color.White,
                                contentColor = Color.Black,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = ModernElevation.Level2
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(ModernIconSize.Small)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 渲染主FAB按钮
        fabButton()
    }
}