package com.morgen.rentalmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * 毛玻璃效果顶部栏组件
 * 
 * 该组件创建一个具有毛玻璃效果的顶部栏，可以固定在可滚动界面的顶部
 * 
 * @param modifier Modifier修饰符
 * @param height 顶部栏高度
 * @param backgroundColor 背景颜色（半透明，将与模糊效果混合）
 * @param content 内容组件
 */
@Composable
fun FrostedGlassTopBar(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.4f),
    content: @Composable BoxScope.() -> Unit
) {
    FrostedGlassBar(
        modifier = modifier,
        height = height,
        backgroundColor = backgroundColor,
        position = FrostedGlassPosition.TOP,
        content = content
    )
}

/**
 * 毛玻璃效果底部栏组件
 * 
 * 该组件创建一个具有毛玻璃效果的底部栏，可以固定在可滚动界面的底部
 * 
 * @param modifier Modifier修饰符
 * @param height 底部栏高度
 * @param backgroundColor 背景颜色（半透明，将与模糊效果混合）
 * @param content 内容组件
 */
@Composable
fun FrostedGlassBottomBar(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.4f),
    content: @Composable BoxScope.() -> Unit
) {
    FrostedGlassBar(
        modifier = modifier,
        height = height,
        backgroundColor = backgroundColor,
        position = FrostedGlassPosition.BOTTOM,
        content = content
    )
}

/**
 * 毛玻璃效果栏位置枚举
 */
enum class FrostedGlassPosition {
    TOP, BOTTOM
}

/**
 * 毛玻璃效果基础栏组件
 * 
 * @param modifier Modifier修饰符
 * @param height 栏高度
 * @param backgroundColor 背景颜色（半透明，将与模糊效果混合）
 * @param position 栏位置（顶部或底部）
 * @param shape 形状
 * @param content 内容组件
 */
@Composable
fun FrostedGlassBar(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.4f),
    position: FrostedGlassPosition,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit
) {
    val gradient = when (position) {
        FrostedGlassPosition.TOP -> Brush.verticalGradient(
            colors = listOf(
                backgroundColor,
                backgroundColor.copy(alpha = backgroundColor.alpha * 0.85f)
            )
        )
        FrostedGlassPosition.BOTTOM -> Brush.verticalGradient(
            colors = listOf(
                backgroundColor.copy(alpha = backgroundColor.alpha * 0.85f),
                backgroundColor
            )
        )
    }
    
    // 创建毛玻璃效果容器
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .zIndex(10f) // 确保在最上层
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        // 内容层 - 确保内容在最上层且清晰可见
        content()
    }
}

/**
 * 毛玻璃效果容器组件
 * 
 * 该组件可以用于创建一个包含背景内容和毛玻璃效果前景的布局
 * 
 * @param modifier Modifier修饰符
 * @param backgroundContent 背景内容
 * @param frostedTopBarHeight 顶部毛玻璃栏高度（0表示不显示）
 * @param frostedBottomBarHeight 底部毛玻璃栏高度（0表示不显示）
 * @param backgroundColor 背景颜色（半透明）
 * @param topBarContent 顶部栏内容
 * @param bottomBarContent 底部栏内容
 */
@Composable
fun FrostedGlassContainer(
    modifier: Modifier = Modifier,
    backgroundContent: @Composable () -> Unit,
    frostedTopBarHeight: Dp = 56.dp,
    frostedBottomBarHeight: Dp = 0.dp,
    backgroundColor: Color = Color.White.copy(alpha = 0.4f),
    topBarContent: @Composable (BoxScope.() -> Unit)? = null,
    bottomBarContent: @Composable (BoxScope.() -> Unit)? = null
) {
    Box(modifier = modifier) {
        // 背景内容
        backgroundContent()
        
        // 顶部毛玻璃栏
        if (frostedTopBarHeight > 0.dp && topBarContent != null) {
            FrostedGlassTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                height = frostedTopBarHeight,
                backgroundColor = backgroundColor,
                content = topBarContent
            )
        }
        
        // 底部毛玻璃栏
        if (frostedBottomBarHeight > 0.dp && bottomBarContent != null) {
            FrostedGlassBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                height = frostedBottomBarHeight,
                backgroundColor = backgroundColor,
                content = bottomBarContent
            )
        }
    }
} 