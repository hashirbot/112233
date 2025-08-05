package com.batteryrepair.erp

import android.app.Application
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.batteryrepair.erp.data.repository.FirebaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BatteryRepairApplication : Application() {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Enable offline persistence for Firebase Realtime Database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        
        // Initialize default data
        initializeDefaultData()
    }
    
    private fun initializeDefaultData() {
        applicationScope.launch {
            val repository = FirebaseRepository()
            repository.initializeDefaultSettings()
            repository.createDefaultUsers()
        }
    }
}