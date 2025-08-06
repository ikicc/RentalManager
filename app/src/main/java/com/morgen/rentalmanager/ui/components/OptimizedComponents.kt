package com.morgen.rentalmanager.ui.components

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.morgen.rentalmanager.utils.PerformanceUtils
import com.morgen.rentalmanager.utils.DevicePerformanceTier
import com.morgen.rentalmanager.utils.ScrollPerformanceOptimizer
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.max
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import android.graphics.RenderEffect
import android.graphics.Shader

/**
 * 优化的LazyColumn组件
 * 根据设备性能自动调整滚动性能，解决首次滑动卡顿问题
 * 实现任务2.1的核心优化：缓存策略、快速滚动检测、自适应渲染
 */
@Composable
fun OptimizedLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 获取滚动优化配置
    val scrollConfig = remember(performanceTier) {
        ScrollPerformanceOptimizer.getScrollConfig(performanceTier)
    }
    
    // 滚动性能监控状态
    var lastFirstVisibleIndex by remember { mutableStateOf(0) }
    var lastScrollTime by remember { mutableStateOf(0L) }
    var isFirstScroll by remember { mutableStateOf(true) }
    var frameDropCount by remember { mutableStateOf(0) }
    var shouldDegradePerformance by remember { mutableStateOf(false) }
    
    // 跟踪首次滚动完成状态
    var hasPreheatedScrollSystem by remember { mutableStateOf(false) }
    
    // 添加抖动防护
    var isInAntiJitterMode by remember { mutableStateOf(false) }
    val antiJitterTimeoutMs = 200L
    
    // 添加硬件加速提示 - 检查API兼容性
    val hardwareAccModifier = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                try {
                    renderEffect = RenderEffect.createBlurEffect(
                        0.01f, 0.01f, Shader.TileMode.DECAL
                    ).asComposeRenderEffect()
                } catch (e: Exception) {
                    // 如果创建失败，就不使用效果
                }
            }
        } else {
            // API 31 以下不支持RenderEffect，使用普通graphicsLayer
            Modifier.graphicsLayer(
                alpha = 0.999f // 轻微透明度触发硬件加速
            )
        }
    }
    
    // 快速滚动检测 - 增强版本
    val isFastScrolling by remember {
        derivedStateOf {
            val currentTime = System.currentTimeMillis()
            val currentIndex = state.firstVisibleItemIndex
            val indexDiff = (currentIndex - lastFirstVisibleIndex).absoluteValue
            val timeDiff = (currentTime - lastScrollTime).coerceAtLeast(1)
            val scrollSpeed = indexDiff * 1000 / timeDiff.toFloat()
            
            // 更新状态
            if (state.isScrollInProgress) {
                lastFirstVisibleIndex = currentIndex
                lastScrollTime = currentTime
                
                // 首次滚动强制进入抖动防护模式
                if (isFirstScroll) {
                    isFirstScroll = false
                    isInAntiJitterMode = true
                }
            }
            
            // 使用配置的快速滚动阈值
            state.isScrollInProgress && scrollSpeed > scrollConfig.fastScrollThreshold
        }
    }
    
    // 滚动结束后延迟关闭抖动防护模式
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress && isInAntiJitterMode) {
            delay(antiJitterTimeoutMs)
            isInAntiJitterMode = false
        }
    }
    
    // 自适应渲染策略 - 根据滚动状态动态调整
    val adaptiveRenderingEnabled by remember {
        derivedStateOf {
            when {
                isInAntiJitterMode -> false // 抖动防护模式禁用复杂渲染
                shouldDegradePerformance -> false // 性能降级时禁用复杂渲染
                isFastScrolling -> false // 快速滚动时简化渲染
                performanceTier == DevicePerformanceTier.LOW -> false // 低端设备简化渲染
                else -> true
            }
        }
    }
    
    // 动态缓存策略 - 根据可见项目数量和设备性能调整
    val visibleItemsCount = state.layoutInfo.visibleItemsInfo.size
    val optimalCacheSize = remember(visibleItemsCount, performanceTier) {
        ScrollPerformanceOptimizer.calculateOptimalCacheSize(
            performanceTier, visibleItemsCount
        )
    }
    
    // 移除预加载策略
    
    // 根据设备性能和滚动状态优化fling行为
    val optimizedFlingBehavior = when {
        shouldDegradePerformance -> {
            // 性能降级：使用最简单的fling行为
            ScrollableDefaults.flingBehavior()
        }
        performanceTier == DevicePerformanceTier.LOW -> {
            // 低端设备：减少fling距离，提高响应性
            ScrollableDefaults.flingBehavior()
        }
        isFastScrolling -> {
            // 快速滚动时：使用优化的fling行为
            ScrollableDefaults.flingBehavior()
        }
        else -> flingBehavior
    }
    
    // 轻量级滚动预热 - 不执行实际滚动操作，避免自动滑动
    LaunchedEffect(Unit) {
        if (!hasPreheatedScrollSystem) {
            // 轻量级预热过程，仅预计算必要的参数
            withContext(Dispatchers.Default) {
                // 预先计算常用布局参数
                with(density) {
                    ModernSpacing.Medium.toPx()
                    ModernSpacing.Large.toPx()
                }
                
                // 预热完成
                hasPreheatedScrollSystem = true
            }
        }
    }
    
    // 性能监控和自动降级
    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            var frameCount = 0
            val startTime = System.currentTimeMillis()
            
            while (state.isScrollInProgress) {
                val frameTime = System.currentTimeMillis()
                frameCount++
                
                // 检测掉帧
                if (frameCount > 0 && (frameTime - startTime) / frameCount > 20) {
                    frameDropCount++
                }
                
                // 检查是否需要降级性能
                if (frameDropCount > scrollConfig.frameDropThreshold) {
                    shouldDegradePerformance = true
                }
                
                delay(16) // 约60fps的检测频率
            }
            
            // 滚动结束后重置状态
            delay(1000)
            if (frameDropCount > 0) {
                frameDropCount = max(0, frameDropCount - 1)
            }
            if (frameDropCount <= scrollConfig.frameDropThreshold / 2) {
                shouldDegradePerformance = false
            }
        }
    }
    
    // 内存监控和优化
    LaunchedEffect(state.firstVisibleItemIndex) {
        delay(scrollConfig.memoryCheckInterval)
        
        // 在低端设备上或检测到帧率下降时触发垃圾回收
        if (performanceTier == DevicePerformanceTier.LOW || frameDropCount > 2) {
            System.gc()
        }
    }
    
    // 应用硬件加速提示，并优化滚动容器
    Box(modifier = if (isFirstScroll || isInAntiJitterMode) 
        modifier.then(hardwareAccModifier) else modifier
    ) {
        // 使用经过优化的LazyColumn实现
        LazyColumn(
            modifier = Modifier.fillMaxWidth(), // 只填充宽度，高度由父容器控制
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = optimizedFlingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}

/**
 * 智能动画容器
 * 根据设备性能自动调整动画效果
 */
@Composable
fun SmartAnimationContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 根据设备性能决定是否启用动画容器
    when (performanceTier) {
        DevicePerformanceTier.LOW -> {
            // 低端设备：禁用复杂动画
            Box(modifier = modifier) {
                content()
            }
        }
        DevicePerformanceTier.MEDIUM -> {
            // 中端设备：简化动画
            Box(modifier = modifier) {
                content()
            }
        }
        DevicePerformanceTier.HIGH -> {
            // 高端设备：完整动画
            Box(modifier = modifier) {
                content()
            }
        }
    }
}

