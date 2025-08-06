package com.morgen.rentalmanager.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * AmountFormatter的单元测试
 */
class AmountFormatterTest {

    @Test
    fun formatAmount_normalValues_returnsCorrectFormat() {
        assertEquals("123.45", AmountFormatter.formatAmount(123.45))
        assertEquals("0.00", AmountFormatter.formatAmount(0.0))
        assertEquals("1000.00", AmountFormatter.formatAmount(1000.0))
        assertEquals("0.01", AmountFormatter.formatAmount(0.01))
        assertEquals("999.99", AmountFormatter.formatAmount(999.99))
    }

    @Test
    fun formatAmount_edgeCases_returnsDefaultValue() {
        assertEquals("0.00", AmountFormatter.formatAmount(Double.NaN))
        assertEquals("0.00", AmountFormatter.formatAmount(Double.POSITIVE_INFINITY))
        assertEquals("0.00", AmountFormatter.formatAmount(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun formatAmountWithCurrency_normalValues_returnsCorrectFormat() {
        assertEquals("¥123.45", AmountFormatter.formatAmountWithCurrency(123.45))
        assertEquals("¥0.00", AmountFormatter.formatAmountWithCurrency(0.0))
        assertEquals("$123.45", AmountFormatter.formatAmountWithCurrency(123.45, "$"))
    }

    @Test
    fun formatUsage_normalValues_returnsCorrectFormat() {
        assertEquals("123.4", AmountFormatter.formatUsage(123.45))
        assertEquals("0.0", AmountFormatter.formatUsage(0.0))
        assertEquals("1000.0", AmountFormatter.formatUsage(1000.0))
        assertEquals("0.1", AmountFormatter.formatUsage(0.1))
    }

    @Test
    fun formatUsage_edgeCases_returnsDefaultValue() {
        assertEquals("0.0", AmountFormatter.formatUsage(Double.NaN))
        assertEquals("0.0", AmountFormatter.formatUsage(Double.POSITIVE_INFINITY))
        assertEquals("0.0", AmountFormatter.formatUsage(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun formatUsageWithUnit_normalValues_returnsCorrectFormat() {
        assertEquals("123.4度", AmountFormatter.formatUsageWithUnit(123.45))
        assertEquals("0.0度", AmountFormatter.formatUsageWithUnit(0.0))
        assertEquals("123.4kWh", AmountFormatter.formatUsageWithUnit(123.45, "kWh"))
    }

    @Test
    fun parseAmount_validStrings_returnsCorrectValue() {
        assertEquals(123.45, AmountFormatter.parseAmount("123.45"), 0.001)
        assertEquals(123.45, AmountFormatter.parseAmount("¥123.45"), 0.001)
        assertEquals(123.45, AmountFormatter.parseAmount("1,23.45"), 0.001)
        assertEquals(0.0, AmountFormatter.parseAmount(""), 0.001)
        assertEquals(100.0, AmountFormatter.parseAmount("invalid", 100.0), 0.001)
    }

    @Test
    fun isValidAmount_variousValues_returnsCorrectResult() {
        assertTrue(AmountFormatter.isValidAmount(123.45))
        assertTrue(AmountFormatter.isValidAmount(0.0))
        assertFalse(AmountFormatter.isValidAmount(Double.NaN))
        assertFalse(AmountFormatter.isValidAmount(Double.POSITIVE_INFINITY))
        assertFalse(AmountFormatter.isValidAmount(-1.0))
    }

    @Test
    fun isValidUsage_variousValues_returnsCorrectResult() {
        assertTrue(AmountFormatter.isValidUsage(123.45))
        assertTrue(AmountFormatter.isValidUsage(0.0))
        assertFalse(AmountFormatter.isValidUsage(Double.NaN))
        assertFalse(AmountFormatter.isValidUsage(Double.POSITIVE_INFINITY))
        assertFalse(AmountFormatter.isValidUsage(-1.0))
    }
}