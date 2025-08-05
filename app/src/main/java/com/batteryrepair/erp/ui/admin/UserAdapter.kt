package com.batteryrepair.erp.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.batteryrepair.erp.R
import com.batteryrepair.erp.data.models.User
import com.batteryrepair.erp.databinding.ItemUserBinding
import java.text.SimpleDateFormat
import java.util.*

class UserAdapter(
    private val onEditUser: (User) -> Unit,
    private val onDeleteUser: (User) -> Unit,
    private val onToggleUserStatus: (User) -> Unit
) : ListAdapter<User, UserAdapter.ViewHolder>(UserDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onEditUser, onDeleteUser, onToggleUserStatus)
    }
    
    class ViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(
            user: User,
            onEditUser: (User) -> Unit,
            onDeleteUser: (User) -> Unit,
            onToggleUserStatus: (User) -> Unit
        ) {
            binding.tvFullName.text = user.fullName
            binding.tvUsername.text = "@${user.username}"
            binding.tvRole.text = user.role.displayName
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.tvCreatedAt.text = "Created: ${dateFormat.format(Date(user.createdAt))}"
            
            // Set status
            if (user.isActive) {
                binding.tvStatus.text = "Active"
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_ready))
                binding.btnToggleStatus.text = "Deactivate"
            } else {
                binding.tvStatus.text = "Inactive"
                binding.tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.status_not_repairable))
                binding.btnToggleStatus.text = "Activate"
            }
            
            // Set role color
            val roleColor = when (user.role) {
                com.batteryrepair.erp.data.models.UserRole.ADMIN -> R.color.status_delivered
                com.batteryrepair.erp.data.models.UserRole.SHOP_STAFF -> R.color.status_pending
                com.batteryrepair.erp.data.models.UserRole.TECHNICIAN -> R.color.status_ready
            }
            binding.tvRole.setTextColor(ContextCompat.getColor(binding.root.context, roleColor))
            
            // Set click listeners
            binding.btnEdit.setOnClickListener { onEditUser(user) }
            binding.btnDelete.setOnClickListener { onDeleteUser(user) }
            binding.btnToggleStatus.setOnClickListener { onToggleUserStatus(user) }
        }
    }
    
    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}