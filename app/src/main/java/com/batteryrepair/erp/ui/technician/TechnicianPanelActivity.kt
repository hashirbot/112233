package com.batteryrepair.erp.ui.technician

import android.os.Bundle
import android.widget.Toast
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
        
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing technician panel: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupRecyclerView() {
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up RecyclerView: ${e.message}", Toast.LENGTH_LONG).show()
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
                Toast.makeText(this@TechnicianPanelActivity, "Error loading batteries: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun updateBatteryStatus(batteryId: String, status: BatteryStatus, comments: String, servicePrice: Double?) {
        lifecycleScope.launch {
            try {
                repository.updateBatteryStatus(batteryId, status, comments, servicePrice).fold(
                    onSuccess = {
                        Toast.makeText(this@TechnicianPanelActivity, "Status updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@TechnicianPanelActivity, "Failed to update status: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@TechnicianPanelActivity, "Error updating status: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}