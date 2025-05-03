package com.tekhmos.vuelinghelp.viewmodel

import androidx.lifecycle.ViewModel
import com.tekhmos.vuelinghelp.model.ChatMessage
import com.tekhmos.vuelinghelp.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NearbyViewModel : ViewModel() {
    private val _devices = MutableStateFlow<Map<String, DeviceInfo>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceInfo>> = _devices

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    fun addOrUpdateDevice(endpointId: String, name: String, isConnected: Boolean) {
        _devices.value = _devices.value.toMutableMap().apply {
            this[endpointId] = DeviceInfo(endpointId, name, isConnected)
        }
    }

    fun markConnected(endpointId: String, connected: Boolean) {
        _devices.value = _devices.value.toMutableMap().apply {
            val device = this[endpointId]
            if (device != null) {
                this[endpointId] = device.copy(isConnected = connected)
            }
        }
    }

    fun removeDevice(endpointId: String) {
        _devices.value = _devices.value.toMutableMap().apply {
            remove(endpointId)
        }
    }

    fun addMessage(from: String, msg: String) {
        _messages.value = _messages.value + ChatMessage(from, msg)
    }
}