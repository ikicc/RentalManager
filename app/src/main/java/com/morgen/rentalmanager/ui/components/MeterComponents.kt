package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.morgen.rentalmanager.ui.addbillall.ExtraFee
import com.morgen.rentalmanager.ui.addbillall.MeterInput
import com.morgen.rentalmanager.ui.theme.*

/**
 * A component that displays a list of meters with their readings and calculations.
 */
@Composable
fun MeterList(
    roomNumber: String,
    meters: List<MeterInput>,
    meterTypeName: String,
    onAddMeter: () -> Unit,
    onRemoveMeter: (String) -> Unit,
    onTogglePrevEditable: (String) -> Unit,
    onPreviousReadingChange: (String, String) -> Unit,
    onReadingChange: (String, String) -> Unit,
    onUsageChange: (String, String) -> Unit,
    onAmountChange: (String, String) -> Unit,
    onNameChange: ((String, String) -> Unit)? = null // 新增：表名称变更回调
) {
    Column(verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)) {
        meters.forEach { meter ->
            MeterInputItem(
                meter = meter,
                onRemove = { onRemoveMeter(meter.id) },
                onTogglePrevEditable = { onTogglePrevEditable(meter.id) },
                onPreviousReadingChange = { onPreviousReadingChange(meter.id, it) },
                onReadingChange = { onReadingChange(meter.id, it) },
                onUsageChange = { onUsageChange(meter.id, it) },
                onAmountChange = { onAmountChange(meter.id, it) },
                onNameChange = if (onNameChange != null) { newName -> onNameChange(meter.id, newName) } else null
            )
        }
        
        Button(
            onClick = onAddMeter,
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(ModernCorners.Small),
            colors = ButtonDefaults.buttonColors(
                containerColor = ModernColors.Primary,
                contentColor = ModernColors.OnPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = ModernElevation.Level1
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Meter",
                modifier = Modifier.size(ModernIconSize.Small)
            )
            Spacer(modifier = Modifier.width(ModernSpacing.XSmall))
            Text(
                "添加$meterTypeName",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A component that displays input fields for a single meter.
 * 优化版本：减少动画开销，提升性能，支持自定义表名称编辑
 */
@Composable
fun MeterInputItem(
    meter: MeterInput,
    onRemove: () -> Unit,
    onTogglePrevEditable: () -> Unit,
    onPreviousReadingChange: (String) -> Unit,
    onReadingChange: (String) -> Unit,
    onUsageChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onNameChange: ((String) -> Unit)? = null // 新增：表名称变更回调
) {
    // 使用记忆化的回调函数减少重组
    val onRemoveCallback = remember { onRemove }
    val onTogglePrevEditableCallback = remember { onTogglePrevEditable }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // 使用更平滑的内容大小动画
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = ModernAnimations.PerfectSmoothOut  // 使用最平滑的缓动
                )
            ),
        shape = RoundedCornerShape(ModernCorners.Small),
        color = ModernColors.SurfaceVariant,
        tonalElevation = ModernElevation.Level1
    ) {
        Column(
            modifier = Modifier.padding(ModernSpacing.Medium)
        ) {
            // 表名称显示和编辑区域
            if (!meter.isPrimary && onNameChange != null) {
                // 非主表：可编辑名称
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = meter.name,
                        onValueChange = onNameChange,
                        label = { Text("表名称") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ModernColors.Primary,
                            unfocusedBorderColor = ModernColors.Outline,
                            focusedLabelColor = ModernColors.Primary,
                            unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                            focusedContainerColor = ModernColors.Surface,
                            unfocusedContainerColor = ModernColors.Surface
                        ),
                        shape = RoundedCornerShape(ModernCorners.Small)
                    )
                    
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove Meter",
                            tint = ModernColors.Error
                        )
                    }
                }
            } else {
                // 主表：只显示名称，不可编辑
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        meter.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ModernColors.OnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (!meter.isPrimary) {
                        IconButton(onClick = onRemove) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove Meter",
                                tint = ModernColors.Error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = meter.previousReading,
                    onValueChange = onPreviousReadingChange,
                    label = { Text("上月读数") },
                    enabled = meter.isPrevEditable,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ModernColors.Primary,
                        unfocusedBorderColor = ModernColors.Outline,
                        disabledBorderColor = ModernColors.Outline,
                        focusedLabelColor = ModernColors.Primary,
                        unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                        focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                        unfocusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                        disabledContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                    ),
                    shape = RoundedCornerShape(ModernCorners.Small)
                )
                
                IconButton(onClick = onTogglePrevEditable) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Previous Reading",
                        tint = if (meter.isPrevEditable) ModernColors.Primary else ModernColors.OnSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            OutlinedTextField(
                value = meter.currentReading,
                onValueChange = onReadingChange,
                label = { Text("本月读数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ModernColors.Primary,
                    unfocusedBorderColor = ModernColors.Outline,
                    focusedLabelColor = ModernColors.Primary,
                    unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                    focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                    unfocusedContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                ),
                shape = RoundedCornerShape(ModernCorners.Small)
            )
            
            Spacer(modifier = Modifier.height(ModernSpacing.Small))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = meter.usage,
                    onValueChange = onUsageChange,
                    label = { Text("用量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.8f), // 减少用量字段的权重
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ModernColors.Primary,
                        unfocusedBorderColor = ModernColors.Outline,
                        focusedLabelColor = ModernColors.Primary,
                        unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                        focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                        unfocusedContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                    ),
                    shape = RoundedCornerShape(ModernCorners.Small)
                )
                
                OutlinedTextField(
                    value = meter.amount,
                    onValueChange = onAmountChange,
                    label = { Text("金额") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f), // 增加金额字段的权重，给更多空间显示数字
                    leadingIcon = { Text("¥", color = ModernColors.OnSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ModernColors.Primary,
                        unfocusedBorderColor = ModernColors.Outline,
                        focusedLabelColor = ModernColors.Primary,
                        unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                        focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                        unfocusedContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                    ),
                    shape = RoundedCornerShape(ModernCorners.Small)
                )
            }
            
            // Show manual input indicators - 优化：移除不必要的动画
            if (meter.isUsageManual || meter.isAmountManual) {
                Spacer(modifier = Modifier.height(ModernSpacing.XSmall))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 移除动画，直接显示文本以提升性能
                    Text(
                        text = if (meter.isUsageManual && meter.isAmountManual) "手动输入: 用量和金额" 
                              else if (meter.isUsageManual) "手动输入: 用量" 
                              else "手动输入: 金额",
                        style = MaterialTheme.typography.bodySmall,
                        color = ModernColors.OnSurfaceSecondary
                    )
                }
            }
        }
    }
}

