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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val repository = FirebaseRepository()
    private lateinit var dashboardAdapter: DashboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Toast.makeText(this, "MainActivity loaded", Toast.LENGTH_SHORT).show()

            setupSwipeRefresh()
            setupRecyclerView()
            setupFAB()
            loadDashboardData()
        } catch (e: Exception) {
            Log.e("MainActivity", "Crash: ${e.message}")
            Toast.makeText(this, "MainActivity crash: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    
    private fun setupRecyclerView() {
        dashboardAdapter = DashboardAdapter { action ->
            when (action) {
                DashboardAction.NEW_BATTERY -> startActivity(Intent(this, BatteryEntryActivity::class.java))
                DashboardAction.TECHNICIAN_PANEL -> startActivity(Intent(this, TechnicianPanelActivity::class.java))
                DashboardAction.SEARCH -> {
                    // TODO: Implement search
                }
                DashboardAction.REPORTS -> {
                    // TODO: Implement reports
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
            repository.getDashboardStats().fold(
                onSuccess = { stats ->
                    dashboardAdapter.updateStats(stats)
                    binding.swipeRefresh.isRefreshing = false
                },
                onFailure = { error ->
                    binding.swipeRefresh.isRefreshing = false
                    // Show error message or retry option
                }
            )
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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
        loadDashboardData()
    }
}