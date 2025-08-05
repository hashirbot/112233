package com.batteryrepair.erp.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.TECHNICIAN,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    SHOP_STAFF("Shop Staff"),
    TECHNICIAN("Technician")
}