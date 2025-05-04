package com.tekhmos.vuelinghelp.viewmodel

import androidx.lifecycle.ViewModel
import com.tekhmos.vuelinghelp.model.DeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MessageData(
    val from: String,
    val timestamp: Long,
    val type: String, // "message" o "flight-info"
    val content: String = "",
    val infoLevel: String? = null,
    val flightNumber: String? = null,
    val newGate: String? = null,
    val newDeparture: String? = null,
    val newArrival: String? = null
)

class NearbyViewModel : ViewModel() {

    private val _devices = MutableStateFlow<Map<String, DeviceInfo>>(emptyMap())
    val devices: StateFlow<Map<String, DeviceInfo>> = _devices

    private val _messages = MutableStateFlow<List<MessageData>>(emptyList())
    val messages: StateFlow<List<MessageData>> = _messages

    private val seenMessages = mutableSetOf<Pair<String, Long>>() // (from, timestamp)

    fun addOrUpdateDevice(endpointId: String, name: String, isConnected: Boolean) {
        _devices.value = _devices.value + (endpointId to DeviceInfo(endpointId, name, isConnected))
    }

    fun markConnected(endpointId: String, isConnected: Boolean) {
        _devices.value = _devices.value[endpointId]?.let {
            _devices.value + (endpointId to it.copy(isConnected = isConnected))
        } ?: _devices.value
    }

    fun removeDevice(endpointId: String) {
        _devices.value = _devices.value - endpointId
    }

    fun addMessage(from: String, message: String, infoLevel: String = "normal") {
        val msg = MessageData(
            from = from,
            timestamp = System.currentTimeMillis(),
            type = "message",
            content = message,
            infoLevel = infoLevel
        )
        addStructuredMessage(msg)
    }

    fun addStructuredMessage(msg: MessageData) {
        val key = msg.from to msg.timestamp
        if (key in seenMessages) return
        seenMessages.add(key)
        _messages.value = _messages.value + msg
    }
}
