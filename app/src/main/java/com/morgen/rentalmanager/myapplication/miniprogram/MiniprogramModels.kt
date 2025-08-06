package com.morgen.rentalmanager.myapplication.miniprogram

import com.google.gson.annotations.SerializedName

/**
 * 微信小程序导出的JSON数据结构
 */
data class MiniprogramData(
    @SerializedName("tenants")
    val tenants: List<MiniprogramTenant> = emptyList(),
    
    @SerializedName("bills")
    val bills: Map<String, Map<String, MiniprogramBill>> = emptyMap(),
    
    @SerializedName("prices")
    val prices: MiniprogramPrices? = null,
    
    @SerializedName("contracts")
    val contracts: Map<String, MiniprogramContract>? = null
)

/**
 * 微信小程序的租户数据结构
 */
data class MiniprogramTenant(
    @SerializedName("room_number")
    val roomNumber: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("rent")
    val rent: Double
)

/**
 * 微信小程序的价格数据结构
 */
data class MiniprogramPrices(
    @SerializedName("waterPrice")
    val waterPrice: Double,
    
    @SerializedName("electricityPrice")
    val electricityPrice: Double
)

/**
 * 微信小程序的账单数据结构
 */
data class MiniprogramBill(
    @SerializedName("water")
    val water: MiniprogramMeterData? = null,
    
    @SerializedName("electricity")
    val electricity: MiniprogramMeterData? = null,
    
    @SerializedName("rent")
    val rent: Double? = null,
    
    @SerializedName("total")
    val total: Double? = null,
    
    @SerializedName("extraFees")
    val extraFees: List<MiniprogramExtraFee>? = null,
    
    @SerializedName("meters")
    val meters: List<MiniprogramMeter>? = null,
    
    @SerializedName("extraData")
    val extraData: MiniprogramExtraData? = null,
    
    @SerializedName("date")
    val date: String? = null,
    
    @SerializedName("paid")
    val paid: Boolean? = null,
    
    @SerializedName("notes")
    val notes: String? = null
)

/**
 * 微信小程序的表计数据结构
 */
data class MiniprogramMeterData(
    @SerializedName("previous")
    val previous: Double? = null,
    
    @SerializedName("current")
    val current: Double? = null,
    
    @SerializedName("usage")
    val usage: Double? = null,
    
    @SerializedName("amount")
    val amount: Double? = null
)

/**
 * 微信小程序的额外费用数据结构
 */
data class MiniprogramExtraFee(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("amount")
    val amount: Double
)

/**
 * 微信小程序的表计数据结构
 */
data class MiniprogramMeter(
    @SerializedName("type")
    val type: String? = null,
    
    @SerializedName("name")
    val name: String? = null,
    
    @SerializedName("previous")
    val previous: String? = null,
    
    @SerializedName("current")
    val current: String? = null,
    
    @SerializedName("usage")
    val usage: String? = null,
    
    @SerializedName("amount")
    val amount: String? = null,
    
    @SerializedName("manualAmount")
    val manualAmount: Boolean? = null
) {
    // 辅助方法：将字符串转换为Double
    fun getPreviousAsDouble(): Double? = previous?.toDoubleOrNull()
    fun getCurrentAsDouble(): Double? = current?.toDoubleOrNull()
    fun getUsageAsDouble(): Double? = usage?.toDoubleOrNull()
    fun getAmountAsDouble(): Double? = amount?.toDoubleOrNull()
}

/**
 * 微信小程序的额外数据结构
 */
data class MiniprogramExtraData(
    @SerializedName("subWaterMeters")
    val subWaterMeters: List<MiniprogramMeter>? = null,
    
    @SerializedName("subElectricityMeters")
    val subElectricityMeters: List<MiniprogramMeter>? = null
)

/**
 * 微信小程序的合同数据结构
 */
data class MiniprogramContract(
    @SerializedName("startDate")
    val startDate: String? = null,
    
    @SerializedName("endDate")
    val endDate: String? = null,
    
    @SerializedName("deposit")
    val deposit: Double? = null,
    
    @SerializedName("terms")
    val terms: String? = null
)