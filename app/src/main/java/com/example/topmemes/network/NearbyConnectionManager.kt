package com.example.topmemes.network

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.*

class NearbyConnectionManager(private val context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val _connectedDevices = MutableStateFlow<List<Endpoint>>(emptyList())
    val connectedDevices: StateFlow<List<Endpoint>> = _connectedDevices.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _gameData = MutableStateFlow<GameData?>(null)
    val gameData: StateFlow<GameData?> = _gameData.asStateFlow()

    private val _roomName = MutableStateFlow<String>("")
    val roomName: StateFlow<String> = _roomName.asStateFlow()

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with $endpointId")
            // Автоматически принимаем все входящие подключения
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _connectionState.value = ConnectionState.CONNECTED
                    val endpoint = Endpoint(endpointId, "Player")
                    _connectedDevices.value = _connectedDevices.value + endpoint
                    Log.d(TAG, "Connected to $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.e(TAG, "Connection rejected by $endpointId")
                    _connectionState.value = ConnectionState.ERROR
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection error with $endpointId: ${result.status.statusMessage}")
                    _connectionState.value = ConnectionState.ERROR
                }
                else -> {
                    Log.e(TAG, "Connection failed with $endpointId: ${result.status.statusMessage}")
                    _connectionState.value = ConnectionState.ERROR
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            _connectedDevices.value = _connectedDevices.value.filter { it.id != endpointId }
            if (_connectedDevices.value.isEmpty()) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val data = String(payload.asBytes()!!, StandardCharsets.UTF_8)
                    try {
                        val json = JSONObject(data)
                        when (json.getString("type")) {
                            "room_info" -> {
                                _roomName.value = json.getString("room_name")
                            }
                            else -> {
                                val gameData = GameData(
                                    type = json.getString("type"),
                                    content = json.getString("content"),
                                    senderId = endpointId
                                )
                                _gameData.value = gameData
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing data", e)
                    }
                }
                Payload.Type.FILE -> {
                    // Handle file transfer if needed
                }
                Payload.Type.STREAM -> {
                    // Handle stream if needed
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d(TAG, "Payload transfer successful")
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.e(TAG, "Payload transfer failed")
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = (update.bytesTransferred * 100 / update.totalBytes).toInt()
                    Log.d(TAG, "Transfer progress: $progress%")
                }
            }
        }
    }

    fun startAdvertising(serviceId: String, roomName: String) {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        _roomName.value = roomName
        _connectionState.value = ConnectionState.ADVERTISING
        connectionsClient.startAdvertising(
            roomName,
            serviceId,
            connectionCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Started advertising")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start advertising", e)
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun startDiscovery(serviceId: String) {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()

        _connectionState.value = ConnectionState.DISCOVERING
        connectionsClient.startDiscovery(
            serviceId,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
                    Log.d(TAG, "Found endpoint: $endpointId")
                    _roomName.value = discoveredEndpointInfo.endpointName
                    connectionsClient.requestConnection(
                        "Client",
                        endpointId,
                        connectionCallback
                    )
                }

                override fun onEndpointLost(endpointId: String) {
                    Log.d(TAG, "Lost endpoint: $endpointId")
                }
            },
            discoveryOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Started discovering")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to start discovery", e)
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun sendGameData(type: String, content: String) {
        val json = JSONObject().apply {
            put("type", type)
            put("content", content)
        }
        val payload = Payload.fromBytes(json.toString().toByteArray(StandardCharsets.UTF_8))
        
        _connectedDevices.value.forEach { endpoint ->
            connectionsClient.sendPayload(endpoint.id, payload)
        }
    }

    fun stopAll() {
        try {
            connectionsClient.stopAllEndpoints()
            _connectionState.value = ConnectionState.DISCONNECTED
            _connectedDevices.value = emptyList()
            _gameData.value = null
            _roomName.value = ""
            Log.d(TAG, "Stopped all connections")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping connections", e)
        }
    }

    companion object {
        private const val TAG = "NearbyConnectionManager"
    }
}

enum class ConnectionState {
    DISCONNECTED,
    ADVERTISING,
    DISCOVERING,
    CONNECTED,
    ERROR
}

data class GameData(
    val type: String,
    val content: String,
    val senderId: String
)

data class Endpoint(
    val id: String,
    val name: String
) 