/**
 * A component that displays a list of extra fees.
 */
@Composable
fun ExtraFeeList(
    roomNumber: String,
    fees: List<ExtraFee>,
    onAddFee: () -> Unit,
    onRemoveFee: (String) -> Unit,
    onNameChange: (String, String) -> Unit,
    onAmountChange: (String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(ModernSpacing.Medium)) {
        fees.forEach { fee ->
            ExtraFeeInputItem(
                fee = fee,
                onRemove = { onRemoveFee(fee.id) },
                onNameChange = { onNameChange(fee.id, it) },
                onAmountChange = { onAmountChange(fee.id, it) }
            )
        }
        
        Button(
            onClick = onAddFee,
            modifier = Modifier.align(Alignment.End),
            shape = RoundedCornerShape(ModernCorners.Small),
            colors = ButtonDefaults.buttonColors(
                containerColor = ModernColors.Primary,
                contentColor = ModernColors.OnPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = ModernElevation.Level1
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Fee",
                modifier = Modifier.size(ModernIconSize.Small)
            )
            Spacer(modifier = Modifier.width(ModernSpacing.XSmall))
            Text(
                "添加附加费",
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A component that displays input fields for a single extra fee.
 */
@Composable
fun ExtraFeeInputItem(
    fee: ExtraFee,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ModernCorners.Small),
        color = ModernColors.SurfaceVariant,
        tonalElevation = ModernElevation.Level1
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ModernSpacing.Small),
            modifier = Modifier.padding(ModernSpacing.Medium)
        ) {
            OutlinedTextField(
                value = fee.name,
                onValueChange = onNameChange,
                label = { Text("费用名称") },
                modifier = Modifier.weight(1.2f), // 给费用名称稍微多一点空间
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ModernColors.Primary,
                    unfocusedBorderColor = ModernColors.Outline,
                    focusedLabelColor = ModernColors.Primary,
                    unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                    focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                    unfocusedContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                ),
                shape = RoundedCornerShape(ModernCorners.Small)
            )
            
            OutlinedTextField(
                value = fee.amount,
                onValueChange = onAmountChange,
                label = { Text("金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1.0f), // 保持金额字段有足够空间显示数字
                leadingIcon = { Text("¥", color = ModernColors.OnSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ModernColors.Primary,
                    unfocusedBorderColor = ModernColors.Outline,
                    focusedLabelColor = ModernColors.Primary,
                    unfocusedLabelColor = ModernColors.OnSurfaceVariant,
                    focusedContainerColor = ModernColors.Surface, // 修复：设置容器背景色，自动适配深色模式
                    unfocusedContainerColor = ModernColors.Surface // 修复：设置容器背景色，自动适配深色模式
                ),
                shape = RoundedCornerShape(ModernCorners.Small)
            )
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove Fee",
                    tint = ModernColors.Error
                )
            }
        }
    }
}

