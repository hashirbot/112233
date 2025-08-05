package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Customer(
    val id: String = "",
    val name: String = "",
    val mobile: String = "",
    val mobileSecondary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable