package com.batteryrepair.erp.data.repository

import com.batteryrepair.erp.data.models.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class FirebaseRepository {
    
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // References
    private val usersRef = database.getReference("users")
    private val customersRef = database.getReference("customers")
    private val batteriesRef = database.getReference("batteries")
    private val statusHistoryRef = database.getReference("status_history")
    private val staffNotesRef = database.getReference("staff_notes")
    private val settingsRef = database.getReference("settings")
    
    // Authentication
    suspend fun signIn(username: String, password: String): Result<User> {
        return try {
            // For demo purposes, we'll use email format for Firebase Auth
            val email = "$username@batteryrepair.local"
            val result = auth.signInWithEmailAndPassword(email, password).await()
            
            result.user?.let { firebaseUser ->
                val userSnapshot = usersRef.child(firebaseUser.uid).get().await()
                val user = userSnapshot.getValue(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    // Create default user if not exists
                    val defaultUser = createDefaultUser(firebaseUser.uid, username)
                    usersRef.child(firebaseUser.uid).setValue(defaultUser).await()
                    Result.success(defaultUser)
                }
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createDefaultUser(uid: String, username: String): User {
        val role = when (username) {
            "admin" -> UserRole.ADMIN
            "staff" -> UserRole.SHOP_STAFF
            else -> UserRole.TECHNICIAN
        }
        return User(
            id = uid,
            username = username,
            fullName = username.replaceFirstChar { it.uppercase() },
            role = role,
            isActive = true
        )
    }
    
    fun getCurrentUser(): User? {
        return null // Will be loaded asynchronously in activities
    }
    
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val snapshot = usersRef.child(userId).get().await()
            val user = snapshot.getValue(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    // Battery Operations
    suspend fun createBattery(battery: Battery): Result<String> {
        return try {
            val batteryId = generateBatteryId()
            val batteryWithId = battery.copy(id = batteriesRef.push().key ?: "", batteryId = batteryId)
            
            batteriesRef.child(batteryWithId.id).setValue(batteryWithId).await()
            
            // Add initial status history
            addStatusHistory(BatteryStatusHistory(
                id = statusHistoryRef.push().key ?: "",
                batteryId = batteryWithId.id,
                status = BatteryStatus.RECEIVED,
                comments = "Battery received from customer${if (battery.isPickup) " - Pickup service" else ""}",
                updatedBy = auth.currentUser?.uid ?: ""
            ))
            
            Result.success(batteryWithId.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateBatteryStatus(batteryId: String, status: BatteryStatus, comments: String, servicePrice: Double? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status
            )
            servicePrice?.let { updates["servicePrice"] = it }
            
            batteriesRef.child(batteryId).updateChildren(updates).await()
            
            // Add status history
            addStatusHistory(BatteryStatusHistory(
                id = statusHistoryRef.push().key ?: "",
                batteryId = batteryId,
                status = status,
                comments = comments,
                updatedBy = auth.currentUser?.uid ?: ""
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getBatteries(): Flow<List<Battery>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteries = mutableListOf<Battery>()
                val totalBatteries = snapshot.childrenCount.toInt()
                var processedCount = 0
                
                for (batterySnapshot in snapshot.children) {
                    batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                        // Load customer data for each battery
                        repositoryScope.launch {
                            val customerResult = getCustomer(battery.customerId)
                            customerResult.fold(
                                onSuccess = { customer ->
                                    synchronized(batteries) {
                                        batteries.add(battery.copy(customer = customer))
                                        processedCount++
                                        if (processedCount == totalBatteries) {
                                            trySend(batteries.sortedByDescending { it.inwardDate })
                                        }
                                    }
                                },
                                onFailure = {
                                    synchronized(batteries) {
                                        batteries.add(battery)
                                        processedCount++
                                        if (processedCount == totalBatteries) {
                                            trySend(batteries.sortedByDescending { it.inwardDate })
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                if (totalBatteries == 0) {
                    trySend(emptyList())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        batteriesRef.addValueEventListener(listener)
        awaitClose { batteriesRef.removeEventListener(listener) }
    }
    
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>> = callbackFlow {
        val query = batteriesRef.orderByChild("status").equalTo(status.name)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val batteries = mutableListOf<Battery>()
                val totalBatteries = snapshot.childrenCount.toInt()
                var processedCount = 0
                
                for (batterySnapshot in snapshot.children) {
                    batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                        // Load customer data for each battery
                        repositoryScope.launch {
                            val customerResult = getCustomer(battery.customerId)
                            customerResult.fold(
                                onSuccess = { customer ->
                                    synchronized(batteries) {
                                        batteries.add(battery.copy(customer = customer))
                                        processedCount++
                                        if (processedCount == totalBatteries) {
                                            trySend(batteries.sortedByDescending { it.inwardDate })
                                        }
                                    }
                                },
                                onFailure = {
                                    synchronized(batteries) {
                                        batteries.add(battery)
                                        processedCount++
                                        if (processedCount == totalBatteries) {
                                            trySend(batteries.sortedByDescending { it.inwardDate })
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                if (totalBatteries == 0) {
                    trySend(emptyList())
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    suspend fun searchBatteries(query: String): Result<List<Battery>> {
        return try {
            val snapshot = batteriesRef.get().await()
            val batteries = mutableListOf<Battery>()
            
            for (batterySnapshot in snapshot.children) {
                batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                    if (battery.batteryId.contains(query, ignoreCase = true) ||
                        battery.batteryType.contains(query, ignoreCase = true)) {
                        batteries.add(battery)
                    }
                }
            }
            
            Result.success(batteries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Customer Operations
    suspend fun createOrGetCustomer(customer: Customer): Result<String> {
        return try {
            // Check if customer exists by mobile number
            val existingCustomer = customersRef.orderByChild("mobile").equalTo(customer.mobile).get().await()
            
            if (existingCustomer.exists()) {
                val customerId = existingCustomer.children.first().key ?: ""
                Result.success(customerId)
            } else {
                val customerId = customersRef.push().key ?: ""
                val customerWithId = customer.copy(id = customerId)
                customersRef.child(customerId).setValue(customerWithId).await()
                Result.success(customerId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCustomer(customerId: String): Result<Customer> {
        return try {
            val snapshot = customersRef.child(customerId).get().await()
            val customer = snapshot.getValue(Customer::class.java)
            if (customer != null) {
                Result.success(customer)
            } else {
                Result.failure(Exception("Customer not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Status History
    private suspend fun addStatusHistory(statusHistory: BatteryStatusHistory) {
        statusHistoryRef.child(statusHistory.id).setValue(statusHistory).await()
    }
    
    fun getStatusHistory(batteryId: String): Flow<List<BatteryStatusHistory>> = callbackFlow {
        val query = statusHistoryRef.orderByChild("batteryId").equalTo(batteryId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val history = mutableListOf<BatteryStatusHistory>()
                for (historySnapshot in snapshot.children) {
                    historySnapshot.getValue(BatteryStatusHistory::class.java)?.let { item ->
                        history.add(item)
                    }
                }
                trySend(history.sortedByDescending { it.updatedAt })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    // Staff Notes
    suspend fun addStaffNote(note: StaffNote): Result<Unit> {
        return try {
            val noteWithId = note.copy(id = staffNotesRef.push().key ?: "")
            staffNotesRef.child(noteWithId.id).setValue(noteWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getStaffNotes(batteryId: String): Flow<List<StaffNote>> = callbackFlow {
        val query = staffNotesRef.orderByChild("batteryId").equalTo(batteryId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notes = mutableListOf<StaffNote>()
                for (noteSnapshot in snapshot.children) {
                    noteSnapshot.getValue(StaffNote::class.java)?.let { note ->
                        notes.add(note)
                    }
                }
                trySend(notes.sortedByDescending { it.createdAt })
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }
    
    // Utility Functions
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersRef.get().await()
            val users = mutableListOf<User>()
            
            for (userSnapshot in snapshot.children) {
                userSnapshot.getValue(User::class.java)?.let { user ->
                    users.add(user)
                }
            }
            
            Result.success(users.sortedBy { it.fullName })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getShopSettings(): Result<ShopSettings> {
        return try {
            val snapshot = settingsRef.get().await()
            val shopName = snapshot.child("shop_name").getValue(String::class.java) ?: "Battery Repair Service"
            val batteryIdPrefix = snapshot.child("battery_id_prefix").getValue(String::class.java) ?: "BAT"
            val batteryIdStart = snapshot.child("battery_id_start").getValue(String::class.java)?.toIntOrNull() ?: 1
            val batteryIdPadding = snapshot.child("battery_id_padding").getValue(String::class.java)?.toIntOrNull() ?: 4
            
            val settings = ShopSettings(
                shopName = shopName,
                batteryIdPrefix = batteryIdPrefix,
                batteryIdStart = batteryIdStart,
                batteryIdPadding = batteryIdPadding
            )
            
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateShopSettings(shopName: String, prefix: String, start: Int, padding: Int): Result<Unit> {
        return try {
            val updates = mapOf(
                "shop_name" to shopName,
                "battery_id_prefix" to prefix,
                "battery_id_start" to start.toString(),
                "battery_id_padding" to padding.toString()
            )
            
            settingsRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createUser(username: String, fullName: String, password: String, role: UserRole): Result<Unit> {
        return try {
            val email = "$username@batteryrepair.local"
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            
            result.user?.let { firebaseUser ->
                val user = User(
                    id = firebaseUser.uid,
                    username = username,
                    fullName = fullName,
                    role = role,
                    isActive = true
                )
                
                usersRef.child(firebaseUser.uid).setValue(user).await()
                Result.success(Unit)
            } ?: Result.failure(Exception("Failed to create user"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("User with this username already exists"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, fullName: String, password: String?, role: UserRole): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "fullName" to fullName,
                "role" to role
            )
            
            usersRef.child(userId).updateChildren(updates).await()
            
            // Update password if provided
            password?.let { newPassword ->
                val currentUser = auth.currentUser
                if (currentUser?.uid == userId) {
                    currentUser.updatePassword(newPassword).await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // Remove user data from database
            usersRef.child(userId).removeValue().await()
            
            // Note: Firebase Auth user deletion requires the user to be currently signed in
            // In a production app, you'd need admin SDK for this
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleUserStatus(userId: String, isActive: Boolean): Result<Unit> {
        return try {
            usersRef.child(userId).child("isActive").setValue(isActive).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createBackup(): Result<BackupData> {
        return try {
            // Get all data
            val batteriesSnapshot = batteriesRef.get().await()
            val customersSnapshot = customersRef.get().await()
            val usersSnapshot = usersRef.get().await()
            val statusHistorySnapshot = statusHistoryRef.get().await()
            val staffNotesSnapshot = staffNotesRef.get().await()
            val settingsSnapshot = settingsRef.get().await()
            
            val batteries = mutableListOf<Battery>()
            val customers = mutableListOf<Customer>()
            val users = mutableListOf<User>()
            val statusHistory = mutableListOf<BatteryStatusHistory>()
            val staffNotes = mutableListOf<StaffNote>()
            
            // Parse batteries
            for (batterySnapshot in batteriesSnapshot.children) {
                batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                    batteries.add(battery)
                }
            }
            
            // Parse customers
            for (customerSnapshot in customersSnapshot.children) {
                customerSnapshot.getValue(Customer::class.java)?.let { customer ->
                    customers.add(customer)
                }
            }
            
            // Parse users
            for (userSnapshot in usersSnapshot.children) {
                userSnapshot.getValue(User::class.java)?.let { user ->
                    users.add(user)
                }
            }
            
            // Parse status history
            for (historySnapshot in statusHistorySnapshot.children) {
                historySnapshot.getValue(BatteryStatusHistory::class.java)?.let { history ->
                    statusHistory.add(history)
                }
            }
            
            // Parse staff notes
            for (noteSnapshot in staffNotesSnapshot.children) {
                noteSnapshot.getValue(StaffNote::class.java)?.let { note ->
                    staffNotes.add(note)
                }
            }
            
            // Parse settings
            val shopName = settingsSnapshot.child("shop_name").getValue(String::class.java) ?: "Battery Repair Service"
            val batteryIdPrefix = settingsSnapshot.child("battery_id_prefix").getValue(String::class.java) ?: "BAT"
            val batteryIdStart = settingsSnapshot.child("battery_id_start").getValue(String::class.java)?.toIntOrNull() ?: 1
            val batteryIdPadding = settingsSnapshot.child("battery_id_padding").getValue(String::class.java)?.toIntOrNull() ?: 4
            
            val settings = ShopSettings(
                shopName = shopName,
                batteryIdPrefix = batteryIdPrefix,
                batteryIdStart = batteryIdStart,
                batteryIdPadding = batteryIdPadding
            )
            
            val backupData = BackupData(
                batteries = batteries,
                customers = customers,
                users = users,
                statusHistory = statusHistory,
                staffNotes = staffNotes,
                settings = settings
            )
            
            Result.success(backupData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateBatteryId(): String {
        return try {
            val prefix = getSetting("battery_id_prefix", "BAT")
            val startNum = getSetting("battery_id_start", "1").toInt()
            val padding = getSetting("battery_id_padding", "4").toInt()
            
            // Get the last battery to determine next number
            val snapshot = batteriesRef.get().await()
            var maxNum = startNum - 1
            
            for (batterySnapshot in snapshot.children) {
                val battery = batterySnapshot.getValue(Battery::class.java)
                battery?.batteryId?.let { batteryId ->
                    val numPart = batteryId.removePrefix(prefix)
                    val num = numPart.toIntOrNull()
                    if (num != null && num > maxNum) {
                        maxNum = num
                    }
                }
            }
            
            val nextNum = maxNum + 1
            "$prefix${nextNum.toString().padStart(padding, '0')}"
        } catch (e: Exception) {
            "BAT${System.currentTimeMillis().toString().takeLast(4)}"
        }
    }
    
    // Initialize default settings if they don't exist
    suspend fun initializeDefaultSettings() {
        try {
            val settingsSnapshot = settingsRef.get().await()
            if (!settingsSnapshot.exists()) {
                val defaultSettings = mapOf(
                    "battery_id_prefix" to "BAT",
                    "battery_id_start" to "1",
                    "battery_id_padding" to "4",
                    "shop_name" to "Battery Repair Service"
                )
                settingsRef.setValue(defaultSettings).await()
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    // Create default users for demo
    suspend fun createDefaultUsers() {
        try {
            val defaultUsers = listOf(
                Triple("admin@batteryrepair.local", "admin123", "admin"),
                Triple("staff@batteryrepair.local", "staff123", "staff"),
                Triple("technician@batteryrepair.local", "tech123", "technician")
            )
            
            for ((email, password, username) in defaultUsers) {
                try {
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    result.user?.let { firebaseUser ->
                        val user = createDefaultUser(firebaseUser.uid, username)
                        usersRef.child(firebaseUser.uid).setValue(user).await()
                    }
                } catch (e: Exception) {
                    // User might already exist, continue with next user
                }
            }
        } catch (e: Exception) {
            // Handle top-level errors silently for initialization
        }
    }


    private suspend fun getSetting(key: String, defaultValue: String): String {
        return try {
            val snapshot = settingsRef.child(key).get().await()
            snapshot.getValue(String::class.java) ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    // Statistics
    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val batteriesSnapshot = batteriesRef.get().await()
            val batteries = mutableListOf<Battery>()
            
            for (batterySnapshot in batteriesSnapshot.children) {
                batterySnapshot.getValue(Battery::class.java)?.let { battery ->
                    batteries.add(battery)
                }
            }
            
            val stats = DashboardStats(
                totalBatteries = batteries.size,
                pendingBatteries = batteries.count { it.status in listOf(BatteryStatus.RECEIVED, BatteryStatus.PENDING) },
                readyBatteries = batteries.count { it.status == BatteryStatus.READY },
                completedBatteries = batteries.count { it.status in listOf(BatteryStatus.DELIVERED, BatteryStatus.RETURNED) },
                notRepairableBatteries = batteries.count { it.status == BatteryStatus.NOT_REPAIRABLE },
                totalRevenue = batteries.filter { it.status in listOf(BatteryStatus.DELIVERED, BatteryStatus.RETURNED) }
                    .sumOf { it.servicePrice + if (it.isPickup) it.pickupCharge else 0.0 }
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class DashboardStats(
    val totalBatteries: Int,
    val pendingBatteries: Int,
    val readyBatteries: Int,
    val completedBatteries: Int,
    val notRepairableBatteries: Int,
    val totalRevenue: Double
)