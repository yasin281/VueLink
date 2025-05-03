package com.tekhmos.vuelinghelp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class MainActivity : ComponentActivity() {

    private val serviceId = "com.tekhmos.vuelinghelp.SERVICE"
    private val strategy = Strategy.P2P_CLUSTER
    private val userName = "User${System.currentTimeMillis()}"

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startNearby()
        } else {
            Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startNearby()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun startNearby() {
        val client = Nearby.getConnectionsClient(this)
        client.stopAllEndpoints()
        client.stopAdvertising()
        client.stopDiscovery()

        startAdvertising()
        startDiscovery()
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        Nearby.getConnectionsClient(this)
            .startAdvertising(
                userName,
                serviceId,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Anunciando Nearby", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Nearby", "Error al anunciar", e)
                Toast.makeText(this, "Error al anunciar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        Nearby.getConnectionsClient(this)
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Descubriendo dispositivos", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("Nearby", "Error al descubrir", e)
                Toast.makeText(this, "Error al descubrir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Nearby.getConnectionsClient(applicationContext)
                .acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                Toast.makeText(applicationContext, "Conectado con $endpointId", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Conexión fallida con $endpointId", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onDisconnected(endpointId: String) {
            Toast.makeText(applicationContext, "Desconectado de $endpointId", Toast.LENGTH_SHORT).show()
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(applicationContext)
                .requestConnection(userName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val message = payload.asBytes()?.toString(Charsets.UTF_8)
            Toast.makeText(applicationContext, "Mensaje recibido: $message", Toast.LENGTH_SHORT).show()
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
