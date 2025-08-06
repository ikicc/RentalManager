# 收据界面自定义表名称功能

## 功能概述

收据界面 (`ReceiptUi`) 已更新以支持自定义额外水表和电表名称显示。该功能满足以下需求：

- **需求 3.1**: 在收据中使用自定义的额外表名称
- **需求 3.2**: 分别显示每个表的自定义名称、用量和金额
- **需求 3.4**: 确保自定义名称正确显示在输出中
- **需求 3.5**: 适当处理长名称以保持收据格式美观

## 实现特性

### 1. 自定义名称加载
- 使用 `LaunchedEffect` 在组件初始化时加载自定义表名称
- 通过 `TenantRepository.getMeterDisplayName()` 获取自定义名称
- 如果没有自定义名称，则使用默认名称

### 2. 长名称处理
- 表名称显示区域使用 `weight(1f)` 确保布局灵活性
- 设置 `maxLines = 2` 允许长名称换行显示
- 使用 `TextOverflow.Ellipsis` 处理超长文本
- 增加收据宽度至 480dp 以适应更长的名称

### 3. 布局优化
- 表名称和金额使用 `Arrangement.SpaceBetween` 分布
- 垂直对齐改为 `Alignment.Top` 以适应多行文本
- 在表名称和金额之间添加 8dp 间距

### 4. 错误处理
- 如果 `repository` 为 null，则使用默认名称
- 加载失败时记录错误日志并降级到默认名称
- 确保 UI 不会因为名称加载失败而崩溃

## 使用方法

```kotlin
@Composable
fun MyReceiptScreen() {
    val repository = // 获取 TenantRepository 实例
    val tenant = // 租户信息
    val billWithDetails = // 账单详情
    
    ReceiptUi(
        tenant = tenant,
        billWithDetails = billWithDetails,
        repository = repository, // 传入 repository 以启用自定义名称
        privacyKeywords = emptyList()
    )
}
```

## 技术细节

### 状态管理
```kotlin
var customMeterNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
```

### 名称获取逻辑
```kotlin
LaunchedEffect(repository) {
    if (repository != null) {
        val nameMap = mutableMapOf<String, String>()
        billWithDetails.details.forEach { detail ->
            if (detail.type in listOf("water", "electricity")) {
                val customName = repository.getMeterDisplayName(detail.name)
                nameMap[detail.name] = customName
            }
        }
        customMeterNames = nameMap
    }
}
```

### 显示名称辅助函数
```kotlin
fun getDisplayName(originalName: String): String {
    return customMeterNames[originalName] ?: originalName
}
```

## 兼容性

- 向后兼容：如果不传入 `repository` 参数，将使用默认名称
- 性能优化：只在有 repository 时才加载自定义名称
- 错误恢复：加载失败时自动降级到默认名称

## 测试建议

1. 测试自定义名称正确显示
2. 测试长名称的换行和截断
3. 测试无 repository 时的默认行为
4. 测试加载失败时的错误处理
5. 测试不同屏幕尺寸下的布局适应性