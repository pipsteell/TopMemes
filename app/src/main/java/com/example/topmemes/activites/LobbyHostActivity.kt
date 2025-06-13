// LobbyHostActivity.kt
package com.example.topmemes.activites

import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.topmemes.adapters.DeviceListAdapter
import com.example.topmemes.databinding.ActivityLobbyHostBinding
import com.example.topmemes.network.NearbyConnectionManager
import com.example.topmemes.network.ConnectionState
import com.example.topmemes.network.Endpoint
import com.example.topmemes.utils.PermissionUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class LobbyHostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLobbyHostBinding
    private lateinit var connectionManager: NearbyConnectionManager
    private lateinit var deviceListAdapter: DeviceListAdapter
    private val hostEndpoint = Endpoint("host", "Host")
    private var isAdvertising = false

    companion object {
        private const val SERVICE_ID = "com.example.topmemes.lobby"
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkRequirements()
    }

    private fun setupUI() {
        deviceListAdapter = DeviceListAdapter(this, listOf(hostEndpoint))
        binding.playersListView.adapter = deviceListAdapter

        binding.backButton.setOnClickListener {
            connectionManager.stopAll()
            finish()
        }

        binding.startGameme.setOnClickListener {
            startActivity(Intent(this, StartGamemeActivity::class.java))
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
        if (isAdvertising) return

        connectionManager = NearbyConnectionManager(this)
        
        lifecycleScope.launch {
            connectionManager.connectedDevices.collectLatest { devices ->
                val allDevices = listOf(hostEndpoint) + devices
                deviceListAdapter.updateDevices(allDevices)
                binding.playersCountText.text = "Игроков: ${allDevices.size}/6"
            }
        }

        lifecycleScope.launch {
            connectionManager.connectionState.collectLatest { state ->
                when (state) {
                    ConnectionState.ADVERTISING -> {
                        isAdvertising = true
                        binding.statusText.text = "Ожидание подключения игроков..."
                        Toast.makeText(this@LobbyHostActivity, "Лобби создано", Toast.LENGTH_SHORT).show()
                    }
                    ConnectionState.CONNECTED -> {
                        binding.statusText.text = "Игрок подключен!"
                    }
                    ConnectionState.ERROR -> {
                        isAdvertising = false
                        binding.statusText.text = "Ошибка подключения"
                        val missingPermissions = PermissionUtils.getMissingPermissions(this@LobbyHostActivity)
                        if (missingPermissions.isNotEmpty()) {
                            if (PermissionUtils.shouldShowRequestPermissionRationale(this@LobbyHostActivity)) {
                                Toast.makeText(
                                    this@LobbyHostActivity,
                                    "Для работы приложения необходимы следующие разрешения: ${missingPermissions.joinToString(", ")}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            ActivityCompat.requestPermissions(
                                this@LobbyHostActivity,
                                missingPermissions.toTypedArray(),
                                PERMISSION_REQUEST_CODE
                            )
                        } else if (!PermissionUtils.isWifiEnabled(this@LobbyHostActivity)) {
                            Toast.makeText(
                                this@LobbyHostActivity,
                                "WiFi отключен",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this@LobbyHostActivity,
                                "Ошибка создания лобби. Попробуйте перезапустить приложение",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    ConnectionState.DISCONNECTED -> {
                        isAdvertising = false
                        binding.statusText.text = "Создание лобби..."
                    }
                    else -> {
                        binding.statusText.text = "Создание лобби..."
                    }
                }
            }
        }

        lifecycleScope.launch {
            connectionManager.roomName.collectLatest { roomName ->
                binding.roomNameText.text = "Комната: $roomName"
            }
        }

        // Генерируем уникальное имя комнаты
        val roomName = "Комната ${Random().nextInt(1000)}"
        // Начинаем рекламу для подключения других устройств
        connectionManager.startAdvertising(SERVICE_ID, roomName)
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
            if (!::connectionManager.isInitialized || !isAdvertising) {
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