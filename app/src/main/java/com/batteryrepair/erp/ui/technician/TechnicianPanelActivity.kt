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
            repository.getBatteriesByStatus(BatteryStatus.PENDING).collect { batteries ->
                batteryAdapter.submitList(batteries)
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun updateBatteryStatus(batteryId: String, status: BatteryStatus, comments: String, servicePrice: Double?) {
        lifecycleScope.launch {
            repository.updateBatteryStatus(batteryId, status, comments, servicePrice).fold(
                onSuccess = {
                    // Success handled by real-time updates
                },
                onFailure = { error ->
                    // Handle error
                }
            )
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}