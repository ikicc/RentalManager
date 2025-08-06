package com.morgen.rentalmanager.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/**
 * 设备性能等级
 * 用于根据设备性能调整应用行为
 */
enum class DevicePerformanceTier {
    /**
     * 低端设备: 有限的内存和处理能力
     * - 使用简化动画
     * - 减少视觉效果
     * - 最小化内存使用
     */
    LOW,
    
    /**
     * 中端设备: 足够的资源但有一定限制
     * - 使用适中的动画效果
     * - 保留部分视觉效果
     * - 适度缓存
     */
    MEDIUM,
    
    /**
     * 高端设备: 充足的资源
     * - 使用完整动画
     * - 启用所有视觉效果
     * - 更大的缓存
     */
    HIGH
}

/**
 * 性能等级枚举
 * 用于动态调整应用性能表现
 */
enum class PerformanceLevel {
    /**
     * 超低性能
     * 帧率 < 20，内存使用 > 90%
     */
    ULTRA_LOW,
    
    /**
     * 低性能
     * 帧率 < 30，内存使用 > 80%
     */
    LOW,
    
    /**
     * 中低性能
     * 帧率 < 45，内存使用 > 70%
     */
    MEDIUM_LOW,
    
    /**
     * 中性能
     * 帧率 < 55，内存使用 > 60%
     */
    MEDIUM,
    
    /**
     * 中高性能
     * 帧率 < 58，内存使用 > 50%
     */
    MEDIUM_HIGH,
    
    /**
     * 高性能
     * 帧率 >= 58，内存使用 <= 50%
     */
    HIGH
}

/**
 * 性能工具类，用于检测设备性能等级并提供性能优化相关功能
 */
object PerformanceUtils {
    /**
     * 检测设备性能等级
     * 根据设备RAM、CPU和API级别评估性能
     */
    fun detectDevicePerformanceTier(context: Context): DevicePerformanceTier {
        // 获取内存信息
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem / (1024 * 1024) // 转换为MB
        
        // 检测CPU核心数
        val cpuCores = Runtime.getRuntime().availableProcessors()
        
        // 根据设备参数评估性能等级
        return when {
            // 高端设备: 8GB+ RAM, 8+ 核心, Android 11+
            totalRam >= 8 * 1024 && cpuCores >= 8 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> 
                DevicePerformanceTier.HIGH
                
            // 中端设备: 4GB+ RAM, 6+ 核心, Android 10+
            totalRam >= 4 * 1024 && cpuCores >= 6 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> 
                DevicePerformanceTier.MEDIUM
                
            // 低端设备
            else -> DevicePerformanceTier.LOW
        }
    }
    
    /**
     * 优化动画持续时间
     * 根据设备性能调整动画持续时间
     */
    fun optimizeAnimationDuration(
        baseDuration: Int, 
        performanceTier: DevicePerformanceTier
    ): Int {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> (baseDuration * 0.7).toInt() // 低端设备减少30%动画时间
            DevicePerformanceTier.MEDIUM -> (baseDuration * 0.9).toInt() // 中端设备减少10%动画时间
            DevicePerformanceTier.HIGH -> baseDuration // 高端设备使用完整动画时间
        }
    }
    
    /**
     * 优化模糊半径
     * 根据设备性能调整模糊效果
     */
    fun optimizeBlurRadius(
        baseRadius: Float, 
        performanceTier: DevicePerformanceTier
    ): Float {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> baseRadius * 0.5f // 低端设备减少50%模糊半径
            DevicePerformanceTier.MEDIUM -> baseRadius * 0.8f // 中端设备减少20%模糊半径
            DevicePerformanceTier.HIGH -> baseRadius // 高端设备使用完整模糊效果
        }
    }
    
    /**
     * 获取设备是否应该使用简化渲染模式
     */
    fun shouldUseSimplifiedRendering(context: Context): Boolean {
        return detectDevicePerformanceTier(context) == DevicePerformanceTier.LOW
    }
    
    /**
     * 获取推荐的动画持续时间乘数 (性能较低的设备可能需要更短的动画)
     */
    fun getAnimationDurationMultiplier(context: Context): Float {
        return when(detectDevicePerformanceTier(context)) {
            DevicePerformanceTier.LOW -> 0.7f
            DevicePerformanceTier.MEDIUM -> 0.9f
            DevicePerformanceTier.HIGH -> 1.0f
        }
    }
    
    /**
     * 获取推荐的LazyList预加载项数量 (根据设备性能调整)
     */
    fun getRecommendedLazyListPrefetchItems(context: Context): Int {
        return when(detectDevicePerformanceTier(context)) {
            DevicePerformanceTier.LOW -> 3
            DevicePerformanceTier.MEDIUM -> 6
            DevicePerformanceTier.HIGH -> 10
        }
    }
}

/**
 * 帧率监控Composable
 * 用于在开发过程中监控应用帧率
 */
