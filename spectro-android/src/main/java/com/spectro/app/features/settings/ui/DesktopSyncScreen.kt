package com.spectro.app.features.settings.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectro.app.R
import com.spectro.app.core.connect.SpectroConnectManager
import com.spectro.app.core.designsystem.components.GradientButton
import com.spectro.app.core.designsystem.icons.SpectroIcons
import com.spectro.app.core.designsystem.theme.SpectroTheme
import com.spectro.app.features.settings.ui.components.SettingsGroup
import com.spectro.app.features.settings.ui.components.SettingsPage

@Composable
fun DesktopSyncScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs = remember { context.getSharedPreferences("spectro_connect", android.content.Context.MODE_PRIVATE) }
    var connectedDevice by remember { mutableStateOf(prefs.getString("connected_device", null)) }
    var pairingCode by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    fun sendSync() {
        val clean = pairingCode.replace("-", "").replace(" ", "").trim()
        if (clean.length != 6) {
            Toast.makeText(context, "Masukkan 6 digit kode dari Windows.", Toast.LENGTH_SHORT).show()
            return
        }
        keyboardController?.hide()
        isSending = true
        SpectroConnectManager.syncAccountToDesktop(clean, context) { success, err ->
            isSending = false
            if (success) {
                prefs.edit().putString("connected_device", "Spectro Windows (Desktop PC)").apply()
                connectedDevice = "Spectro Windows (Desktop PC)"
                Toast.makeText(context, context.getString(R.string.sync_desktop_success), Toast.LENGTH_LONG).show()
                onBack()
            } else {
                Toast.makeText(context, err ?: "Gagal mengirim sesi.", Toast.LENGTH_LONG).show()
            }
        }
    }

    SettingsPage(
        title = stringResource(R.string.sync_desktop_title),
        onBack = onBack,
        modifier = modifier
    ) {
        if (!connectedDevice.isNullOrBlank()) {
            SettingsGroup(title = "Perangkat Terhubung") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Status: Aktif & Terhubung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SpectroTheme.colors.playerAccent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Akun ini terhubung dengan: $connectedDevice",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        SettingsGroup(title = "Koneksi Perangkat") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SpectroIcons.Integrations,
                        contentDescription = null,
                        tint = SpectroTheme.colors.playerAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Sinkronkan Akun ke Spectro Windows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.sync_desktop_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }.take(6)
                        pairingCode = digits
                    },
                    modifier = Modifier.fillMaxWidth(0.75f),
                    placeholder = {
                        Text(
                            text = "000000",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                letterSpacing = 6.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = 6.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = SpectroTheme.colors.playerAccent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { sendSync() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SpectroTheme.colors.playerAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isSending) {
                    CircularProgressIndicator(
                        color = SpectroTheme.colors.playerAccent,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    GradientButton(
                        text = stringResource(R.string.sync_desktop_btn),
                        onClick = { sendSync() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
