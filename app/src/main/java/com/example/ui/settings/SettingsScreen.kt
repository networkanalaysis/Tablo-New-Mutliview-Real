package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TabloRepository
import com.example.model.TabloDevice
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    repository: TabloRepository,
    activeDevice: TabloDevice?,
    isLoading: Boolean,
    statusMessage: String?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val serverInfo by repository.serverInfo.collectAsStateWithLifecycle()
    val tuners by repository.tuners.collectAsStateWithLifecycle()
    val discoveredDevices by repository.discoveredDevices.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var manualIp by remember { mutableStateOf("") }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isActionRunning by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(24.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Tablo Multiview Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Connect directly to your Tablo hardware on your local network or configure cloud options.",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Section 1: Active Device Status
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = if (activeDevice != null) AccentGreen else TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = activeDevice?.name ?: "Local Tuner Mode (Unpaired)",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (activeDevice != null) "SID: ${activeDevice.sid} • ${activeDevice.localUrl}" else "Ready to scan for Tablo on local network",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (activeDevice != null) {
                            Button(
                                onClick = { repository.logout() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect", fontSize = 12.sp, color = AccentRed)
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SurfaceRaised,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = statusMessage,
                                fontSize = 12.sp,
                                color = AccentGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Automatic Network Discovery (Documented Tablo Discovery API)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ActiveAudioPill)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Auto-Discover Tablo (No Login Required)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Scans via Tablo Assocserver & LAN UDP broadcast (Port 8881/8882)",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isActionRunning = true
                                actionMessage = "Scanning local network..."
                                scope.launch {
                                    try {
                                        val devs = repository.scanLocalNetwork()
                                        if (devs.isNotEmpty()) {
                                            actionMessage = "Discovered ${devs.size} Tablo unit(s)"
                                        } else {
                                            actionMessage = "No Tablo found. Try Direct IP below."
                                        }
                                    } catch (e: Exception) {
                                        actionMessage = "Scan error: ${e.message}"
                                    } finally {
                                        isActionRunning = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveAudioPill),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isActionRunning
                        ) {
                            if (isActionRunning) {
                                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Scan Network", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (discoveredDevices.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Discovered Tablo Devices:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        discoveredDevices.forEach { dev ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceRaised,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = dev.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text(
                                            text = "${dev.localUrl} • Tuners: ${dev.tunerCount} ${if (dev.version.isNotEmpty()) "• v${dev.version}" else ""}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            repository.saveDevice(dev)
                                            repository.refreshAll()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Hardware Details & Tuner Allocation
        if (activeDevice != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = AccentGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tablo Tuner Architecture & Hardware Status",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        val detectedTuners = serverInfo?.tunerCount ?: activeDevice.tunerCount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceRaised,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Physical Tuners", fontSize = 11.sp, color = TextSecondary)
                                    Text("$detectedTuners Tuners", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceRaised,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Device Model", fontSize = 11.sp, color = TextSecondary)
                                    Text(serverInfo?.model?.ifEmpty { "4th Gen" } ?: "4th Gen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SurfaceRaised,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Firmware Version", fontSize = 11.sp, color = TextSecondary)
                                    Text(serverInfo?.version?.ifEmpty { "Latest" } ?: "Latest", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tuner Allocation Notes: In 4-channel multiview, each simultaneous live OTA broadcast stream occupies 1 physical Tablo tuner. OTT/FAST cloud channels and duplicate channels share resources, allowing all 4 slots to run smoothly.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Section 4: Tablo Cloud Account Login
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = ActiveAudioPill)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tablo Cloud Account (Automatic Discovery)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Sign in with your Tablo account to automatically discover all 4th Gen units on your home network.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Tablo Email") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised,
                                focusedBorderColor = ActiveAudioPill,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised,
                                focusedBorderColor = ActiveAudioPill,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                isActionRunning = true
                                actionMessage = "Connecting to Tablo Cloud..."
                                scope.launch {
                                    try {
                                        val devs = repository.login(email.trim(), password.trim())
                                        actionMessage = "Successfully connected to ${devs.firstOrNull()?.name ?: "Tablo"}"
                                    } catch (e: Exception) {
                                        actionMessage = "Error: ${e.message}"
                                    } finally {
                                        isActionRunning = false
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveAudioPill),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isActionRunning && email.isNotBlank() && password.isNotBlank()
                    ) {
                        if (isActionRunning) {
                            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Sign In & Pair Tablo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Direct LAN IP Connection
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = AccentGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Direct Local IP Connection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Connect directly to your Tablo 4th Gen IP address (port 8885) on your local Wi-Fi / Ethernet.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("IP Address (e.g. 192.168.1.150)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceRaised,
                                unfocusedContainerColor = SurfaceRaised,
                                focusedBorderColor = ActiveAudioPill,
                                unfocusedBorderColor = SurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (manualIp.isNotBlank()) {
                                    isActionRunning = true
                                    actionMessage = "Probing Tablo at $manualIp:8885..."
                                    scope.launch {
                                        try {
                                            val dev = repository.connectManualIp(manualIp.trim())
                                            actionMessage = if (dev != null) {
                                                "Connected to ${dev.name}"
                                            } else {
                                                "No Tablo detected at $manualIp:8885"
                                            }
                                        } finally {
                                            isActionRunning = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isActionRunning && manualIp.isNotBlank()
                        ) {
                            Text("Connect IP", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 4: Refresh & Sync Actions
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Channel & Guide Synchronization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Re-scan OTA channels, EPG airings, and DVR recordings from Tablo",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { repository.refreshAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (actionMessage != null) {
            item {
                Text(
                    text = actionMessage!!,
                    fontSize = 13.sp,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
