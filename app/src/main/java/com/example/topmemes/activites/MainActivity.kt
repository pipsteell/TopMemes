package com.example.topmemes.activites

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.topmemes.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.startGameme.setOnClickListener {
            startActivity(Intent(this, StartGamemeActivity::class.java))
        }

        binding.startBtn.setOnClickListener {
            startActivity(Intent(this, LobbySettings::class.java))
        }

        binding.connectBtn.setOnClickListener {
            startActivity(Intent(this, LobbyClientActivity::class.java))
        }

        binding.addSituations.setOnClickListener {
            startActivity(Intent(this, SituationsAddActivity::class.java))
        }

        binding.addMemesBtn.setOnClickListener {
            startActivity(Intent(this, ControllMemesActivity::class.java))
        }
    }
}