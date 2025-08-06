package com.morgen.rentalmanager.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Insert
    suspend fun insertBill(bill: Bill): Long // Returns the new billId

    @Insert
    suspend fun insertBillDetails(details: List<BillDetail>)

    @Update
    suspend fun updateBill(bill: Bill)

    @Query("DELETE FROM bill_details WHERE parent_bill_id = :billId")
    suspend fun deleteDetailsForBill(billId: Int)

    @Query("SELECT * FROM bills WHERE tenant_room_number = :tenantRoomNumber AND month = :month")
    suspend fun getBillByTenantAndMonth(tenantRoomNumber: String, month: String): Bill?

    @Transaction
    @Query("SELECT * FROM bills WHERE billId = :billId")
    fun getBillWithDetails(billId: Long): Flow<BillWithDetails>

    @Transaction
    @Query("SELECT * FROM bills WHERE tenant_room_number = :tenantRoomNumber AND month = :month")
    fun getBillWithDetailsByTenantAndMonthFlow(tenantRoomNumber: String, month: String): Flow<BillWithDetails?>

    @Transaction
    @Query("SELECT * FROM bills WHERE tenant_room_number = :tenantRoomNumber AND month = :month")
    suspend fun getBillWithDetailsByTenantAndMonth(tenantRoomNumber: String, month: String): BillWithDetails?

    @Transaction
    @Query("SELECT * FROM bills ORDER BY created_date DESC")
    fun getAllBillsWithDetails(): Flow<List<BillWithDetails>>

    @Transaction
    suspend fun upsertBillTransaction(bill: Bill, details: List<BillDetail>) {
        val existingBill = getBillByTenantAndMonth(bill.tenantRoomNumber, bill.month)
        if (existingBill != null) {
            // Update existing bill
            val updatedBill = bill.copy(billId = existingBill.billId)
            updateBill(updatedBill)
            deleteDetailsForBill(existingBill.billId)
            val detailsWithId = details.map { it.copy(parentBillId = existingBill.billId) }
            insertBillDetails(detailsWithId)
        } else {
            // Insert new bill
            val newBillId = insertBill(bill)
            val detailsWithId = details.map { it.copy(parentBillId = newBillId.toInt()) }
            insertBillDetails(detailsWithId)
        }
    }

    @Query("DELETE FROM bills WHERE tenant_room_number = :roomNumber")
    suspend fun deleteBillsByTenant(roomNumber: String)
    
    @Query("SELECT * FROM bills ORDER BY created_date DESC LIMIT :limit")
    suspend fun getRecentBills(limit: Int): List<Bill>
    
    @Query("SELECT * FROM bill_details WHERE parent_bill_id = :billId")
    suspend fun getBillDetailsByBillId(billId: Int): List<BillDetail>
} 