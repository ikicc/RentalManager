package com.morgen.rentalmanager.ui.billlist

/**
 * 账单列表功能的常量定义
 */
object BillListConstants {
    
    /**
     * 费用类型常量
     */
    object BillType {
        const val WATER = "water"
        const val ELECTRICITY = "electricity"
        const val EXTRA = "extra"
        const val RENT = "rent"
    }
    
    /**
     * 月份格式常量
     */
    object DateFormat {
        const val MONTH_FORMAT = "yyyy-MM"
        const val DISPLAY_MONTH_FORMAT = "yyyy年MM月"
        const val FULL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    }
    
    /**
     * 数值格式化常量
     */
    object NumberFormat {
        const val AMOUNT_DECIMAL_PLACES = 2
        const val USAGE_DECIMAL_PLACES = 1
        const val PERCENTAGE_DECIMAL_PLACES = 1
        const val AMOUNT_FORMAT = "%.2f"
        const val USAGE_FORMAT = "%.1f"
        const val PERCENTAGE_FORMAT = "%.1f%%"
    }
    
    /**
     * UI相关常量
     */
    object UI {
        const val CHART_MIN_SIZE = 200
        const val CHART_ANIMATION_DURATION = 300
        const val LIST_ITEM_MIN_HEIGHT = 80
        const val CARD_ELEVATION = 4
        const val LOADING_DELAY = 500L
    }
    
    /**
     * 错误消息常量
     */
    object ErrorMessages {
        const val NETWORK_ERROR = "网络连接异常，请检查网络设置"
        const val DATABASE_ERROR = "数据读取失败，请重试"
        const val UNKNOWN_ERROR = "未知错误，请重试"
        const val INVALID_MONTH_FORMAT = "无效的月份格式"
        const val DATA_VALIDATION_FAILED = "数据验证失败"
        const val NO_DATA_AVAILABLE = "暂无数据"
        const val CALCULATION_ERROR = "计算错误"
    }
    
    /**
     * 空状态消息常量
     */
    object EmptyMessages {
        const val NO_BILLS_FOR_MONTH = "月暂无账单数据"
        const val NO_TENANTS = "暂无租户信息"
        const val NO_CHART_DATA = "暂无图表数据"
        const val NO_SEARCH_RESULTS = "未找到匹配的结果"
    }
    
    /**
     * 单位常量
     */
    object Units {
        const val CURRENCY = "元"
        const val WATER_UNIT = "度"
        const val ELECTRICITY_UNIT = "度"
        const val PERCENTAGE = "%"
    }
    
    /**
     * 标签常量
     */
    object Labels {
        const val RENT_LABEL = "租金"
        const val WATER_LABEL = "水费"
        const val ELECTRICITY_LABEL = "电费"
        const val TOTAL_LABEL = "总计"
        const val USAGE_LABEL = "用量"
        const val AMOUNT_LABEL = "金额"
        const val MONTH_LABEL = "月份"
        const val TENANT_LABEL = "租户"
        const val ROOM_LABEL = "房间"
    }
    
    /**
     * 默认值常量
     */
    object Defaults {
        const val DEFAULT_AMOUNT = 0.0
        const val DEFAULT_USAGE = 0.0
        const val DEFAULT_PERCENTAGE = 0f
        const val DEFAULT_ROOM_NUMBER = ""
        const val DEFAULT_TENANT_NAME = ""
        const val DEFAULT_PHONE = ""
        const val MIN_CHART_VALUE = 0.01 // 最小图表显示值
    }
    
    /**
     * 验证规则常量
     */
    object Validation {
        const val MIN_AMOUNT = 0.0
        const val MAX_AMOUNT = 999999.99
        const val MIN_USAGE = 0.0
        const val MAX_USAGE = 99999.9
        const val MIN_PERCENTAGE = 0f
        const val MAX_PERCENTAGE = 100f
        const val MAX_ROOM_NUMBER_LENGTH = 10
        const val MAX_TENANT_NAME_LENGTH = 50
        const val MAX_PHONE_LENGTH = 20
    }
    
    /**
     * 颜色常量（十六进制值）
     */
    object Colors {
        // 浅色主题颜色
        const val LIGHT_RENT_COLOR = 0xFF2E7D32
        const val LIGHT_WATER_COLOR = 0xFF1565C0
        const val LIGHT_ELECTRICITY_COLOR = 0xFFE65100
        
        // 深色主题颜色
        const val DARK_RENT_COLOR = 0xFF4CAF50
        const val DARK_WATER_COLOR = 0xFF2196F3
        const val DARK_ELECTRICITY_COLOR = 0xFFFF9800
        
        // 状态颜色
        const val SUCCESS_COLOR = 0xFF4CAF50
        const val WARNING_COLOR = 0xFFFF9800
        const val ERROR_COLOR = 0xFFF44336
        const val INFO_COLOR = 0xFF2196F3
    }
    
    /**
     * 动画常量
     */
    object Animation {
        const val FADE_IN_DURATION = 300
        const val FADE_OUT_DURATION = 200
        const val SLIDE_DURATION = 250
        const val CHART_ANIMATION_DURATION = 500
        const val LOADING_ANIMATION_DURATION = 1000
        const val BOUNCE_ANIMATION_DURATION = 400
    }
    
    /**
     * 布局常量
     */
    object Layout {
        const val CARD_CORNER_RADIUS = 12
        const val SMALL_PADDING = 8
        const val MEDIUM_PADDING = 16
        const val LARGE_PADDING = 24
        const val EXTRA_LARGE_PADDING = 32
        const val DIVIDER_THICKNESS = 1
        const val ELEVATION_SMALL = 2
        const val ELEVATION_MEDIUM = 4
        const val ELEVATION_LARGE = 8
    }
    
    /**
     * 性能相关常量
     */
    object Performance {
        const val LIST_PREFETCH_DISTANCE = 5
        const val CHART_CACHE_SIZE = 10
        const val DATA_CACHE_TIMEOUT = 300000L // 5分钟
        const val MAX_CONCURRENT_OPERATIONS = 3
    }
}