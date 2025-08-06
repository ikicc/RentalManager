package com.morgen.rentalmanager.ui.billlist

import org.junit.Test
import org.junit.Assert.*

/**
 * 月份选择器组件的单元测试
 * 测试月份字符串解析、格式化和导航功能
 */
class MonthSelectorTest {

    @Test
    fun `test getCurrentMonthString returns valid format`() {
        val currentMonth = getCurrentMonthString()
        
        // 验证格式是否正确 (YYYY-MM)
        assertTrue("Current month should match YYYY-MM format", 
            currentMonth.matches(Regex("\\d{4}-\\d{2}")))
        
        // 验证年份是否合理 (2020-2030)
        val year = currentMonth.split("-")[0].toInt()
        assertTrue("Year should be reasonable", year in 2020..2030)
        
        // 验证月份是否在有效范围内 (01-12)
        val month = currentMonth.split("-")[1].toInt()
        assertTrue("Month should be between 1 and 12", month in 1..12)
    }

    @Test
    fun `test getNextMonth correctly calculates next month`() {
        // 测试普通月份
        assertEquals("2024-02", getNextMonth("2024-01"))
        assertEquals("2024-12", getNextMonth("2024-11"))
        
        // 测试年末跨年
        assertEquals("2025-01", getNextMonth("2024-12"))
        
        // 测试二月
        assertEquals("2024-03", getNextMonth("2024-02"))
    }

    @Test
    fun `test getPreviousMonth correctly calculates previous month`() {
        // 测试普通月份
        assertEquals("2024-01", getPreviousMonth("2024-02"))
        assertEquals("2024-11", getPreviousMonth("2024-12"))
        
        // 测试年初跨年
        assertEquals("2023-12", getPreviousMonth("2024-01"))
        
        // 测试三月
        assertEquals("2024-02", getPreviousMonth("2024-03"))
    }

    @Test
    fun `test month string parsing with valid input`() {
        // 这个测试需要访问私有函数，我们通过公共函数间接测试
        val testMonth = "2024-05"
        val nextMonth = getNextMonth(testMonth)
        val prevMonth = getPreviousMonth(testMonth)
        
        assertEquals("2024-06", nextMonth)
        assertEquals("2024-04", prevMonth)
    }

    @Test
    fun `test month string parsing with invalid input`() {
        // 测试无效输入时的容错处理
        val invalidInputs = listOf(
            "invalid",
            "2024",
            "2024-13",
            "2024-00",
            "",
            "abc-def"
        )
        
        invalidInputs.forEach { invalidInput ->
            // 应该不会抛出异常，而是返回合理的默认值
            assertNotNull("Should handle invalid input gracefully", 
                getNextMonth(invalidInput))
            assertNotNull("Should handle invalid input gracefully", 
                getPreviousMonth(invalidInput))
        }
    }

    @Test
    fun `test month navigation consistency`() {
        val testMonth = "2024-06"
        
        // 测试往返一致性
        val nextThenPrev = getPreviousMonth(getNextMonth(testMonth))
        val prevThenNext = getNextMonth(getPreviousMonth(testMonth))
        
        assertEquals("Next then previous should return original", testMonth, nextThenPrev)
        assertEquals("Previous then next should return original", testMonth, prevThenNext)
    }

    @Test
    fun `test edge cases for month boundaries`() {
        // 测试月份边界情况
        val edgeCases = mapOf(
            "2024-01" to "2023-12", // 年初到上年年末
            "2024-12" to "2025-01", // 年末到下年年初
            "2024-02" to "2024-01", // 二月到一月
            "2024-03" to "2024-04"  // 三月到四月
        )
        
        edgeCases.forEach { (current, expectedPrevious) ->
            assertEquals("Previous month calculation failed for $current", 
                expectedPrevious, getPreviousMonth(getNextMonth(current)))
        }
    }

    @Test
    fun `test month format consistency`() {
        val months = listOf(
            "2024-01", "2024-02", "2024-03", "2024-04", "2024-05", "2024-06",
            "2024-07", "2024-08", "2024-09", "2024-10", "2024-11", "2024-12"
        )
        
        months.forEach { month ->
            val next = getNextMonth(month)
            val prev = getPreviousMonth(month)
            
            // 验证格式一致性
            assertTrue("Next month format should be consistent", 
                next.matches(Regex("\\d{4}-\\d{2}")))
            assertTrue("Previous month format should be consistent", 
                prev.matches(Regex("\\d{4}-\\d{2}")))
        }
    }
}