@Composable
fun FrameRateMonitor(
    onFrameRate: (Float) -> Unit
) {
    var frameCount by remember { mutableStateOf(0) }
    var lastFrameTime by remember { mutableStateOf(System.nanoTime()) }
    
    DisposableEffect(Unit) {
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                
                // 每秒计算一次帧率
                val currentTime = System.nanoTime()
                val elapsedNanos = currentTime - lastFrameTime
                val elapsedSeconds = elapsedNanos / 1_000_000_000.0
                
                if (elapsedSeconds >= 1.0) {
                    val fps = frameCount / elapsedSeconds
                    onFrameRate(fps.toFloat())
                    
                    // 重置计数器
                    frameCount = 0
                    lastFrameTime = currentTime
                }
                
                // 继续监听下一帧
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        
        // 开始监听帧回调
        Choreographer.getInstance().postFrameCallback(frameCallback)
        
        // 清理
        onDispose {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }
}

/**
 * 内存使用监控Composable
 * 用于在开发过程中监控应用内存使用情况
 */
@Composable
fun MemoryUsageMonitor(
    intervalMs: Long = 5000,
    onMemoryUsage: (Float) -> Unit
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        while (true) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val usedMemory = memoryInfo.totalMem - memoryInfo.availMem
            val memoryUsage = usedMemory.toFloat() / memoryInfo.totalMem
            
            onMemoryUsage(memoryUsage)
            
            delay(intervalMs)
        }
    }
}

/**
 * 自动降级动画效果
 * 根据当前帧率自动调整动画效果
 */
@Composable
fun AutoDegradeAnimations(
    onPerformanceLevelChanged: (PerformanceLevel) -> Unit
) {
    var currentFps by remember { mutableStateOf(60f) }
    var memoryUsage by remember { mutableStateOf(0f) }
    var performanceLevel by remember { mutableStateOf(PerformanceLevel.MEDIUM) }
    
    // 监控帧率
    FrameRateMonitor { fps ->
        currentFps = fps
    }
    
    // 监控内存使用
    MemoryUsageMonitor { usage ->
        memoryUsage = usage
    }
    
    // 根据帧率和内存使用调整性能等级
    LaunchedEffect(currentFps, memoryUsage) {
        val newPerformanceLevel = when {
            // 严重性能问题
            currentFps < 20 || memoryUsage > 0.9f -> PerformanceLevel.ULTRA_LOW
            
            // 明显性能问题
            currentFps < 30 || memoryUsage > 0.8f -> PerformanceLevel.LOW
            
            // 轻微性能问题
            currentFps < 45 || memoryUsage > 0.7f -> PerformanceLevel.MEDIUM_LOW
            
            // 良好性能
            currentFps < 55 || memoryUsage > 0.6f -> PerformanceLevel.MEDIUM
            
            // 很好的性能
            currentFps < 58 || memoryUsage > 0.5f -> PerformanceLevel.MEDIUM_HIGH
            
            // 优秀性能
            else -> PerformanceLevel.HIGH
        }
        
        if (newPerformanceLevel != performanceLevel) {
            performanceLevel = newPerformanceLevel
            onPerformanceLevelChanged(performanceLevel)
        }
    }
}
/*
*
 * 滚动配置数据类
 * 包含针对不同设备性能的滚动优化参数
 */
data class ScrollOptimizationConfig(
    val itemCount: Int,
    val fastScrollThreshold: Float,
    val enableScrollAnimation: Boolean,
    val memoryCheckInterval: Long,
    val frameDropThreshold: Int
)

/**
 * 滚动性能优化扩展
 */
object ScrollPerformanceOptimizer {
    
