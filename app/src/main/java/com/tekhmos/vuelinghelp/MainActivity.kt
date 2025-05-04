package com.tekhmos.vuelinghelp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.tekhmos.vuelinghelp.ui.VisualUI1
import com.tekhmos.vuelinghelp.ui.VuelingDarkColorScheme
import com.tekhmos.vuelinghelp.ui.mainScreen
import com.tekhmos.vuelinghelp.viewmodel.MessageData
import com.tekhmos.vuelinghelp.viewmodel.NearbyViewModel
import kotlinx.coroutines.delay
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.*

fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

class MainActivity : ComponentActivity() {

    private val serviceId = "com.tekhmos.vuelinghelp.SERVICE"
    private val strategy = Strategy.P2P_CLUSTER
    private val userName: String by lazy { getUsername() }
    private val seenMessages = mutableSetOf<String>()
    private val viewModel: NearbyViewModel by viewModels()

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.NEARBY_WIFI_DEVICES,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Usamos un estado para controlar qué pantalla mostrar
            var showSplashScreen by remember { mutableStateOf(true) }
            var hasPermissions by remember { mutableStateOf(hasAllPermissions()) }

            LaunchedEffect(Unit) {
                delay(2000)
                showSplashScreen = false
            }

            // Permission launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                hasPermissions = permissions.values.all { it }
            }

            // Theme wrapper
            MaterialTheme(colorScheme = mainScreen) {
                when {
                    showSplashScreen -> {
                        MaterialTheme(colorScheme = VuelingDarkColorScheme) {
                            // Pantalla de carga
                            VisualUI1()
                        }
                    }

                    !hasPermissions -> {
                        MaterialTheme(colorScheme = VuelingDarkColorScheme) {
                            PermissionDeniedScreen(onRequestPermissions = {
                                permissionLauncher.launch(requiredPermissions)
                            })
                        }
                    }

                    else -> {
                        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        val savedUsername = sharedPref.getString("username", null)
                        val showLoginScreen = remember { mutableStateOf(savedUsername.isNullOrBlank()) }

                        if (showLoginScreen.value) {
                            MaterialTheme(colorScheme = VuelingDarkColorScheme) {
                                LoginScreen(onLogin = { username ->
                                    saveUsername(username)
                                    showLoginScreen.value = false
                                })
                            }
                        } else {
                            MainAppContent()
                        }
                    }
                }
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    @Composable
    fun PermissionDeniedScreen(
        onRequestPermissions: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Para continuar, necesitas conceder permisos de ubicación y dispositivos cercanos.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Conceder permisos")
            }
        }
    }

    private fun getUsername(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("username", android.os.Build.MODEL) ?: android.os.Build.MODEL
    }

    private fun saveUsername(username: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putString("username", username)
            apply()
        }
    }

    private fun checkLocationAndStartNearby() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!enabled) {
            Toast.makeText(this, "Activa la ubicación del dispositivo", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        startNearby()
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
                Log.d("Nearby", "Anuncio iniciado")
            }
            .addOnFailureListener { e ->
                Log.e("Nearby", "Error al anunciar", e)
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
                Log.d("Nearby", "Descubrimiento iniciado")
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
                viewModel.markConnected(endpointId, true)
                Log.d("Nearby", "Conectado a ${viewModel.devices.value[endpointId]?.name}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            viewModel.markConnected(endpointId, false)
            Log.d("Nearby", "Desconectado de $endpointId")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            viewModel.addOrUpdateDevice(endpointId, info.endpointName, false)
            Nearby.getConnectionsClient(applicationContext)
                .requestConnection(userName, endpointId, connectionLifecycleCallback)
        }

        override fun onEndpointLost(endpointId: String) {
            viewModel.removeDevice(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val raw = payload.asBytes()?.toString(Charsets.UTF_8) ?: return

            val json = JSONObject(raw)
            val timestamp = json.optLong("timestamp")
            val originDevice = json.optString("originDevice")
            val messageId = "$originDevice:$timestamp"

            if (originDevice == userName || seenMessages.contains(messageId)) return

            seenMessages.add(messageId)

            val type = json.optString("type")
            val messageData = when (type) {
                "message" -> MessageData(
                    from = originDevice,
                    timestamp = timestamp,
                    type = "message",
                    content = json.optString("message"),
                    infoLevel = json.optString("infoLevel")
                )
                "flight-info" -> MessageData(
                    from = originDevice,
                    timestamp = timestamp,
                    type = "flight-info",
                    content = "",
                    flightNumber = json.optString("flightNumber"),
                    newGate = json.optString("newGate"),
                    newDeparture = json.optString("newDeparture"),
                    newArrival = json.optString("newArrival")
                )
                else -> null
            }

            messageData?.let { viewModel.addStructuredMessage(it) }

            // Reenvío
            val relay = Payload.fromBytes(json.toString().toByteArray(Charsets.UTF_8))
            viewModel.devices.value.forEach { (id, device) ->
                if (device.isConnected && id != endpointId) {
                    Nearby.getConnectionsClient(applicationContext).sendPayload(id, relay)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private fun sendMessage(content: String, infoLevel: String = "normal") {
        val json = JSONObject().apply {
            put("timestamp", System.currentTimeMillis())
            put("originDevice", userName)
            put("type", "message")
            put("infoLevel", infoLevel)
            put("message", content)
        }

        val payload = Payload.fromBytes(json.toString().toByteArray(Charsets.UTF_8))
        viewModel.devices.value.forEach { (endpointId, device) ->
            if (device.isConnected) {
                Nearby.getConnectionsClient(applicationContext).sendPayload(endpointId, payload)
            }
        }

        viewModel.addMessage("me", content, infoLevel)

    }

    @Composable
    fun LoginScreen(onLogin: (String) -> Unit) {
        var username by remember { mutableStateOf("") }
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Introduce tu nombre de usuario",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = {
                    Text("Nombre de usuario", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onLogin(username)
                    } else {
                        Toast.makeText(context, "El nombre de usuario no puede estar vacío", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Acceder")
            }
        }
    }

    @Composable
    fun MainAppContent() {
        var criticalInfo by remember { mutableStateOf(" --- CRITICAL INFO ---") }
        val context = LocalContext.current
        checkLocationAndStartNearby()
        MaterialTheme(colorScheme = VuelingDarkColorScheme) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                // Refrescar cuando se pulsa
                fun refreshNearby() {
                    checkLocationAndStartNearby()
                    Toast.makeText(
                        context,
                        "Actualizando dispositivos cercanos...",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Botón de recarga
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { refreshNearby() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Actualizar")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Título  información crítica
                Text(
                    text = "Información Importante",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.height(4.dp))

                // Información crítica
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = criticalInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                DeviceList(viewModel = viewModel)
                Spacer(Modifier.height(16.dp))
                ChatUI(viewModel = viewModel, onSend = { msg -> sendMessage(msg) })
            }
        }
    }

    @Composable
    fun DeviceList(viewModel: NearbyViewModel) {
        val devices by viewModel.devices.collectAsState()
        Column {
            Text("Dispositivos cercanos:", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 90.dp)
            ) {
                items(devices.values.toList()) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (device.isConnected)
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(device.name, style = MaterialTheme.typography.bodyMedium)
                            Text(if (device.isConnected) "Conectado ✅" else "No conectado ❌", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ChatUI(
        viewModel: NearbyViewModel,
        onSend: (String) -> Unit
    ) {
        val messages by viewModel.messages.collectAsState()
        var text by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.scrollToItem(messages.lastIndex)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text(
                "Chat",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                state = listState
            ) {
                items(messages) { msg ->
                    val isMe = msg.from == "me"
                    val align = if (isMe) Arrangement.End else Arrangement.Start

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = align
                    ) {
                        Column(
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                            modifier = Modifier.widthIn(max = 300.dp)
                        ) {
                            Text(
                                text = if (isMe) "Yo" else msg.from,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        msg.type == "message" && msg.infoLevel == "critical" -> MaterialTheme.colorScheme.errorContainer
                                        msg.type == "message" && isMe -> MaterialTheme.colorScheme.primaryContainer
                                        msg.type == "flight-info" -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    when (msg.type) {
                                        "message" -> {
                                            Text(msg.content)
                                            if (msg.infoLevel == "critical") {
                                                Text("⚠️ CRÍTICO", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                        "flight-info" -> {
                                            Text("✈️ Info vuelo ${msg.flightNumber}")
                                            msg.newGate?.takeIf { it != "9999" }?.let {
                                                Text("➡️ Nueva puerta: $it")
                                            }
                                            msg.newDeparture?.takeIf { it != "9999" }?.let {
                                                Text("🕐 Nueva salida: $it")
                                            }
                                            msg.newArrival?.takeIf { it != "9999" }?.let {
                                                Text("🕘 Nueva llegada: $it")
                                            }
                                        }
                                    }

                                    Text(
                                        text = formatTime(msg.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Mensaje") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Button(onClick = {
                    if (text.isNotBlank()) {
                        onSend(text)
                        text = ""
                    }
                }) {
                    Text("Enviar")
                }
            }
        }
    }
}