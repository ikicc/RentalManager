package com.morgen.rentalmanager.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bill_details",
    foreignKeys = [
        ForeignKey(
            entity = Bill::class,
            parentColumns = ["billId"],
            childColumns = ["parent_bill_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BillDetail(
    @PrimaryKey(autoGenerate = true)
    val detailId: Int = 0,

    @ColumnInfo(name = "parent_bill_id", index = true)
    val parentBillId: Int,

    @ColumnInfo(name = "type")
    val type: String, // "water", "electricity", "extra"

    @ColumnInfo(name = "name")
    val name: String, // "主卧电费", "卫生费"

    @ColumnInfo(name = "previous_reading")
    val previousReading: Double? = null,

    @ColumnInfo(name = "current_reading")
    val currentReading: Double? = null,

    @ColumnInfo(name = "usage")
    val usage: Double? = null,

    @ColumnInfo(name = "price_per_unit")
    val pricePerUnit: Double? = null,

    @ColumnInfo(name = "amount")
    val amount: Double
) 