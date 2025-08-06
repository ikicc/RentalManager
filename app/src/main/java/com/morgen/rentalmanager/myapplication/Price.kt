package com.morgen.rentalmanager.myapplication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_table")
data class Price(
    @PrimaryKey val id:Int = 1,
    @ColumnInfo(name="water_price") val water: Double,
    @ColumnInfo(name="electricity_price") val electricity: Double,
    @ColumnInfo(name="privacy_keywords") val privacyKeywords: String = "[]"
) 