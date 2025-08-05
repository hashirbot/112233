package com.batteryrepair.erp.ui.technician

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.batteryrepair.erp.data.models.Battery
import com.batteryrepair.erp.data.models.BatteryStatus
import com.batteryrepair.erp.databinding.ItemTechnicianBatteryBinding
import com.batteryrepair.erp.data.models.displayName
import java.text.SimpleDateFormat
import java.util.*

class TechnicianBatteryAdapter(
    private val onStatusUpdate: (Battery, BatteryStatus, String, Double?) -> Unit
) : ListAdapter<Battery, TechnicianBatteryAdapter.ViewHolder>(BatteryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTechnicianBatteryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onStatusUpdate)
    }
    
    class ViewHolder(private val binding: ItemTechnicianBatteryBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(battery: Battery, onStatusUpdate: (Battery, BatteryStatus, String, Double?) -> Unit) {
            binding.tvBatteryId.text = battery.batteryId
            binding.tvBatteryType.text = "${battery.batteryType} (${battery.voltage}/${battery.capacity})"
            binding.tvCustomerName.text = battery.customer?.name ?: "Unknown Customer"
            binding.tvCustomerMobile.text = battery.customer?.mobile ?: ""
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            binding.tvInwardDate.text = dateFormat.format(Date(battery.inwardDate))
            
            // Setup status spinner
            val statusAdapter = ArrayAdapter(
                binding.root.context,
                android.R.layout.simple_spinner_item,
                BatteryStatus.values().map { it.displayName() }
            )
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerStatus.adapter = statusAdapter
            binding.spinnerStatus.setSelection(battery.status.ordinal)
            
            // Set current service price
            if (battery.servicePrice > 0) {
                binding.etServicePrice.setText(battery.servicePrice.toString())
            }
            
            binding.btnUpdate.setOnClickListener {
                val selectedStatus = BatteryStatus.values()[binding.spinnerStatus.selectedItemPosition]
                val comments = binding.etComments.text.toString()
                val servicePrice = binding.etServicePrice.text.toString().toDoubleOrNull()
                
                if (comments.isEmpty()) {
                    binding.etComments.error = "Please add comments"
                    return@setOnClickListener
                }
                
                onStatusUpdate(battery, selectedStatus, comments, servicePrice)
                
                // Clear comments after update
                binding.etComments.setText("")
            }
        }
    }
    
    class BatteryDiffCallback : DiffUtil.ItemCallback<Battery>() {
        override fun areItemsTheSame(oldItem: Battery, newItem: Battery): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Battery, newItem: Battery): Boolean {
            return oldItem == newItem
        }
    }
}