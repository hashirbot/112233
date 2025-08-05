package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatteryStatusHistory(
    val id: String = "",
    val batteryId: String = "",
    val status: BatteryStatus = BatteryStatus.RECEIVED,
    val comments: String = "",
    val updatedBy: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedByUser: User? = null // For joined data
) : Parcelable