package com.adeloc.app.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.adeloc.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Load existing token if saved
        val currentToken = prefs.getString("rd_token", "")
        binding.etApiToken.setText(currentToken)

        binding.btnSave.setOnClickListener {
            val token = binding.etApiToken.text.toString().trim()
            if (token.isNotEmpty()) {
                prefs.edit().putString("rd_token", token).apply()
                Toast.makeText(this, "Logged in!", Toast.LENGTH_SHORT).show()
                finish() // Close screen
            } else {
                Toast.makeText(this, "Please paste a token", Toast.LENGTH_SHORT).show()
            }
        }
    }
}