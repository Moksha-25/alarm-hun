package com.example

import android.content.res.Configuration
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldClockRoute(viewModel: AlarmViewModel) {
    val cities by viewModel.worldClockCities.collectAsState()
    val favorites by viewModel.worldClockFavorites.collectAsState()

    WorldClockScreen(
        cities = cities,
        favorites = favorites,
        onAddWorldClockCity = { viewModel.addWorldClockCity(it) },
        onRemoveWorldClockCity = { viewModel.removeWorldClockCity(it) },
        onToggleWorldClockFavorite = { viewModel.toggleWorldClockFavorite(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    cities: List<String>,
    favorites: List<String>,
    onAddWorldClockCity: (String) -> Unit,
    onRemoveWorldClockCity: (String) -> Unit,
    onToggleWorldClockFavorite: (String) -> Unit
) {
    var showAddCityDialog by remember { mutableStateOf(false) }
    var addCitySearchQuery by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    // Real-time ticker
    var tickTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tickTrigger++
        }
    }

    var screenSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoltBg)
            .padding(horizontal = 16.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "World Clock",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Track global times and offsets",
                    fontSize = 12.sp,
                    color = VoltTextSecondary
                )
            }

            FloatingActionButton(
                onClick = {
                    HapticUtils.triggerFabTap(haptic)
                    addCitySearchQuery = ""
                    showAddCityDialog = true
                },
                containerColor = VoltAccentViolet,
                contentColor = Color.White,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add City", modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Local Clock Card displaying real-time sweeping analog clock
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = VoltSurface),
            border = BorderStroke(1.dp, VoltBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "LOCAL SYSTEM TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VoltAccentViolet,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SweepingAnalogClock(sizeDp = 140.dp)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val localTimeStr = remember(tickTrigger) {
                    val calendar = Calendar.getInstance()
                    val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy · hh:mm:ss a", Locale.getDefault())
                    sdf.format(calendar.time)
                }
                
                Text(
                    text = localTimeStr,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltTextPrimary,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Premium Search Bar
        OutlinedTextField(
            value = screenSearchQuery,
            onValueChange = { screenSearchQuery = it },
            placeholder = { Text("Search added cities...", color = VoltTextSecondary, fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = VoltAccentViolet) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("world_clock_search_bar"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoltAccentViolet,
                unfocusedBorderColor = VoltBorder,
                focusedContainerColor = VoltSurface,
                unfocusedContainerColor = VoltSurface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            singleLine = true
        )

        // Filter cities
        val filteredCities = cities.filter { cityId ->
            val cityData = WorldClockData.allCities.find { it.timezoneId == cityId }
            cityData?.name?.contains(screenSearchQuery, ignoreCase = true) == true ||
                    cityId.contains(screenSearchQuery, ignoreCase = true)
        }

        if (cities.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    imageResId = R.drawable.ic_empty_globe,
                    title = "No World Clocks",
                    subtitle = "Add international cities to monitor times and offsets worldwide.",
                    actionText = "Add City",
                    onActionClick = {
                        HapticUtils.triggerFabTap(haptic)
                        addCitySearchQuery = ""
                        showAddCityDialog = true
                    }
                )
            }
        } else if (filteredCities.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No added cities match \"$screenSearchQuery\"",
                    color = VoltTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Favorites Section
                val favCities = filteredCities.filter { favorites.contains(it) }
                if (favCities.isNotEmpty()) {
                    item {
                        Text(
                            text = "PINNED CITIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.12.sp,
                            color = VoltAccentViolet,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(favCities, key = { _, cityId -> "fav_$cityId" }) { index, cityId ->
                        val cityData = WorldClockData.allCities.find { it.timezoneId == cityId }
                        if (cityData != null) {
                            WorldClockCityCard(
                                city = cityData,
                                isFavorite = true,
                                index = index,
                                tickTrigger = tickTrigger,
                                onToggleFavorite = {
                                    HapticUtils.triggerTick(haptic)
                                    onToggleWorldClockFavorite(cityId)
                                },
                                onDelete = {
                                    HapticUtils.triggerTick(haptic)
                                    onRemoveWorldClockCity(cityId)
                                }
                            )
                        }
                    }
                }

                // Others Section
                val otherCities = filteredCities.filter { !favorites.contains(it) }
                if (otherCities.isNotEmpty()) {
                    item {
                        Text(
                            text = "ALL CITIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.12.sp,
                            color = VoltTextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    itemsIndexed(otherCities, key = { _, cityId -> "other_$cityId" }) { index, cityId ->
                        val cityData = WorldClockData.allCities.find { it.timezoneId == cityId }
                        if (cityData != null) {
                            WorldClockCityCard(
                                city = cityData,
                                isFavorite = false,
                                index = index + favCities.size,
                                tickTrigger = tickTrigger,
                                onToggleFavorite = {
                                    HapticUtils.triggerTick(haptic)
                                    onToggleWorldClockFavorite(cityId)
                                },
                                onDelete = {
                                    HapticUtils.triggerTick(haptic)
                                    onRemoveWorldClockCity(cityId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Search Dialog
    if (showAddCityDialog) {
        Dialog(onDismissRequest = { showAddCityDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = BorderStroke(1.dp, VoltBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Search Location",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = addCitySearchQuery,
                        onValueChange = { addCitySearchQuery = it },
                        placeholder = { Text("Search location...", color = VoltTextSecondary, fontSize = 14.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = VoltAccentViolet) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VoltAccentViolet,
                            unfocusedBorderColor = VoltBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val matchingCities = WorldClockData.allCities.filter {
                        it.name.contains(addCitySearchQuery, ignoreCase = true) ||
                                it.timezoneId.contains(addCitySearchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(matchingCities) { _, city ->
                            val isAlreadyAdded = cities.contains(city.timezoneId)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyAdded) {
                                        HapticUtils.triggerTick(haptic)
                                        onAddWorldClockCity(city.timezoneId)
                                        showAddCityDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(city.flagEmoji, fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(city.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(city.timezoneId, color = VoltTextSecondary, fontSize = 11.sp)
                                    }
                                }

                                if (isAlreadyAdded) {
                                    Text("Added", color = VoltAccentViolet, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = VoltTextSecondary)
                                }
                            }
                            Divider(color = VoltDivider)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorldClockCityCard(
    city: WorldClockCity,
    isFavorite: Boolean,
    index: Int,
    tickTrigger: Int,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    // Calculate current time inside the selected time zone
    val timeString = remember(tickTrigger, city.timezoneId) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(city.timezoneId))
        val sdf = SimpleDateFormat("hh:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(city.timezoneId)
        }
        sdf.format(calendar.time)
    }

    val amPmString = remember(tickTrigger, city.timezoneId) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(city.timezoneId))
        val sdf = SimpleDateFormat("a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(city.timezoneId)
        }
        sdf.format(calendar.time)
    }

    val dateString = remember(tickTrigger, city.timezoneId) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(city.timezoneId))
        val sdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone(city.timezoneId)
        }
        sdf.format(calendar.time)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index)
            .pressScale {},
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = BorderStroke(1.dp, VoltBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(city.flagEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = city.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$dateString (${city.offsetDisplay})",
                    fontSize = 12.sp,
                    color = VoltTextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeString,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = amPmString,
                            fontSize = 12.sp,
                            color = VoltTextSecondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Pin City",
                        tint = if (isFavorite) VoltAccentViolet else VoltTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = VoltTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview(
    name = "World Clock Screen Preview",
    showBackground = true,
    showSystemUi = true,
    backgroundColor = 0xFF121820,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = Devices.PIXEL_6
)
@Composable
fun WorldClockScreenPreview() {
    VoltAlarmTheme {
        WorldClockScreen(
            cities = listOf("America/New_York", "Europe/London", "Asia/Tokyo"),
            favorites = listOf("America/New_York"),
            onAddWorldClockCity = {},
            onRemoveWorldClockCity = {},
            onToggleWorldClockFavorite = {}
        )
    }
}
