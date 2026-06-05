package com.example.bionichand

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

@SuppressLint("MissingPermission")
class ClassicBluetoothManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var onConnectionStateChange: ((Boolean) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    fun connectToDevice(deviceAddress: String) {
        thread {
            try {
                val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                bluetoothAdapter?.cancelDiscovery()

                bluetoothSocket = device?.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                Log.d("BTClassic", "Підключено успішно до HC-06!")
                onConnectionStateChange?.invoke(true)

                startListening()
            } catch (e: IOException) {
                Log.e("BTClassic", "Помилка підключення", e)
                disconnect()
            }
        }
    }

    private fun startListening() {
        thread {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                while (bluetoothSocket?.isConnected == true) {
                    val line = reader.readLine()
                    if (line != null) {
                        onMessageReceived?.invoke(line.trim())
                    }
                }
            } catch (e: IOException) {
                Log.e("BTClassic", "Помилка читання або пристрій відключено", e)
                disconnect()
            }
        }
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BTClassic", "Помилка при закритті", e)
        }
        bluetoothSocket = null
        outputStream = null
        inputStream = null
        onConnectionStateChange?.invoke(false)
    }

    fun sendData(data: String) {
        thread {
            try {
                outputStream?.write(data.toByteArray())
                Log.d("BTClassic", "Відправлено: $data")
            } catch (e: IOException) {
                Log.e("BTClassic", "Помилка відправки", e)
                disconnect()
            }
        }
    }
}