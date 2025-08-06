package com.morgen.rentalmanager.ui.billlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.morgen.rentalmanager.myapplication.TenantRepository
import com.morgen.rentalmanager.ui.theme.*

/**
 * 账单列表主界面组件
 * 协调各个子组件显示，集成ViewModel和状态管理，添加加载状态和错误处理UI
 * 
 * 需求: 7.1, 7.2, 7.3, 7.4 - 数据加载和错误处理
 * 
 * @param navController 导航控制器
 * @param repository 租户数据仓库
 * @param modifier 修饰符
 */
@Composable
fun BillListScreen(
    navController: NavController,
    repository: TenantRepository? = null,
    modifier: Modifier = Modifier
) {
    // 获取ViewModel实例
    val viewModel: BillListViewModel = if (repository != null) {
        viewModel(factory = BillListViewModelFactory(repository))
    } else {
        // 如果没有提供repository，使用默认的ViewModel（用于预览和测试）
        viewModel()
    }
    
    // 收集状态
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    // 主界面内容
    BillListScreenContent(
        uiState = uiState,
        selectedMonth = selectedMonth,
        isRefreshing = isRefreshing,
        onMonthSelected = viewModel::selectMonth,
        onRefresh = viewModel::refreshData,
        onRetry = viewModel::retryLoadData,
        onBillClick = { roomNumber, month ->
            // 导航到账单详情页面，添加完善的错误处理
            try {
                // 验证参数
                if (roomNumber.isBlank() || month.isBlank()) {
                    android.util.Log.w("BillListScreen", "导航参数无效: roomNumber=$roomNumber, month=$month")
                    return@BillListScreenContent
                }
                
                // URL编码处理特殊字符
                val encodedRoomNumber = java.net.URLEncoder.encode(roomNumber, "UTF-8")
                val encodedMonth = java.net.URLEncoder.encode(month, "UTF-8")
                
                navController.navigate("bill_detail/$encodedRoomNumber/$encodedMonth")
            } catch (e: Exception) {
                android.util.Log.e("BillListScreen", "导航失败", e)
                // 这里可以添加Toast提示用户导航失败
                // 目前记录日志，避免崩溃
            }
        },
        navController = navController,
        modifier = modifier
    )
}

/**
 * 账单列表界面内容组件
 * 根据UI状态显示不同的界面内容
 */
@Composable
private fun BillListScreenContent(
    uiState: BillListUiState,
    selectedMonth: String,
    isRefreshing: Boolean,
    onMonthSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onBillClick: (String, String) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ModernColors.Background) // 添加纯色背景，防止透出主页内容
    ) {
        // 顶部栏
        BillListTopBar(
            selectedMonth = selectedMonth,
            isRefreshing = isRefreshing,
            onMonthSelected = onMonthSelected,
            onRefresh = onRefresh,
            navController = navController
        )
        
        // 主要内容区域
        when (uiState) {
            is BillListUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is BillListUiState.Success -> {
                SuccessContent(
                    uiState = uiState,
                    onBillClick = onBillClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is BillListUiState.Error -> {
                ErrorContent(
                    error = uiState,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            is BillListUiState.Empty -> {
                EmptyContent(
                    emptyState = uiState,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 顶部栏组件
 * 包含标题、月份选择器和刷新按钮
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillListTopBar(
    selectedMonth: String,
    isRefreshing: Boolean,
    onMonthSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ModernColors.Surface,
        shadowElevation = ModernElevation.Level1
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部栏
            TopAppBar(
                title = {
                Text(
                        "账单列表",
                        fontWeight = FontWeight.Medium,
                    color = ModernColors.OnSurface
                )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = ModernColors.OnSurface
                        )
                    }
                },
                actions = {
                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ModernIconSize.Medium),
                            strokeWidth = 2.dp,
                            color = ModernColors.Primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新数据",
                            tint = ModernColors.OnSurfaceVariant
                        )
                    }
                }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ModernColors.Surface
                )
            )
            
            // 月份选择器 - 与顶部栏融为一体
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ModernSpacing.Medium)
                    .padding(bottom = ModernSpacing.Medium)
            ) {
            MonthSelector(
                selectedMonth = selectedMonth,
                onMonthSelected = onMonthSelected
            )
            }
        }
    }
}

