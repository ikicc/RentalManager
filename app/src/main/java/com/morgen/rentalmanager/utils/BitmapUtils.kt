package com.morgen.rentalmanager.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.graphics.Canvas
import android.view.ViewTreeObserver
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View.MeasureSpec

suspend fun captureComposableToBitmap(
    activity: androidx.activity.ComponentActivity,
    content: @Composable () -> Unit
): Bitmap = withContext(Dispatchers.Main) {
    android.util.Log.d("BitmapUtils", "开始创建ComposeView用于收据生成")
    
    val composeView = ComposeView(activity).apply {
        setContent {
            DisposableEffect(Unit) {
                onDispose { }
            }
            content()
        }
    }

    val rootView = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
    composeView.visibility = android.view.View.INVISIBLE
    rootView.addView(composeView)

    // 优化后的测量和渲染流程 - 基于old myapplication的成功实现
    val displayMetrics = activity.resources.displayMetrics
    composeView.measure(
        MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, MeasureSpec.AT_MOST),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
    )
    
    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)
    
    // 确保视图层级完全渲染
    composeView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            composeView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    })
    
    // 创建兼容的Bitmap
    val bitmap = try {
        Bitmap.createBitmap(
            composeView.measuredWidth,
            composeView.measuredHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            // 确保白色背景，打印友好
            canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC)
            composeView.draw(canvas)
        }
    } catch (e: Exception) {
        android.util.Log.e("BitmapUtils", "创建Bitmap失败: ${e.message}")
        // 回退方案
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    rootView.removeView(composeView)
    android.util.Log.d("BitmapUtils", "Bitmap生成完成: ${bitmap.width}x${bitmap.height}")
    return@withContext bitmap
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File? {
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, fileName)
    try {
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return file
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
} 