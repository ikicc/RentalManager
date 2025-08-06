package com.morgen.rentalmanager.utils

/**
 * 备份元数据
 */
data class BackupMetadata(
    val version: String,
    val exportTime: String,
    val appVersion: String,
    val dataStructureVersion: String,
    val totalRecords: Map<String, Int>
)

/**
 * 导入结果
 */
data class ImportResult(
    val success: Boolean,
    val message: String,
    val stats: ImportStats,
    val errors: List<ImportError>
)

/**
 * 导入统计信息
 */
data class ImportStats(
    val tenantsImported: Int = 0,
    val billsImported: Int = 0,
    val pricesImported: Int = 0,
    val meterConfigsImported: Int = 0,
    val conflictsResolved: Int = 0,
    val errorsEncountered: Int = 0
)

/**
 * 导入错误信息
 */
data class ImportError(
    val type: ErrorType,
    val message: String,
    val details: String? = null
)

/**
 * 错误类型枚举
 */
enum class ErrorType {
    FILE_FORMAT_ERROR,
    DATA_VALIDATION_ERROR,
    DATABASE_ERROR,
    CONFLICT_ERROR,
    UNKNOWN_ERROR
}

/**
 * 冲突处理策略
 */
enum class ConflictStrategy {
    OVERWRITE,  // 覆盖现有数据
    SKIP,       // 跳过冲突数据
    MERGE       // 智能合并数据
}

/**
 * 数据验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * 数据完整性检查结果
 */
data class IntegrityResult(
    val isIntact: Boolean,
    val missingReferences: List<String> = emptyList(),
    val orphanedRecords: List<String> = emptyList()
)

/**
 * 内部使用的导入结果（包含错误信息）
 */
data class ImportResultWithErrors(
    val stats: ImportStats,
    val errors: List<ImportError>
)