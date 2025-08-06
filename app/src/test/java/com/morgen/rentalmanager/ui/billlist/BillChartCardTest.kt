package com.morgen.rentalmanager.ui.billlist

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

/**
 * BillChartCard组件的单元测试
 * 验证图表数据处理逻辑和格式化功能
 */
class BillChartCardTest {

    @Test
    fun `test ChartData validation`() {
        // 测试有效数据
        val validChartData = ChartData(
            label = "租金",
            value = 1000.0,
            color = Color.Blue,
            percentage = 60f
        )
        assertTrue("Valid chart data should pass validation", validChartData.isValid())

        // 测试无效数据（空标签）
        val invalidLabelData = ChartData(
            label = "",
            value = 1000.0,
            color = Color.Blue,
            percentage = 60f
        )
        assertFalse("Chart data with empty label should fail validation", invalidLabelData.isValid())

        // 测试无效数据（负值）
        val invalidValueData = ChartData(
            label = "租金",
            value = -100.0,
            color = Color.Blue,
            percentage = 60f
        )
        assertFalse("Chart data with negative value should fail validation", invalidValueData.isValid())

        // 测试无效数据（超出百分比范围）
        val invalidPercentageData = ChartData(
            label = "租金",
            value = 1000.0,
            color = Color.Blue,
            percentage = 150f
        )
        assertFalse("Chart data with percentage > 100 should fail validation", invalidPercentageData.isValid())
    }

    @Test
    fun `test ChartData formatting`() {
        val chartData = ChartData(
            label = "租金",
            value = 1234.567,
            color = Color.Blue,
            percentage = 66.789f
        )

        assertEquals("1234.57", chartData.getFormattedValue())
        assertEquals("66.8%", chartData.getFormattedPercentage())
        assertEquals("租金: 1234.57元 (66.8%)", chartData.getDisplayText())
    }

    @Test
    fun `test ChartData fromBillSummary creates correct data`() {
        val summary = BillSummary(
            totalRent = 1000.0,
            totalWater = 300.0,
            totalElectricity = 200.0,
            totalAmount = 1500.0
        )

        val chartData = ChartData.fromBillSummary(
            summary = summary,
            rentColor = Color.Blue,
            waterColor = Color.Green,
            electricityColor = Color.Red
        )

        // 验证数据项数量
        assertEquals(3, chartData.size)

        // 验证租金数据
        val rentData = chartData.find { it.label == "租金" }
        assertNotNull("Rent data should exist", rentData)
        assertEquals(1000.0, rentData!!.value, 0.01)
        assertEquals(66.7f, rentData.percentage, 0.1f) // 1000/1500 * 100

        // 验证水费数据
        val waterData = chartData.find { it.label == "水费" }
        assertNotNull("Water data should exist", waterData)
        assertEquals(300.0, waterData!!.value, 0.01)
        assertEquals(20.0f, waterData.percentage, 0.1f) // 300/1500 * 100

        // 验证电费数据
        val electricityData = chartData.find { it.label == "电费" }
        assertNotNull("Electricity data should exist", electricityData)
        assertEquals(200.0, electricityData!!.value, 0.01)
        assertEquals(13.3f, electricityData.percentage, 0.1f) // 200/1500 * 100
    }

    @Test
    fun `test ChartData fromBillSummary handles zero total`() {
        val summary = BillSummary(
            totalRent = 0.0,
            totalWater = 0.0,
            totalElectricity = 0.0,
            totalAmount = 0.0
        )

        val chartData = ChartData.fromBillSummary(
            summary = summary,
            rentColor = Color.Blue,
            waterColor = Color.Green,
            electricityColor = Color.Red
        )

        // 应该返回空列表
        assertTrue("Chart data should be empty for zero total", chartData.isEmpty())
    }

    @Test
    fun `test ChartData fromBillSummary skips zero values`() {
        val summary = BillSummary(
            totalRent = 1000.0,
            totalWater = 0.0,
            totalElectricity = 200.0,
            totalAmount = 1200.0
        )

        val chartData = ChartData.fromBillSummary(
            summary = summary,
            rentColor = Color.Blue,
            waterColor = Color.Green,
            electricityColor = Color.Red
        )

        // 应该只有2个数据项（跳过水费）
        assertEquals(2, chartData.size)
        assertFalse("Should not contain water data", chartData.any { it.label == "水费" })
        assertTrue("Should contain rent data", chartData.any { it.label == "租金" })
        assertTrue("Should contain electricity data", chartData.any { it.label == "电费" })
    }

    @Test
    fun `test ChartData fromBillSummary percentage calculation`() {
        val summary = BillSummary(
            totalRent = 600.0,
            totalWater = 200.0,
            totalElectricity = 200.0,
            totalAmount = 1000.0
        )

        val chartData = ChartData.fromBillSummary(
            summary = summary,
            rentColor = Color.Blue,
            waterColor = Color.Green,
            electricityColor = Color.Red
        )

        // 验证百分比计算
        val rentData = chartData.find { it.label == "租金" }
        assertEquals(60.0f, rentData!!.percentage, 0.1f)

        val waterData = chartData.find { it.label == "水费" }
        assertEquals(20.0f, waterData!!.percentage, 0.1f)

        val electricityData = chartData.find { it.label == "电费" }
        assertEquals(20.0f, electricityData!!.percentage, 0.1f)

        // 验证总百分比为100%
        val totalPercentage = chartData.sumOf { it.percentage.toDouble() }
        assertEquals(100.0, totalPercentage, 0.1)
    }

    @Test
    fun `test ChartData zero value formatting`() {
        val chartData = ChartData(
            label = "测试",
            value = 0.0,
            color = Color.Blue,
            percentage = 0f
        )

        assertEquals("0.00", chartData.getFormattedValue())
        assertEquals("0.0%", chartData.getFormattedPercentage())
        assertEquals("测试: 0.00元 (0.0%)", chartData.getDisplayText())
    }
}