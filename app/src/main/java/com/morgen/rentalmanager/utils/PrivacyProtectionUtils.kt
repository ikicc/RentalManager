package com.morgen.rentalmanager.utils

/**
 * 隐私保护工具类
 * 提供房号隐私保护功能，支持关键字隐藏
 */
object PrivacyProtectionUtils {
    
    /**
     * 应用隐私保护到房号
     * 智能移除房号中匹配的关键字，处理空格和多余字符
     * 
     * @param roomNumber 原始房号
     * @param keywords 需要隐藏的关键字列表
     * @return 应用隐私保护后的房号
     */
    fun applyPrivacyProtection(roomNumber: String, keywords: List<String>): String {
        android.util.Log.d("PrivacyProtectionUtils", "Input - roomNumber: '$roomNumber', keywords: $keywords")
        
        if (roomNumber.isBlank() || keywords.isEmpty()) {
            android.util.Log.d("PrivacyProtectionUtils", "Early return - roomNumber blank or keywords empty")
            return roomNumber
        }
        
        var protectedRoomNumber = roomNumber
        
        // 遍历所有关键字，使用更智能的匹配策略
        keywords.forEach { keyword ->
            if (keyword.isNotBlank()) {
                val trimmedKeyword = keyword.trim()
                
                // 策略1: 直接匹配
                if (protectedRoomNumber.contains(trimmedKeyword)) {
                    protectedRoomNumber = protectedRoomNumber.replace(trimmedKeyword, "")
                }
                
                // 策略2: 处理空格变化 - 如果关键字不包含空格，尝试匹配房号中带空格的版本
                if (!trimmedKeyword.contains(" ")) {
                    // 尝试匹配 "关键字 " 和 " 关键字" 的模式
                    protectedRoomNumber = protectedRoomNumber.replace("$trimmedKeyword ", "")
                    protectedRoomNumber = protectedRoomNumber.replace(" $trimmedKeyword", "")
                    protectedRoomNumber = protectedRoomNumber.replace(" $trimmedKeyword ", "")
                }
                
                // 策略3: 如果关键字包含空格，也尝试匹配无空格版本
                if (trimmedKeyword.contains(" ")) {
                    val keywordNoSpace = trimmedKeyword.replace(" ", "")
                    if (protectedRoomNumber.contains(keywordNoSpace)) {
                        protectedRoomNumber = protectedRoomNumber.replace(keywordNoSpace, "")
                    }
                }
                
                // 策略4: 使用正则表达式进行更灵活的匹配
                try {
                    // 将关键字中的空格替换为可选的空格匹配
                    val regexPattern = trimmedKeyword.replace(" ", "\\s*")
                    val regex = Regex(regexPattern)
                    protectedRoomNumber = protectedRoomNumber.replace(regex, "")
                } catch (e: Exception) {
                    // 如果正则表达式失败，忽略这个策略
                }
            }
        }
        
        // 清理多余的空格
        val result = protectedRoomNumber
            .replace(Regex("\\s+"), " ")  // 将多个连续空格替换为单个空格
            .trim()                       // 移除首尾空格
        
        android.util.Log.d("PrivacyProtectionUtils", "Result: '$result'")
        return result
    }
    
    /**
     * 验证关键字是否有效
     * 
     * @param keyword 待验证的关键字
     * @return 是否有效
     */
    fun isValidKeyword(keyword: String): Boolean {
        return keyword.isNotBlank() && keyword.length in 1..20
    }
    
    /**
     * 验证关键字列表是否有效
     * 
     * @param keywords 关键字列表
     * @return 是否有效
     */
    fun isValidKeywordList(keywords: List<String>): Boolean {
        return keywords.size <= 10 && keywords.all { isValidKeyword(it) }
    }
    
    /**
     * 清理和标准化关键字列表
     * 移除空白和重复的关键字
     * 
     * @param keywords 原始关键字列表
     * @return 清理后的关键字列表
     */
    fun cleanKeywords(keywords: List<String>): List<String> {
        return keywords
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(10) // 限制最多10个关键字
    }
}