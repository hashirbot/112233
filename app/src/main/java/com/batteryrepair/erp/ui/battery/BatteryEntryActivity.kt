package com.batteryrepair.erp.ui.battery

import android.os.Bundle
import android.widget.Toast
import com.batteryrepair.erp.data.models.BatteryStatus
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.batteryrepair.erp.data.models.Battery
import com.batteryrepair.erp.data.models.Customer
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityBatteryEntryBinding
import kotlinx.coroutines.launch

class BatteryEntryActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityBatteryEntryBinding
    private val repository = FirebaseRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatteryEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Register New Battery"
        }

        setupUI()
    }
    

    
    private fun setupUI() {
        binding.switchPickup.setOnCheckedChangeListener { _, isChecked ->
            binding.etPickupCharge.isEnabled = isChecked
            if (!isChecked) {
                binding.etPickupCharge.setText("0")
            }
        }
        
        binding.btnRegister.setOnClickListener {
            registerBattery()
        }
    }
    
    private fun registerBattery() {
        val customerName = binding.etCustomerName.text.toString().trim()
        val mobile = binding.etMobile.text.toString().trim()
        val mobileSecondary = binding.etMobileSecondary.text.toString().trim().takeIf { it.isNotEmpty() }
        val batteryType = binding.etBatteryType.text.toString().trim()
        val voltage = binding.etVoltage.text.toString().trim()
        val capacity = binding.etCapacity.text.toString().trim()
        val isPickup = binding.switchPickup.isChecked
        val pickupCharge = binding.etPickupCharge.text.toString().toDoubleOrNull() ?: 0.0
        
        if (customerName.isEmpty() || mobile.isEmpty() || batteryType.isEmpty() || 
            voltage.isEmpty() || capacity.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (mobile.length < 10) {
            Toast.makeText(this, "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
            return
        }
        
        binding.btnRegister.isEnabled = false
        binding.btnRegister.text = "Registering..."
        
        lifecycleScope.launch {
            // First create or get customer
            val customer = Customer(
                name = customerName,
                mobile = mobile,
                mobileSecondary = mobileSecondary
            )
            
            repository.createOrGetCustomer(customer).fold(
                onSuccess = { customerId ->
                    // Create battery
                    val battery = Battery(
                        customerId = customerId,
                        batteryType = batteryType,
                        voltage = voltage,
                        capacity = capacity,
                        isPickup = isPickup,
                        pickupCharge = pickupCharge,
                        status = BatteryStatus.RECEIVED
                    )
                    
                    repository.createBattery(battery).fold(
                        onSuccess = { _ ->
                            Toast.makeText(this@BatteryEntryActivity, "Battery registered successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { error ->
                            Toast.makeText(this@BatteryEntryActivity, "Failed to register battery: ${error.message}", Toast.LENGTH_LONG).show()
                            binding.btnRegister.isEnabled = true
                            binding.btnRegister.text = "Register Battery"
                        }
                    )
                },
                onFailure = { error ->
                    Toast.makeText(this@BatteryEntryActivity, "Failed to create customer: ${error.message}", Toast.LENGTH_LONG).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Register Battery"
                }
            )
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}