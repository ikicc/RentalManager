package com.morgen.rentalmanager.ui.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
// 移除不再使用的CloudUpload图标导入
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.morgen.rentalmanager.ui.theme.ModernTouchTarget
// 移除导入功能相关的导入语句

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingScreen(navController: NavController){
    val app = LocalContext.current.applicationContext as TenantApplication
    val vm: SettingViewModel = viewModel(factory = SettingVMFactory(app.repository))
    val water by vm.water.collectAsState()
    val elec by vm.electricity.collectAsState()
    val privacyKeywords by vm.privacyKeywords.collectAsState()

    var waterTxt by remember(water){ mutableStateOf(water.toString()) }
    var elecTxt by remember(elec){ mutableStateOf(elec.toString()) }
    var newKeyword by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    // 获取根据当前主题的界面背景色
    val backgroundColor = if (isSystemInDarkTheme()) Color(0xFF121212) else Color.White
    
    // 获取根据当前主题的卡片背景色
    val cardBackgroundColor = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color.White
    
    Scaffold(
        containerColor = backgroundColor, // 根据主题使用适当的背景色
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置",
                        fontWeight = FontWeight.Medium,
                        color = ModernColors.OnSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = ModernColors.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor) // 根据主题使用适当的背景色
        ) {
            // 创建滚动状态以检测滚动位置
            val scrollState = rememberLazyListState()
            // 检测是否正在滚动或已滚动
            val isScrolled = scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0
            
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = ModernSpacing.Medium),
                contentPadding = PaddingValues(
                    top = ModernSpacing.Medium, // 保持统一的顶部间距
                    bottom = ModernSpacing.Medium
                ),
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.Large) // 组件间距
            ) {
                // 价格配置区域
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.98f)
                            .widthIn(max = 500.dp)
                            .padding(ModernSpacing.Small),
                        shape = RoundedCornerShape(ModernCorners.Large),
                        color = cardBackgroundColor,
                        shadowElevation = ModernElevation.Level2
                    ) {
                        Column(modifier = Modifier.padding(ModernSpacing.Medium)) {
                            // 标题区域
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = ModernSpacing.Medium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = ModernColors.Primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(ModernCorners.Medium)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = ModernColors.Primary,
                                        modifier = Modifier.size(ModernIconSize.Large)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                                
                                Column {
                                    Text(
                                        text = "价格配置",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ModernColors.OnSurface
                                    )
                                    Text(
                                        text = "设置水电费单价",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ModernColors.OnSurfaceVariant
                                    )
                                }
                            }
    
                            HorizontalDivider(
                                color = ModernColors.OutlineVariant,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = ModernSpacing.Small)
                            )
                            
                            // 水价设置
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = ModernSpacing.Medium)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = ModernColors.Success.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(ModernCorners.Small)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Water,
                                        contentDescription = null,
                                        tint = ModernColors.Success,
                                        modifier = Modifier.size(ModernIconSize.Medium)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                                
                                Text(
                                    text = "水费单价",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ModernColors.OnSurface
                                )
                            }
                            
                            OutlinedTextField(
                                value = waterTxt,
                                onValueChange = { waterTxt = it; vm.onWaterChange(it) },
                                label = { Text("水价 (元/m³)", color = ModernColors.Success) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ModernCorners.Medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ModernColors.Success,
                                    unfocusedBorderColor = ModernColors.Outline,
                                    focusedLabelColor = ModernColors.Success
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                            
                            // 电价设置
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = ModernSpacing.Medium)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = ModernColors.Warning.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(ModernCorners.Small)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = ModernColors.Warning,
                                        modifier = Modifier.size(ModernIconSize.Medium)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                                
                                Text(
                                    text = "电费单价",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ModernColors.OnSurface
                                )
                            }
                            
                            OutlinedTextField(
                                value = elecTxt,
                                onValueChange = { elecTxt = it; vm.onElecChange(it) },
                                label = { Text("电价 (元/kWh)", color = ModernColors.Warning) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(ModernCorners.Medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ModernColors.Warning,
                                    unfocusedBorderColor = ModernColors.Outline,
                                    focusedLabelColor = ModernColors.Warning
                                )
                            )
                        }
                    }
                }
                
                // 隐私保护设置区域
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.98f)
                            .widthIn(max = 500.dp)
                            .padding(ModernSpacing.Small),
                        shape = RoundedCornerShape(ModernCorners.Large),
                        color = cardBackgroundColor,
                        shadowElevation = ModernElevation.Level2
                    ) {
                        Column(modifier = Modifier.padding(ModernSpacing.Medium)) {
                            // 隐私保护标题
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = ModernSpacing.Medium)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = ModernColors.Primary.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(ModernCorners.Small)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ModernColors.Primary,
                                        modifier = Modifier.size(ModernIconSize.Medium)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                                
                                Column {
                                    Text(
                                        text = "隐私保护",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ModernColors.OnSurface
                                    )
                                    Text(
                                        text = "设置需要隐藏的关键字",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ModernColors.OnSurfaceVariant
                                    )
                                }
                            }
                            
                            HorizontalDivider(
                                color = ModernColors.OutlineVariant,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = ModernSpacing.Small)
                            )
                            
                            // 关键字输入区域
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newKeyword,
                                    onValueChange = { newKeyword = it },
                                    label = { Text("添加关键字", color = ModernColors.Primary) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(ModernCorners.Medium),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ModernColors.Primary,
                                        unfocusedBorderColor = ModernColors.Outline,
                                        focusedLabelColor = ModernColors.Primary
                                    ),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Small))
                                
                                IconButton(
                                    onClick = {
                                        if (newKeyword.isNotBlank()) {
                                            vm.addPrivacyKeyword(newKeyword.trim())
                                            newKeyword = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            color = ModernColors.Primary,
                                            shape = RoundedCornerShape(ModernCorners.Medium)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "添加关键字",
                                        tint = Color.White
                                    )
                                }
                            }
                            
                            // 关键字列表
                            if (privacyKeywords.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                                
                                Text(
                                    text = "已设置的关键字 (${privacyKeywords.size}/10)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = ModernColors.OnSurface,
                                    modifier = Modifier.padding(bottom = ModernSpacing.Small)
                                )
                                
                                // 使用Flow布局实现自适应换行效果
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ModernSpacing.XSmall),
                                    verticalArrangement = Arrangement.spacedBy(ModernSpacing.XSmall),
                                    maxItemsInEachRow = 4 // 每行最多4个标签
                                ) {
                                    privacyKeywords.forEach { keyword ->
                                        Surface(
                                            shape = RoundedCornerShape(ModernCorners.Small),
                                            color = ModernColors.Primary.copy(alpha = 0.08f),
                                            border = BorderStroke(1.dp, ModernColors.Outline.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    start = ModernSpacing.Small,
                                                    end = 4.dp,
                                                    top = 4.dp,
                                                    bottom = 4.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = keyword,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = ModernColors.OnSurface
                                                )
                                                
                                                IconButton(
                                                    onClick = { showDeleteDialog = keyword },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "删除关键字",
                                                        tint = ModernColors.Error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                                
                                Surface(
                                    shape = RoundedCornerShape(ModernCorners.Medium),
                                    color = ModernColors.SurfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, ModernColors.Outline.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "暂无设置关键字，添加关键字后将在收据中隐藏相应内容",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ModernColors.OnSurfaceVariant,
                                        modifier = Modifier.padding(ModernSpacing.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 操作按钮区域
                item {
                    Button(
                        onClick = { vm.save{ navController.popBackStack() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ModernTouchTarget.Comfortable)
                            .padding(horizontal = ModernSpacing.Large),
                        shape = RoundedCornerShape(ModernCorners.Medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModernColors.Primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = ModernElevation.Level1
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(ModernIconSize.Medium),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(ModernSpacing.Small))
                        Text(
                            "保存设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                
                // 添加底部空间，确保内容不被遮挡
                item {
                    Spacer(modifier = Modifier.height(ModernSpacing.Large))
                }
            }
        }
        
        // 删除确认对话框
        showDeleteDialog?.let { keyword ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = {
                    Text(
                        text = "确认删除",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "确定要删除关键字 \"$keyword\" 吗？",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            vm.removePrivacyKeyword(keyword)
                            showDeleteDialog = null
                        }
                    ) {
                        Text(
                            text = "删除",
                            color = ModernColors.Error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = null }
                    ) {
                        Text(
                            text = "取消",
                            color = ModernColors.OnSurfaceVariant
                        )
                    }
                },
                containerColor = ModernColors.Surface,
                shape = RoundedCornerShape(ModernCorners.Large)
            )
        }
    }
}