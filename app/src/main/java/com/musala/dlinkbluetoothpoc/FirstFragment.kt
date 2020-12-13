package com.musala.dlinkbluetoothpoc

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_first.*
import java.security.MessageDigest

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val TAG = "FirstFragment"
    private val REQUEST_ENABLE_BT = 1
    private val SCAN_PERIOD: Long = 10000

    private val BIND_KEY = "M=yEr6gn%2FGpYSeYOOoEtUFlzfID%2BXJNmk23gXTKfmQn7l%2FcpiaMEfyAGrROBC8VL%2BWmP7a81W937soA0tc5EQzTdX4dmMbBpYTz%2FH9igQmPArTqQZ%2FR9AyHehZ5oX0gL4zFVvN04YX4pQeIeEWl%2BPAhCgbeSlADlJQwc38ni8afcrotmuRIfUIYFV3cbSu%2BJc2ZG%2BmgHiFUoGtc3hwlwYEG5rgxkCLld%2FsVcA2gKICrWQm0T%2BpS3QSxe0QshOc5E0W4rCv4YdInKt%2BKHcHmM9zvgMwbjQV3p5Z4fYBHpozCcQ%3D;S=SGrOoQfSjtq5z3uf"

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothGatt: BluetoothGatt
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val dlinkServiceUuid = ParcelUuid.fromString("EB234F0A-CD44-4126-A024-49A7107F892F")

    val characteristics = mapOf(
        "unlock" to "0000a001-0000-1000-8000-00805f9b34fb",
        "apList" to "0000a100-0000-1000-8000-00805f9b34fb",
        "lastAction" to "0000a000-0000-1000-8000-00805f9b34fb",
        "wifiSettings" to "0000a101-0000-1000-8000-00805f9b34fb",
        "connectWifi" to "0000a102-0000-1000-8000-00805f9b34fb",
        "checkConnection" to "0000a103-0000-1000-8000-00805f9b34fb",
        "startRegistration" to "0000a303-0000-1000-8000-00805f9b34fb",
        "registrationStatus" to "0000a304-0000-1000-8000-00805f9b34fb",
        "encryptedMessage" to "0000a400-0000-1000-8000-00805f9b34fb"
    )

    private lateinit var lastAction: BluetoothGattCharacteristic
    private lateinit var apList: BluetoothGattCharacteristic
    private lateinit var unlock: BluetoothGattCharacteristic
    private lateinit var wifiSettings: BluetoothGattCharacteristic
    private lateinit var connectWifi: BluetoothGattCharacteristic
    private lateinit var checkConnection: BluetoothGattCharacteristic
    private lateinit var encryptedMessage: BluetoothGattCharacteristic
    private lateinit var startRegistration: BluetoothGattCharacteristic
    private lateinit var registrationStatus: BluetoothGattCharacteristic

    private lateinit var networksBuilder: StringBuilder

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_first).setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            } else {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                scanLeDevices()
                Log.d(TAG, "Scanning.......")
            }
        }

        list_networks.setOnClickListener {
            bluetoothGatt.readCharacteristic(apList)
        }

        set_network.setOnClickListener {
            val encrypted = encrypted_settings.text.toString()

            if (encrypted.isNotEmpty()) {
                encryptedMessage.value = encrypted.toByteArray()
                bluetoothGatt.writeCharacteristic(encryptedMessage)
            } else {
                encryptedMessage.value = "M=kgmRzD0ojDEPpFiclwVcVw0OxsQ6GG7p40znGoEZtmxM4qtsDxFMqkItbhTApaIt;S=VTQ33152Wy8fcnXj".toByteArray()
                bluetoothGatt.writeCharacteristic(encryptedMessage)
            }

//            wifiSettings.value = "I=ShandorL;M=0;S=3;E=2;K=bananiiq".toByteArray()
//            bluetoothGatt.writeCharacteristic(wifiSettings)
        }

        wi_fi_settings.setOnClickListener {
            bluetoothGatt.readCharacteristic(wifiSettings)
        }

        connect_to_network.setOnClickListener {
            connectWifi.value = "C=1".toByteArray()
            bluetoothGatt.writeCharacteristic(connectWifi)
        }

        connection_status.setOnClickListener {
            bluetoothGatt.readCharacteristic(checkConnection)
        }

        last_action.setOnClickListener {
            bluetoothGatt.readCharacteristic(lastAction)
        }

        start_registration.setOnClickListener {
            startRegistration.value = BIND_KEY.toByteArray()
            bluetoothGatt.writeCharacteristic(startRegistration)
        }

        registration_status.setOnClickListener {
            bluetoothGatt.readCharacteristic(registrationStatus)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    override fun onDestroy() {
        bluetoothAdapter.cancelDiscovery()
        super.onDestroy()
    }

    private fun scanLeDevices() {
        if (!scanning) {
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
            }, SCAN_PERIOD)

            val scanFilter = ScanFilter.Builder()
                .setServiceUuid(dlinkServiceUuid)
                .build()

            scanning = true

            bluetoothLeScanner.startScan(listOf(scanFilter), ScanSettings.Builder().build(), leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            result?.device?.let { device ->
                Log.d(TAG, "Device found: " + device.name + ": " + device.address)
                bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        when (newState) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                bluetoothGatt.discoverServices()
                            }
                        }
                    }

                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        for(service in bluetoothGatt.services) {
                            Log.d(TAG, "Service: " + service.uuid.toString())
                            for(characteristic in service.characteristics) {
                                when (characteristic.uuid.toString()) {
                                    characteristics["unlock"] -> {
                                        Log.d(TAG, "Characteristics: " + characteristic.uuid.toString())
                                        unlock = characteristic
                                        bluetoothGatt.readCharacteristic(characteristic)
                                    }
                                    characteristics["lastAction"] -> {
                                        lastAction = characteristic
                                    }
                                    characteristics["apList"] -> {
                                        apList = characteristic
                                    }
                                    characteristics["wifiSettings"] -> {
                                        wifiSettings = characteristic
                                    }
                                    characteristics["connectWifi"] -> {
                                        connectWifi = characteristic
                                    }
                                    characteristics["checkConnection"] -> {
                                        checkConnection = characteristic
                                    }
                                    characteristics["encryptedMessage"] -> {
                                        encryptedMessage = characteristic
                                    }
                                    characteristics["startRegistration"] -> {
                                        startRegistration = characteristic
                                    }
                                    characteristics["registrationStatus"] -> {
                                        registrationStatus = characteristic
                                    }
                                }
                            }
                        }
                    }

                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        when (characteristic) {
                            unlock -> {
                                unlock(device, characteristic)
                            }
                            apList -> {
                               handleNetworks(characteristic)
                            }
                            checkConnection -> {
                                val contents = String(characteristic.value)
                                out("Connection: $contents", false)
                            }
                            wifiSettings -> {
                                val contents = String(characteristic.value)
                                val ssid = String(Base64.decode(contents.getSSID().substringBefore(";"), Base64.DEFAULT))
                                out("Wi fi settings: $contents\nSSID: $ssid", false)
                            }
                            lastAction -> {
                                val contents = String(characteristic.value)
                                out("Last action: $contents")
                            }
                            registrationStatus -> {
                                val contents = String(characteristic.value)
                                out("Registration status: $contents")
                            }
                            else -> {
                                handleDefault(characteristic)
                            }
                        }
                    }

                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?
                    ) {
                        Log.d(TAG, "Characteristic changed: " + characteristic?.uuid.toString())
                    }

                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        when (characteristic) {
                            unlock -> {
                                out("Unlocked")
                            }
                            encryptedMessage -> {
                                out("Encrypted message written.", false)
                            }
                            connectWifi -> {
                                out("Connect to wi fi written.", false)
                            }
                            wifiSettings -> {
                                val contents = String(wifiSettings.value)
                                out("Wi fi settings unencrypted: $contents", false)
                            }
                            startRegistration -> {
                                val contents = String(startRegistration.value)
                                out("Start registration: $contents")
                            }
                        }
                    }
                })
                bluetoothLeScanner.stopScan(this)
            }
        }
    }

    private fun unlock(device: BluetoothDevice, characteristic: BluetoothGattCharacteristic) {
        val contents = String(characteristic.value!!)
        Log.d(TAG, "Unlock read: $contents")

        val challenge = contents.getChallenge()
        Log.d(TAG, "challenge:$challenge")
        val name = device.name
        Log.d(TAG, "device name: $name")
        activity?.runOnUiThread {
            name_content.text = name
            challenge_content.text = challenge
        }

        val pin = pin.text
        val stringToHash = "${name}$pin$challenge"
        val md5hash = md5(stringToHash).take(12).toByteArray()
        val unlockKey = Base64.encodeToString(md5hash, Base64.DEFAULT)
        Log.d(TAG, "String to hash: $stringToHash")
        Log.d(TAG, "Unlock key: $unlockKey")
        val paramsToUnlock = "M=0;K=$unlockKey"
        characteristic.value = paramsToUnlock.toByteArray()
        val success = bluetoothGatt.writeCharacteristic(characteristic)
        Log.d(TAG, "successful writing: $success")
        out(stringToHash, false)
    }

    private fun handleNetworks(characteristic: BluetoothGattCharacteristic) {
        val value = String(characteristic.value!!)

        val totalPages = value.getAllPages()
        val currentPage = value.getCurrentPage()
        val contents = value.getContents()

        if (!this::networksBuilder.isInitialized) {
            networksBuilder = StringBuilder()
        }

        networksBuilder.append(contents)

        if (currentPage < totalPages) {
            bluetoothGatt.readCharacteristic(apList)
        }

        Log.d(TAG, "Char read: ${characteristic.uuid}")
        Log.d(TAG, "Total pages: $totalPages")
        Log.d(TAG, "Current page: $currentPage")
        Log.d(TAG, "Contents: $contents")

        if (currentPage == totalPages) {
            val networks = networksBuilder.toString()
                    .toNetworksList()
                    .map { it.getSSID() }
            var text  = ""
            networks.forEach {
                Log.d(TAG, "Found network: $it")
                text += it
                text += "\n"
            }

            out(text, false)
        }
    }

    private fun handleDefault(characteristic: BluetoothGattCharacteristic?) {
        val contents = String(characteristic?.value!!)
        Log.d(TAG, "Handled default value: $contents")
        out(contents, false)
    }

    private fun md5(input: String): ByteArray {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray())
    }

    private fun out(text: String, append: Boolean = true) {
        val current = if (append) output.text.toString() + "\n" else ""
        activity?.runOnUiThread {
            output.setText(current + text)
        }
    }
}

fun String.getContents() =
    substringAfter(";")
    .substringAfter(";")

fun String.getCurrentPage() =
    substringAfter(";")
    .substringBefore(";")
    .removePrefix("P=")
    .toInt()

fun String.getAllPages() =
    substringBefore(";")
    .removePrefix("N=")
    .toInt()

fun String.getSSID() =
    substringAfter("I=")
    .substringBefore(",")

fun String.getChallenge() =
        split(';')
        .find { it.startsWith("C=") }
        ?.substringAfter("C=")

fun String.toNetworksList() =
        split("&")

fun String.getConnectionStatus() =
        substringAfter("S=")