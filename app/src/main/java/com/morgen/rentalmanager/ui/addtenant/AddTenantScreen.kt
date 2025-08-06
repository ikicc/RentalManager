package com.morgen.rentalmanager.ui.addtenant

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.morgen.rentalmanager.ui.theme.ModernTypography
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernTouchTarget
import com.morgen.rentalmanager.ui.components.AnimatedButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTenantScreen(
    navController: NavController,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as TenantApplication
    val viewModel: AddTenantViewModel = viewModel(
        factory = AddTenantViewModelFactory(application.repository)
    )

    var roomNumber by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "添加租户",
                        fontWeight = FontWeight.Medium,
                        color = ModernColors.OnSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 确保onDismiss回调被执行
                        onDismiss()
                        // 延迟一小段时间后再次调用，确保状态同步
                        kotlinx.coroutines.MainScope().launch {
                            delay(50)
                            onDismiss()
                        }
                        
                        // 如果是从普通导航进入的，则支持返回导航
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
                // 标题区域 - 现代化设计，带入场动画
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    ) { -it / 2 } + fadeIn(
                        animationSpec = tween(300, easing = LinearOutSlowInEasing)
                    )
                ) {
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
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = ModernColors.Primary,
                                    modifier = Modifier.size(ModernIconSize.Large)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                            
                            Column {
                                Text(
                                    text = "新增租户",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ModernColors.OnSurface
                                )
                                Text(
                                    text = "填写租户基本信息",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ModernColors.OnSurfaceVariant
                                )
                            }
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
                            value = roomNumber,
                            onValueChange = { roomNumber = it },
                            label = { Text("房间号", color = ModernColors.Primary) },
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
                        AnimatedButton(
                            onClick = {
                                val rentValue = rent.toDoubleOrNull()
                                if (roomNumber.isNotBlank() && name.isNotBlank() && rentValue != null) {
                                    val newTenant = Tenant(
                                        roomNumber = roomNumber,
                                        name = name,
                                        rent = rentValue
                                    )
                                    viewModel.addTenant(newTenant)
                                    
                                    // 确保onDismiss回调被正确执行
                                    onDismiss()
                                    // 延迟一小段时间后再次调用，确保状态同步
                                    kotlinx.coroutines.MainScope().launch {
                                        delay(50)
                                        onDismiss()
                                    }
                                    
                                    // 如果是从普通导航进入的，则支持返回导航
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                } else {
                                    Toast.makeText(context, "请填写所有字段", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
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
                                "保存租户",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Large))
                
                // 说明信息 - 现代化设计
                Surface(
                    shape = RoundedCornerShape(ModernCorners.Medium),
                    color = ModernColors.Surface,
                    shadowElevation = ModernElevation.Level1
                ) {
                    Column(modifier = Modifier.padding(ModernSpacing.Medium)) {
                        Text(
                            text = "💡 填写说明",
                            fontSize = ModernTypography.LabelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(ModernSpacing.Small))
                        Text(
                            text = "• 房间号用于唯一标识租户\n• 租户姓名用于生成收据\n• 月租金将自动计入账单",
                            fontSize = ModernTypography.BodyMedium,
                            color = ModernColors.OnSurfaceTertiary,
                            lineHeight = ModernTypography.LineHeightMedium
                        )
                    }
                }
            }
        }
    }
} 