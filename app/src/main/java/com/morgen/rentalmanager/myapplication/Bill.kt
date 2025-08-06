package com.morgen.rentalmanager.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Tenant::class,
            parentColumns = ["room_number"],
            childColumns = ["tenant_room_number"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val billId: Int = 0,

    @ColumnInfo(name = "tenant_room_number", index = true)
    val tenantRoomNumber: String,

    @ColumnInfo(name = "month")
    val month: String, // Format: "YYYY-MM"

    @ColumnInfo(name = "total_amount")
    val totalAmount: Double,

    @ColumnInfo(name = "created_date")
    val createdDate: Long = System.currentTimeMillis()
) 