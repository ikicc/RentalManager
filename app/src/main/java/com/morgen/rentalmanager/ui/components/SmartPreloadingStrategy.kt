package com.morgen.rentalmanager.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.morgen.rentalmanager.utils.DevicePerformanceTier
import com.morgen.rentalmanager.utils.PerformanceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.max
import kotlin.math.min
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

/**
 * 智能预加载策略类
 * 用于优化列表加载，特别解决首次滑动卡顿问题
 */
object SmartPreloadingStrategy {
    
    /**
     * 计算初始渲染批次大小
     * 根据设备性能决定首次渲染多少项目
     */
    fun calculateInitialBatchSize(
        performanceTier: DevicePerformanceTier,
        totalItemCount: Int
    ): Int {
        val baseSize = when (performanceTier) {
            DevicePerformanceTier.LOW -> 4
            DevicePerformanceTier.MEDIUM -> 8
            DevicePerformanceTier.HIGH -> 12
        }
        
        return min(baseSize, totalItemCount)
    }
    
    /**
     * 计算后续批次大小
     * 决定每次增量渲染多少项目
     */
    fun calculateIncrementalBatchSize(
        performanceTier: DevicePerformanceTier
    ): Int {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> 3
            DevicePerformanceTier.MEDIUM -> 5
            DevicePerformanceTier.HIGH -> 8
        }
    }
    
    /**
     * 计算批次间延迟
     * 决定每批渲染之间等待多少毫秒
     */
    fun calculateBatchDelay(
        performanceTier: DevicePerformanceTier
    ): Long {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> 100
            DevicePerformanceTier.MEDIUM -> 50
            DevicePerformanceTier.HIGH -> 30
        }
    }
}

/**
 * 增量加载控制器组件
 * 实现列表项的分批渲染，减轻首次渲染压力
 */
@Composable
fun IncrementalLoadingController(
    totalItemCount: Int,
    onBatchSizeChange: (Int) -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 记录加载已完成的标志
    var loadingCompleted by rememberSaveable { mutableStateOf(false) }
    
    // 计算初始批次大小
    val initialBatchSize = remember(performanceTier, totalItemCount) {
        SmartPreloadingStrategy.calculateInitialBatchSize(performanceTier, totalItemCount)
    }
    
    // 计算增量批次大小
    val incrementalBatchSize = remember(performanceTier) {
        SmartPreloadingStrategy.calculateIncrementalBatchSize(performanceTier)
    }
    
    // 计算批次间延迟
    val batchDelay = remember(performanceTier) {
        SmartPreloadingStrategy.calculateBatchDelay(performanceTier)
    }
    
    // 当前可见项数
    var visibleItemCount by remember { mutableStateOf(initialBatchSize) }
    
    // 当应用从后台恢复时，检查是否需要立即显示全部项目
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && loadingCompleted) {
                // 如果已经完成加载并从后台恢复，立即显示所有项目
                visibleItemCount = totalItemCount
                onBatchSizeChange(visibleItemCount)
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 启动增量加载过程
    LaunchedEffect(totalItemCount) {
        // 如果已经完成过加载，直接显示所有项目
        if (loadingCompleted) {
            visibleItemCount = totalItemCount
            onBatchSizeChange(visibleItemCount)
            return@LaunchedEffect
        }
        
        // 首先渲染初始批次 - 改进初次显示策略，确保首屏立即可见
        visibleItemCount = when (performanceTier) {
            DevicePerformanceTier.LOW -> max(initialBatchSize, 3)
            DevicePerformanceTier.MEDIUM -> max(initialBatchSize, 5) 
            DevicePerformanceTier.HIGH -> max(initialBatchSize, 8)
        }
        onBatchSizeChange(visibleItemCount)
        
        // 如果总数大于初始批次，启动增量加载
        if (totalItemCount > visibleItemCount) {
            // 给UI线程时间渲染初始批次 - 确保流畅
            delay(50)
            
            // 分批加载剩余项目，注意使用更激进的加载策略
            var currentSize = visibleItemCount
            
            // 第一批次快速加载一些项目，让列表可以立即滚动
            val fastBatchSize = min(currentSize * 2, totalItemCount)
            currentSize = fastBatchSize
            visibleItemCount = currentSize
            onBatchSizeChange(visibleItemCount)
            
            // 休息一会，让UI线程有机会处理触摸事件
            delay(100)
            
            // 继续加载剩余项目，但使用更小的批次和更长的延迟
            // 以避免在用户可能开始滚动时造成卡顿
            while (currentSize < totalItemCount) {
                yield() // 让出执行权，避免阻塞UI线程
                
                // 如果太快加载可能导致卡顿，所以加长延迟
                delay(batchDelay * 2)
                
                // 计算下一批次大小
                val nextBatchSize = min(currentSize + incrementalBatchSize, totalItemCount)
                currentSize = nextBatchSize
                visibleItemCount = currentSize
                onBatchSizeChange(visibleItemCount)
            }
            
            // 所有项目加载完成
            loadingCompleted = true
        } else {
            // 项目数量很少，直接标记为完成
            loadingCompleted = true
        }
    }
}

/**
 * 滚动渲染优化组件
 * 根据滚动状态动态调整渲染质量
 */
@Composable
fun ScrollRenderOptimizer(
    listState: LazyListState,
    onRenderQualityChange: (quality: Float) -> Unit
) {
    val context = LocalContext.current
    val performanceTier = remember { PerformanceUtils.detectDevicePerformanceTier(context) }
    
    // 跟踪滚动速度
    var lastFirstVisibleIndex by remember { mutableStateOf(0) }
    var lastScrollTime by remember { mutableStateOf(0L) }
    
    // 监控滚动速度
    LaunchedEffect(listState.isScrollInProgress) {
        while (listState.isScrollInProgress) {
            val currentTime = System.currentTimeMillis()
            val currentIndex = listState.firstVisibleItemIndex
            
            if (lastScrollTime > 0) {
                val indexDiff = kotlin.math.abs(currentIndex - lastFirstVisibleIndex)
                val timeDiff = max(currentTime - lastScrollTime, 1)
                val scrollSpeed = indexDiff * 1000f / timeDiff
                
                // 根据滚动速度动态调整渲染质量
                val renderQuality = when {
                    scrollSpeed > 10 -> 0.3f // 快速滚动，低质量渲染
                    scrollSpeed > 5 -> 0.6f // 中速滚动，中质量渲染
                    else -> 1.0f // 慢速滚动，高质量渲染
                }
                
                onRenderQualityChange(renderQuality)
            }
            
            // 更新状态
            lastFirstVisibleIndex = currentIndex
            lastScrollTime = currentTime
            
            delay(16) // 约60fps的检测频率
        }
        
        // 滚动停止时恢复高质量渲染
        delay(100) // 短暂延迟，确保滚动真的停止了
        onRenderQualityChange(1.0f)
    }
}
