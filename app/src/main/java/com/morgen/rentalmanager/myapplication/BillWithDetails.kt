package com.morgen.rentalmanager.myapplication

import androidx.room.Embedded
import androidx.room.Relation

data class BillWithDetails(
    @Embedded val bill: Bill,
    @Relation(
        parentColumn = "billId",
        entityColumn = "parent_bill_id"
    )
    val details: List<BillDetail>
) 