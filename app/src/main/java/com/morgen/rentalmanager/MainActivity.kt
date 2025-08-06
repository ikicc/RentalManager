package com.morgen.rentalmanager

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.ui.components.*
import com.morgen.rentalmanager.ui.main.MainViewModel
import com.morgen.rentalmanager.ui.main.MainViewModelFactory
import com.morgen.rentalmanager.ui.receipt.ReceiptUi
import com.morgen.rentalmanager.ui.AppNavigation
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernElevation
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.MyApplicationTheme
import com.morgen.rentalmanager.utils.captureComposableToBitmap
import com.morgen.rentalmanager.utils.saveBitmapToFile

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.withContext
import android.view.ViewGroup

// 在类级别添加预热标志和延迟加载机制
class MainActivity : ComponentActivity() {
    // 添加应用预热标志
    private var isAppPrewarmed = false
    // 添加UI已准备好标志
    private var isUiReady = false
    // 添加滚动系统预热标志
    private var isScrollSystemPrewarmed = false
    // 添加预热锁定，防止重复预热
    private val prewarmLock = Any()
    
    // 延迟初始化组件 - 减少冷启动时间
    private val mainScope = CoroutineScope(Dispatchers.Default)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 应用启动预热 - 在UI显示之前进行
        //prewarmApplication()
        
        // 强制禁用动画，提高首次加载性能
        (window.decorView as ViewGroup).layoutTransition = null
        
        // 检查并请求存储权限
        checkAndRequestStoragePermissions()
        
        setContent {
            // 先使用简单主题，避免首次加载卡顿
            val isFirstLoad = remember { mutableStateOf(true) }
            
            // 使用LaunchedEffect延迟加载完整主题，采用两阶段加载策略
            LaunchedEffect(Unit) {
                // 第一阶段：快速显示基本UI (50ms)
                delay(30) // 更短的延迟，让界面先渲染出来
                isFirstLoad.value = false
                
                // 第二阶段：在后台完成UI预热 (不阻塞用户交互)
                mainScope.launch(Dispatchers.Default) {
                    try {
                        delay(200) // 给UI线程一点时间稳定
                        prewarmScrollSystem() // 预热滚动系统
                        isUiReady = true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "UI预热失败，但不影响应用使用", e)
                    }
                }
            }
            
