package com.batteryrepair.erp.ui.technician

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.batteryrepair.erp.data.models.BatteryStatus
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityTechnicianPanelBinding
import kotlinx.coroutines.launch

class TechnicianPanelActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityTechnicianPanelBinding
    private val repository = FirebaseRepository()
    private lateinit var batteryAdapter: TechnicianBatteryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTechnicianPanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Technician Panel"
        }
        
        setupRecyclerView()
        loadPendingBatteries()
    }
    

    
    private fun setupRecyclerView() {
        batteryAdapter = TechnicianBatteryAdapter { battery, status, comments, servicePrice ->
            updateBatteryStatus(battery.id, status, comments, servicePrice)
        }
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TechnicianPanelActivity)
            adapter = batteryAdapter
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            loadPendingBatteries()
        }
    }
    
    private fun loadPendingBatteries() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                repository.getBatteriesByStatus(BatteryStatus.PENDING).collect { batteries ->
                    batteryAdapter.submitList(batteries)
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                // Handle error - could show a toast or error message
            }
        }
    }
    
    private fun updateBatteryStatus(batteryId: String, status: BatteryStatus, comments: String, servicePrice: Double?) {
        lifecycleScope.launch {
            try {
                repository.updateBatteryStatus(batteryId, status, comments, servicePrice).fold(
                    onSuccess = {
                        // Success handled by real-time updates
                        android.widget.Toast.makeText(this@TechnicianPanelActivity, "Status updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        android.widget.Toast.makeText(this@TechnicianPanelActivity, "Failed to update status: ${error.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@TechnicianPanelActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}