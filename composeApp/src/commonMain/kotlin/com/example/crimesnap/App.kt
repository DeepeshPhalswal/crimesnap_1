@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.crimesnap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class Screen {
    Login, Home, History, Report, Profile, About, ReportDetail
}

data class DetectionResult(
    val label: String,
    val confidence: Float
)

data class CrimeReport(
    val id: String,
    val userId: String,
    val type: String,
    val location: String,
    val description: String,
    val date: String,
    val timestamp: Long,
    val photoPath: String? = null,
    val imageUrl: String? = null,
    val videoPath: String? = null,
    val videoUrl: String? = null,
    val audioPath: String? = null,
    val audioUrl: String? = null,
    val detectionResult: DetectionResult? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@Composable
fun App() {
    val authManager = remember { getAuthManager() }
    val user by authManager.currentUser.collectAsState()
    
    var currentScreen by remember { mutableStateOf(Screen.Login) }
    var selectedReport by remember { mutableStateOf<CrimeReport?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    val reportHistory = remember { mutableStateListOf<CrimeReport>() }
    var isLoadingHistory by remember { mutableStateOf(false) }
    val platform = getPlatform()

    // Auto-navigate based on Auth State
    LaunchedEffect(user) {
        if (user != null) {
            if (currentScreen == Screen.Login) {
                currentScreen = Screen.Home
            }
        } else {
            currentScreen = Screen.Login
        }
    }

    // Fetch reports from Firestore when History screen is shown
    LaunchedEffect(currentScreen, user) {
        if (currentScreen == Screen.History && user != null) {
            isLoadingHistory = true
            try {
                // Fetch reports for the current user
                val reports = Firebase.firestore
                    .collection("reports")
                    .where("userId", equalTo = user!!.id)
                    .get()

                reportHistory.clear()
                // Sort manually if orderBy is problematic with current indices
                val sortedDocs = reports.documents.sortedByDescending { it.get<Long>("timestamp") }

                sortedDocs.forEach { doc ->
                    try {
                        val report = CrimeReport(
                            id = doc.id,
                            userId = doc.get<String?>("userId") ?: "",
                            type = doc.get<String?>("type") ?: "Unknown",
                            location = doc.get<String?>("location") ?: "Unknown",
                            description = doc.get<String?>("description") ?: "",
                            date = doc.get<String?>("date") ?: "Unknown Date",
                            timestamp = doc.get<Long?>("timestamp") ?: 0L,
                            imageUrl = doc.get<String?>("imageUrl"),
                            videoUrl = doc.get<String?>("videoUrl"),
                            audioUrl = doc.get<String?>("audioUrl"),
                            latitude = doc.get<Double?>("latitude") ?: 0.0,
                            longitude = doc.get<Double?>("longitude") ?: 0.0
                        )
                        reportHistory.add(report)
                    } catch (e: Exception) {
                        println("Error parsing report: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error fetching history: ${e.message}")
            } finally {
                isLoadingHistory = false
            }
        }
    }

    if (user == null || currentScreen == Screen.Login) {
        LoginScreen(authManager = authManager)
    } else {
        BackHandler(enabled = currentScreen != Screen.Home || drawerState.isOpen) {
            if (drawerState.isOpen) {
                scope.launch { drawerState.close() }
            } else {
                if (currentScreen == Screen.ReportDetail) {
                    currentScreen = Screen.History
                } else if (currentScreen == Screen.Report) {
                    currentScreen = Screen.Home
                } else {
                    currentScreen = Screen.Home
                }
            }
        }

        MaterialTheme {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "CrimeSnap",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        user?.let {
                            ListItem(
                                headlineContent = { Text(it.name ?: "User") },
                                supportingContent = { Text(it.email ?: "") },
                                leadingContent = {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(40.dp))
                                }
                            )
                        }
                        
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
                            icon = { Icon(Icons.Default.Emergency, contentDescription = null) },
                            label = { Text("SOS Emergency") },
                            selected = false,
                            onClick = {
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
                            icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                            label = { Text("Logout") },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    authManager.signOut()
                                }
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
                            isLoading = isLoadingHistory,
                            onBack = { currentScreen = Screen.Home },
                            onReportClick = { report ->
                                selectedReport = report
                                currentScreen = Screen.ReportDetail
                            }
                        )
                        Screen.Report -> ReportScreen(
                            user = user,
                            onBack = { currentScreen = Screen.Home },
                            onSubmit = { report ->
                                // After submission, go to history to see the result
                                currentScreen = Screen.History
                            }
                        )
                        Screen.Profile -> ProfileScreen(user = user, onBack = { currentScreen = Screen.Home })
                        Screen.About -> AboutScreen(onBack = { currentScreen = Screen.Home })
                        Screen.ReportDetail -> ReportDetailScreen(
                            report = selectedReport,
                            onBack = { currentScreen = Screen.History }
                        )
                        else -> {}
                    }
                }
            }
        }
    }
}

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
                Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(32.dp))
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

