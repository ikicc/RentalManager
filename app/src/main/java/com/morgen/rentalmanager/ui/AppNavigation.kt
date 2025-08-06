package com.morgen.rentalmanager.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.navArgument
import com.morgen.rentalmanager.MainScreen
import com.morgen.rentalmanager.TenantApplication
import com.morgen.rentalmanager.ui.addbillall.AddBillAllScreen
import com.morgen.rentalmanager.ui.addtenant.AddTenantScreen
import com.morgen.rentalmanager.ui.billlist.BillListScreen
import com.morgen.rentalmanager.ui.billdetail.BillDetailScreen
import com.morgen.rentalmanager.ui.edittenant.EditTenantScreen
import com.morgen.rentalmanager.ui.datatransfer.DataTransferScreen
import com.morgen.rentalmanager.ui.datatransfer.DataTransferViewModelFactory
import com.morgen.rentalmanager.ui.about.AboutScreen
import com.morgen.rentalmanager.ui.receipt.ReceiptScreen
import com.morgen.rentalmanager.ui.setting.SettingScreen
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import java.net.URLDecoder
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    // 控制叠加式页面显示状态
    var showAddTenant by remember { mutableStateOf(false) }
    var showAddBillAll by remember { mutableStateOf(false) }
    
    // 控制模糊效果 - 完全独立的状态
    var blurRadius by remember { mutableStateOf(0f) }
    
        // 主界面滑动状态 - 恢复主界面偏移控制
    var mainContentOffset by remember { mutableStateOf(Offset.Zero) }
    
    // 为主界面添加响应动画，当被覆盖时的移动效果
    val mainContentScope = rememberCoroutineScope()

    // 主界面偏移动画
    val animatedOffsetX by animateFloatAsState(
        targetValue = mainContentOffset.x.toFloat(),
        animationSpec = tween(
            durationMillis = 500, // 增加动画时长，让覆盖过程更明显
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // 使用特殊缓动曲线
        ),
        label = "offset_animation_x"
    )
    
    val animatedOffsetY by animateFloatAsState(
        targetValue = mainContentOffset.y.toFloat(),
        animationSpec = tween(
            durationMillis = 500, // 增加动画时长，让覆盖过程更明显
            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // 使用特殊缓动曲线
        ),
        label = "offset_animation_y"
    )

    // 使用与页面滑入相同的tween动画，确保完美同步
    val animatedBlurRadius by animateFloatAsState(
        targetValue = blurRadius,
        animationSpec = tween(
            durationMillis = 300, // 与最终的滑入动画完全一致
            easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f) // 使用相同的Material Design缓动
        ),
        label = "blur_animation"
    )
    
    // 重置模糊的函数
    val resetBlur = {
        blurRadius = 0f
    }
    
    // 应用模糊的函数 - 减少模糊强度以提升性能
    val applyBlur = {
        blurRadius = 6f // 减少模糊强度，提升动画流畅度
    }
    
    // 处理系统返回键事件
    BackHandler(enabled = showAddTenant || showAddBillAll) {
        // 如果任一叠加页面显示，关闭它而不是退出应用
        if (showAddTenant) {
            showAddTenant = false
            resetBlur() // 立即重置模糊效果
        } else if (showAddBillAll) {
            showAddBillAll = false
            resetBlur() // 立即重置模糊效果
        }
    }
    
    // 在组件销毁时确保模糊被重置
    DisposableEffect(Unit) {
        onDispose {
            // 组件销毁时重置模糊
            resetBlur()
        }
    }
    
    // 监听叠加页面状态，控制模糊效果与主界面移动效果 - 完全同步
    LaunchedEffect(showAddTenant, showAddBillAll) {
        if (showAddTenant) {
            // 立即开始模糊，与动画完全同步
            applyBlur()
            // 当AddTenant显示时，向上移动主界面 - 恢复下推上的效果
            mainContentOffset = Offset(0f, -600f)
        } else if (showAddBillAll) {
            // 立即开始模糊，与动画完全同步
            applyBlur()
            // 当AddBill显示时，向上移动主界面
            mainContentOffset = Offset(0f, -600f)
        } else {
            // 立即重置模糊和位移，让退出动画更流畅
            resetBlur()
            mainContentOffset = Offset.Zero
        }
    }
    
    // 监听导航变化，为普通页面导航添加主界面位移效果
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            // 排除叠加页面的影响（它们有自己的动画处理）
            if (!showAddTenant && !showAddBillAll) {
                // 为不同页面设置不同的偏移效果
                when (destination.route) {
                    "main" -> {
                        // 主页面重置偏移
                        mainContentOffset = Offset.Zero
                    }
                    else -> {
                        // 导航动画中已经有页面偏移，这里不需要额外偏移
                        // 将偏移量设置为零，避免干扰导航动画
                        mainContentOffset = Offset.Zero
                    }
                }
            }
        }
        
        navController.addOnDestinationChangedListener(listener)
        
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    
    // 主导航和叠加页面容器
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 主导航内容 - 性能优化
        Box(modifier = Modifier
            .fillMaxSize()
            .blur(radius = animatedBlurRadius.dp)
            .graphicsLayer {
                // 应用动画偏移
                translationX = animatedOffsetX
                translationY = animatedOffsetY
                // 优化合成性能
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
        ) {
            NavHost(
                navController = navController, 
                startDestination = "main",
                // 设置默认的导航动画 - 使用右侧覆盖式动画
                enterTransition = { 
                    // 新页面从右侧进入，同时推动当前页面向左移动
                    slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    ) { fullWidth -> fullWidth } // 从右侧进入
                },
                exitTransition = { 
                    // 当前页面向左退出
                    slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 450,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    ) { fullWidth -> -fullWidth / 3 } // 向左轻微移出
                },
                popEnterTransition = { 
                    // 返回时，上一页从左侧进入
                    slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    ) { fullWidth -> -fullWidth / 3 } // 从左侧进入
                },
                popExitTransition = { 
                    // 当前页面向右退出
                    slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 450,
                            easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                        )
                    ) { fullWidth -> fullWidth } // 向右退出
                }
            ) {
                // 主界面 - 保持显示
                composable("main") {
                    MainScreen(
                        navController = navController,
                        onAddTenantClick = { 
                            showAddTenant = true // 只设置状态，让LaunchedEffect处理模糊
                        },
                        onAddBillClick = { 
                            showAddBillAll = true // 只设置状态，让LaunchedEffect处理模糊
                        }
                    )
                }
                
                // 编辑租户
                composable(
                    route = "edit_tenant/{roomNumber}",
                    arguments = listOf(navArgument("roomNumber") { type = NavType.StringType })
                ) { _ ->
                    EditTenantScreen(navController = navController)
                }
                
                // 账单列表 - 使用与其他页面一致的横向覆盖动画
                composable(
                    route = "bill_list",
                    enterTransition = { 
                        // 从右侧滑入，与其他页面保持一致
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                            )
                        ) { fullWidth -> fullWidth }
                    },
                    exitTransition = { 
                        // 向左轻微移出
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 450,
                                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                            )
                        ) { fullWidth -> -fullWidth / 3 }
                    },
                    popEnterTransition = { 
                        // 返回时从左侧进入
                        slideInHorizontally(
                            animationSpec = tween(
                                durationMillis = 500,
                                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                            )
                        ) { fullWidth -> -fullWidth / 3 }
                    },
                    popExitTransition = { 
                        // 返回时向右退出
                        slideOutHorizontally(
                            animationSpec = tween(
                                durationMillis = 450,
                                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
                            )
                        ) { fullWidth -> fullWidth }
                    }
                ) { 
                    val context = LocalContext.current
                    val application = context.applicationContext as TenantApplication
                    BillListScreen(
                        navController = navController,
                        repository = application.repository
                    ) 
                }
                
                // 账单详情
                composable(
                    route = "bill_detail/{roomNumber}/{month}",
                    arguments = listOf(
                        navArgument("roomNumber") { type = NavType.StringType },
                        navArgument("month") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val roomNumber = backStackEntry.arguments?.getString("roomNumber")?.let { 
                        try {
                            java.net.URLDecoder.decode(it, "UTF-8") 
                        } catch (e: Exception) {
                            android.util.Log.w("AppNavigation", "房间号解码失败", e)
                            it
                        }
                    } ?: ""
                    val month = backStackEntry.arguments?.getString("month")?.let { 
                        try {
                            java.net.URLDecoder.decode(it, "UTF-8") 
                        } catch (e: Exception) {
                            android.util.Log.w("AppNavigation", "月份解码失败", e)
                            it
                        }
                    } ?: ""
                    
                    // 验证参数有效性
                    if (roomNumber.isNotBlank() && month.isNotBlank()) {
                        val context = LocalContext.current
                        val application = context.applicationContext as TenantApplication
                        BillDetailScreen(
                            navController = navController,
                            roomNumber = roomNumber,
                            month = month,
                            repository = application.repository
                        )
                    } else {
                        // 参数无效时显示错误页面或返回
                        LaunchedEffect(Unit) {
                            android.util.Log.w("AppNavigation", "账单详情参数无效: roomNumber=$roomNumber, month=$month")
                            navController.popBackStack()
                        }
                    }
                }
                
                // 备份与恢复界面
                composable("data_transfer") { 
                    val context = LocalContext.current
                    val application = context.applicationContext as TenantApplication
                    DataTransferScreen(
                        navController = navController,
                        viewModel = viewModel(
                            factory = DataTransferViewModelFactory(application.repository)
                        )
                    )
                }
                
                // 收据界面
                composable(
                    route = "receipt/{imageUri}",
                    arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                    ReceiptScreen(navController = navController, imageUri = URLDecoder.decode(imageUri, "UTF-8"))
                }
                
                // 设置界面
                composable("settings") { 
                    SettingScreen(navController) 
                }
                
                // 关于应用界面
                composable("about") {
                    AboutScreen(navController)
                }
            }
        }
        
        // 自定义覆盖动画，保留原覆盖效果，确保有完整的上推效果
        // 租户页面使用从底部覆盖动画 - 重新定义以确保效果正确
        val tenantEnterTransition = slideInVertically(
            animationSpec = tween(
                durationMillis = 500,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        ) { fullHeight -> fullHeight }
        
        val tenantExitTransition = slideOutVertically(
            animationSpec = tween(
                durationMillis = 450,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        ) { fullHeight -> fullHeight }
        
        // 账单页面使用从底部覆盖动画
        val billEnterTransition = slideInVertically(
            animationSpec = tween(
                durationMillis = 500,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        ) { fullHeight -> fullHeight }
        
        val billExitTransition = slideOutVertically(
            animationSpec = tween(
                durationMillis = 450,
                easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
            )
        ) { fullHeight -> fullHeight }
        
        // 自定义覆盖动画组合
        
        // 添加租户页面 - 使用覆盖式动画
        AnimatedVisibility(
            visible = showAddTenant,
            enter = tenantEnterTransition,
            exit = tenantExitTransition,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f) // 确保覆盖页面始终在顶层
                .graphicsLayer {
                    // 启用硬件加速，提升动画性能
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            AddTenantScreen(
                navController = navController,
                onDismiss = {
                    showAddTenant = false
                }
            )
        }
        
        // 添加账单页面 - 使用覆盖式动画
        AnimatedVisibility(
            visible = showAddBillAll,
            enter = billEnterTransition,
            exit = billExitTransition,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f) // 确保覆盖页面始终在顶层
                .graphicsLayer {
                    // 启用硬件加速，提升动画性能
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            AddBillAllScreen(
                navController = navController,
                onDismiss = {
                    showAddBillAll = false
                }
            )
        }
    }
}

