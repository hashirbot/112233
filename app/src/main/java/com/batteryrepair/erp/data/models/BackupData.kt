package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BackupData(
    val batteries: List<Battery> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val users: List<User> = emptyList(),
    val statusHistory: List<BatteryStatusHistory> = emptyList(),
    val staffNotes: List<StaffNote> = emptyList(),
    val settings: ShopSettings = ShopSettings(),
    val backupDate: Long = System.currentTimeMillis(),
    val version: String = "1.0"
) : Parcelable