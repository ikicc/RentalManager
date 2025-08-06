package com.morgen.rentalmanager.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey
    @ColumnInfo(name = "room_number", index = true)
    val roomNumber: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "rent")
    val rent: Double
) 