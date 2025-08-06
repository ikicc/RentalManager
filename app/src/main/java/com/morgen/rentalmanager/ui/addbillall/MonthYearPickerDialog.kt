package com.morgen.rentalmanager.ui.addbillall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Calendar

@Composable
fun MonthYearPickerDialog(
    show: Boolean,
    initialYear: Int,
    initialMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    if (show) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).toList()
        val months = (1..12).toList()

        var expandedYear by remember { mutableStateOf(false) }
        var selectedYear by remember(show) { mutableStateOf(initialYear) }

        var expandedMonth by remember { mutableStateOf(false) }
        var selectedMonth by remember(show) { mutableStateOf(initialMonth) }


        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择月份") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Year Picker
                    Box {
                        OutlinedButton(onClick = { expandedYear = true }) {
                            Text("$selectedYear 年")
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
                        OutlinedButton(onClick = { expandedMonth = true }) {
                            Text("$selectedMonth 月")
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
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(selectedYear, selectedMonth)
                    onDismiss()
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        )
    }
} 