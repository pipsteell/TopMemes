// DeviceListAdapter.kt
package com.example.topmemes.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.topmemes.R
import com.example.topmemes.network.Endpoint

class DeviceListAdapter(
    context: Context,
    private var devices: List<Endpoint>
) : ArrayAdapter<Endpoint>(context, 0, devices) {

    fun updateDevices(newDevices: List<Endpoint>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_device, parent, false)

        val device = getItem(position)
        val deviceName = view.findViewById<TextView>(R.id.deviceName)
        val deviceStatus = view.findViewById<TextView>(R.id.deviceStatus)

        deviceName.text = device?.name ?: "Unknown Device"
        deviceStatus.text = "Connected"

        return view
    }
}