package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ShopSettings(
    val shopName: String = "Battery Repair Service",
    val batteryIdPrefix: String = "BAT",
    val batteryIdStart: Int = 1,
    val batteryIdPadding: Int = 4,
    val address: String = "",
    val phone: String = "",
    val email: String = ""
) : Parcelable