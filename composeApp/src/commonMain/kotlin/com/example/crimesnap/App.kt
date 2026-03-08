package com.example.crimesnap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch

enum class Screen {
    Home, History, Report, Profile, About
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val reportHistory = remember { 
        mutableStateListOf(
            "Theft reported at Central Park on Oct 12, 2023",
            "Vandalism reported at 5th Ave on Nov 5, 2023",
            "Suspicious activity reported on Dec 1, 2023"
        )
    }

    // Handle back button navigation
    BackHandler(enabled = currentScreen != Screen.Home) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = Screen.Home
        }
    }

    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "CrimeSnap",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("Dashboard") },
                        selected = currentScreen == Screen.Home,
                        onClick = {
                            currentScreen = Screen.Home
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("Profile") },
                        selected = currentScreen == Screen.Profile,
                        onClick = {
                            currentScreen = Screen.Profile
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("History") },
                        selected = currentScreen == Screen.History,
                        onClick = {
                            currentScreen = Screen.History
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Sos, contentDescription = null) },
                        label = { Text("SOS Emergency") },
                        selected = false,
                        onClick = {
                            // Instant SOS trigger
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("About App") },
                        selected = currentScreen == Screen.About,
                        onClick = {
                            currentScreen = Screen.About
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.weight(1f))
                    HorizontalDivider()
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                        label = { Text("Logout") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onNavigateToHistory = { currentScreen = Screen.History },
                        onReportCrime = { currentScreen = Screen.Report }
                    )
                    Screen.History -> HistoryScreen(
                        historyItems = reportHistory,
                        onBack = { currentScreen = Screen.Home }
                    )
                    Screen.Report -> ReportScreen(
                        onBack = { currentScreen = Screen.Home },
                        onSubmit = { type, location, _ ->
                            val report = "$type reported at $location on ${getCurrentDate()}"
                            reportHistory.add(0, report)
                            currentScreen = Screen.Home
                        }
                    )
                    Screen.Profile -> ProfileScreen(onBack = { currentScreen = Screen.Home })
                    Screen.About -> AboutScreen(onBack = { currentScreen = Screen.Home })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onMenuClick: () -> Unit, onNavigateToHistory: () -> Unit, onReportCrime: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrimeSnap", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Keep Your Community Safe",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onReportCrime,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "REPORT CRIME",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "VIEW REPORT HISTORY",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Rapid SOS trigger */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.Default.Sos, contentDescription = null, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SOS EMERGENCY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var crimeType by remember { mutableStateOf("") }
    var detectedLocation by remember { mutableStateOf("Detecting location...") }
    var description by remember { mutableStateOf("") }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showAudioPermissionDialog by remember { mutableStateOf(false) }
    
    var photoPath by remember { mutableStateOf<String?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    
    val platform = getPlatform()

    val refreshLocation = {
        platform.getCurrentLocation { result ->
            when (result) {
                "PERMISSION_REQUIRED" -> showPermissionDialog = true
                "GPS_DISABLED" -> showGpsDialog = true
                else -> detectedLocation = result
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshLocation()
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission") },
            text = { Text("CrimeSnap needs location access to verify incident coordinates.") },
            confirmButton = {
                TextButton(onClick = { 
                    showPermissionDialog = false
                    platform.requestLocationPermission()
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("GPS Disabled") },
            text = { Text("Please turn on GPS to capture the incident location.") },
            confirmButton = {
                TextButton(onClick = { 
                    showGpsDialog = false
                    platform.requestLocationSettings()
                }) { Text("Turn On") }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            title = { Text("Camera Permission") },
            text = { Text("CrimeSnap needs camera access to capture visual evidence.") },
            confirmButton = {
                TextButton(onClick = { 
                    showCameraPermissionDialog = false
                    platform.requestCameraPermission()
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAudioPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionDialog = false },
            title = { Text("Microphone Permission") },
            text = { Text("CrimeSnap needs microphone access to record audio evidence.") },
            confirmButton = {
                TextButton(onClick = { 
                    showAudioPermissionDialog = false
                    platform.requestAudioPermission()
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = { showAudioPermissionDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Incident Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Incident Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = crimeType,
                onValueChange = { crimeType = it },
                label = { Text("Type of Crime") },
                placeholder = { Text("e.g. Theft, Vandalism") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = detectedLocation,
                onValueChange = { },
                label = { Text("Auto-Detected Location") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text("Location is captured automatically for evidence integrity.")
                },
                trailingIcon = {
                    if (detectedLocation == "GPS_DISABLED" || detectedLocation == "Detecting location..." || detectedLocation == "PERMISSION_REQUIRED") {
                        TextButton(onClick = { refreshLocation() }) { Text("Retry") }
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                minLines = 3
            )

            Text(
                text = "Add Evidence",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EvidenceButton(
                    icon = Icons.Default.PhotoCamera, 
                    label = "Photo",
                    isCaptured = photoPath != null,
                    onClick = { 
                        platform.capturePhoto { result ->
                            if (result == "PERMISSION_REQUIRED") showCameraPermissionDialog = true
                            else photoPath = result
                        }
                    }
                )
                EvidenceButton(
                    icon = Icons.Default.Videocam, 
                    label = "Video",
                    isCaptured = videoPath != null,
                    onClick = { 
                        platform.captureVideo { result ->
                            if (result == "PERMISSION_REQUIRED") showCameraPermissionDialog = true
                            else videoPath = result
                        }
                    }
                )
                EvidenceButton(
                    icon = Icons.Default.Mic, 
                    label = "Audio",
                    isCaptured = audioPath != null,
                    onClick = { 
                        platform.recordAudio { result ->
                            if (result == "PERMISSION_REQUIRED") showAudioPermissionDialog = true
                            else audioPath = result
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    if (crimeType.isNotEmpty() && detectedLocation.contains("Lat:")) {
                        onSubmit(crimeType, detectedLocation, description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = crimeType.isNotEmpty() && detectedLocation.contains("Lat:")
            ) {
                Text("Submit Verified Report", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun EvidenceButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String,
    isCaptured: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
            colors = if (isCaptured) IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) else IconButtonDefaults.filledTonalIconButtonColors()
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
        }
        Text(
            text = if (isCaptured) "Captured" else label, 
            style = MaterialTheme.typography.labelMedium, 
            modifier = Modifier.padding(top = 4.dp),
            color = if (isCaptured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(historyItems: List<String>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lifetime History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
        }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No reports found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Guest User", style = MaterialTheme.typography.headlineMedium)
            Text(text = "guest@example.com", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Account Details", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Join Date: Oct 2023")
                    Text(text = "Reports Filed: 3")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About CrimeSnap") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Our Mission",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "CrimeSnap is a community-driven incident reporting tool designed to empower citizens and improve local safety. By providing verified, geo-tagged reports with multi-media evidence, we help authorities respond faster and more effectively."
            )
            
            Text(
                text = "Key Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Auto-location tagging for integrity\n" +
                       "• Photo, Video, and Audio evidence\n" +
                       "• Community safety dashboard\n" +
                       "• Real-time incident updates"
            )
            
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "© 2023 CrimeSnap Team",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

fun getCurrentDate(): String {
    return "Recent Date"
}

@Preview
@Composable
fun AppPreview() {
    App()
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(onMenuClick = {}, onNavigateToHistory = {}, onReportCrime = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ReportScreenPreview() {
    MaterialTheme {
        ReportScreen(onBack = {}, onSubmit = { _, _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    MaterialTheme {
        val sampleHistory = listOf(
            "Theft reported at Central Park on Oct 12, 2023",
            "Vandalism reported at 5th Ave on Nov 5, 2023",
            "Suspicious activity reported on Dec 1, 2023"
        )
        HistoryScreen(historyItems = sampleHistory, onBack = {})
    }
}