            // 始终使用完整主题，确保深色模式正确适配
            MyApplicationTheme {
                if (isFirstLoad.value) {
                    // 极简版界面，但使用完整主题确保深色模式正确
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background // 使用主题背景色，自动适配深色模式
                    ) {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                } else {
                    // 完整应用界面
                    AppNavigation()
                }
            }
        }
        
        // 应用程序启动完成
        Log.d("MainActivity", "应用程序已完全启动")
        
        // 进行表名诊断（仅开发时使用）
        mainScope.launch {
            try {
                val application = applicationContext as TenantApplication
                com.morgen.rentalmanager.utils.MeterNameDebugUtils.diagnoseNameIssues(application.repository)
            } catch (e: Exception) {
                Log.e("MainActivity", "表名诊断失败", e)
            }
        }
    }
    
    // 移除应用预热函数
    
    // 预热滚动系统和UI组件
    private fun prewarmScrollSystem() {
        synchronized(prewarmLock) {
            if (isScrollSystemPrewarmed) return
            
            mainScope.launch(Dispatchers.Default) {
                try {
                    // 使用反射方式预热关键组件
                    withContext(Dispatchers.Main) {
                        // 预热动画系统
                        val animBuilder = androidx.compose.animation.core.Animatable(0f)
                        animBuilder.animateTo(1f)
                        
                        // 预热触摸处理系统
                        val interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource()
                        interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Press(androidx.compose.ui.geometry.Offset.Zero))
                        interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Release(androidx.compose.foundation.interaction.PressInteraction.Press(androidx.compose.ui.geometry.Offset.Zero)))
                        
                        // 注意：不能在这里调用Composable函数，移除以下代码
                        // val tempScrollState = androidx.compose.foundation.lazy.rememberLazyListState()
                        // tempScrollState.scrollToItem(0)
                        
                        // 触发一次垃圾回收，减少首次滚动时的GC压力
                        System.gc()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "滚动系统预热失败，但不影响应用使用", e)
                }
            }
            
            isScrollSystemPrewarmed = true
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        mainScope.cancel()
    }
    
    // 使用ActivityResultContracts替代了onRequestPermissionsResult
    // 这个空方法只是为了避免删除代码，实际处理在BackupFileHelper中完成

    // 检查并请求存储权限
    private fun checkAndRequestStoragePermissions() {
        // 对于Android 10 (Q)及更高版本，需要请求MANAGE_EXTERNAL_STORAGE权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                // 先尝试使用requestLegacyExternalStorage属性方式
                if (!android.os.Environment.isExternalStorageManager()) {
                    Log.d("MainActivity", "请求存储权限 (Android 11+)")
                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = android.net.Uri.parse("package:$packageName")
                    intent.data = uri
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        // 如果上面的方法失败，使用ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                        val alternativeIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(alternativeIntent)
                    }
                } else {
                    Log.d("MainActivity", "已有存储权限 (Android 11+)")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "请求存储权限失败", e)
            }
        } 
        // 对于Android 6到Android 9，使用传统的权限请求方式
        else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val requiredPermissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            val permissionsToRequest = requiredPermissions.filter {
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
            
            if (permissionsToRequest.isNotEmpty()) {
                Log.d("MainActivity", "请求存储权限 (Android 6-9)")
                requestPermissions(permissionsToRequest, STORAGE_PERMISSION_REQUEST_CODE)
            } else {
                Log.d("MainActivity", "已有存储权限 (Android 6-9)")
            }
        }
    }
    
    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }
}

