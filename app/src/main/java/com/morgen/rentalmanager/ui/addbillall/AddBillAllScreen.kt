package com.morgen.rentalmanager.ui.addbillall

import android.widget.Toast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.morgen.rentalmanager.TenantApplication
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernElevation
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernBlur
import com.morgen.rentalmanager.ui.theme.ModernAnimations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBillAllScreen(
    navController: NavController,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as TenantApplication
    val viewModel: AddBillAllViewModel = viewModel(
        factory = AddBillAllViewModelFactory(application.repository)
    )

    // 直接加载数据，不显示加载动画
    LaunchedEffect(Unit) {
        viewModel.loadData()
        // 设置Context用于自动备份
        viewModel.setContext(context)
    }

    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var currentMeterType by remember { mutableStateOf("water") }

    // Parse selectedMonth (yyyy-MM) to year & month
    val parts = selectedMonth.split("-")
    val initYear = parts[0].toInt()
    val initMonth = parts[1].toInt()

    // 1. 为模糊效果创建动画状态
    val blurRadius = remember { ModernBlur.Medium.value } // 使用Medium级别的模糊
    val animatedBlurRadius by animateFloatAsState(
        targetValue = if (showMonthPicker) blurRadius else 0f,
        animationSpec = tween(
            durationMillis = ModernAnimations.STANDARD_DURATION,
            easing = ModernAnimations.SmoothOutEasing
        ),
        label = "blurRadius"
    )

    // 主内容和模糊效果
    Box(modifier = Modifier.fillMaxSize()) {
        // 使用与主页完全相同的模糊实现
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(
                    radius = animatedBlurRadius.dp
                )
        ) {
            // 主界面内容 - 与主页实现一致
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "批量添加账单",
                                fontWeight = FontWeight.Medium,
                                color = ModernColors.OnSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { 
                                // 简化返回逻辑
                                onDismiss()
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                }
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                    tint = ModernColors.OnSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ModernColors.Surface
                        ),
                        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                    )
                },
                bottomBar = {
                    Surface(
                        shadowElevation = ModernElevation.Level2,
                        color = ModernColors.Surface
                    ) {
                        Button(
                            onClick = {
                                // 简化保存逻辑，移除模糊效果
                                viewModel.saveBills()
                                Toast.makeText(context, "账单已保存", Toast.LENGTH_SHORT).show()
                                
                                // 直接执行回调和导航
                                onDismiss()
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ModernSpacing.Medium)
                                .height(56.dp),
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ModernColors.Primary,
                                contentColor = ModernColors.OnPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = ModernElevation.Level1
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(ModernIconSize.Medium)
                            )
                            Spacer(modifier = Modifier.width(ModernSpacing.Small))
                            Text(
                                "全部保存",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ModernColors.Background)
                        .padding(paddingValues)
                ) {
                    // 固定的头部控件
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ModernColors.Surface,
                        shadowElevation = ModernElevation.Level1
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(ModernSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
                        ) {
                            // 月份选择按钮
                            OutlinedButton(
                                onClick = { showMonthPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ModernCorners.Medium),
                                border = BorderStroke(1.dp, ModernColors.Primary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ModernColors.Primary
                                )
                            ) {
                                Text(
                                    text = "账单月份: $selectedMonth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // 分段按钮
                            com.morgen.rentalmanager.ui.components.SegmentedButtonRow(
                                options = mapOf("water" to "水费", "electricity" to "电费", "extra" to "附加费"),
                                selectedOption = currentMeterType,
                                onOptionSelected = { newType -> 
                                    currentMeterType = newType
                                }
                            )
                        }
                    }
                    
                    // 可滚动的租户卡片列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = ModernSpacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = ModernSpacing.Medium)
                    ) {
                        // 租户卡片列表
                        if (uiState.isEmpty()) {
                            item {
                                Text("没有找到租户信息或正在加载...")
                            }
                        } else {
                            items(
                                items = uiState,
                                key = { state -> state.roomNumber }
                            ) { state ->
                                com.morgen.rentalmanager.ui.components.TenantBillInputCard(
                                    state = state,
                                    meterType = currentMeterType,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
        // 月份选择器 - 在前景层
        if (showMonthPicker) {
            // 添加点击拦截层，防止点击传递到背景
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 拦截所有点击事件
                    )
            )
            
            com.morgen.rentalmanager.ui.components.ModernMonthYearPicker(
                show = showMonthPicker,
                initialYear = initYear,
                initialMonth = initMonth,
                onDismiss = { showMonthPicker = false },
                onConfirm = { year, month ->
                    viewModel.onMonthSelected(year, month)
                    showMonthPicker = false
                }
            )
        }
    }
}