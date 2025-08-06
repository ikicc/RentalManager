package com.morgen.rentalmanager.ui.receipt

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.morgen.rentalmanager.ui.theme.ModernColors
import com.morgen.rentalmanager.ui.theme.ModernElevation
import com.morgen.rentalmanager.ui.theme.ModernSpacing
import com.morgen.rentalmanager.ui.theme.ModernCorners
import com.morgen.rentalmanager.ui.theme.ModernIconSize
import com.morgen.rentalmanager.ui.theme.ModernTouchTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(navController: NavController, imageUri: String) {
    val context = LocalContext.current
    val decodedUri = Uri.parse(imageUri)
    
    // 添加日志以便调试
    android.util.Log.d("ReceiptScreen", "显示收据图片: $imageUri")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "收据详情",
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
                        .padding(horizontal = 8.dp, vertical = ModernSpacing.Large) // 更小的水平间距，最大化显示区域宽度
                ) {
                    // 收据显示区域 - 现代化设计
                    Surface(
                        shape = RoundedCornerShape(ModernCorners.Large),
                        color = ModernColors.Surface,
                        shadowElevation = ModernElevation.Level1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp), // 减小内边距，给图片更多空间
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = decodedUri,
                                // 禁用缓存，确保每次都加载最新的图片
                                imageLoader = coil.ImageLoader.Builder(context)
                                    .memoryCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.DISABLED)
                                    .build()
                            ),
                            contentDescription = "生成的收据图片",
                            modifier = Modifier
                                .fillMaxWidth(0.95f) // 使用相对宽度，确保在不同设备上都有适当的尺寸
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(ModernSpacing.Large))
                
                // 操作按钮区域 - 现代化设计
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_STREAM, decodedUri)
                                    type = "image/png"
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "分享收据")
                                context.startActivity(shareIntent)
                            },
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
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(ModernIconSize.Medium),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(ModernSpacing.Small))
                            Text(
                                "分享收据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                }
            }
        }
    }
} 