package com.example.crimesnap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

enum class Screen {
    Home, History, Report
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.Home) }
    
    // In-memory history list. For "lifetime" history, we will later connect this to a database like Room.
    val reportHistory = remember { 
        mutableStateListOf(
            "Theft reported at Central Park on Oct 12, 2023",
            "Vandalism reported at 5th Ave on Nov 5, 2023",
            "Suspicious activity reported on Dec 1, 2023"
        )
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onNavigateToHistory = { currentScreen = Screen.History },
                    onReportCrime = { currentScreen = Screen.Report }
                )
                Screen.History -> HistoryScreen(
                    historyItems = reportHistory,
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Report -> ReportScreen(
                    onBack = { currentScreen = Screen.Home },
                    onSubmit = { type, location, desc ->
                        val report = "$type reported at $location on ${getCurrentDate()}"
                        reportHistory.add(0, report)
                        currentScreen = Screen.History
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToHistory: () -> Unit, onReportCrime: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrimeSnap", fontWeight = FontWeight.Bold) },
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var crimeType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Incident Report") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Cancel")
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
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("Street name or Area") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                minLines = 3
            )

            Button(
                onClick = { 
                    if (crimeType.isNotEmpty() && location.isNotEmpty()) {
                        onSubmit(crimeType, location, description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = crimeType.isNotEmpty() && location.isNotEmpty()
            ) {
                Text("Submit Report", fontSize = 18.sp)
            }
        }
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
                    TextButton(onClick = onBack) {
                        Text("Home")
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

fun getCurrentDate(): String {
    return "Recent Date"
}
