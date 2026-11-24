package com.spectro.app.core.connect

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import com.spectro.app.core.innertube.MusicOrigin
import com.spectro.app.core.playback.PlayerConnection
import com.spectro.app.features.auth.data.AccountClient
import com.spectro.app.features.auth.data.AccountSession
import com.spectro.app.features.auth.data.AccountStore
import com.spectro.app.features.auth.data.ProfilePhotoStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

// Connects Android playback and session with Spectro Desktop over MQTT relay (Spotify Connect style)
object SpectroConnectManager {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> /* Ignore background sync errors */ }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var syncJob: Job? = null
    private var listenerJob: Job? = null
    private val clientId = "spectro_android_" + UUID.randomUUID().toString().substring(0, 8)
    private const val MQTT_HOST = "broker.emqx.io"
    private const val MQTT_PORT = 1883
    private const val TOPIC = "spectro/connect/sync"

    private val _connectedDeviceFlow = MutableStateFlow<String?>(null)
    val connectedDeviceFlow: StateFlow<String?> = _connectedDeviceFlow.asStateFlow()

    fun start(context: Context) {
        val prefs = context.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
        _connectedDeviceFlow.value = prefs.getString("connected_device", null)

        startPlaybackSync()
        startControlListener(context.applicationContext)
    }

    fun restartListener(context: Context) {
        listenerJob?.cancel()
        listenerJob = null
        startControlListener(context.applicationContext)
    }

    private fun startPlaybackSync() {
        if (syncJob != null) return
        syncJob = scope.launch {
            var lastTrackId: String? = null
            var lastIsPlaying: Boolean? = null

            // Observe player changes
            launch {
                PlayerConnection.track.collect { track ->
                    if (track?.id != lastTrackId) {
                        lastTrackId = track?.id
                        broadcastCurrentState()
                    }
                }
            }

            launch {
                PlayerConnection.isPlaying.collect { isPlaying ->
                    if (isPlaying != lastIsPlaying) {
                        lastIsPlaying = isPlaying
                        broadcastCurrentState()
                    }
                }
            }

            // Periodic progress sync every 3 seconds while playing
            while (isActive) {
                delay(3000)
                if (PlayerConnection.isPlaying.value) {
                    broadcastCurrentState()
                }
            }
        }
    }

    private fun getDeviceName(): String {
        val model = Build.MODEL ?: "Android"
        val manufacturer = Build.MANUFACTURER ?: ""
        val full = if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
        return "Android ($full)"
    }

    private fun broadcastCurrentState() {
        try {
            val track = PlayerConnection.track.value ?: return
            val isPlaying = PlayerConnection.isPlaying.value
            val posSec = PlayerConnection.positionMs / 1000.0
            val durSec = PlayerConnection.durationMs / 1000.0

            val artUrl = track.artworkUrl ?: "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg"

            val json = JSONObject().apply {
                put("source", "android")
                put("device", getDeviceName())
                put("isPlaying", isPlaying)
                put("currentTime", posSec)
                put("timestamp", System.currentTimeMillis())
                put("track", JSONObject().apply {
                    put("id", track.id)
                    put("videoId", track.id)
                    put("title", track.title)
                    put("artist", track.artist ?: "Artis")
                    put("art", artUrl)
                    put("duration", durSec)
                })
            }

            scope.launch(Dispatchers.IO) {
                sendMqttPublish(TOPIC, json.toString())
            }
        } catch (_: Exception) {
            // Never crash playback on state broadcast failure
        }
    }

    fun syncAccountToDesktop(
        pairingCode: String,
        context: Context,
        onResult: (Boolean, String?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val cleanCode = pairingCode.replace("-", "").replace(" ", "").trim()
                if (cleanCode.length != 6) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Kode pairing harus 6 digit angka.")
                    }
                    return@launch
                }

                val accountStore = AccountStore(context)
                val cookieManager = CookieManager.getInstance()
                var cookies = cookieManager.getCookie(MusicOrigin) ?: ""
                if (cookies.isBlank()) {
                    cookies = accountStore.cookies.firstOrNull() ?: ""
                }

                if (cookies.isBlank() || (!cookies.contains("SAPISID=") && !cookies.contains("__Secure-3PAPISID="))) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Akun Google belum login di Spectro HP ini.")
                    }
                    return@launch
                }

                // Refresh and verify account details with YouTube Music channel profile
                val freshAccount = AccountClient.fetchAccount(cookies).getOrNull()
                if (freshAccount != null) {
                    AccountSession.apply(context, freshAccount)
                }

                val details = accountStore.details.firstOrNull()
                val photoUrl = freshAccount?.photoUrl ?: ProfilePhotoStore.getPhotoUrl(context).orEmpty()
                val name = freshAccount?.name?.takeIf { it.isNotBlank() }
                    ?: details?.name?.takeIf { it.isNotBlank() }
                    ?: "Invisible"
                val handle = freshAccount?.handle ?: details?.handle.orEmpty()
                val photoBytes = ProfilePhotoStore.getPhotoBytes(context)
                val photoBase64 = if (photoBytes != null) {
                    android.util.Base64.encodeToString(photoBytes, android.util.Base64.NO_WRAP)
                } else ""
                val pairId = "pair_$cleanCode"
                val topic = "spectro/sync/pair/$cleanCode"

                val payload = JSONObject().apply {
                    put("type", "spectro_account_sync")
                    put("code", cleanCode)
                    put("pair_id", pairId)
                    put("name", name)
                    put("handle", handle)
                    put("photo_url", photoUrl)
                    put("photo_base64", photoBase64)
                    put("cookies", cookies)
                    put("device", getDeviceName())
                    put("timestamp", System.currentTimeMillis())
                }

                val sent = sendMqttPublish(topic, payload.toString())
                withContext(Dispatchers.Main) {
                    if (sent) {
                        val prefs = context.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("pair_id", pairId)
                            .putString("connected_device", "Spectro Windows (Desktop PC)")
                            .apply()
                        _connectedDeviceFlow.value = "Spectro Windows (Desktop PC)"
                        restartListener(context)
                        onResult(true, null)
                    } else {
                        onResult(false, "Gagal terhubung ke relay broker MQTT. Periksa koneksi internet.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Gagal mengirim sesi")
                }
            }
        }
    }

    fun notifyLogout(context: Context) {
        try {
            val prefs = context.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
            prefs.edit().remove("connected_device").remove("pair_id").apply()
            _connectedDeviceFlow.value = null
        } catch (_: Exception) {}
    }

    fun disconnect(context: Context, onComplete: (() -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
                val pairId = prefs.getString("pair_id", null)
                if (!pairId.isNullOrBlank()) {
                    val topic = "spectro/pair/$pairId/control"
                    val payload = JSONObject().apply {
                        put("type", "spectro_control")
                        put("action", "disconnect")
                        put("source", "android")
                        put("pair_id", pairId)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendMqttPublish(topic, payload.toString())
                }
                prefs.edit().remove("connected_device").remove("pair_id").apply()
                _connectedDeviceFlow.value = null
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                onComplete?.invoke()
            }
        }
    }

    private fun startControlListener(appContext: Context) {
        if (listenerJob != null) return
        listenerJob = scope.launch(Dispatchers.IO) {
            val prefs = appContext.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
            while (isActive) {
                val pairId = prefs.getString("pair_id", null)
                if (pairId.isNullOrBlank()) {
                    delay(3000)
                    continue
                }

                val controlTopic = "spectro/pair/$pairId/control"
                var socket: Socket? = null
                try {
                    socket = Socket()
                    socket.connect(InetSocketAddress(MQTT_HOST, MQTT_PORT), 4000)
                    socket.soTimeout = 15000
                    val out = socket.getOutputStream()
                    val input = socket.getInputStream()

                    val subClientId = "spectro_sub_" + UUID.randomUUID().toString().substring(0, 8)

                    // 1. MQTT CONNECT packet
                    val connectVarHeader = byteArrayOf(
                        0x00, 0x04, 'M'.code.toByte(), 'Q'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(),
                        0x04, // Level 4 (3.1.1)
                        0x02, // Clean session flag
                        0x00, 0x3C // Keepalive 60s
                    )
                    val clientIdBytes = subClientId.toByteArray(Charsets.UTF_8)
                    val connectPayload = ByteArrayOutputStream().apply {
                        write(byteArrayOf((clientIdBytes.size shr 8).toByte(), (clientIdBytes.size and 0xFF).toByte()))
                        write(clientIdBytes)
                    }.toByteArray()

                    val connectPacket = ByteArrayOutputStream().apply {
                        write(0x10) // CONNECT
                        val remaining = connectVarHeader.size + connectPayload.size
                        writeRemainingLength(this, remaining)
                        write(connectVarHeader)
                        write(connectPayload)
                    }.toByteArray()

                    out.write(connectPacket)
                    out.flush()

                    // Read CONNACK (4 bytes)
                    val connAck = ByteArray(4)
                    var readLen = 0
                    while (readLen < 4) {
                        val c = input.read(connAck, readLen, 4 - readLen)
                        if (c < 0) break
                        readLen += c
                    }
                    if (readLen < 4 || connAck[0].toInt() != 0x20 || connAck[3].toInt() != 0) {
                        socket.close()
                        delay(4000)
                        continue
                    }

                    // 2. MQTT SUBSCRIBE packet (0x82)
                    val topicBytes = controlTopic.toByteArray(Charsets.UTF_8)
                    val subPayload = ByteArrayOutputStream().apply {
                        write(byteArrayOf((topicBytes.size shr 8).toByte(), (topicBytes.size and 0xFF).toByte()))
                        write(topicBytes)
                        write(0x00) // QoS 0
                    }.toByteArray()

                    val subPacket = ByteArrayOutputStream().apply {
                        write(0x82) // SUBSCRIBE
                        val remaining = 2 + subPayload.size
                        writeRemainingLength(this, remaining)
                        write(0x00) // Packet ID MSB
                        write(0x01) // Packet ID LSB
                        write(subPayload)
                    }.toByteArray()

                    out.write(subPacket)
                    out.flush()

                    // Read SUBACK (5 bytes)
                    val subAck = ByteArray(5)
                    readLen = 0
                    while (readLen < 5) {
                        val c = input.read(subAck, readLen, 5 - readLen)
                        if (c < 0) break
                        readLen += c
                    }

                    // 3. Blocking receive loop with SocketTimeoutException keepalive
                    while (isActive && !socket.isClosed) {
                        try {
                            val headerByte = input.read()
                            if (headerByte < 0) break // Stream closed by broker
                            val remLength = readRemainingLength(input)
                            val packetBytes = ByteArray(remLength)
                            var offset = 0
                            while (offset < remLength) {
                                val c = input.read(packetBytes, offset, remLength - offset)
                                if (c < 0) break
                                offset += c
                            }

                            // If PUBLISH packet (0x30 to 0x3F)
                            if ((headerByte and 0xF0) == 0x30 && remLength >= 2) {
                                val tLen = ((packetBytes[0].toInt() and 0xFF) shl 8) or (packetBytes[1].toInt() and 0xFF)
                                if (remLength >= 2 + tLen) {
                                    val rTopic = String(packetBytes, 2, tLen, Charsets.UTF_8)
                                    val payloadStr = String(packetBytes, 2 + tLen, remLength - (2 + tLen), Charsets.UTF_8)
                                    handleIncomingControl(appContext, rTopic, payloadStr)
                                }
                            }
                        } catch (_: java.net.SocketTimeoutException) {
                            // 15 seconds idle: send MQTT PINGREQ (0xC0, 0x00) to keep broker connection alive
                            try {
                                out.write(byteArrayOf(0xC0.toByte(), 0x00))
                                out.flush()
                            } catch (_: Exception) {
                                break // Broken socket, break and reconnect
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Socket error, retry
                } finally {
                    try { socket?.close() } catch (_: Exception) {}
                }
                delay(3000)
            }
        }
    }

    private fun handleIncomingControl(appContext: Context, topic: String, payload: String) {
        try {
            val json = JSONObject(payload)
            val action = json.optString("action")
            if (action == "logout" || action == "disconnect" || action == "windows_disconnected" || action == "windows_uninstalled") {
                val prefs = appContext.getSharedPreferences("spectro_connect", Context.MODE_PRIVATE)
                prefs.edit().remove("connected_device").remove("pair_id").apply()
                _connectedDeviceFlow.value = null
            }
        } catch (_: Exception) {}
    }

    private fun readRemainingLength(input: InputStream): Int {
        var multiplier = 1
        var value = 0
        do {
            val b = input.read()
            if (b < 0) break
            value += (b and 127) * multiplier
            multiplier *= 128
        } while ((b and 128) != 0)
        return value
    }

    private fun sendMqttPublish(topic: String, message: String): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(MQTT_HOST, MQTT_PORT), 3000)
                socket.soTimeout = 3000
                val out = socket.getOutputStream()
                val input = socket.getInputStream()

                // MQTT 3.1.1 CONNECT packet
                val connectVarHeader = byteArrayOf(
                    0x00, 0x04, 'M'.code.toByte(), 'Q'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(),
                    0x04, // Level 4 (3.1.1)
                    0x02, // Clean session flag
                    0x00, 0x3C // Keepalive 60s
                )
                val clientIdBytes = clientId.toByteArray(Charsets.UTF_8)
                val connectPayload = ByteArrayOutputStream().apply {
                    write(byteArrayOf((clientIdBytes.size shr 8).toByte(), (clientIdBytes.size and 0xFF).toByte()))
                    write(clientIdBytes)
                }.toByteArray()

                val connectPacket = ByteArrayOutputStream().apply {
                    write(0x10) // CONNECT
                    val remaining = connectVarHeader.size + connectPayload.size
                    writeRemainingLength(this, remaining)
                    write(connectVarHeader)
                    write(connectPayload)
                }.toByteArray()

                out.write(connectPacket)
                out.flush()

                // Read CONNACK (4 bytes: 0x20, 0x02, sessionPresent, returnCode)
                val connAck = ByteArray(4)
                var readBytes = 0
                while (readBytes < 4) {
                    val count = input.read(connAck, readBytes, 4 - readBytes)
                    if (count < 0) break
                    readBytes += count
                }

                if (readBytes < 4 || connAck[0].toInt() != 0x20 || connAck[3].toInt() != 0) {
                    return false
                }

                // MQTT PUBLISH packet (QoS 0)
                val topicBytes = topic.toByteArray(Charsets.UTF_8)
                val payloadBytes = message.toByteArray(Charsets.UTF_8)
                val publishPacket = ByteArrayOutputStream().apply {
                    write(0x30) // PUBLISH QoS 0
                    val remaining = 2 + topicBytes.size + payloadBytes.size
                    writeRemainingLength(this, remaining)
                    write((topicBytes.size shr 8).toByte().toInt())
                    write((topicBytes.size and 0xFF).toByte().toInt())
                    write(topicBytes)
                    write(payloadBytes)
                }.toByteArray()

                out.write(publishPacket)
                out.flush()
                Thread.sleep(100)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun writeRemainingLength(out: ByteArrayOutputStream, length: Int) {
        var num = length
        do {
            var digit = (num % 128).toByte()
            num /= 128
            if (num > 0) {
                digit = (digit.toInt() or 0x80).toByte()
            }
            out.write(digit.toInt())
        } while (num > 0)
    }
}
