package com.example.topmemes.activites

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.topmemes.adapters.DeviceListAdapter
import com.example.topmemes.databinding.ActivityLobbyClientBinding
import com.example.topmemes.network.NearbyConnectionManager
import com.example.topmemes.network.ConnectionState
import com.example.topmemes.network.Endpoint
import com.example.topmemes.utils.PermissionUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LobbyClientActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLobbyClientBinding
    private lateinit var connectionManager: NearbyConnectionManager
    private lateinit var deviceListAdapter: DeviceListAdapter
    private var isDiscovering = false

    companion object {
        private const val SERVICE_ID = "com.example.topmemes.lobby"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkRequirements()
    }

    private fun setupUI() {
        deviceListAdapter = DeviceListAdapter(this, emptyList())
        binding.playersListView.adapter = deviceListAdapter

        binding.backButton.setOnClickListener {
            connectionManager.stopAll()
            finish()
        }


    }

    private fun checkRequirements() {
        if (!PermissionUtils.isWifiEnabled(this)) {
            showWifiDialog()
            return
        }

        val missingPermissions = PermissionUtils.getMissingPermissions(this)
        if (missingPermissions.isNotEmpty()) {
            if (PermissionUtils.shouldShowRequestPermissionRationale(this)) {
                Toast.makeText(
                    this,
                    "Для работы приложения необходимы следующие разрешения: ${missingPermissions.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()
            }
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return
        }

        initConnectionManager()
    }

    private fun showWifiDialog() {
        Toast.makeText(this, "Пожалуйста, включите WiFi", Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    private fun initConnectionManager() {
        if (isDiscovering) return

        connectionManager = NearbyConnectionManager(this)
        
        lifecycleScope.launch {
            connectionManager.connectedDevices.collectLatest { devices ->
                deviceListAdapter.updateDevices(devices)
                binding.playersCountText.text = "Игроков: ${devices.size}/6"
            }
        }

        lifecycleScope.launch {
            connectionManager.connectionState.collectLatest { state ->
                when (state) {
                    ConnectionState.DISCOVERING -> {
                        isDiscovering = true
                        binding.statusText.text = "Поиск доступных лобби..."
                    }
                    ConnectionState.CONNECTED -> {
                        binding.statusText.text = "Подключено к лобби!"
                    }
                    ConnectionState.ERROR -> {
                        isDiscovering = false
                        binding.statusText.text = "Ошибка подключения"
                        val missingPermissions = PermissionUtils.getMissingPermissions(this@LobbyClientActivity)
                        if (missingPermissions.isNotEmpty()) {
                            if (PermissionUtils.shouldShowRequestPermissionRationale(this@LobbyClientActivity)) {
                                Toast.makeText(
                                    this@LobbyClientActivity,
                                    "Для работы приложения необходимы следующие разрешения: ${missingPermissions.joinToString(", ")}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            ActivityCompat.requestPermissions(
                                this@LobbyClientActivity,
                                missingPermissions.toTypedArray(),
                                PERMISSION_REQUEST_CODE
                            )
                        } else if (!PermissionUtils.isWifiEnabled(this@LobbyClientActivity)) {
                            Toast.makeText(
                                this@LobbyClientActivity,
                                "WiFi отключен",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@LobbyClientActivity,
                                "Ошибка подключения к лобби. Попробуйте перезапустить приложение",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    ConnectionState.DISCONNECTED -> {
                        isDiscovering = false
                        binding.statusText.text = "Поиск лобби..."
                    }
                    else -> {
                        binding.statusText.text = "Поиск лобби..."
                    }
                }
            }
        }

        lifecycleScope.launch {
            connectionManager.roomName.collectLatest { roomName ->
                binding.roomNameText.text = "Комната: $roomName"
            }
        }

        // Начинаем поиск доступных лобби
        connectionManager.startDiscovery(SERVICE_ID)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initConnectionManager()
            } else {
                val missingPermissions = PermissionUtils.getMissingPermissions(this)
                if (missingPermissions.isNotEmpty()) {
                    Toast.makeText(
                        this,
                        "Для работы приложения необходимы следующие разрешения: ${missingPermissions.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()
                    if (PermissionUtils.shouldShowRequestPermissionRationale(this)) {
                        ActivityCompat.requestPermissions(
                            this,
                            missingPermissions.toTypedArray(),
                            PERMISSION_REQUEST_CODE
                        )
                    } else {
                        Toast.makeText(
                            this,
                            "Пожалуйста, предоставьте необходимые разрешения в настройках приложения",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtils.isWifiEnabled(this) && PermissionUtils.hasRequiredPermissions(this)) {
            if (!::connectionManager.isInitialized || !isDiscovering) {
                initConnectionManager()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectionManager.isInitialized) {
            connectionManager.stopAll()
        }
    }
} 