@Composable
fun ReportScreen(user: User?, onBack: () -> Unit, onSubmit: (CrimeReport) -> Unit) {
    var crimeType by remember { mutableStateOf("") }
    var detectedLocation by remember { mutableStateOf("Detecting location...") }
    var description by remember { mutableStateOf("") }
    var currentLatitude by remember { mutableStateOf(0.0) }
    var currentLongitude by remember { mutableStateOf(0.0) }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showAudioPermissionDialog by remember { mutableStateOf(false) }

    var photoPath by remember { mutableStateOf<String?>(null) }
    var videoPath by remember { mutableStateOf<String?>(null) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    var detectionResult by remember { mutableStateOf<DetectionResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadStatus by remember { mutableStateOf("") }

    val platform = getPlatform()
    val scope = rememberCoroutineScope()

    val refreshLocation = {
        platform.getCurrentLocation { result ->
            when (result) {
                "PERMISSION_REQUIRED" -> showPermissionDialog = true
                "GPS_DISABLED" -> showGpsDialog = true
                else -> {
                    detectedLocation = result
                    if (result.contains("Lat:")) {
                        val lat = result.substringAfter("Lat: ").substringBefore(",").toDoubleOrNull() ?: 0.0
                        val lon = result.substringAfter("Lon: ").substringBefore("\n").toDoubleOrNull() ?: 0.0
                        currentLatitude = lat
                        currentLongitude = lon
                    }
                }
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
                            if (result == "PERMISSION_REQUIRED") {
                                showCameraPermissionDialog = true
                            } else if (result != null) {
                                photoPath = result
                                isAnalyzing = true
                                platform.analyzeImage(result) { detection ->
                                    detectionResult = detection
                                    isAnalyzing = false
                                }
                            }
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

            if (isAnalyzing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI analyzing photo...", style = MaterialTheme.typography.bodySmall)
                }
            } else if (detectionResult != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("AI Detection Result", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Detected: ${detectionResult?.label} (${(detectionResult?.confidence ?: 0f * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Uploaded Files List
            if (photoPath != null || videoPath != null || audioPath != null) {
                Text(
                    text = "Attached Evidence Details:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (photoPath != null) FileItem(platform.getFileInfo(photoPath), "IMAGE")
                    if (videoPath != null) FileItem(platform.getFileInfo(videoPath), "VIDEO")
                    if (audioPath != null) FileItem(platform.getFileInfo(audioPath), "AUDIO")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(uploadStatus, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Button(
                    onClick = {
                        if (crimeType.isNotEmpty() && (detectedLocation.contains("Lat:") || detectedLocation == "Location Captured")) {
                            isUploading = true
                            uploadStatus = "Preparing upload..."
                            scope.launch {
                                try {
                                    val timestamp = System.currentTimeMillis()
                                    var imageUrl: String? = null
                                    var videoUrl: String? = null
                                    var audioUrl: String? = null

                                    if (photoPath != null) {
                                        uploadStatus = "Uploading photo to Firebase Storage..."
                                        imageUrl = platform.uploadFile(photoPath!!, "reports/$timestamp/image.jpg")
                                    }
                                    
                                    if (videoPath != null) {
                                        uploadStatus = "Uploading video to Firebase Storage..."
                                        videoUrl = platform.uploadFile(videoPath!!, "reports/$timestamp/video.mp4")
                                    }
                                    
                                    if (audioPath != null) {
                                        uploadStatus = "Uploading audio to Firebase Storage..."
                                        audioUrl = platform.uploadFile(audioPath!!, "reports/$timestamp/audio.m4a")
                                    }

                                    uploadStatus = "Finalizing report in Firestore..."
                                    val report = CrimeReport(
                                        id = timestamp.toString(),
                                        userId = user?.id ?: "anonymous",
                                        type = crimeType,
                                        location = detectedLocation,
                                        description = description,
                                        date = platform.getCurrentDate(),
                                        timestamp = timestamp,
                                        photoPath = photoPath,
                                        imageUrl = imageUrl,
                                        videoPath = videoPath,
                                        videoUrl = videoUrl,
                                        audioPath = audioPath,
                                        audioUrl = audioUrl,
                                        detectionResult = detectionResult,
                                        latitude = currentLatitude,
                                        longitude = currentLongitude
                                    )

                                    // Use platform to save report for consistency
                                    platform.saveReport(report)

                                    uploadStatus = "Upload complete!"
                                    delay(500)
                                    isUploading = false
                                    onSubmit(report)
                                } catch (e: Exception) {
                                    uploadStatus = "Upload failed: ${e.message}"
                                    delay(2000)
                                    isUploading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = crimeType.isNotEmpty() && (detectedLocation.contains("Lat:") || detectedLocation == "Location Captured" || detectedLocation.length > 15)
                ) {
                    Text("Submit Verified Report", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun FileItem(fileInfo: FileInfo?, forcedType: String, onClick: (() -> Unit)? = null) {
    fileInfo?.let {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = when(forcedType) {
                        "IMAGE" -> Icons.Default.Image
                        "VIDEO" -> Icons.Default.VideoFile
                        else -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$forcedType EVIDENCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            if (onClick != null) {
                Icon(Icons.Default.OpenInNew, contentDescription = "Open", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            } else {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Text(it.sizeFormatted, modifier = Modifier.padding(4.dp))
                }
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

@Composable
fun HistoryScreen(
    historyItems: List<CrimeReport>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onReportClick: (CrimeReport) -> Unit
) {
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (historyItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No crime reported yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
                        onClick = { onReportClick(item) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.type,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                            Text(
                                text = "Location: ${item.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Date: ${item.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.imageUrl != null || item.videoUrl != null || item.audioUrl != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(" Evidence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailScreen(report: CrimeReport?, onBack: () -> Unit) {
    val platform = getPlatform()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incident Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (report == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Report not found.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DetailItem(label = "Crime Type", value = report.type, style = MaterialTheme.typography.headlineSmall)
                DetailItem(label = "Location", value = report.location)
                DetailItem(label = "Date", value = report.date)

                if (report.detectionResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("AI Verification", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Detected: ${report.detectionResult.label} (${(report.detectionResult.confidence * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (report.description.isEmpty()) "No description provided." else report.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                HorizontalDivider()

                Text(
                    text = "Evidence (Tap to View)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EvidenceStatus(icon = Icons.Default.PhotoCamera, label = "Photo", isAvailable = report.imageUrl != null || report.photoPath != null)
                    EvidenceStatus(icon = Icons.Default.Videocam, label = "Video", isAvailable = report.videoUrl != null || report.videoPath != null)
                    EvidenceStatus(icon = Icons.Default.Mic, label = "Audio", isAvailable = report.audioUrl != null || report.audioPath != null)
                }

                if (report.imageUrl != null || report.photoPath != null || report.videoUrl != null || report.videoPath != null || report.audioUrl != null || report.audioPath != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (report.imageUrl != null || report.photoPath != null) {
                            FileItem(
                                platform.getFileInfo(report.imageUrl ?: report.photoPath), 
                                "IMAGE",
                                onClick = { (report.imageUrl ?: report.photoPath)?.let { platform.openUrl(it) } }
                            )
                        }
                        if (report.videoUrl != null || report.videoPath != null) {
                            FileItem(
                                platform.getFileInfo(report.videoUrl ?: report.videoPath), 
                                "VIDEO",
                                onClick = { (report.videoUrl ?: report.videoPath)?.let { platform.openUrl(it) } }
                            )
                        }
                        if (report.audioUrl != null || report.audioPath != null) {
                            FileItem(
                                platform.getFileInfo(report.audioUrl ?: report.audioPath), 
                                "AUDIO",
                                onClick = { (report.audioUrl ?: report.audioPath)?.let { platform.openUrl(it) } }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "This evidence has been securely uploaded to Firebase and cannot be modified.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = style)
    }
}

@Composable
fun EvidenceStatus(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isAvailable: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = if (isAvailable) "Uploaded" else "N/A",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isAvailable) FontWeight.Bold else FontWeight.Normal,
            color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun ProfileScreen(user: User?, onBack: () -> Unit) {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            Text(text = user?.name ?: "Guest User", style = MaterialTheme.typography.headlineMedium)
            Text(text = user?.email ?: "Not available", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Account Details", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "User ID: ${user?.id ?: "N/A"}")
                    Text(text = "Join Date: ${user?.joinDate ?: "Oct 2023"}")
                    Text(text = "Reports Filed: ${user?.reportsCount ?: 0}")
                }
            }
        }
    }
}

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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                       "• AI-powered visual evidence verification\n" +
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

@Preview
@Composable
fun AppPreview() {
    App()
}
