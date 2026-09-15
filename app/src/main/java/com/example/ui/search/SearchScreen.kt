package com.example.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TabloChannel
import com.example.model.TabloRecording
import com.example.ui.picker.ChannelRowItem
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.ActiveAudioPill
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceRaised
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    channels: List<TabloChannel>,
    recordings: List<TabloRecording>,
    onSelectChannel: (TabloChannel) -> Unit,
    onSelectRecording: (TabloRecording) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }

    val matchedChannels = remember(query, channels) {
        if (query.isBlank()) emptyList()
        else channels.filter {
            it.displayName.contains(query, ignoreCase = true) ||
            it.network.contains(query, ignoreCase = true) ||
            (it.currentProgram?.title?.contains(query, ignoreCase = true) == true)
        }
    }

    val matchedRecordings = remember(query, recordings) {
        if (query.isBlank()) emptyList()
        else recordings.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(24.dp)
            .testTag("search_screen")
    ) {
        // Search Input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            placeholder = {
                Text("Search channels, live programs, or DVR recordings...", color = TextSecondary)
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = ActiveAudioPill,
                unfocusedBorderColor = SurfaceBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Quick Search Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            listOf("Sports", "News", "NBC", "CBS", "FOX", "ABC", "Drama").forEach { chip ->
                Button(
                    onClick = { query = chip },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceRaised),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(chip, fontSize = 11.sp)
                }
            }
        }

        // Search Results List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (matchedChannels.isNotEmpty()) {
                item {
                    Text(
                        text = "Matching Live Channels (${matchedChannels.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(matchedChannels, key = { it.identifier }) { ch ->
                    ChannelRowItem(
                        channel = ch,
                        isFavorite = false,
                        onToggleFavorite = { },
                        onSelect = { onSelectChannel(ch) }
                    )
                }
            }

            if (matchedRecordings.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Matching DVR Recordings (${matchedRecordings.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(matchedRecordings, key = { it.identifier }) { rec ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRecording(rec) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(rec.description, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                            }
                            Button(
                                onClick = { onSelectRecording(rec) },
                                colors = ButtonDefaults.buttonColors(containerColor = ActiveAudioPill),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Play", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (query.isNotEmpty() && matchedChannels.isEmpty() && matchedRecordings.isEmpty()) {
                item {
                    Text("No matching results for \"$query\"", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}