// EmptyTheme已移除，现在始终使用完整主题确保深色模式正确适配

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    onAddTenantClick: () -> Unit = {},
    onAddBillClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as TenantApplication
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(application.repository)
    )
    
    // 监听应用就绪状态
    val isAppReady by application.isReady.collectAsState(initial = false)
    
    // 租户数据收集
    val tenants by viewModel.allTenants.collectAsState()
    
    // 使用协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 全局数据同步监听
    var globalRefreshTrigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        // 监听隐私关键字变更，触发全局刷新
        com.morgen.rentalmanager.myapplication.DataSyncManager.privacyKeywordsChanged.collect {
            android.util.Log.d("MainScreen", "全局监听到隐私关键字变更: $it")
            globalRefreshTrigger++
        }
    }
    
    // 生命周期观察
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // 使用rememberSaveable保存滚动位置的关键信息
    val scrollIndexKey = "main_list_first_visible_item_index"
    val scrollOffsetKey = "main_list_first_visible_item_offset" 
    
    // 保存上次滚动位置
    val savedScrollIndex = rememberSaveable(key = scrollIndexKey) { mutableStateOf(0) }
    val savedScrollOffset = rememberSaveable(key = scrollOffsetKey) { mutableStateOf(0) }
    
    // 创建列表状态，使用保存的位置
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedScrollIndex.value,
        initialFirstVisibleItemScrollOffset = savedScrollOffset.value
    )
    
    // 当滚动位置改变时保存它
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        savedScrollIndex.value = listState.firstVisibleItemIndex
        savedScrollOffset.value = listState.firstVisibleItemScrollOffset
    }
    
    // 添加生命周期监听器，但只用于数据预加载
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 页面恢复时刷新数据
                    coroutineScope.launch {
                        // 移除预加载相关代码
                    }
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // 优化性能：使用密度计算缓存常用尺寸
    val density = LocalDensity.current
    val commonPadding = with(density) { ModernSpacing.Medium.toPx() }
    
    // 预计算常用值以减少运行时计算
    SideEffect {
        // 预热UI相关计算
    }
    
    // 调试信息 - 检查租户数据
    LaunchedEffect(tenants) {
        Log.d("MainActivity", "租户数据更新: ${tenants.size} 个租户")
    }
    
    // 列表滚动状态
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var fabExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var fabVisible by remember { mutableStateOf(true) } // 默认始终显示FAB
    var isLaunching by remember { mutableStateOf(false) }
    
    // FAB自动隐藏计时器
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    val autoHideTimeout = 5000L // 5秒后自动隐藏
    
    // 更新交互时间
    fun updateInteractionTime() {
        lastInteractionTime = System.currentTimeMillis()
        fabVisible = true
    }
    
    // 监听滚动和交互，更新时间
    LaunchedEffect(isScrolling) {
        if (isScrolling) {
            updateInteractionTime()
        }
    }
    
    // 自动隐藏FAB的定时检查
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 每秒检查一次
            val currentTime = System.currentTimeMillis()
            // 如果FAB展开或正在启动动画，不自动隐藏
            if (!fabExpanded && !isLaunching && currentTime - lastInteractionTime > autoHideTimeout) {
                fabVisible = false
            }
        }
    }
    
    // 点击屏幕时显示FAB
    val clickableModifier = Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null // 无视觉反馈
    ) {
        updateInteractionTime()
    }
    
    // 简化FAB可见性控制，减少状态变化
    LaunchedEffect(tenants.isEmpty()) {
        // 如果没有租户数据，始终保持按钮可见
        updateInteractionTime()
    }

    // 使用分层UI实现全局模糊效果
    LayeredUI(
        showBlur = fabExpanded || isLaunching,
        backgroundContent = {
            // 主界面内容 - 会被模糊
            Scaffold(
                topBar = {
                    FrostedBlurTopBar(
                        height = 56.dp,
                        title = "租房管家",
                        onMenuClick = { menuExpanded = true },
                        menuExpanded = menuExpanded,
                        onDismissMenu = { menuExpanded = false },
                        menuContent = {
                            val buttons = listOf(
                                "账单列表" to "bill_list",
                                "备份与恢复" to "data_transfer",
                                "设置" to "settings",
                                "关于" to "about"
                            )
                            buttons.forEach { button ->
                                val (text, route) = button
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = text,
                                            color = MaterialTheme.colorScheme.onSurface, // 使用主题文字色，自动适配深色模式
                                            fontWeight = FontWeight.Medium // 使文字更加醒目
                                        ) 
                                    },
                                    onClick = {
                                        navController.navigate(route = route)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background // 使用主题背景色，自动适配深色模式
            ) { paddingValues ->
                // 内容区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .then(clickableModifier) // 应用点击监听，显示FAB
                ) {
                    // 租户列表
                    if (tenants.isEmpty()) {
                        // 空状态设计
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(ModernSpacing.XXLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(ModernIconSize.XXLarge),
                                tint = ModernColors.OnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                            Text(
                                text = "暂无租户信息",
                                color = ModernColors.OnSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(ModernSpacing.Small))
                            Text(
                                text = "点击左下角按钮添加第一个租户",
                                color = ModernColors.OnSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        // 使用租户数据加载状态
                        val isFirstRender = remember { mutableStateOf(true) }
                        
                        // 应用滚动系统预热：短延迟后设置为非首次渲染状态
                        LaunchedEffect(Unit) {
                            // 给UI时间进行初始渲染
                            delay(800)
                            isFirstRender.value = false
                        }
                        
                        // 使用标准LazyColumn，移除可能导致闪烁的自定义优化
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = ModernSpacing.Medium),
                            state = listState,
                            contentPadding = PaddingValues(
                                top = ModernSpacing.Small,
                                bottom = ModernSpacing.XXXLarge
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 添加间距，保证卡片分离
                        ) {
                            items(
                                items = tenants,
                                key = { tenant -> tenant.roomNumber }
                            ) { tenant ->
                                // 直接使用TenantCard，不添加额外的包装器或修饰符
                                TenantCard(
                                    tenant = tenant,
                                    navController = navController,
                                    modifier = Modifier // 不添加padding，避免重复间距
                                )
                            }
                        }
                        
                        // 移除滚动性能监控
                    }
                }
            }
        },
        foregroundContent = {
            // 浮动按钮区域 - 不受模糊影响
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 点击外部区域收起菜单的透明覆盖层
                if (fabExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                fabExpanded = false
                            }
                    )
                }
                
                // 浮动按钮区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, bottom = 48.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    AnimatedVisibility(
                        visible = fabVisible,
                        enter = scaleIn(transformOrigin = TransformOrigin(0f, 1f)),
                        exit = scaleOut(transformOrigin = TransformOrigin(0f, 1f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // 添加账单按钮
                            AnimatedVisibility(
                                visible = fabExpanded,
                                enter = slideInVertically(
                                    animationSpec = tween(durationMillis = 300, delayMillis = 50, easing = ModernAnimations.PerfectSmoothOut),
                                    initialOffsetY = { it / 2 } // 从中间位置向上飞出
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 50)
                                ) + scaleIn(
                                    animationSpec = tween(durationMillis = 300, delayMillis = 50, easing = ModernAnimations.PerfectSmoothOut),
                                    initialScale = 0.7f
                                ),
                                exit = slideOutVertically(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 75),
                                    targetOffsetY = { it / 2 } // 向中间位置收回
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 150, delayMillis = 75)
                                ) + scaleOut(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 75),
                                    targetScale = 0.7f
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    AppLaunchButton(
                                        onClick = { fabExpanded = false },
                                        navController = navController,
                                        route = "add_bill_all", // 这个路由不会真正使用，但需要保留参数
                                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                        text = "添加账单",
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        onLaunchStateChange = { launching -> isLaunching = launching },
                                        onRouteNavigate = { 
                                            // 立即触发回调函数，显示叠加页面
                                            onAddBillClick()
                                            // 返回false，阻止默认导航行为
                                            false 
                                        }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = fabExpanded,
                                enter = slideInVertically(
                                    animationSpec = tween(durationMillis = 300, delayMillis = 100, easing = ModernAnimations.PerfectSmoothOut),
                                    initialOffsetY = { it / 3 } // 从中间位置向上飞出，比第一个稍低
                                ) + fadeIn(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 100)
                                ) + scaleIn(
                                    animationSpec = tween(durationMillis = 300, delayMillis = 100, easing = ModernAnimations.PerfectSmoothOut),
                                    initialScale = 0.7f
                                ),
                                exit = slideOutVertically(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 0),
                                    targetOffsetY = { it / 3 } // 向中间位置收回
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 150, delayMillis = 0)
                                ) + scaleOut(
                                    animationSpec = tween(durationMillis = 200, delayMillis = 0),
                                    targetScale = 0.7f
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    AppLaunchButton(
                                        onClick = { fabExpanded = false },
                                        navController = navController,
                                        route = "add_tenant", // 这个路由不会真正使用，但需要保留参数
                                        icon = Icons.Default.PersonAdd,
                                        text = "添加租户",
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = Color.White,
                                        onLaunchStateChange = { launching -> isLaunching = launching },
                                        onRouteNavigate = { 
                                            // 立即触发回调函数，显示叠加页面
                                            onAddTenantClick()
                                            // 返回false，阻止默认导航行为
                                            false 
                                        }
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            
                            // 圆角矩形的浮动按钮 - 添加旋转动画
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (fabExpanded) 45f else 0f, // 展开时旋转45度
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = ModernAnimations.PerfectSmoothOut
                                ),
                                label = "fab_rotation"
                            )
                            
                            FloatingActionButton(
                                onClick = { 
                                    fabExpanded = !fabExpanded
                                    updateInteractionTime() // 更新交互时间
                                },
                                containerColor = ModernColors.Primary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(ModernCorners.Large),
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = ModernElevation.Level2,
                                    pressedElevation = ModernElevation.Level3
                                )
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "更多操作",
                                    modifier = Modifier
                                        .size(ModernIconSize.Large)
                                        .graphicsLayer { rotationZ = rotationAngle } // 应用旋转动画
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TenantCard(tenant: Tenant, navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory((context.applicationContext as TenantApplication).repository)
    )
    
    // 监听数据变更事件，强制刷新
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 使用collectAsState监听隐私关键字变化，就像账单列表一样
    val app = context.applicationContext as TenantApplication
    val currentPrivacyKeywords by app.repository.privacyKeywordsFlow.collectAsState(initial = emptyList())
    
    LaunchedEffect(currentPrivacyKeywords) {
        // 当隐私关键字变化时，触发刷新
        android.util.Log.d("TenantCard", "隐私关键字变化: $currentPrivacyKeywords")
        refreshTrigger++
    }
    
    LaunchedEffect(Unit) {
        // 监听租户数据变更
        com.morgen.rentalmanager.myapplication.DataSyncManager.tenantDataChanged.collect { roomNumber ->
            if (roomNumber == tenant.roomNumber) {
                android.util.Log.d("TenantCard", "收到租户数据变更通知: $roomNumber")
                refreshTrigger++
            }
        }
    }

    // 定义固定的阴影高度 - 不再使用动画效果
    val cardElevation = ModernElevation.Level2 // 固定较高的阴影效果
    
    // 卡片组件 - 使用固定阴影高度和纯白色背景
    Card(
        // 使用传入的modifier，而不是硬编码padding
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = ModernElevation.Level2,
            pressedElevation = ModernElevation.Level3
        ),
        shape = RoundedCornerShape(ModernCorners.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer, // 使用更中性的表面容器色
            contentColor = MaterialTheme.colorScheme.onSurface // 使用主题文字色
        )
    ) {
        Column(
            modifier = Modifier
                .padding(ModernSpacing.Large)
                .fillMaxWidth()
        ) {
                // 顶部区域：房间号和租金
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 房间号
                    Text(
                        text = tenant.roomNumber,
                        style = MaterialTheme.typography.headlineSmall,
                        color = ModernColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 租金标签 - 简约设计
                    Text(
                        text = "¥${tenant.rent.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = ModernColors.OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                
                // 租户信息 - 极简设计
                Column {
                    Text(
                        text = tenant.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ModernColors.OnSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "租户",
                        style = MaterialTheme.typography.bodySmall,
                        color = ModernColors.OnSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Large))
                
                // 操作按钮 - 简约设计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
                ) {
                    // 编辑按钮 - 使用动画按钮
                    OutlinedButton(
                        onClick = { navController.navigate("edit_tenant/${tenant.roomNumber}") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(ModernCorners.Small),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ModernColors.OnSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, ModernColors.OutlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(ModernIconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(ModernSpacing.XSmall))
                        Text("编辑", fontWeight = FontWeight.Medium)
                    }
                    
                    // 收据按钮 - 活力蓝主色，使用自定义按钮
                    AnimatedButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    // 每次点击收据按钮都记录时间戳
                                    val clickTime = System.currentTimeMillis()
                                    android.util.Log.d("MainActivity", "收据按钮点击，时间戳: $clickTime, refreshTrigger: $refreshTrigger")
                                    
                                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                                    val billWithDetails = viewModel.getBillForTenantByMonth(tenant.roomNumber, currentMonth)
                                    
                                    if (billWithDetails != null) {
                                        try {
                                            // 重新从数据库获取最新的租户数据，确保收据显示最新信息
                                            val app = context.applicationContext as TenantApplication
                                            
                                            // 强制等待一小段时间，确保数据库事务完成
                                            // 强制刷新数据，确保获取最新状态
                                            // 不再需要强制刷新，Flow会自动提供最新数据
                                            
                                            // 在生成收据之前预加载表名配置
                                            android.util.Log.d("MainActivity", "开始预加载自定义表名...")

                                            // 额外等待一小段时间，确保数据库操作完成
                                            delay(50)

                                            // 每次点击都重新获取最新的租户信息和隐私关键字
                                            android.util.Log.d("MainActivity", "重新获取所有数据，点击时间: ${System.currentTimeMillis()}")
                                            val privacyKeywords = app.repository.getPrivacyKeywords()
                                            
                                            // 获取当前租户的最新信息
                                            val dbTenant = app.repository.getTenantForReceipt(tenant.roomNumber) ?: tenant
                                            android.util.Log.d("MainActivity", "最新租户信息: 房号=${dbTenant.roomNumber}, 姓名=${dbTenant.name}, 租金=${dbTenant.rent}")
                                            android.util.Log.d("MainActivity", "最新隐私关键字: $privacyKeywords")
                                                
                                            android.util.Log.d("MainActivity", "=== 收据生成开始 ===")
                                            android.util.Log.d("MainActivity", "原始租户信息: 房号=${dbTenant.roomNumber}, 租金=${dbTenant.rent}")
                                            android.util.Log.d("MainActivity", "隐私关键字: $privacyKeywords")
                                
                                            // 再次验证数据库中的隐私关键字设置
                                            val verifyKeywords = app.repository.getPrivacyKeywords()
                                            android.util.Log.d("MainActivity", "验证隐私关键字: $verifyKeywords")
                                                
                                            // 预览隐私保护效果
                                            val previewProtected = com.morgen.rentalmanager.utils.PrivacyProtectionUtils.applyPrivacyProtection(
                                                dbTenant.roomNumber, 
                                                privacyKeywords
                                            )
                                            android.util.Log.d("MainActivity", "隐私保护预览: ${dbTenant.roomNumber} -> $previewProtected")
                                            android.util.Log.d("MainActivity", "=== 开始生成收据图片 ===")
                                            
                                            // 创建一个带有隐私保护房号的租户对象，确保收据显示正确
                                            val actualTenant = dbTenant
                                            val protectedRoomNumber = com.morgen.rentalmanager.utils.PrivacyProtectionUtils.applyPrivacyProtection(
                                                actualTenant.roomNumber, 
                                                privacyKeywords
                                            )
                                            val receiptTenant = actualTenant.copy(roomNumber = protectedRoomNumber)
                                            
                                            android.util.Log.d("MainActivity", "收据租户对象: 房号=${receiptTenant.roomNumber}, 租金=${receiptTenant.rent}")
                                            
                                            // 预先加载自定义表名称映射
                                            android.util.Log.d("MainActivity", "=== 开始预加载自定义表名称 ===")
                                            val customMeterNames = mutableMapOf<String, String>()
                                            
                                            // 使用更宽松的表名识别规则
                                            val extraMetersList = billWithDetails.details.filter { detail ->
                                                val containsWaterMeter = detail.name.contains("水表") 
                                                val containsElectricityMeter = detail.name.contains("电表")
                                                val isMainMeter = detail.name == "主水表" || detail.name == "主电表"
                                                val isExtraMeter = (containsWaterMeter || containsElectricityMeter) && !isMainMeter
                                                
                                                (detail.type == "water" || detail.type == "electricity") && isExtraMeter
                                            }
                                            
                                            android.util.Log.d("MainActivity", "找到 ${extraMetersList.size} 个额外表需要加载自定义名称:")
                                            extraMetersList.forEach { meter ->
                                                android.util.Log.d("MainActivity", "  - ${meter.name}")
                                            }
                                            
                                            // 同步加载所有自定义表名
                                            extraMetersList.forEach { detail ->
                                                android.util.Log.d("MainActivity", ">>> 查询表名: '${detail.name}' (租户: '${actualTenant.roomNumber}')")
                                                
                                                val customName = app.repository.getMeterDisplayName(detail.name, actualTenant.roomNumber)
                                                
                                                android.util.Log.d("MainActivity", ">>> 查询结果: '${detail.name}' -> '$customName'")
                                                
                                                // 添加到映射表中
                                                customMeterNames[detail.name] = customName
                                                
                                                if (customName != detail.name) {
                                                    android.util.Log.d("MainActivity", "✓ 发现自定义名称: '${detail.name}' -> '$customName'")
                                                } else {
                                                    android.util.Log.d("MainActivity", "○ 使用默认名称: '${detail.name}'")
                                                }
                                            }
                                            
                                            android.util.Log.d("MainActivity", "=== 自定义表名预加载完成 ===")
                                            android.util.Log.d("MainActivity", "预加载映射表: $customMeterNames")
                                            
                                            // 生成收据
                                            val bitmap = captureComposableToBitmap(context as androidx.activity.ComponentActivity) {
                                                // 在lambda中再次打印，确保传递的数据正确
                                                android.util.Log.d("MainActivity", "Lambda中的租户: 房号=${receiptTenant.roomNumber}, 租金=${receiptTenant.rent}")
                                                android.util.Log.d("MainActivity", "Lambda中的自定义表名: $customMeterNames")
                                                
                                                // 使用预加载的自定义名称
                                                ReceiptUi(
                                                    tenant = receiptTenant, 
                                                    billWithDetails = billWithDetails, 
                                                    repository = null, // 不传入 repository，避免重复加载
                                                    privacyKeywords = emptyList(),
                                                    preloadedCustomNames = customMeterNames
                                                )
                                            }
                                            
                                            // 使用时间戳确保每次生成的文件名都不同，避免缓存问题
                                            val timestamp = System.currentTimeMillis()
                                            val fileName = "receipt_${tenant.roomNumber}_${billWithDetails.bill.month}_${timestamp}.png"
                                            
                                            // 清理旧的收据文件，避免存储空间浪费
                                            try {
                                                val cacheDir = context.cacheDir
                                                val oldFiles = cacheDir.listFiles { _, name -> 
                                                    name.startsWith("receipt_${tenant.roomNumber}_${billWithDetails.bill.month}_") && 
                                                    name.endsWith(".png") && 
                                                    name != fileName
                                                }
                                                oldFiles?.forEach { oldFile ->
                                                    if (oldFile.delete()) {
                                                        android.util.Log.d("MainActivity", "删除旧收据文件: ${oldFile.name}")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.w("MainActivity", "清理旧收据文件失败", e)
                                            }
                                            
                                            val file = saveBitmapToFile(context, bitmap, fileName)
                                            
                                            if (file != null) {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                                val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                                                navController.navigate("receipt/$encodedUri")
                                            } else {
                                                Toast.makeText(context, "创建收据图片文件失败", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "生成收据图片失败", e)
                                            Toast.makeText(context, "生成收据图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "未找到 ${currentMonth} 的账单，请先创建账单", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "生成收据过程中发生错误", e)
                                    Toast.makeText(context, "生成收据失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(ModernCorners.Small),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModernColors.Primary,
                            contentColor = ModernColors.OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = ModernElevation.Level1
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(ModernIconSize.Small)
                        )
                        Spacer(modifier = Modifier.width(ModernSpacing.XSmall))
                        Text("收据", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

@Composable
fun TenantList(
    tenants: List<Tenant>,
    navController: NavController,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    if (tenants.isEmpty()) {
        Text("当前没有租户信息，请添加租户。")
    } else {
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tenants) { tenant ->
                TenantCard(tenant = tenant, navController = navController)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        val navController = rememberNavController()
        // Preview with sample data as we can't get ViewModel here easily
        Column {
            TenantList(
                tenants = listOf(
                    Tenant("101", "张三", 1500.0),
                    Tenant("102", "李四", 1600.0)
                ),
                navController = navController,
                listState = rememberLazyListState()
            )
        }
    }
}