/**
 * A component that displays a card for tenant bill input.
 * 优化版本：添加平滑的动画效果，提升用户体验
 */
@Composable
fun TenantBillInputCard(
    state: com.morgen.rentalmanager.ui.addbillall.BillInputState,
    meterType: String,
    viewModel: com.morgen.rentalmanager.ui.addbillall.AddBillAllViewModel
) {
    // 使用记忆化来减少重组
    val filteredMeters = remember(state.meters, meterType) {
        state.meters.filter { it.type == meterType }
    }
    
    // 使用ElevatedCard来添加点击动画效果
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        defaultElevation = ModernElevation.Level1,
        pressedElevation = ModernElevation.Level2,
        shape = RoundedCornerShape(ModernCorners.Medium)
    ) {
        Column(modifier = Modifier.padding(ModernSpacing.Large)) {
            Text(
                text = "${state.tenantName} (${state.roomNumber})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ModernColors.OnSurface
            )
            Spacer(modifier = Modifier.height(ModernSpacing.Medium))

            when (meterType) {
                "water" -> {
                    MeterList(
                        roomNumber = state.roomNumber,
                        meters = filteredMeters, // 使用已经过滤的meters
                        meterTypeName = "水表",
                        onAddMeter = { viewModel.addMeter(state.roomNumber, "water") },
                        onRemoveMeter = { meterId ->
                            viewModel.removeMeter(
                                state.roomNumber,
                                meterId
                            )
                        },
                        onTogglePrevEditable = { meterId ->
                            viewModel.togglePrevEditable(
                                state.roomNumber,
                                meterId
                            )
                        },
                        onPreviousReadingChange = { meterId, value ->
                            viewModel.onMeterPreviousReadingChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onReadingChange = { meterId, value ->
                            viewModel.onMeterReadingChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onUsageChange = { meterId, value ->
                            viewModel.onMeterUsageChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onAmountChange = { meterId, value ->
                            viewModel.onMeterAmountChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onNameChange = { meterId, newName ->
                            viewModel.onMeterNameChange(
                                state.roomNumber,
                                meterId,
                                newName
                            )
                        }
                    )
                }

                "electricity" -> {
                    MeterList(
                        roomNumber = state.roomNumber,
                        meters = filteredMeters, // 使用已经过滤的meters
                        meterTypeName = "电表",
                        onAddMeter = { viewModel.addMeter(state.roomNumber, "electricity") },
                        onRemoveMeter = { meterId ->
                            viewModel.removeMeter(
                                state.roomNumber,
                                meterId
                            )
                        },
                        onTogglePrevEditable = { meterId ->
                            viewModel.togglePrevEditable(
                                state.roomNumber,
                                meterId
                            )
                        },
                        onPreviousReadingChange = { meterId, value ->
                            viewModel.onMeterPreviousReadingChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onReadingChange = { meterId, value ->
                            viewModel.onMeterReadingChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onUsageChange = { meterId, value ->
                            viewModel.onMeterUsageChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onAmountChange = { meterId, value ->
                            viewModel.onMeterAmountChange(
                                state.roomNumber,
                                meterId,
                                value
                            )
                        },
                        onNameChange = { meterId, newName ->
                            viewModel.onMeterNameChange(
                                state.roomNumber,
                                meterId,
                                newName
                            )
                        }
                    )
                }

                "extra" -> {
                    ExtraFeeList(
                        roomNumber = state.roomNumber,
                        fees = state.extraFees,
                        onAddFee = { viewModel.addExtraFee(state.roomNumber) },
                        onRemoveFee = { feeId ->
                            viewModel.removeExtraFee(
                                state.roomNumber,
                                feeId
                            )
                        },
                        onNameChange = { feeId, value ->
                            viewModel.onExtraFeeNameChange(
                                state.roomNumber,
                                feeId,
                                value
                            )
                        },
                        onAmountChange = { feeId, value ->
                            viewModel.onExtraFeeAmountChange(
                                state.roomNumber,
                                feeId,
                                value
                            )
                        }
                    )
                }
            }
        }
    }
}