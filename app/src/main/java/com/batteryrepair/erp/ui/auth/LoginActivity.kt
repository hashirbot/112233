package com.batteryrepair.erp.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.batteryrepair.erp.data.repository.FirebaseRepository
import com.batteryrepair.erp.databinding.ActivityLoginBinding
import com.batteryrepair.erp.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val repository = FirebaseRepository()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            login(username, password)
        }
        
        // Set default credentials for demo
        binding.etUsername.setText("admin")
        binding.etPassword.setText("admin123")
    }
    
    private fun login(username: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.btnLogin.text = "Signing in..."
        
        lifecycleScope.launch {
            repository.signIn(username, password).fold(
                onSuccess = { user ->
                    Toast.makeText(this@LoginActivity, "Welcome ${user.fullName}!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                },
                onFailure = { error ->
                    val errorMessage = when {
                        error.message?.contains("password") == true -> "Invalid username or password"
                        error.message?.contains("network") == true -> "Network error. Please check your connection."
                        else -> "Login failed: ${error.message}"
                    }
                    Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Sign In"
                }
            )
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Check if user is already signed in
        if (repository.getCurrentUser() != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}