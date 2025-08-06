package com.morgen.rentalmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.morgen.rentalmanager.ui.theme.*
import java.util.Calendar

/**
 * A modern month-year picker dialog with blur effects and animations.
 */
@Composable
fun ModernMonthYearPicker(
    show: Boolean,
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 5..currentYear + 5).toList()
    val months = (1..12).toList()

    var selectedYear by remember(show) { mutableStateOf(initialYear) }
    var selectedMonth by remember(show) { mutableStateOf(initialMonth) }
    
    var expandedYear by remember { mutableStateOf(false) }
    var expandedMonth by remember { mutableStateOf(false) }

    if (show) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .padding(ModernSpacing.Large),
                contentAlignment = Alignment.Center
            ) {
                // Dialog content with animation
                AnimatedVisibility(
                    visible = show,
                    enter = fadeIn(tween(ModernAnimations.STANDARD_DURATION)) +
                            slideInVertically(
                                initialOffsetY = { it / 2 },
                                animationSpec = tween(ModernAnimations.STANDARD_DURATION)
                            ),
                    exit = fadeOut(tween(ModernAnimations.FAST_DURATION)) +
                            slideOutVertically(
                                targetOffsetY = { it / 2 },
                                animationSpec = tween(ModernAnimations.FAST_DURATION)
                            )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .wrapContentHeight(),
                        shape = RoundedCornerShape(ModernCorners.Large),
                        color = ModernColors.Surface,
                        shadowElevation = ModernElevation.Level3
                    ) {
                        Column(
                            modifier = Modifier.padding(ModernSpacing.Large),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "选择月份",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ModernColors.OnSurface
                            )
                            
                            Spacer(modifier = Modifier.height(ModernSpacing.Large))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Year Picker
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedYear = true },
                                        shape = RoundedCornerShape(ModernCorners.Medium),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = ModernColors.Primary
                                        )
                                    ) {
                                        Text(
                                            "$selectedYear 年",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expandedYear,
                                        onDismissRequest = { expandedYear = false }
                                    ) {
                                        years.forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text(year.toString()) },
                                                onClick = {
                                                    selectedYear = year
                                                    expandedYear = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Month Picker
                                Box {
                                    OutlinedButton(
                                        onClick = { expandedMonth = true },
                                        shape = RoundedCornerShape(ModernCorners.Medium),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = ModernColors.Primary
                                        )
                                    ) {
                                        Text(
                                            "$selectedMonth 月",
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = expandedMonth,
                                        onDismissRequest = { expandedMonth = false }
                                    ) {
                                        months.forEach { month ->
                                            DropdownMenuItem(
                                                text = { Text(month.toString()) },
                                                onClick = {
                                                    selectedMonth = month
                                                    expandedMonth = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(ModernSpacing.XLarge))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = ModernColors.OnSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        "取消",
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(ModernSpacing.Medium))
                                
                                Button(
                                    onClick = {
                                        onConfirm(selectedYear, selectedMonth)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(ModernCorners.Medium),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ModernColors.Primary
                                    )
                                ) {
                                    Text(
                                        "确认",
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}