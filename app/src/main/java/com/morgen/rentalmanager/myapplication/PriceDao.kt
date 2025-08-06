package com.morgen.rentalmanager.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceDao {
    @Query("SELECT * FROM price_table WHERE id=1")
    fun getPriceFlow(): Flow<Price?>

    @Query("SELECT * FROM price_table WHERE id=1")
    suspend fun getPrice(): Price?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(price: Price)
} 