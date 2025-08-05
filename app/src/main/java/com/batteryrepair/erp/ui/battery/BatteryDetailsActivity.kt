package com.batteryrepair.erp.ui.battery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.batteryrepair.erp.data.models.Battery
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityBatteryDetailsBinding
import kotlinx.coroutines.launch
import com.batteryrepair.erp.data.models.displayName
import java.text.SimpleDateFormat
import java.util.*

class BatteryDetailsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBatteryDetailsBinding
    private val repository = FirebaseRepository()
    private lateinit var battery: Battery
    
    companion object {
        const val EXTRA_BATTERY = "extra_battery"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        battery = intent.getParcelableExtra(EXTRA_BATTERY) ?: return

        setupUI()
        loadStatusHistory()
    }

    
    private fun setupUI() {
        binding.tvBatteryId.text = battery.batteryId
        binding.tvBatteryType.text = "${battery.batteryType} (${battery.voltage}/${battery.capacity})"
        binding.tvCustomerName.text = battery.customer?.name ?: "Unknown Customer"
        binding.tvCustomerMobile.text = battery.customer?.mobile ?: ""
        binding.tvStatus.text = battery.status.displayName()


        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvInwardDate.text = dateFormat.format(Date(battery.inwardDate))
        
        if (battery.servicePrice > 0) {
            binding.tvServicePrice.text = "₹${battery.servicePrice}"
        } else {
            binding.tvServicePrice.text = "Not set"
        }
        
        if (battery.isPickup) {
            binding.tvPickupCharge.text = "₹${battery.pickupCharge}"
        } else {
            binding.tvPickupCharge.text = "No pickup"
        }
        
        val totalAmount = battery.servicePrice + (if (battery.isPickup) battery.pickupCharge else 0.0)
        binding.tvTotalAmount.text = "₹$totalAmount"
    }
    
    private fun loadStatusHistory() {
        lifecycleScope.launch {
            repository.getStatusHistory(battery.id).collect { history ->
                // TODO: Setup RecyclerView for status history
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}