/**
 * 加载状态内容
 * 需求: 7.1 - 数据加载时显示加载指示器
 */
@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = ModernColors.Primary,
                strokeWidth = 4.dp
            )
            
            Text(
                text = "正在加载账单数据...",
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 成功状态内容
 * 显示账单汇总、图表和列表 - 修复嵌套滚动问题
 */
@Composable
private fun SuccessContent(
    uiState: BillListUiState.Success,
    onBillClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用LazyColumn统一管理所有内容，避免嵌套滚动
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(ModernSpacing.Medium),
        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
    ) {
        // 账单汇总卡片
        item {
            BillSummaryCard(
                summary = uiState.summary
            )
        }
        
        // 费用分布图表卡片
        if (uiState.hasChartData()) {
            item {
                BillChartCard(
                    chartData = uiState.chartData
                )
            }
        }
        
        // 账单列表标题
        if (uiState.hasBills()) {
            item {
                Text(
                    text = "账单详情",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.OnSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = ModernSpacing.Small)
                )
            }
            
            // 账单列表项
            items(
                items = uiState.bills.filter { bill ->
                    try {
                        bill.isValid() && bill.roomNumber.isNotBlank()
                    } catch (e: Exception) {
                        android.util.Log.w("SuccessContent", "账单数据无效", e)
                        false
                    }
                },
                key = { bill -> 
                    try {
                        "${bill.roomNumber}_${bill.tenantName}_${uiState.selectedMonth}"
                    } catch (e: Exception) {
                        bill.hashCode()
                    }
                }
            ) { bill ->
                BillItemCard(
                    billItem = bill,
                    onClick = { 
                        try {
                            onBillClick(bill.roomNumber, uiState.selectedMonth)
                        } catch (e: Exception) {
                            android.util.Log.e("SuccessContent", "点击处理失败", e)
                        }
                    }
                )
            }
        } else {
            // 如果有汇总数据但没有账单列表，显示提示
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = ModernColors.SurfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ModernSpacing.Large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "暂无详细账单数据",
                            style = MaterialTheme.typography.titleMedium,
                            color = ModernColors.OnSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(ModernSpacing.Small))
                        
                        Text(
                            text = "仅显示汇总统计信息",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ModernColors.OnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 错误状态内容
 * 需求: 7.2 - 数据加载失败时显示错误提示信息和重试选项
 */
@Composable
private fun ErrorContent(
    error: BillListUiState.Error,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Large),
            colors = CardDefaults.cardColors(
                containerColor = ModernColors.ErrorContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ModernSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
            ) {
                Text(
                    text = "数据加载失败",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.OnErrorContainer,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = error.getUserFriendlyMessage(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ModernColors.OnErrorContainer,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ModernColors.Error,
                        contentColor = ModernColors.OnError
                    )
                ) {
                    Text("重试")
                }
                
                // 根据错误类型显示额外提示
                when {
                    error.isNetworkError() -> {
                        Text(
                            text = "请检查网络连接后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernColors.OnErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    error.isDatabaseError() -> {
                        Text(
                            text = "数据库访问异常，请稍后重试",
                            style = MaterialTheme.typography.bodySmall,
                            color = ModernColors.OnErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 空状态内容
 * 需求: 7.4 - 数据为空时显示友好的空状态界面
 */
@Composable
private fun EmptyContent(
    emptyState: BillListUiState.Empty,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Large),
            colors = CardDefaults.cardColors(
                containerColor = ModernColors.SurfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ModernSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
            ) {
                Text(
                    text = emptyState.getEmptyMessage(),
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernColors.OnSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "当前月份还没有生成账单\n请先添加租户信息和费用数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ModernColors.OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 
/**

 * 错误账单项占位符
 * 当单个账单项渲染失败时显示
 */
@Composable
private fun ErrorBillItemPlaceholder(
    roomNumber: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.SurfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ModernSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "房间 $roomNumber 数据异常",
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurfaceVariant
            )
            Text(
                text = "请刷新重试",
                style = MaterialTheme.typography.bodySmall,
                color = ModernColors.OnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}