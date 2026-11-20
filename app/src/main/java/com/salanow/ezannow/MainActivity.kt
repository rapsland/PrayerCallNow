package com.salanow.ezannow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.salanow.ezannow.data.location.DeviceLocationHelper
import com.salanow.ezannow.ui.MainViewModel
import com.salanow.ezannow.ui.components.EzanIcons
import com.salanow.ezannow.ui.screens.dhikr.DhikrScreen
import com.salanow.ezannow.ui.screens.home.HomeScreen
import com.salanow.ezannow.ui.screens.location.CityPickerSheet
import com.salanow.ezannow.ui.screens.qibla.QiblaScreen
import com.salanow.ezannow.ui.screens.settings.SettingsScreen
import com.salanow.ezannow.ui.screens.splash.ZeusGalaxySplashScreen
import com.salanow.ezannow.ui.screens.timetable.TimetableScreen
import com.salanow.ezannow.ui.theme.EzanNowTheme

private enum class EzanTab(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Vakitler", EzanIcons.Mosque),
    TIMETABLE("timetable", "İmsakiye", EzanIcons.CalendarTable),
    QIBLA("qibla", "Kıble", EzanIcons.Compass),
    DHIKR("dhikr", "Zikir", EzanIcons.Tasbih),
    SETTINGS("settings", "Ayarlar", EzanIcons.SettingsGear)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            EzanNowTheme(themeMode = uiState.settings.themeMode) {
                EzanNowRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshOnForeground()
    }
}

@Composable
private fun EzanNowRoot(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EzanTab.HOME.route

    val snackbarHostState = remember { SnackbarHostState() }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.detectGpsLocation()
        } else {
            viewModel.showStatusMessage("Konum izni reddedildi. Şehrinizi listeden seçebilirsiniz.")
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshOnForeground()
            viewModel.showStatusMessage("Bildirim izni verildi.")
        } else {
            viewModel.showStatusMessage("Bildirim izni verilmedi.")
        }
    }

    val customAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = runCatching {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
            }.getOrNull() ?: "Özel Ses"
            viewModel.setCustomNotificationSound(uri.toString(), displayName)
        }
    }

    val requestGpsOrDetect: () -> Unit = {
        if (DeviceLocationHelper.hasLocationPermission(context)) {
            viewModel.detectGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val requestNotificationPerm: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    EzanTab.entries.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = EzanTab.HOME.route
                ) {
                    composable(EzanTab.HOME.route) {
                        HomeScreen(
                            uiState = uiState,
                            onOpenCityPicker = { showCityPicker = true },
                            onDetectGps = requestGpsOrDetect,
                            onSelectDate = viewModel::selectDate,
                            onJumpToToday = viewModel::jumpToToday,
                            onTogglePrayerNotification = viewModel::togglePrayerNotification,
                            onToggleCustomTestTime = viewModel::setCustomTestTimeEnabled,
                            onToggleTestPlayPause = viewModel::toggleTestModePlayPause,
                            onTestMinutesChange = viewModel::setTestModeMinutes,
                            onTestSpeedSelect = viewModel::setTestModeSpeed,
                            onTestResetClock = viewModel::resetTestModeClock,
                            onTestJumpBeforeNextPrayer = viewModel::jumpBeforeNextSimulatedPrayer,
                            onTestClose = { viewModel.setTestModeEnabled(false) },
                            onRequestNotificationPermission = requestNotificationPerm
                        )
                    }
                    composable(EzanTab.TIMETABLE.route) {
                        TimetableScreen(
                            uiState = uiState,
                            onPrevMonth = viewModel::prevTimetableMonth,
                            onNextMonth = viewModel::nextTimetableMonth,
                            onResetMonth = viewModel::resetTimetableMonth
                        )
                    }
                    composable(EzanTab.QIBLA.route) {
                        QiblaScreen(
                            uiState = uiState,
                            onOpenCityPicker = { showCityPicker = true }
                        )
                    }
                    composable(EzanTab.DHIKR.route) {
                        DhikrScreen(
                            uiState = uiState,
                            onSelectDhikr = viewModel::selectDhikr,
                            onIncrementDhikr = viewModel::incrementDhikr,
                            onDecrementDhikr = viewModel::decrementDhikr,
                            onResetCurrentDhikr = viewModel::resetCurrentDhikr,
                            onAddCustomDhikr = viewModel::addCustomDhikr,
                            onDeleteCustomDhikr = viewModel::deleteCustomDhikr,
                            onToggleHaptic = viewModel::toggleDhikrHaptic,
                            onAdjustQada = viewModel::adjustQada,
                            onResetAllQada = viewModel::resetAllQada
                        )
                    }
                    composable(EzanTab.SETTINGS.route) {
                        SettingsScreen(
                            uiState = uiState,
                            onOpenCityPicker = { showCityPicker = true },
                            onDetectGps = requestGpsOrDetect,
                            onSelectMethod = viewModel::setCalculationMethod,
                            onSelectMadhab = viewModel::setMadhab,
                            onSelectHighLat = viewModel::setHighLatitudeRule,
                            onSaveAdjustments = viewModel::setAdjustments,
                            onSetHijriOffset = viewModel::setHijriDayOffset,
                            onToggleKerahatBanner = viewModel::setShowKerahatBanner,
                            onTogglePrayerNotif = viewModel::togglePrayerNotification,
                            onSetLeadMinutes = viewModel::setNotificationLeadMinutes,
                            onToggleNotificationSound = viewModel::setNotificationSoundEnabled,
                            onTogglePersistentStatusNotif = viewModel::setPersistentStatusNotificationEnabled,
                            onSelectSoundPreset = viewModel::selectNotificationSoundPreset,
                            onSetSoundDurationSeconds = viewModel::setNotificationSoundDurationSeconds,
                            onPickCustomSound = {
                                customAudioPickerLauncher.launch(arrayOf("audio/*"))
                            },
                            onPreviewSound = viewModel::previewGentleTone,
                            onSendTestNotification = viewModel::sendTestNotification,
                            onScheduleBackgroundSwipeTest = viewModel::scheduleBackgroundSwipeTestAlarm,
                            onRequestNotificationPermission = requestNotificationPerm,
                            onSelectTheme = viewModel::setThemeMode,
                            onToggleTestMode = viewModel::setTestModeEnabled,
                            onLaunchTestModeOnHome = {
                                viewModel.setTestModeEnabled(true)
                                navController.navigate(EzanTab.HOME.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onToggleCustomTestTime = viewModel::setCustomTestTimeEnabled,
                            onSetCustomTestTime = viewModel::setCustomTestTime,
                            onSetCustomTestMinutesAhead = viewModel::setCustomTestTimeMinutesFromNow,
                            onReplaySplash = { showSplash = true }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = 450))
        ) {
            ZeusGalaxySplashScreen(
                onFinished = { showSplash = false }
            )
        }
    }

    if (showCityPicker) {
        CityPickerSheet(
            currentLocation = uiState.settings.location,
            isDetectingLocation = uiState.isDetectingLocation,
            onSelectCity = viewModel::selectCity,
            onDetectGps = requestGpsOrDetect,
            onDismiss = { showCityPicker = false }
        )
    }
}
