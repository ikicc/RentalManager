package com.morgen.rentalmanager.ui.billlist

import org.junit.Test
import org.junit.Assert.*

/**
 * BillSummaryCard组件的单元测试
 * 验证数据处理逻辑和格式化功能
 */
class BillSummaryCardTest {

    @Test
    fun `test BillSummary data formatting`() {
        // 准备测试数据
        val testSummary = BillSummary(
            totalRent = 3000.0,
            totalWater = 150.50,
            totalElectricity = 280.75,
            totalAmount = 3431.25
        )

        // 验证格式化方法
        assertEquals("3000.00", testSummary.getFormattedTotalRent())
        assertEquals("150.50", testSummary.getFormattedTotalWater())
        assertEquals("280.75", testSummary.getFormattedTotalElectricity())
        assertEquals("3431.25", testSummary.getFormattedTotalAmount())
    }

    @Test
    fun `test BillSummary empty state detection`() {
        // 测试空数据
        val emptySummary = BillSummary()
        assertFalse("Empty summary should not have any amount", emptySummary.hasAnyAmount())
        assertTrue("Empty summary should be valid", emptySummary.isValid())

        // 测试有数据的情况
        val nonEmptySummary = BillSummary(totalRent = 100.0)
        assertTrue("Non-empty summary should have amount", nonEmptySummary.hasAnyAmount())
    }

    @Test
    fun `test BillSummary validation`() {
        // 测试有效数据
        val validSummary = BillSummary(
            totalRent = 1000.0,
            totalWater = 50.0,
            totalElectricity = 80.0,
            totalAmount = 1130.0
        )
        assertTrue("Valid summary should pass validation", validSummary.isValid())

        // 测试无效数据（负值）
        val invalidSummary = BillSummary(
            totalRent = -100.0,
            totalWater = 50.0,
            totalElectricity = 80.0,
            totalAmount = 30.0
        )
        assertFalse("Invalid summary with negative values should fail validation", invalidSummary.isValid())
    }

    @Test
    fun `test BillSummary zero values formatting`() {
        // 测试零值格式化
        val zeroSummary = BillSummary()
        
        assertEquals("0.00", zeroSummary.getFormattedTotalRent())
        assertEquals("0.00", zeroSummary.getFormattedTotalWater())
        assertEquals("0.00", zeroSummary.getFormattedTotalElectricity())
        assertEquals("0.00", zeroSummary.getFormattedTotalAmount())
    }

    @Test
    fun `test BillSummary partial data`() {
        // 测试部分数据
        val partialSummary = BillSummary(
            totalRent = 2000.0,
            totalWater = 0.0,
            totalElectricity = 100.0,
            totalAmount = 2100.0
        )

        assertTrue("Partial summary should have amount", partialSummary.hasAnyAmount())
        assertTrue("Partial summary should be valid", partialSummary.isValid())
        assertEquals("2000.00", partialSummary.getFormattedTotalRent())
        assertEquals("0.00", partialSummary.getFormattedTotalWater())
        assertEquals("100.00", partialSummary.getFormattedTotalElectricity())
        assertEquals("2100.00", partialSummary.getFormattedTotalAmount())
    }

    @Test
    fun `test BillSummary decimal precision`() {
        // 测试小数精度
        val precisionSummary = BillSummary(
            totalRent = 1000.123,
            totalWater = 50.999,
            totalElectricity = 80.001,
            totalAmount = 1131.123
        )

        // 验证格式化保留两位小数
        assertEquals("1000.12", precisionSummary.getFormattedTotalRent())
        assertEquals("51.00", precisionSummary.getFormattedTotalWater())
        assertEquals("80.00", precisionSummary.getFormattedTotalElectricity())
        assertEquals("1131.12", precisionSummary.getFormattedTotalAmount())
    }

    @Test
    fun `test BillSummary large numbers formatting`() {
        // 测试大数值格式化
        val largeSummary = BillSummary(
            totalRent = 999999.99,
            totalWater = 1000000.00,
            totalElectricity = 500000.50,
            totalAmount = 2499999.49
        )

        assertEquals("999999.99", largeSummary.getFormattedTotalRent())
        assertEquals("1000000.00", largeSummary.getFormattedTotalWater())
        assertEquals("500000.50", largeSummary.getFormattedTotalElectricity())
        assertEquals("2499999.49", largeSummary.getFormattedTotalAmount())
    }
}