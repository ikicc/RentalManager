package com.morgen.rentalmanager.myapplication.miniprogram

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * 解析微信小程序导出的JSON文件
 */
object JsonParser {
    private const val TAG = "JsonParser"
    private val gson: Gson = GsonBuilder().setLenient().create()
    
    /**
     * 从URI解析微信小程序导出的JSON数据
     * @param context 上下文
     * @param uri JSON文件的URI
     * @return 解析后的MiniprogramData对象，如果解析失败则返回null
     */
    fun parseMiniprogramJson(context: Context, uri: Uri): MiniprogramData? {
        Log.d(TAG, "开始解析小程序JSON文件: $uri")
        
        return try {
            // 打开文件输入流
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IOException("无法打开文件流")
            
            // 读取JSON内容
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }
            
            if (jsonString.isBlank()) {
                Log.e(TAG, "JSON文件内容为空")
                throw IOException("JSON文件内容为空")
            }
            
            Log.d(TAG, "成功读取JSON文件，长度: ${jsonString.length}")
            
            // 尝试直接解析为MiniprogramData
            var miniprogramData: MiniprogramData? = null
            
            try {
                miniprogramData = gson.fromJson(jsonString, MiniprogramData::class.java)
                
                // 检查是否成功解析到有效数据
                if (miniprogramData?.tenants?.isNotEmpty() == true || miniprogramData?.bills?.isNotEmpty() == true) {
                    Log.d(TAG, "直接解析成功")
                } else {
                    Log.d(TAG, "直接解析未获取到有效数据，尝试其他方法")
                    miniprogramData = null
                }
            } catch (e: Exception) {
                Log.d(TAG, "直接解析失败: ${e.message}，尝试从JSON对象中提取数据")
                miniprogramData = null
            }
            
            // 如果直接解析失败，尝试从JSON对象中提取小程序数据
            if (miniprogramData == null) {
                try {
                    // 尝试清理JSON字符串，移除可能的BOM标记和非法字符
                    val cleanedJson = cleanJsonString(jsonString)
                    
                    val jsonObject = gson.fromJson(cleanedJson, com.google.gson.JsonObject::class.java)
                    
                    // 检查是否包含tenants字段
                    if (jsonObject.has("tenants")) {
                        val tenantsList = try {
                            gson.fromJson(jsonObject.get("tenants"), Array<MiniprogramTenant>::class.java)?.toList() ?: emptyList()
                        } catch (e: Exception) {
                            Log.w(TAG, "解析tenants字段失败: ${e.message}")
                            emptyList()
                        }
                        
                        // 检查是否包含bills字段
                        val billsMap = if (jsonObject.has("bills")) {
                            try {
                                gson.fromJson(jsonObject.get("bills"), 
                                    object : com.google.gson.reflect.TypeToken<Map<String, Map<String, MiniprogramBill>>>() {}.type) ?: emptyMap()
                            } catch (e: Exception) {
                                Log.w(TAG, "解析bills字段失败: ${e.message}")
                                emptyMap<String, Map<String, MiniprogramBill>>()
                            }
                        } else {
                            emptyMap<String, Map<String, MiniprogramBill>>()
                        }
                        
                        // 检查是否包含prices字段
                        val prices = if (jsonObject.has("prices")) {
                            try {
                                gson.fromJson(jsonObject.get("prices"), MiniprogramPrices::class.java)
                            } catch (e: Exception) {
                                Log.w(TAG, "解析prices字段失败: ${e.message}")
                                null
                            }
                        } else {
                            null
                        }
                        
                        // 检查是否包含contracts字段
                        val contracts = if (jsonObject.has("contracts")) {
                            try {
                                gson.fromJson(jsonObject.get("contracts"), 
                                    object : com.google.gson.reflect.TypeToken<Map<String, MiniprogramContract>>() {}.type)
                            } catch (e: Exception) {
                                Log.w(TAG, "解析contracts字段失败: ${e.message}")
                                null
                            }
                        } else {
                            null
                        }
                        
                        miniprogramData = MiniprogramData(
                            tenants = tenantsList,
                            bills = billsMap,
                            prices = prices,
                            contracts = contracts
                        )
                        
                        if (tenantsList.isNotEmpty() || billsMap.isNotEmpty()) {
                            Log.d(TAG, "从JSON对象中提取数据成功")
                        } else {
                            Log.e(TAG, "从JSON对象中提取的数据为空")
                            return null
                        }
                    } else {
                        Log.e(TAG, "JSON文件中未找到tenants字段")
                        
                        // 尝试检查文件是否包含其他可能的数据结构
                        val keys = jsonObject.keySet()
                        if (keys.isNotEmpty()) {
                            Log.d(TAG, "JSON文件包含以下字段: ${keys.joinToString(", ")}")
                        }
                        
                        return null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "从JSON对象中提取数据失败", e)
                    return null
                }
            }
            
            // 使用DataValidator验证解析结果
            val validator = DataValidator()
            val validationResult = validator.validateMiniprogramData(miniprogramData)
            
            // 记录验证结果
            if (!validationResult.isValid) {
                Log.w(TAG, "JSON数据验证发现错误: ${validationResult.errors.size} 个错误")
                validationResult.errors.forEach { Log.w(TAG, "错误: $it") }
            }
            
            if (validationResult.warnings.isNotEmpty()) {
                Log.w(TAG, "JSON数据验证发现警告: ${validationResult.warnings.size} 个警告")
                validationResult.warnings.forEach { Log.w(TAG, "警告: $it") }
            }
            
            Log.d(TAG, "JSON解析成功: 租户数量=${miniprogramData.tenants.size}, 账单房间数量=${miniprogramData.bills.size}")
            miniprogramData
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "JSON格式错误", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "文件读取错误", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "内存不足，无法解析大型JSON文件", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON文件失败", e)
            null
        }
    }
    
    /**
     * 清理JSON字符串，移除BOM标记和非法字符
     */
    private fun cleanJsonString(jsonString: String): String {
        // 移除UTF-8 BOM标记（如果存在）
        var cleaned = if (jsonString.startsWith("\uFEFF")) {
            jsonString.substring(1)
        } else {
            jsonString
        }
        
        // 移除可能导致解析错误的控制字符
        cleaned = cleaned.replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")
        
        return cleaned
    }
    
    /**
     * 验证解析后的数据
     * @param data 解析后的小程序数据
     * @throws IllegalArgumentException 如果数据无效
     */
    private fun validateParsedData(data: MiniprogramData) {
        // 检查租户数据
        if (data.tenants.isEmpty()) {
            Log.w(TAG, "警告: 没有租户数据")
        } else {
            // 检查租户数据的完整性
            data.tenants.forEach { tenant ->
                if (tenant.roomNumber.isBlank()) {
                    Log.w(TAG, "警告: 租户房间号为空")
                }
                if (tenant.name.isBlank()) {
                    Log.w(TAG, "警告: 租户姓名为空, 房间号: ${tenant.roomNumber}")
                }
            }
        }
        
        // 检查账单数据
        if (data.bills.isEmpty()) {
            Log.w(TAG, "警告: 没有账单数据")
        }
        
        // 检查价格数据
        if (data.prices == null) {
            Log.w(TAG, "警告: 没有价格数据，将使用默认价格或从账单中推断")
        }
    }
}