package com.musala.dlinkbluetoothpoc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val TAG = FirstFragment::javaClass.name

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val uuid = UUID.randomUUID()

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
            val success = bluetoothAdapter.startDiscovery()
            Log.d(TAG, "Success $success")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val name = device?.name
                    val address = device?.address
                    Log.d(TAG, "Device found: $name, $address")
                }
            }
        }
    }

    override fun onDestroy() {
        bluetoothAdapter.cancelDiscovery()
        super.onDestroy()
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {

    }
}