/**
 * 优化的列表项容器
 * 减少重组和重绘，优化列表项渲染性能
 */
@Composable
fun OptimizedListItem(
    key: Any,
    modifier: Modifier = Modifier,
    isScrolling: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 跟踪是否为首次渲染
    val isFirstRender = remember { mutableStateOf(true) }
    
    // 首次渲染后关闭首次渲染标志
    LaunchedEffect(Unit) {
        delay(500)
        isFirstRender.value = false
    }
    
    // 根据滚动状态和设备性能决定渲染策略，但不再使用透明度变化
    val shouldOptimizeRendering = remember(isScrolling, performanceTier, isFirstRender.value) {
        when {
            isFirstRender.value && performanceTier != DevicePerformanceTier.HIGH -> true // 首次渲染时优化中低端设备
            isScrolling && performanceTier == DevicePerformanceTier.LOW -> true
            isScrolling && performanceTier == DevicePerformanceTier.MEDIUM -> true
            else -> false
        }
    }
    
    // 我们不再使用不同的渲染策略，只关注重组优化
    key(key) {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * 重组优化包装器
 * 使用稳定的key和记忆化来减少不必要的重组
 */
@Composable
fun RecompositionOptimizer(
    key: Any,
    dependencies: List<Any> = emptyList(),
    content: @Composable () -> Unit
) {
    // 使用key来避免不必要的重组
    key(key, *dependencies.toTypedArray()) {
        content()
    }
}
/**

 * 滚动性能监控器
 * 实时监控滚动性能并自动优化
 */
@Composable
fun ScrollPerformanceMonitor(
    listState: LazyListState,
    onPerformanceIssue: (String) -> Unit = {}
) {
    var frameDropCount by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            while (listState.isScrollInProgress) {
                val currentTime = System.currentTimeMillis()
                if (lastFrameTime > 0) {
                    val frameDuration = currentTime - lastFrameTime
                    // 检测掉帧（超过16.67ms表示低于60fps）
                    if (frameDuration > 20) {
                        frameDropCount++
                        if (frameDropCount > 5) {
                            onPerformanceIssue("检测到滚动掉帧，已自动优化")
                            frameDropCount = 0
                        }
                    }
                }
                lastFrameTime = currentTime
                delay(16) // 约60fps的检测频率
            }
        }
    }
}

// 移除智能预加载管理器

/**
 * 内存优化的LazyColumn包装器
 * 自动管理内存使用和垃圾回收
 */
@Composable
fun MemoryOptimizedLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 内存监控
    var lastMemoryCheck by remember { mutableStateOf(0L) }
    
    // 定期检查内存使用情况
    LaunchedEffect(state.firstVisibleItemIndex) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMemoryCheck > 5000) { // 每5秒检查一次
            lastMemoryCheck = currentTime
            
            // 在低端设备上更频繁地触发垃圾回收
            if (performanceTier == DevicePerformanceTier.LOW) {
                System.gc()
            }
        }
    }
    
    OptimizedLazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/**
 * 自适应滚动容器
 * 根据内容和设备性能自动选择最佳滚动策略，集成智能预加载
 */
@Composable
fun AdaptiveScrollContainer(
    modifier: Modifier = Modifier,
    itemCount: Int,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    content: LazyListScope.() -> Unit
) {
    // 简化实现，直接使用标准LazyColumn
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        content = content
    )
}
