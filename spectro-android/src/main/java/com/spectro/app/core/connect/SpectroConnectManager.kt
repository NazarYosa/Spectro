package com.spectro.app.core.connect

import android.content.Context
import android.os.Build
import android.webkit.CookieManager
import com.spectro.app.core.innertube.MusicOrigin
import com.spectro.app.core.playback.PlayerConnection
import com.spectro.app.features.auth.data.AccountStore
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

// Connects Android playback to Spectro Desktop over MQTT relay (Spotify Connect style)
object SpectroConnectManager {
    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> /* Ignore background sync errors */ }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private var syncJob: Job? = null
    private val clientId = "spectro_android_" + UUID.randomUUID().toString().substring(0, 8)
    private const val MQTT_HOST = "broker.emqx.io"
    private const val MQTT_PORT = 1883
    private const val TOPIC = "spectro/connect/sync"

    fun start(context: Context) {
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

                val details = accountStore.details.firstOrNull()
                val topic = "spectro/sync/pair/$cleanCode"

                val payload = JSONObject().apply {
                    put("type", "spectro_account_sync")
                    put("code", cleanCode)
                    put("name", details?.name ?: "Akun Google")
                    put("handle", details?.handle ?: "")
                    put("cookies", cookies)
                    put("device", getDeviceName())
                    put("timestamp", System.currentTimeMillis())
                }

                val sent = sendMqttPublish(topic, payload.toString())
                withContext(Dispatchers.Main) {
                    if (sent) {
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
