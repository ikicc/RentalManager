package com.morgen.rentalmanager.ui.billlist

import org.junit.Test
import org.junit.Assert.*

class BillListContentTest {

    @Test
    fun billItem_formatsAmountsCorrectly() {
        val billItem = BillItem(
            roomNumber = "101",
            tenantName = "张三",
            phone = "13800138001",
            rent = 1500.0,
            waterAmount = 50.0,
            waterUsage = 25.0,
            electricityAmount = 80.0,
            electricityUsage = 100.0,
            totalAmount = 1630.0
        )

        // 验证金额格式化
        assertEquals("1500.00", billItem.getFormattedRent())
        assertEquals("50.00", billItem.getFormattedWaterAmount())
        assertEquals("80.00", billItem.getFormattedElectricityAmount())
        assertEquals("1630.00", billItem.getFormattedTotalAmount())
        
        // 验证用量格式化
        assertEquals("25.0", billItem.getFormattedWaterUsage())
        assertEquals("100.0", billItem.getFormattedElectricityUsage())
    }

    @Test
    fun billItem_checksAmountExistence() {
        val billItemWithAllAmounts = BillItem(
            roomNumber = "101",
            tenantName = "张三",
            rent = 1500.0,
            waterAmount = 50.0,
            electricityAmount = 80.0,
            totalAmount = 1630.0
        )

        assertTrue(billItemWithAllAmounts.hasWaterAmount())
        assertTrue(billItemWithAllAmounts.hasElectricityAmount())
        assertTrue(billItemWithAllAmounts.hasAnyAmount())

        val billItemWithoutWaterElectricity = BillItem(
            roomNumber = "102",
            tenantName = "李四",
            rent = 1200.0,
            waterAmount = 0.0,
            electricityAmount = 0.0,
            totalAmount = 1200.0
        )

        assertFalse(billItemWithoutWaterElectricity.hasWaterAmount())
        assertFalse(billItemWithoutWaterElectricity.hasElectricityAmount())
        assertTrue(billItemWithoutWaterElectricity.hasAnyAmount()) // 仍有租金

        val billItemWithNoAmounts = BillItem(
            roomNumber = "103",
            tenantName = "王五",
            rent = 0.0,
            waterAmount = 0.0,
            electricityAmount = 0.0,
            totalAmount = 0.0
        )

        assertFalse(billItemWithNoAmounts.hasWaterAmount())
        assertFalse(billItemWithNoAmounts.hasElectricityAmount())
        assertFalse(billItemWithNoAmounts.hasAnyAmount())
    }

    @Test
    fun billItem_validatesData() {
        val validBillItem = BillItem(
            roomNumber = "101",
            tenantName = "张三",
            rent = 1500.0,
            waterAmount = 50.0,
            electricityAmount = 80.0,
            totalAmount = 1630.0
        )

        assertTrue(validBillItem.isValid())

        val invalidBillItemEmptyRoom = BillItem(
            roomNumber = "",
            tenantName = "张三",
            rent = 1500.0,
            totalAmount = 1500.0
        )

        assertFalse(invalidBillItemEmptyRoom.isValid())

        val invalidBillItemEmptyName = BillItem(
            roomNumber = "101",
            tenantName = "",
            rent = 1500.0,
            totalAmount = 1500.0
        )

        assertFalse(invalidBillItemEmptyName.isValid())

        val invalidBillItemNegativeAmount = BillItem(
            roomNumber = "101",
            tenantName = "张三",
            rent = -100.0,
            totalAmount = -100.0
        )

        assertFalse(invalidBillItemNegativeAmount.isValid())
    }

    @Test
    fun billItem_generatesDisplayText() {
        val billItem = BillItem(
            roomNumber = "101",
            tenantName = "张三",
            waterAmount = 50.0,
            waterUsage = 25.0,
            electricityAmount = 80.0,
            electricityUsage = 0.0 // 无用量
        )

        // 有用量的水费显示
        assertEquals("50.00元 (25.0度)", billItem.getWaterDisplayText())
        
        // 无用量的电费显示
        assertEquals("80.00元", billItem.getElectricityDisplayText())
    }
}