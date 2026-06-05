package com.example.bionichand

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.bionichand.ui.theme.BionicHandTheme

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: ClassicBluetoothManager
    private lateinit var gestureStorage: GestureStorage

    private var isConnectedState = mutableStateOf(false)
    private var batteryLevelState = mutableStateOf("--")
    private var emgThresholdState = mutableStateOf("--")

    private val hc06MacAddress = "98:DA:60:0A:21:28"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (!permissions.entries.all { it.value }) {
            Toast.makeText(this, "Потрібні дозволи для Bluetooth", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gestureStorage = GestureStorage(this)
        bluetoothManager = ClassicBluetoothManager(this)

        bluetoothManager.onConnectionStateChange = { isConnected ->
            runOnUiThread {
                isConnectedState.value = isConnected
                if (!isConnected) {
                    batteryLevelState.value = "--"
                    emgThresholdState.value = "--"
                } else {
                    bluetoothManager.sendData("R*")
                }
            }
        }

        bluetoothManager.onMessageReceived = { message ->
            runOnUiThread {
                when {
                    message.startsWith("B:") -> batteryLevelState.value = message.substringAfter("B:")
                    message.startsWith("T:") -> emgThresholdState.value = message.substringAfter("T:")
                    message.startsWith("CAL:") -> Toast.makeText(this, message.substringAfter("CAL:"), Toast.LENGTH_SHORT).show()
                    message.startsWith("ERR:") -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        checkAndRequestPermissions()

        setContent {
            BionicHandTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BionicHandScreen(
                        isConnected = isConnectedState.value,
                        batteryLevel = batteryLevelState.value,
                        emgThreshold = emgThresholdState.value,
                        gestureStorage = gestureStorage,
                        onConnectClick = {
                            if (isConnectedState.value) {
                                bluetoothManager.disconnect()
                            } else {
                                bluetoothManager.connectToDevice(hc06MacAddress)
                            }
                        },
                        onSendBluetoothCommand = { command -> bluetoothManager.sendData(command) }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
