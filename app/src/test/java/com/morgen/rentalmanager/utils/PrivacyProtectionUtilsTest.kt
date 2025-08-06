package com.morgen.rentalmanager.utils

import org.junit.Test
import org.junit.Assert.*

class PrivacyProtectionUtilsTest {

    @Test
    fun `applyPrivacyProtection should remove keywords from room number`() {
        // Given
        val roomNumber = "阳光小区101"
        val keywords = listOf("阳光")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("小区101", result)
    }

    @Test
    fun `applyPrivacyProtection should remove multiple keywords`() {
        // Given
        val roomNumber = "阳光小区A栋101"
        val keywords = listOf("阳光", "A栋")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("小区101", result)
    }

    @Test
    fun `applyPrivacyProtection should return original when no keywords match`() {
        // Given
        val roomNumber = "阳光小区101"
        val keywords = listOf("星光", "月光")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("阳光小区101", result)
    }

    @Test
    fun `applyPrivacyProtection should return original when keywords list is empty`() {
        // Given
        val roomNumber = "阳光小区101"
        val keywords = emptyList<String>()
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("阳光小区101", result)
    }

    @Test
    fun `applyPrivacyProtection should return original when room number is blank`() {
        // Given
        val roomNumber = ""
        val keywords = listOf("阳光")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("", result)
    }

    @Test
    fun `applyPrivacyProtection should handle partial keyword matches`() {
        // Given
        val roomNumber = "阳光小区阳光大厦101"
        val keywords = listOf("阳光")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("小区大厦101", result)
    }

    @Test
    fun `applyPrivacyProtection should handle spaces in room number`() {
        // Given
        val roomNumber = "阳光小区 101"
        val keywords = listOf("阳光小区")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("101", result)
    }

    @Test
    fun `applyPrivacyProtection should handle spaces in keywords`() {
        // Given
        val roomNumber = "阳光小区A栋101"
        val keywords = listOf("阳光 小区")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("A栋101", result)
    }

    @Test
    fun `applyPrivacyProtection should handle multiple spaces`() {
        // Given
        val roomNumber = "阳光小区  A栋  101"
        val keywords = listOf("阳光小区", "A栋")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("101", result)
    }

    @Test
    fun `applyPrivacyProtection should handle mixed space patterns`() {
        // Given
        val roomNumber = "阳光 小区101"
        val keywords = listOf("阳光小区")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("101", result)
    }

    @Test
    fun `applyPrivacyProtection should ignore blank keywords`() {
        // Given
        val roomNumber = "阳光小区101"
        val keywords = listOf("阳光", "", "  ")
        
        // When
        val result = PrivacyProtectionUtils.applyPrivacyProtection(roomNumber, keywords)
        
        // Then
        assertEquals("小区101", result)
    }

    @Test
    fun `isValidKeyword should return true for valid keyword`() {
        // Given
        val keyword = "阳光小区"
        
        // When
        val result = PrivacyProtectionUtils.isValidKeyword(keyword)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidKeyword should return false for blank keyword`() {
        // Given
        val keyword = ""
        
        // When
        val result = PrivacyProtectionUtils.isValidKeyword(keyword)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidKeyword should return false for keyword too long`() {
        // Given
        val keyword = "这是一个非常非常非常非常长的关键字超过了二十个字符的限制"
        
        // When
        val result = PrivacyProtectionUtils.isValidKeyword(keyword)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidKeywordList should return true for valid list`() {
        // Given
        val keywords = listOf("阳光", "小区", "大厦")
        
        // When
        val result = PrivacyProtectionUtils.isValidKeywordList(keywords)
        
        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidKeywordList should return false for list too large`() {
        // Given
        val keywords = (1..11).map { "关键字$it" }
        
        // When
        val result = PrivacyProtectionUtils.isValidKeywordList(keywords)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidKeywordList should return false for list with invalid keywords`() {
        // Given
        val keywords = listOf("阳光", "", "小区")
        
        // When
        val result = PrivacyProtectionUtils.isValidKeywordList(keywords)
        
        // Then
        assertFalse(result)
    }

    @Test
    fun `cleanKeywords should remove blank and duplicate keywords`() {
        // Given
        val keywords = listOf("阳光", "", "小区", "  ", "阳光", "大厦")
        
        // When
        val result = PrivacyProtectionUtils.cleanKeywords(keywords)
        
        // Then
        assertEquals(listOf("阳光", "小区", "大厦"), result)
    }

    @Test
    fun `cleanKeywords should limit to 10 keywords`() {
        // Given
        val keywords = (1..15).map { "关键字$it" }
        
        // When
        val result = PrivacyProtectionUtils.cleanKeywords(keywords)
        
        // Then
        assertEquals(10, result.size)
        assertEquals((1..10).map { "关键字$it" }, result)
    }

    @Test
    fun `cleanKeywords should trim whitespace`() {
        // Given
        val keywords = listOf("  阳光  ", " 小区 ", "大厦")
        
        // When
        val result = PrivacyProtectionUtils.cleanKeywords(keywords)
        
        // Then
        assertEquals(listOf("阳光", "小区", "大厦"), result)
    }
}