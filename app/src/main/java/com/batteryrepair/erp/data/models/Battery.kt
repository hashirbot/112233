package com.batteryrepair.erp.data.models

import android.os.Parcelable
import com.batteryrepair.erp.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class Battery(
    val id: String = "",
    val batteryId: String = "",
    val customerId: String = "",
    val batteryType: String = "",
    val voltage: String = "",
    val capacity: String = "",
    val status: BatteryStatus = BatteryStatus.RECEIVED, // ✅ enum used here
    val inwardDate: Long = System.currentTimeMillis(),
    val servicePrice: Double = 0.0,
    val pickupCharge: Double = 0.0,
    val isPickup: Boolean = false,
    val customer: Customer? = null
) : Parcelable

enum class BatteryStatus {
    RECEIVED,
    PENDING,
    READY,
    DELIVERED,
    RETURNED,
    NOT_REPAIRABLE
}

fun BatteryStatus.displayName(): String = when (this) {
    BatteryStatus.RECEIVED -> "Received"
    BatteryStatus.PENDING -> "Pending"
    BatteryStatus.READY -> "Ready"
    BatteryStatus.DELIVERED -> "Delivered"
    BatteryStatus.RETURNED -> "Returned"
    BatteryStatus.NOT_REPAIRABLE -> "Not Repairable"
}

fun BatteryStatus.getColorResource(): Int = when (this) {
    BatteryStatus.RECEIVED -> com.batteryrepair.erp.R.color.status_received
    BatteryStatus.PENDING -> com.batteryrepair.erp.R.color.status_pending
    BatteryStatus.READY -> com.batteryrepair.erp.R.color.status_ready
    BatteryStatus.DELIVERED -> com.batteryrepair.erp.R.color.status_delivered
    BatteryStatus.RETURNED -> com.batteryrepair.erp.R.color.status_returned
    BatteryStatus.NOT_REPAIRABLE -> com.batteryrepair.erp.R.color.status_not_repairable
}