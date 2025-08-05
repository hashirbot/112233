package com.batteryrepair.erp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.batteryrepair.erp.R
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityMainBinding
import com.batteryrepair.erp.ui.auth.LoginActivity
import com.batteryrepair.erp.ui.battery.BatteryEntryActivity
import com.batteryrepair.erp.ui.technician.TechnicianPanelActivity
import com.batteryrepair.erp.ui.reports.ReportsActivity
import com.batteryrepair.erp.ui.admin.AdminActivity
import com.batteryrepair.erp.data.models.UserRole
import com.batteryrepair.erp.data.models.User
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val repository = FirebaseRepository()
    private lateinit var dashboardAdapter: DashboardAdapter
    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Toast.makeText(this, "MainActivity loaded", Toast.LENGTH_SHORT).show()

            setupSwipeRefresh()
            setupRecyclerView()
            setupFAB()
            loadCurrentUser()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("MainActivity", "Crash: ${e.message}")
            Toast.makeText(this, "MainActivity crash: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadCurrentUser() {
        lifecycleScope.launch {
            val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            firebaseUser?.let { user ->
                repository.getUser(user.uid).fold(
                    onSuccess = { userData ->
                        currentUser = userData
                        invalidateOptionsMenu() // Refresh menu to show/hide admin option
                    },
                    onFailure = { /* Handle error silently */ }
                )
            }
        }
    }

    
    private fun setupRecyclerView() {
        dashboardAdapter = DashboardAdapter { action ->
            when (action) {
                DashboardAction.NEW_BATTERY -> startActivity(Intent(this, BatteryEntryActivity::class.java))
                DashboardAction.TECHNICIAN_PANEL -> startActivity(Intent(this, TechnicianPanelActivity::class.java))
                DashboardAction.SEARCH -> {
                    Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show()
                }
                DashboardAction.REPORTS -> {
                    startActivity(Intent(this, ReportsActivity::class.java))
                }
                DashboardAction.BACKUP_DATA -> {
                    if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.SHOP_STAFF) {
                        performBackup()
                    } else {
                        Toast.makeText(this, "Only admin and staff can create backups", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dashboardAdapter
        }
    }
    
    private fun setupFAB() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, BatteryEntryActivity::class.java))
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }
    
    private fun loadDashboardData() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                repository.getDashboardStats().fold(
                    onSuccess = { stats ->
                        dashboardAdapter.updateStats(stats)
                        binding.swipeRefresh.isRefreshing = false
                    },
                    onFailure = { error ->
                        binding.swipeRefresh.isRefreshing = false
                        Toast.makeText(this@MainActivity, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "Error loading data: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun performBackup() {
        lifecycleScope.launch {
            repository.createBackup().fold(
                onSuccess = { backupData ->
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Backup Created")
                        .setMessage("Backup contains:\n• ${backupData.batteries.size} batteries\n• ${backupData.customers.size} customers\n• ${backupData.users.size} users\n• ${backupData.statusHistory.size} status records")
                        .setPositiveButton("OK", null)
                        .show()
                },
                onFailure = { error ->
                    Toast.makeText(this@MainActivity, "Failed to create backup: ${error.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        title = "Battery Repair ERP"
        
        // Show admin menu item only for admin users
        val adminMenuItem = menu.findItem(R.id.action_admin)
        adminMenuItem?.isVisible = currentUser?.role == UserRole.ADMIN
        
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_admin -> {
                startActivity(Intent(this, AdminActivity::class.java))
                true
            }
            R.id.action_logout -> {
                repository.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadCurrentUser()
        loadDashboardData()
    }
}