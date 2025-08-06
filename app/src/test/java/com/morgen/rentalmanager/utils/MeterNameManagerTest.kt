package com.morgen.rentalmanager.utils

import com.morgen.rentalmanager.myapplication.MeterNameConfig
import com.morgen.rentalmanager.myapplication.MeterNameConfigDao
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * MeterNameManager的单元测试
 * 验证自定义名称映射功能
 */
class MeterNameManagerTest {

    @Test
    fun `getDisplayName should return custom name when available`() = runTest {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        val defaultName = "额外水表1"
        val customName = "厨房水表"
        
        `when`(mockDao.getCustomNameByDefault(defaultName)).thenReturn(customName)
        
        // Act
        val result = manager.getDisplayName(defaultName)
        
        // Assert
        assertEquals(customName, result)
        verify(mockDao).getCustomNameByDefault(defaultName)
    }

    @Test
    fun `getDisplayName should return default name when custom name not available`() = runTest {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        val defaultName = "额外水表1"
        
        `when`(mockDao.getCustomNameByDefault(defaultName)).thenReturn(null)
        
        // Act
        val result = manager.getDisplayName(defaultName)
        
        // Assert
        assertEquals(defaultName, result)
        verify(mockDao).getCustomNameByDefault(defaultName)
    }

    @Test
    fun `saveCustomName should validate and save name successfully`() = runTest {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        val defaultName = "额外水表1"
        val customName = "厨房水表"
        val meterType = "water"
        
        // Act
        val result = manager.saveCustomName(defaultName, customName, meterType)
        
        // Assert
        assertTrue(result.isSuccess)
        verify(mockDao).insertOrUpdate(any(MeterNameConfig::class.java))
    }

    @Test
    fun `saveCustomName should fail for empty name`() = runTest {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        val defaultName = "额外水表1"
        val customName = ""
        val meterType = "water"
        
        // Act
        val result = manager.saveCustomName(defaultName, customName, meterType)
        
        // Assert
        assertTrue(result.isFailure)
        verifyNoInteractions(mockDao)
    }

    @Test
    fun `validateCustomName should return true for valid names`() {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        
        // Act & Assert
        assertTrue(manager.validateCustomName("厨房水表"))
        assertTrue(manager.validateCustomName("A"))
        assertTrue(manager.validateCustomName("12345678901234567890")) // 20 characters
    }

    @Test
    fun `validateCustomName should return false for invalid names`() {
        // Arrange
        val mockDao = mock(MeterNameConfigDao::class.java)
        val manager = MeterNameManager(mockDao)
        
        // Act & Assert
        assertFalse(manager.validateCustomName(""))
        assertFalse(manager.validateCustomName("   "))
        assertFalse(manager.validateCustomName("123456789012345678901")) // 21 characters
    }
}