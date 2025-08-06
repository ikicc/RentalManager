package com.morgen.rentalmanager.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 备份文件辅助工具
 * 提供获取备份文件路径的功能
 */
object BackupFileHelper {
    private const val TAG = "BackupFileHelper"
    private const val PERMISSION_REQUEST_CODE = 1001

    /**
     * 获取自动备份文件存放路径
     * 根据Android版本返回不同的文档目录
     */
    fun getBackupDirectory(activity: Activity): String {
        val folderName = "租房管家备份"
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10及以上使用专用文档目录
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            "${dir.absolutePath}/$folderName"
        } else {
            // 低版本Android使用通用文档目录
            val externalDir = ContextCompat.getExternalFilesDirs(activity, null)
            "${externalDir[0].absolutePath}/$folderName"
        }
    }
    
    /**
     * 检查并请求存储权限
     * @param activity Activity对象
     * @return 是否已有权限
     */
    fun checkStoragePermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+不需要存储权限，使用范围访问
            true
        } else {
            // Android 9及以下需要传统存储权限
            val permission = ContextCompat.checkSelfPermission(
                activity, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
                false
            } else {
                true
            }
        }
    }
} 