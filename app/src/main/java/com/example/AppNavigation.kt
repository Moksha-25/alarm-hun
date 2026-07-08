package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String) {
    object Alarms : Screen("alarms", "Alarms")
    object WorldClock : Screen("world_clock", "World Clock")
    object Timer : Screen("timer", "Timer")
    object Stopwatch : Screen("stopwatch", "Stopwatch")
    object Settings : Screen("settings", "Settings")
    
    // Sub-screens
    object AddEditAlarm : Screen("add_edit_alarm/{alarmId}", "Add/Edit Alarm")
    object RingtoneSelection : Screen("ringtone_selection", "Ringtone")
}

@Composable
fun AppNavigation(
    alarmViewModel: AlarmViewModel,
    timerViewModel: TimerViewModel,
    stopwatchViewModel: StopwatchViewModel,
    batteryViewModel: BatteryViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedRingtoneName by remember { mutableStateOf("Default") }

    Scaffold(
        containerColor = VoltBg,
        bottomBar = {
            // Only show bottom bar for primary tab screens
            val mainTabs = listOf(Screen.Alarms.route, Screen.WorldClock.route, Screen.Timer.route, Screen.Stopwatch.route)
            if (currentRoute in mainTabs) {
                NavigationBar(
                    containerColor = VoltNavBarBg,
                    tonalElevation = 8.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    // Tab 1: Alarms
                    NavigationBarItem(
                        selected = currentRoute == Screen.Alarms.route,
                        onClick = {
                            if (currentRoute != Screen.Alarms.route) {
                                navController.navigate(Screen.Alarms.route) {
                                    popUpTo(Screen.Alarms.route) { inclusive = true }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = "Alarms") },
                        label = { Text("Alarms") },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAccentViolet,
                            unselectedIconColor = VoltTextSecondary,
                            selectedTextColor = VoltAccentViolet,
                            unselectedTextColor = VoltTextSecondary,
                            indicatorColor = VoltAccentViolet.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_alarms_tab")
                    )

                    // Tab 2: World Clock
                    NavigationBarItem(
                        selected = currentRoute == Screen.WorldClock.route,
                        onClick = {
                            if (currentRoute != Screen.WorldClock.route) {
                                navController.navigate(Screen.WorldClock.route) {
                                    popUpTo(Screen.Alarms.route)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Public, contentDescription = "World Clock") },
                        label = { Text("World Clock") },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAccentViolet,
                            unselectedIconColor = VoltTextSecondary,
                            selectedTextColor = VoltAccentViolet,
                            unselectedTextColor = VoltTextSecondary,
                            indicatorColor = VoltAccentViolet.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_world_clock_tab")
                    )

                    // Tab 3: Timer
                    NavigationBarItem(
                        selected = currentRoute == Screen.Timer.route,
                        onClick = {
                            if (currentRoute != Screen.Timer.route) {
                                navController.navigate(Screen.Timer.route) {
                                    popUpTo(Screen.Alarms.route)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.HourglassBottom, contentDescription = "Timer") },
                        label = { Text("Timer") },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAccentViolet,
                            unselectedIconColor = VoltTextSecondary,
                            selectedTextColor = VoltAccentViolet,
                            unselectedTextColor = VoltTextSecondary,
                            indicatorColor = VoltAccentViolet.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_timer_tab")
                    )

                    // Tab 4: Stopwatch (NEW)
                    NavigationBarItem(
                        selected = currentRoute == Screen.Stopwatch.route,
                        onClick = {
                            if (currentRoute != Screen.Stopwatch.route) {
                                navController.navigate(Screen.Stopwatch.route) {
                                    popUpTo(Screen.Alarms.route)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Timer, contentDescription = "Stopwatch") },
                        label = { Text("Stopwatch") },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VoltAccentViolet,
                            unselectedIconColor = VoltTextSecondary,
                            selectedTextColor = VoltAccentViolet,
                            unselectedTextColor = VoltTextSecondary,
                            indicatorColor = VoltAccentViolet.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_stopwatch_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Alarms.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Alarms.route) {
                AlarmRoute(
                    alarmViewModel = alarmViewModel,
                    batteryViewModel = batteryViewModel,
                    onAddEditAlarm = { alarmId ->
                        if (alarmId == null) {
                            navController.navigate("add_edit_alarm/-1")
                        } else {
                            navController.navigate("add_edit_alarm/$alarmId")
                        }
                    },
                    onSettingsShortcut = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.WorldClock.route) {
                WorldClockRoute(viewModel = alarmViewModel)
            }

            composable(Screen.Timer.route) {
                TimerRoute(viewModel = timerViewModel)
            }

            composable(Screen.Stopwatch.route) {
                StopwatchRoute(viewModel = stopwatchViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsRoute(
                    alarmViewModel = alarmViewModel,
                    batteryViewModel = batteryViewModel,
                    settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("add_edit_alarm/{alarmId}") { backStackEntry ->
                val alarmIdArg = backStackEntry.arguments?.getString("alarmId")?.toIntOrNull()
                val realAlarmId = if (alarmIdArg == -1) null else alarmIdArg

                AddEditAlarmRoute(
                    viewModel = alarmViewModel,
                    alarmId = realAlarmId,
                    onBack = { navController.popBackStack() },
                    onNavigateToRingtone = {
                        navController.navigate(Screen.RingtoneSelection.route)
                    },
                    selectedRingtoneName = selectedRingtoneName
                )
            }

            composable(Screen.RingtoneSelection.route) {
                RingtoneSelectionScreen(
                    currentRingtone = selectedRingtoneName,
                    onRingtoneSelected = { selectedRingtoneName = it },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
