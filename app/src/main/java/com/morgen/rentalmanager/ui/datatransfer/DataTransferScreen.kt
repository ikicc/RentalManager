package com.morgen.rentalmanager.ui.datatransfer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Added for sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernElevation
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernTouchTarget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTransferScreen(
    navController: NavController,
    viewModel: DataTransferViewModel = viewModel(factory = DataTransferViewModelFactory((LocalContext.current.applicationContext as com.morgen.rentalmanager.TenantApplication).repository))
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    
    // 操作结果对话框状态
    var showResultDialog by remember { mutableStateOf(false) }
    var resultDialogTitle by remember { mutableStateOf("") }
    var resultDialogMessage by remember { mutableStateOf("") }
    var isSuccessResult by remember { mutableStateOf(true) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var backupFilePath by remember { mutableStateOf("") }
    
    // 获取根据当前主题的界面背景色
    val backgroundColor = if (isSystemInDarkTheme()) Color(0xFF121212) else Color.White
    
    // 获取根据当前主题的卡片背景色
    val cardBackgroundColor = if (isSystemInDarkTheme()) Color(0xFF1A1A1A) else Color.White
    
    // Document picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importData(context, uri)
            isImporting = true
            Toast.makeText(context, "正在恢复数据...", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Document picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportData(context, uri)
            isExporting = true
            Toast.makeText(context, "正在备份数据...", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Observer for operation status
    LaunchedEffect(viewModel.operationStatus) {
        when(viewModel.operationStatus) {
            is DataTransferStatus.Success -> {
                val successStatus = viewModel.operationStatus as DataTransferStatus.Success
                val message = successStatus.message
                
                // 显示成功对话框
                resultDialogTitle = "操作成功"
                resultDialogMessage = message
                isSuccessResult = true
                showResultDialog = true
                isExporting = false
                isImporting = false
                shouldNavigateBack = true
                
                // 保存备份文件路径
                backupFilePath = successStatus.filePath ?: ""
            }
            is DataTransferStatus.Error -> {
                val errorStatus = viewModel.operationStatus as DataTransferStatus.Error
                val message = errorStatus.message
                
                // 显示错误对话框
                resultDialogTitle = "操作失败"
                resultDialogMessage = message
                isSuccessResult = false
                showResultDialog = true
                isExporting = false
                isImporting = false
                shouldNavigateBack = false
            }
            else -> {
                // 其他状态不处理
            }
        }
    }

    // 在导航返回时重置状态
    DisposableEffect(navController) {
        onDispose {
                viewModel.resetStatus()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "备份与恢复",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(horizontal = ModernSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Large)
        ) {
            // 主功能卡片
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                    shape = RoundedCornerShape(ModernCorners.Large),
                    color = cardBackgroundColor,
                    shadowElevation = ModernElevation.Level2
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(ModernSpacing.Large),
                    verticalArrangement = Arrangement.spacedBy(ModernSpacing.Large)
                    ) {
                    // 标题
                        Text(
                        text = "手动备份与恢复",
                        style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ModernColors.OnSurface
                        )
                        
                    // 备份功能
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)  // 增加高度
                            .padding(bottom = 8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                // 使用日期作为默认文件名
                                val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                                val date = LocalDateTime.now().format(dateFormat)
                                val fileName = "租房管家备份_$date.json"
                                exportLauncher.launch(fileName)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),  // 固定高度
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            border = BorderStroke(1.dp, ModernColors.Primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ModernColors.Primary
                            ),
                            enabled = !isExporting && !isImporting
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = ModernColors.Primary,
                                modifier = Modifier.size(20.dp)  // 减小图标
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "备份数据",
                                fontSize = 13.sp,  // 减小字体大小
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(ModernSpacing.Medium))

                        Button(
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),  // 固定高度
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ModernColors.Primary
                            ),
                            enabled = !isExporting && !isImporting
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)  // 减小图标
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "恢复数据",
                                fontSize = 13.sp,  // 减小字体大小
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // 自动备份管理卡片
                Surface(
                    modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                    shape = RoundedCornerShape(ModernCorners.Large),
                    color = cardBackgroundColor,
                    shadowElevation = ModernElevation.Level2
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(ModernSpacing.Large),
                        verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
                    ) {
                    // 标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = ModernColors.Primary,
                                modifier = Modifier.size(ModernIconSize.Large)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                        
                        Column {
                        Text(
                                text = "自动备份管理",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.OnSurface
                        )
                            Text(
                                text = "查看和使用自动备份文件",
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
                    
                    // 自动备份路径
                    val activity = context as? android.app.Activity
                    val backupDir = if (activity != null) {
                        com.morgen.rentalmanager.utils.BackupFileHelper.getBackupDirectory(activity)
                    } else {
                        "文档/租房管家备份"
                    }
                    
                    // 备份路径信息
                    Surface(
                        shape = RoundedCornerShape(ModernCorners.Medium),
                        color = ModernColors.SurfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, ModernColors.Outline.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(vertical = ModernSpacing.Small)
                    ) {
                        Column(
                            modifier = Modifier.padding(ModernSpacing.Medium),
                            verticalArrangement = Arrangement.spacedBy(ModernSpacing.Small)
                        ) {
                            Text(
                                text = "备份文件位置:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = ModernColors.OnSurface
                            )
                            
                            Text(
                                text = "$backupDir/租房管家自动备份.json",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ModernColors.OnSurfaceVariant
                            )
                        }
                    }
                    
                    // 信息提示
                    Surface(
                            shape = RoundedCornerShape(ModernCorners.Medium),
                        color = ModernColors.Primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(ModernSpacing.Medium),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ModernColors.Primary,
                                modifier = Modifier.size(ModernIconSize.Small)
                            )
                            Spacer(modifier = Modifier.width(ModernSpacing.Small))
                            Text(
                                text = "每次保存账单时，应用会自动创建备份文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = ModernColors.Primary
                            )
                        }
                    }
                    
                    // 恢复自动备份按钮
                    Button(
                        onClick = {
                            // 恢复自动备份文件
                            viewModel.importAutoBackup(context)
                            isImporting = true
                            Toast.makeText(context, "正在从自动备份恢复数据...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),  // 与前面按钮保持一致的高度
                        shape = RoundedCornerShape(ModernCorners.Medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ModernColors.Primary
                        ),
                        enabled = !isExporting && !isImporting
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)  // 减小图标大小
                        )
                        Spacer(modifier = Modifier.width(8.dp))  // 固定间距
                        Text(
                            "从自动备份恢复数据",
                            fontSize = 16.sp,  // 减小字体大小
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
            
            // 操作提示信息
            if (isExporting || isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = if (isExporting) "备份中，请稍候..." else "恢复中，请稍候...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ModernColors.OnSurfaceVariant,
                    modifier = Modifier.padding(top = ModernSpacing.Small)
                            )
                        }
                    }
                }
    
    // 结果对话框
    if (showResultDialog) {
        ResultDialog(
            title = resultDialogTitle,
            message = resultDialogMessage,
            isSuccess = isSuccessResult,
            filePath = backupFilePath,
            isExport = viewModel.isExportOperation,
            onDismiss = {
                viewModel.resetStatus()
                showResultDialog = false
                if (shouldNavigateBack) {
                    navController.popBackStack()
            }
            },
            onShare = {
                if (backupFilePath.isNotEmpty() && viewModel.isExportOperation) {
                    shareBackupFile(context, backupFilePath, viewModel.backupUri, viewModel.backupFileName)
                }
            }
        )
    }
}

@Composable
fun ResultDialog(
    title: String,
    message: String,
    isSuccess: Boolean,
    filePath: String,
    isExport: Boolean,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
            ) {
                // 状态图标
                Surface(
                    shape = CircleShape,
                    color = if (isSuccess) 
                        ModernColors.Success.copy(alpha = 0.1f) 
                    else 
                        ModernColors.Error.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
            ) {
                    Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                            tint = if (isSuccess) ModernColors.Success else ModernColors.Error,
                            modifier = Modifier.size(24.dp)
                )
                    }
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ModernColors.OnSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = ModernSpacing.Medium),
                verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
            ) {
                // 分隔线
                HorizontalDivider(
                    color = ModernColors.OutlineVariant,
                    thickness = 1.dp
                )
                
                // 消息内容
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ModernColors.OnSurfaceVariant
            )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 如果是备份成功且有分享回调，显示分享按钮
                if (isSuccess && onShare != null) {
                    OutlinedButton(
                        onClick = onShare,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ModernColors.Primary,
                        ),
                        border = BorderStroke(1.dp, ModernColors.Primary),
                        modifier = Modifier
                            .height(40.dp)
                            .padding(end = ModernSpacing.Medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(ModernSpacing.Small))
                        Text(
                            text = "分享",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ModernColors.Primary
                ),
                    modifier = Modifier.height(40.dp)
            ) {
                Text(
                        text = "确定",
                    fontWeight = FontWeight.Medium
                )
                }
            }
        },
        containerColor = if (isSystemInDarkTheme()) Color(0xFF1D1D1D) else ModernColors.Surface,
        shape = RoundedCornerShape(ModernCorners.Large),
        modifier = Modifier.fillMaxWidth(0.9f),
        tonalElevation = ModernElevation.Level1
    )
}

// 分享备份文件的函数
private fun shareBackupFile(context: android.content.Context, filePath: String, uri: Uri?, fileName: String?) {
    try {
        // 如果有直接的Uri，优先使用
        if (uri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享备份文件"))
            return
        }
        
        // 否则，检查是否是自动备份文件(从Documents目录)
        val file = File(filePath)
        if (file.exists()) {
            // 创建临时文件用于分享
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val tempFile = File(context.cacheDir, fileName ?: "租房管家备份_${timestamp}.json")
            
            // 复制备份内容到临时文件
            file.inputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // 获取临时文件Uri
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
            
            // 分享文件
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享备份文件"))
        } else {
            // 文件不存在
            Toast.makeText(context, "找不到备份文件: $filePath", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "分享文件失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
} 