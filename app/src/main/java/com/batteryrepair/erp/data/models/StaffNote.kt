package com.batteryrepair.erp.data.models

import android.os.Parcelable
import com.batteryrepair.erp.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class StaffNote(
    val id: String = "",
    val batteryId: String = "",
    val note: String = "",
    val noteType: NoteType = NoteType.FOLLOWUP,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val createdByUser: User? = null // For joined data
) : Parcelable

enum class NoteType(val displayName: String, val colorRes: Int) {
    FOLLOWUP("Follow-up", R.color.status_delivered),
    REMINDER("Reminder", R.color.status_pending),
    ISSUE("Issue", R.color.status_not_repairable),
    RESOLVED("Resolved", R.color.status_ready)
}