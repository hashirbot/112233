package com.batteryrepair.erp.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.batteryrepair.erp.data.models.User
import com.batteryrepair.erp.data.models.UserRole
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityAdminBinding
import com.batteryrepair.erp.databinding.DialogAddUserBinding
import com.batteryrepair.erp.databinding.DialogShopSettingsBinding
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAdminBinding
    private val repository = FirebaseRepository()
    private lateinit var userAdapter: UserAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            binding = ActivityAdminBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            // Setup toolbar
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = "Admin Panel"
            }
            
            setupUI()
            setupRecyclerView()
            loadUsers()
            loadShopSettings()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing admin panel: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun setupUI() {
        try {
            binding.btnShopSettings.setOnClickListener {
                showShopSettingsDialog()
            }
            
            binding.btnAddUser.setOnClickListener {
                showAddUserDialog()
            }
            
            binding.btnBackupData.setOnClickListener {
                backupData()
            }
            
            binding.btnRestoreData.setOnClickListener {
                showRestoreDataDialog()
            }
            
            binding.swipeRefresh.setOnRefreshListener {
                loadUsers()
                loadShopSettings()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up UI: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupRecyclerView() {
        try {
            userAdapter = UserAdapter(
                onEditUser = { user -> showEditUserDialog(user) },
                onDeleteUser = { user -> showDeleteUserDialog(user) },
                onToggleUserStatus = { user -> toggleUserStatus(user) }
            )
            
            binding.recyclerViewUsers.apply {
                layoutManager = LinearLayoutManager(this@AdminActivity)
                adapter = userAdapter
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up RecyclerView: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadUsers() {
        binding.swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                repository.getAllUsers().fold(
                    onSuccess = { users ->
                        userAdapter.submitList(users)
                        binding.swipeRefresh.isRefreshing = false
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to load users: ${error.message}", Toast.LENGTH_LONG).show()
                        binding.swipeRefresh.isRefreshing = false
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error loading users: ${e.message}", Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }
    
    private fun loadShopSettings() {
        lifecycleScope.launch {
            try {
                repository.getShopSettings().fold(
                    onSuccess = { settings ->
                        binding.tvShopName.text = settings.shopName
                        binding.tvBatteryIdFormat.text = "${settings.batteryIdPrefix}${settings.batteryIdStart.toString().padStart(settings.batteryIdPadding, '0')}"
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to load settings: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error loading settings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showShopSettingsDialog() {
        try {
            val dialogBinding = DialogShopSettingsBinding.inflate(layoutInflater)
            
            lifecycleScope.launch {
                repository.getShopSettings().fold(
                    onSuccess = { settings ->
                        dialogBinding.etShopName.setText(settings.shopName)
                        dialogBinding.etBatteryPrefix.setText(settings.batteryIdPrefix)
                        dialogBinding.etBatteryStart.setText(settings.batteryIdStart.toString())
                        dialogBinding.etBatteryPadding.setText(settings.batteryIdPadding.toString())
                    },
                    onFailure = { /* Use default values */ }
                )
            }
            
            AlertDialog.Builder(this)
                .setTitle("Shop Settings")
                .setView(dialogBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    val shopName = dialogBinding.etShopName.text.toString().trim()
                    val prefix = dialogBinding.etBatteryPrefix.text.toString().trim()
                    val start = dialogBinding.etBatteryStart.text.toString().toIntOrNull() ?: 1
                    val padding = dialogBinding.etBatteryPadding.text.toString().toIntOrNull() ?: 4
                    
                    if (shopName.isEmpty() || prefix.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    saveShopSettings(shopName, prefix, start, padding)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing settings dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun saveShopSettings(shopName: String, prefix: String, start: Int, padding: Int) {
        lifecycleScope.launch {
            try {
                repository.updateShopSettings(shopName, prefix, start, padding).fold(
                    onSuccess = {
                        Toast.makeText(this@AdminActivity, "Settings saved successfully", Toast.LENGTH_SHORT).show()
                        loadShopSettings()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to save settings: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error saving settings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showAddUserDialog() {
        try {
            val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
            
            // Setup role spinner
            val roleAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                UserRole.values().map { it.displayName }
            )
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerRole.adapter = roleAdapter
            
            AlertDialog.Builder(this)
                .setTitle("Add New User")
                .setView(dialogBinding.root)
                .setPositiveButton("Create") { _, _ ->
                    val username = dialogBinding.etUsername.text.toString().trim()
                    val fullName = dialogBinding.etFullName.text.toString().trim()
                    val password = dialogBinding.etPassword.text.toString().trim()
                    val role = UserRole.values()[dialogBinding.spinnerRole.selectedItemPosition]
                    
                    if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    if (password.length < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    createUser(username, fullName, password, role)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing add user dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun createUser(username: String, fullName: String, password: String, role: UserRole) {
        lifecycleScope.launch {
            try {
                repository.createUser(username, fullName, password, role).fold(
                    onSuccess = {
                        Toast.makeText(this@AdminActivity, "User created successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to create user: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error creating user: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showEditUserDialog(user: User) {
        try {
            val dialogBinding = DialogAddUserBinding.inflate(layoutInflater)
            
            // Pre-fill user data
            dialogBinding.etUsername.setText(user.username)
            dialogBinding.etUsername.isEnabled = false // Username cannot be changed
            dialogBinding.etFullName.setText(user.fullName)
            dialogBinding.etPassword.hint = "Leave empty to keep current password"
            
            // Setup role spinner
            val roleAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                UserRole.values().map { it.displayName }
            )
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerRole.adapter = roleAdapter
            dialogBinding.spinnerRole.setSelection(UserRole.values().indexOf(user.role))
            
            AlertDialog.Builder(this)
                .setTitle("Edit User")
                .setView(dialogBinding.root)
                .setPositiveButton("Update") { _, _ ->
                    val fullName = dialogBinding.etFullName.text.toString().trim()
                    val password = dialogBinding.etPassword.text.toString().trim()
                    val role = UserRole.values()[dialogBinding.spinnerRole.selectedItemPosition]
                    
                    if (fullName.isEmpty()) {
                        Toast.makeText(this, "Please enter full name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    if (password.isNotEmpty() && password.length < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    updateUser(user.id, fullName, password.takeIf { it.isNotEmpty() }, role)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing edit user dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun updateUser(userId: String, fullName: String, password: String?, role: UserRole) {
        lifecycleScope.launch {
            try {
                repository.updateUser(userId, fullName, password, role).fold(
                    onSuccess = {
                        Toast.makeText(this@AdminActivity, "User updated successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to update user: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error updating user: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showDeleteUserDialog(user: User) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete user '${user.fullName}'? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    deleteUser(user.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing delete dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun deleteUser(userId: String) {
        lifecycleScope.launch {
            try {
                repository.deleteUser(userId).fold(
                    onSuccess = {
                        Toast.makeText(this@AdminActivity, "User deleted successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to delete user: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error deleting user: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun toggleUserStatus(user: User) {
        lifecycleScope.launch {
            try {
                repository.toggleUserStatus(user.id, !user.isActive).fold(
                    onSuccess = {
                        val status = if (!user.isActive) "activated" else "deactivated"
                        Toast.makeText(this@AdminActivity, "User $status successfully", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to update user status: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error toggling user status: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun backupData() {
        lifecycleScope.launch {
            try {
                repository.createBackup().fold(
                    onSuccess = { backupData ->
                        Toast.makeText(this@AdminActivity, "Backup created successfully", Toast.LENGTH_SHORT).show()
                        
                        // For demo purposes, show backup info
                        AlertDialog.Builder(this@AdminActivity)
                            .setTitle("Backup Created")
                            .setMessage("Backup contains:\n• ${backupData.batteries.size} batteries\n• ${backupData.customers.size} customers\n• ${backupData.users.size} users\n• ${backupData.statusHistory.size} status records")
                            .setPositiveButton("OK", null)
                            .show()
                    },
                    onFailure = { error ->
                        Toast.makeText(this@AdminActivity, "Failed to create backup: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AdminActivity, "Error creating backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showRestoreDataDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Restore Data")
                .setMessage("This feature would restore data from a backup file. In a production app, you would select a backup file to restore from.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error showing restore dialog: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}