    /**
     * 获取针对设备的滚动优化配置
     */
    fun getScrollConfig(performanceTier: DevicePerformanceTier): ScrollOptimizationConfig {
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> ScrollOptimizationConfig(
                itemCount = 3,
                fastScrollThreshold = 3f,
                enableScrollAnimation = false,
                memoryCheckInterval = 3000L,
                frameDropThreshold = 3
            )
            DevicePerformanceTier.MEDIUM -> ScrollOptimizationConfig(
                itemCount = 6,
                fastScrollThreshold = 5f,
                enableScrollAnimation = true,
                memoryCheckInterval = 5000L,
                frameDropThreshold = 5
            )
            DevicePerformanceTier.HIGH -> ScrollOptimizationConfig(
                itemCount = 10,
                fastScrollThreshold = 8f,
                enableScrollAnimation = true,
                memoryCheckInterval = 10000L,
                frameDropThreshold = 8
            )
        }
    }
    
    /**
     * 计算最佳的列表项缓存数量
     */
    fun calculateOptimalCacheSize(
        performanceTier: DevicePerformanceTier,
        visibleItemCount: Int
    ): Int {
        val baseCache = when (performanceTier) {
            DevicePerformanceTier.LOW -> visibleItemCount + 2
            DevicePerformanceTier.MEDIUM -> visibleItemCount + 4
            DevicePerformanceTier.HIGH -> visibleItemCount + 6
        }
        return baseCache.coerceAtMost(20) // 最大缓存20个项目
    }
    
    /**
     * 检测是否需要降级滚动性能
     */
    fun shouldDegradeScrollPerformance(
        frameDropCount: Int,
        memoryUsage: Float,
        performanceTier: DevicePerformanceTier
    ): Boolean {
        val frameDropThreshold = when (performanceTier) {
            DevicePerformanceTier.LOW -> 2
            DevicePerformanceTier.MEDIUM -> 4
            DevicePerformanceTier.HIGH -> 6
        }
        
        val memoryThreshold = when (performanceTier) {
            DevicePerformanceTier.LOW -> 0.8f
            DevicePerformanceTier.MEDIUM -> 0.85f
            DevicePerformanceTier.HIGH -> 0.9f
        }
        
        return frameDropCount > frameDropThreshold || memoryUsage > memoryThreshold
    }
    
    /**
     * 优化滚动动画参数
     */
    fun optimizeScrollAnimationSpec(
        performanceTier: DevicePerformanceTier,
        isScrolling: Boolean
    ): androidx.compose.animation.core.AnimationSpec<Float> {
        return when {
            performanceTier == DevicePerformanceTier.LOW -> {
                // 低端设备：快速、简单的动画
                androidx.compose.animation.core.tween(
                    durationMillis = 150,
                    easing = androidx.compose.animation.core.LinearEasing
                )
            }
            isScrolling -> {
                // 滚动中：减少动画时间
                androidx.compose.animation.core.tween(
                    durationMillis = 200,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            }
            else -> {
                // 正常情况：完整动画
                androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                )
            }
        }
    }
}



/**
 * 滚动性能监控器
 * 用于收集和分析滚动性能数据，集成到预加载系统中
 */
class ScrollPerformanceTracker {
    private var frameDrops = 0
    private var totalFrames = 0
    private var scrollStartTime = 0L
    private var scrollEndTime = 0L
    private var performanceTier: DevicePerformanceTier = DevicePerformanceTier.MEDIUM
    
    fun initialize(performanceTier: DevicePerformanceTier) {
        this.performanceTier = performanceTier
    }
    
    fun onScrollStart() {
        scrollStartTime = System.currentTimeMillis()
        frameDrops = 0
        totalFrames = 0
    }
    
    fun onScrollEnd() {
        scrollEndTime = System.currentTimeMillis()
    }
    
    fun onFrameDrop() {
        frameDrops++
    }
    
    fun onFrame() {
        totalFrames++
    }
    
    fun getPerformanceReport(): String {
        val duration = scrollEndTime - scrollStartTime
        val fps = if (duration > 0) (totalFrames * 1000f / duration) else 0f
        val dropRate = if (totalFrames > 0) (frameDrops * 100f / totalFrames) else 0f
        
        return "滚动性能报告: FPS=${fps.toInt()}, 掉帧率=${dropRate.toInt()}%, 持续时间=${duration}ms"
    }
    
    /**
     * 获取滚动性能建议
     */
    fun getPerformanceRecommendations(): List<String> {
        val duration = scrollEndTime - scrollStartTime
        val fps = if (duration > 0) (totalFrames * 1000f / duration) else 0f
        val dropRate = if (totalFrames > 0) (frameDrops * 100f / totalFrames) else 0f
        
        val recommendations = mutableListOf<String>()
        
        when (performanceTier) {
            DevicePerformanceTier.LOW -> {
                if (fps < 20) recommendations.add("建议减少动画效果")
                if (dropRate > 15) recommendations.add("建议降低滚动动画复杂度")
            }
            DevicePerformanceTier.MEDIUM -> {
                if (fps < 30) recommendations.add("建议优化滚动动画")
                if (dropRate > 10) recommendations.add("建议减少并发动画数量")
            }
            DevicePerformanceTier.HIGH -> {
                if (fps < 45) recommendations.add("可以进一步优化性能")
                if (dropRate > 5) recommendations.add("建议检查动画实现")
            }
        }
        
        return recommendations
    }
    
    /**
     * 检查是否需要降级性能
     */
    fun shouldDegradePerformance(): Boolean {
        val duration = scrollEndTime - scrollStartTime
        val fps = if (duration > 0) (totalFrames * 1000f / duration) else 0f
        val dropRate = if (totalFrames > 0) (frameDrops * 100f / totalFrames) else 0f
        
        return when (performanceTier) {
            DevicePerformanceTier.LOW -> fps < 15 || dropRate > 20
            DevicePerformanceTier.MEDIUM -> fps < 25 || dropRate > 15
            DevicePerformanceTier.HIGH -> fps < 35 || dropRate > 10
        }
    }
}