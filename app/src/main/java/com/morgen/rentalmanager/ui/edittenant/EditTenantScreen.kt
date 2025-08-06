package com.morgen.rentalmanager.ui.edittenant

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.morgen.rentalmanager.TenantApplication
import com.morgen.rentalmanager.myapplication.Tenant
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernElevation
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTenantScreen(navController: NavController) {
    val context = LocalContext.current
    val application = context.applicationContext as TenantApplication
    val viewModel: EditTenantViewModel = viewModel(
        factory = EditTenantViewModelFactory(application.repository)
    )

    val tenantState by viewModel.tenant.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }

    // Update local states when tenant data is loaded
    LaunchedEffect(tenantState) {
        name = tenantState.name
        rent = tenantState.rent.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "编辑租户",
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
                    containerColor = ModernColors.Surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModernColors.Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(ModernSpacing.Large)
            ) {
                // 标题区域 - 现代化设计
                Surface(
                    shape = RoundedCornerShape(ModernCorners.Large),
                    color = ModernColors.Surface,
                    shadowElevation = ModernElevation.Level1
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(ModernSpacing.Large),
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = ModernColors.Primary,
                                modifier = Modifier.size(ModernIconSize.Large)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                        
                        Column {
                            Text(
                                text = "编辑租户 - ${tenantState.roomNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ModernColors.OnSurface
                            )
                            Text(
                                text = "修改租户基本信息",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ModernColors.OnSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Large))
                
                // 表单区域 - 现代化设计
                Surface(
                    shape = RoundedCornerShape(ModernCorners.Large),
                    color = ModernColors.Surface,
                    shadowElevation = ModernElevation.Level1
                ) {
                    Column(modifier = Modifier.padding(ModernSpacing.Large)) {
                        Text(
                            text = "基本信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.OnSurface,
                            modifier = Modifier.padding(bottom = ModernSpacing.Medium)
                        )
                        
                        OutlinedTextField(
                            value = tenantState.roomNumber,
                            onValueChange = { /* Room number is primary key, not editable */ },
                            label = { Text("房间号", color = ModernColors.OnSurfaceVariant) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = ModernColors.Outline,
                                disabledLabelColor = ModernColors.OnSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("租户姓名", color = ModernColors.Primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ModernColors.Primary,
                                unfocusedBorderColor = ModernColors.Outline,
                                focusedLabelColor = ModernColors.Primary
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(ModernSpacing.Medium))
                        
                        OutlinedTextField(
                            value = rent,
                            onValueChange = { rent = it },
                            label = { Text("月租金 (元)", color = ModernColors.Primary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(ModernCorners.Medium),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ModernColors.Primary,
                                unfocusedBorderColor = ModernColors.Outline,
                                focusedLabelColor = ModernColors.Primary
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Large))
                
                // 操作按钮区域 - 现代化设计
                Surface(
                    shape = RoundedCornerShape(ModernCorners.Large),
                    color = ModernColors.Surface,
                    shadowElevation = ModernElevation.Level1
                ) {
                    Column(modifier = Modifier.padding(ModernSpacing.Large)) {
                        Text(
                            text = "操作",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.OnSurface,
                            modifier = Modifier.padding(bottom = ModernSpacing.Medium)
                        )
                        
                        // 横排按钮布局
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)
                        ) {
                            // 保存按钮
                            Button(
                                onClick = {
                                    val rentValue = rent.toDoubleOrNull()
                                    if (name.isNotBlank() && rentValue != null) {
                                        // 使用协程确保数据库更新完成后再返回
                                        coroutineScope.launch {
                                            val updatedTenant = Tenant(
                                                roomNumber = tenantState.roomNumber,
                                                name = name,
                                                rent = rentValue
                                            )
                                            
                                            // 检查租金是否发生变化
                                            val rentChanged = tenantState.rent != rentValue
                                            
                                            // 更新租户信息
                                            viewModel.updateTenant(updatedTenant)
                                            
                                            // 如果租金发生变化，强制重新计算所有相关账单
                                            if (rentChanged) {
                                                android.util.Log.d("EditTenantScreen", "租金变更: ${tenantState.rent} -> $rentValue，触发账单重新计算")
                                                viewModel.recalculateAllBills(tenantState.roomNumber)
                                            }
                                            
                                            // 等待数据同步完成
                                            kotlinx.coroutines.delay(500)
                                            
                                            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    } else {
                                        Toast.makeText(context, "请填写所有有效字段", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ModernTouchTarget.Comfortable),
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
                                    "保存",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            
                            // 删除按钮 - 改为实心按钮
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        viewModel.deleteTenant()
                                        // 等待一小段时间确保数据库更新完成
                                        kotlinx.coroutines.delay(100)
                                        navController.popBackStack()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(ModernTouchTarget.Comfortable),
                                shape = RoundedCornerShape(ModernCorners.Medium),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ModernColors.Error
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = ModernElevation.Level1
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(ModernIconSize.Medium),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(ModernSpacing.Small))
                                Text(
                                    "删除",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 