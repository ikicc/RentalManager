package com.morgen.rentalmanager.ui.billdetail

import com.morgen.rentalmanager.myapplication.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever
import kotlinx.coroutines.flow.first

/**
 * BillDetailViewModel的单元测试
 * 用于验证数据加载和错误处理逻辑
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BillDetailViewModelTest {

    @Test
    fun `loadBillDetail should handle missing tenant`() = runTest {
        // Arrange
        val mockRepository = mock(TenantRepository::class.java)
        whenever(mockRepository.getTenantByRoomNumber("101")).thenReturn(null)
        
        val viewModel = BillDetailViewModel(mockRepository)
        
        // Act
        viewModel.loadBillDetail("101", "2024-01")
        
        // Assert
        val state = viewModel.uiState.first()
        assertTrue("State should be NotFound", state is BillDetailUiState.NotFound)
    }
    
    @Test
    fun `loadBillDetail should handle missing bill`() = runTest {
        // Arrange
        val mockRepository = mock(TenantRepository::class.java)
        val tenant = Tenant(roomNumber = "101", name = "张三", rent = 1000.0)
        
        whenever(mockRepository.getTenantByRoomNumber("101")).thenReturn(tenant)
        whenever(mockRepository.getBillByRoomAndMonth("101", "2024-01")).thenReturn(null)
        
        val viewModel = BillDetailViewModel(mockRepository)
        
        // Act
        viewModel.loadBillDetail("101", "2024-01")
        
        // Assert
        val state = viewModel.uiState.first()
        assertTrue("State should be NotFound", state is BillDetailUiState.NotFound)
    }
    
    @Test
    fun `loadBillDetail should handle invalid parameters`() = runTest {
        // Arrange
        val mockRepository = mock(TenantRepository::class.java)
        val viewModel = BillDetailViewModel(mockRepository)
        
        // Act
        viewModel.loadBillDetail("", "2024-01")
        
        // Assert
        val state = viewModel.uiState.first()
        assertTrue("State should be Error", state is BillDetailUiState.Error)
        if (state is BillDetailUiState.Error) {
            assertTrue("Error message should mention invalid parameters", 
                state.message.contains("参数无效"))
        }
    }
    
    @Test
    fun `loadBillDetail should calculate amounts correctly`() = runTest {
        // Arrange
        val mockRepository = mock(TenantRepository::class.java)
        val tenant = Tenant(roomNumber = "101", name = "张三", rent = 1000.0)
        val bill = Bill(
            billId = 1,
            tenantRoomNumber = "101",
            month = "2024-01",
            totalAmount = 1350.0
        )
        val billDetails = listOf(
            BillDetail(
                detailId = 1,
                parentBillId = 1,
                type = "water",
                name = "水费",
                usage = 10.0,
                pricePerUnit = 5.0,
                amount = 50.0
            ),
            BillDetail(
                detailId = 2,
                parentBillId = 1,
                type = "electricity",
                name = "电费",
                usage = 100.0,
                pricePerUnit = 3.0,
                amount = 300.0
            )
        )
        
        whenever(mockRepository.getTenantByRoomNumber("101")).thenReturn(tenant)
        whenever(mockRepository.getBillByRoomAndMonth("101", "2024-01")).thenReturn(bill)
        whenever(mockRepository.getBillDetailsByBillId(1)).thenReturn(billDetails)
        whenever(mockRepository.getMeterDisplayName("水费")).thenReturn("水费")
        whenever(mockRepository.getMeterDisplayName("电费")).thenReturn("电费")
        
        val viewModel = BillDetailViewModel(mockRepository)
        
        // Act
        viewModel.loadBillDetail("101", "2024-01")
        
        // Assert
        val state = viewModel.uiState.first()
        assertTrue("State should be Success", state is BillDetailUiState.Success)
        
        if (state is BillDetailUiState.Success) {
            val data = state.billDetail
            assertEquals("Room number should match", "101", data.roomNumber)
            assertEquals("Tenant name should match", "张三", data.tenantName)
            assertEquals("Rent should match", 1000.0, data.rent, 0.01)
            assertEquals("Water amount should match", 50.0, data.waterAmount, 0.01)
            assertEquals("Electricity amount should match", 300.0, data.electricityAmount, 0.01)
            assertEquals("Total amount should match", 1350.0, data.totalAmount, 0.01)
            assertEquals("Water usage should match", 10.0, data.waterUsage, 0.01)
            assertEquals("Electricity usage should match", 100.0, data.electricityUsage, 0.01)
        }
    }
    
    @Test
    fun `loadBillDetail should apply custom meter names`() = runTest {
        // Arrange
        val mockRepository = mock(TenantRepository::class.java)
        val tenant = Tenant(roomNumber = "101", name = "张三", rent = 1000.0)
        val bill = Bill(
            billId = 1,
            tenantRoomNumber = "101",
            month = "2024-01",
            totalAmount = 1350.0
        )
        val billDetails = listOf(
            BillDetail(
                detailId = 1,
                parentBillId = 1,
                type = "water",
                name = "额外水表1",
                usage = 10.0,
                pricePerUnit = 5.0,
                amount = 50.0
            ),
            BillDetail(
                detailId = 2,
                parentBillId = 1,
                type = "electricity",
                name = "额外电表1",
                usage = 100.0,
                pricePerUnit = 3.0,
                amount = 300.0
            )
        )
        
        whenever(mockRepository.getTenantByRoomNumber("101")).thenReturn(tenant)
        whenever(mockRepository.getBillByRoomAndMonth("101", "2024-01")).thenReturn(bill)
        whenever(mockRepository.getBillDetailsByBillId(1)).thenReturn(billDetails)
        // Mock custom meter names
        whenever(mockRepository.getMeterDisplayName("额外水表1")).thenReturn("公共水表")
        whenever(mockRepository.getMeterDisplayName("额外电表1")).thenReturn("空调电表")
        
        val viewModel = BillDetailViewModel(mockRepository)
        
        // Act
        viewModel.loadBillDetail("101", "2024-01")
        
        // Assert
        val state = viewModel.uiState.first()
        assertTrue("State should be Success", state is BillDetailUiState.Success)
        
        if (state is BillDetailUiState.Success) {
            val data = state.billDetail
            assertEquals("Should have 2 details", 2, data.details.size)
            
            // Verify custom names are applied
            val waterDetail = data.details.find { it.type == "water" }
            assertNotNull("Water detail should exist", waterDetail)
            assertEquals("Water detail should have custom name", "公共水表", waterDetail?.name)
            
            val electricityDetail = data.details.find { it.type == "electricity" }
            assertNotNull("Electricity detail should exist", electricityDetail)
            assertEquals("Electricity detail should have custom name", "空调电表", electricityDetail?.name)
        }
        
        // Verify that getMeterDisplayName was called for each detail
        verify(mockRepository).getMeterDisplayName("额外水表1")
        verify(mockRepository).getMeterDisplayName("额外电表1")
    }
}