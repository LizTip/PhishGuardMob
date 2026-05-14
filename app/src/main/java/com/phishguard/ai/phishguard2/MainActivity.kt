package com.phishguard.ai.phishguard2

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phishguard.ai.phishguard2.data.AppDatabase
import com.phishguard.ai.phishguard2.data.PhishGuardRepository
import com.phishguard.ai.phishguard2.data.ScanResultEntity
import com.phishguard.ai.phishguard2.network.PredictionResponse
import com.phishguard.ai.phishguard2.ui.PhishGuardViewModel
import com.phishguard.ai.phishguard2.ui.theme.PhishGuard2Theme
import java.text.SimpleDateFormat
import java.util.*

/**
 * PhishGuard AI - Main Activity
 * Entry point for the Android application. It manages the lifecycle and coordinates 
 * between the system intents and the Compose UI.
 */
class MainActivity : ComponentActivity() {

    // Dependency Injection: Initialisation of the ViewModel via a Factory to provide the Repository.
    // We use 'by viewModels' to delegate the lifecycle management to the Android Framework.
    private val viewModel: PhishGuardViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PhishGuardRepository(database.scanResultDao())
        PhishGuardViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Optimisation for modern displays to draw content behind system bars.
        enableEdgeToEdge() 
        
        // Handling the 'Share' Intent if the app is launched cold from a browser.
        handleIntent(intent)

        // setContent is the entry point for the Jetpack Compose declarative UI.
        setContent {
            PhishGuard2Theme {
                MainAppContainer(viewModel)
            }
        }
    }

    // Handles incoming data if the app is already residing in the background memory.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /**
     * Inter-Process Communication (IPC): Extracts the URL shared from external apps.
     */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedUrl ->
                val url = extractUrl(sharedUrl)
                if (url.isNotEmpty()) {
                    // Triggers the automated scanning behaviour once the URL is recognised.
                    viewModel.performScan(url) 
                }
            }
        }
    }

    // Data Sanitisation: Uses regex to strip away text and isolate the raw URL for the API.
    private fun extractUrl(text: String): String {
        return text.split("\\s+".toRegex()).find { 
            it.startsWith("http://", true) || it.startsWith("https://", true) 
        } ?: text
    }
}

@Composable
fun MainAppContainer(viewModel: PhishGuardViewModel) {
    // remember and mutableIntStateOf are used to persist UI state across recomposition cycles.
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            // NavigationBar implements the Material 3 design pattern for bottom-level navigation.
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Scan") },
                    label = { Text("Scan") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }
        }
    ) { innerPadding ->
        // Box container manages the layout padding provided by the Scaffold.
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                ScanScreen(viewModel)
            } else {
                HistoryScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(viewModel: PhishGuardViewModel) {
    val context = LocalContext.current
    
    // Reactive Programming: Converting Kotlin Flows into Compose States for UI updates.
    val isLoading by viewModel.isLoading.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    
    var urlInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "PhishGuard AI Scanner",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        // Text input field with an inline clear function for improved usability.
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("URL to scan") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Input")
                    }
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.performScan(urlInput) },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && urlInput.isNotBlank() // Logic to prevent redundant API calls.
            ) {
                if (isLoading) {
                    CircularProgressIndicator(size = 24.dp, color = Color.White)
                } else {
                    Text("Scan Now")
                }
            }
            
            // Provides a mechanism to reset the UI state without clearing the permanent history.
            if (scanResult != null || error != null) {
                OutlinedButton(
                    onClick = { viewModel.resetScanState() },
                    modifier = Modifier.weight(0.4f)
                ) {
                    Text("Clear")
                }
            }
        }

        // Conditional rendering for error states returned by the repository layer.
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        scanResult?.let { result ->
            // Side-effect: Triggers physical hardware feedback (vibration) for critical security alerts.
            LaunchedEffect(result) {
                if (result.prediction.equals("Phishing", ignoreCase = true)) {
                    triggerVibration(context)
                }
            }
            ResultElevatedCard(result)
        }
    }
}

/**
 * Visualise the AI analysis results using an ElevatedCard for clear information grouping.
 */
@Composable
fun ResultElevatedCard(prediction: PredictionResponse) {
    val isPhishing = prediction.prediction.equals("Phishing", ignoreCase = true)
    val color = if (isPhishing) MaterialTheme.colorScheme.error else Color(0xFF388E3C)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Entry animation using scaleIn to draw user attention to the result.
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                ) {
                    Icon(
                        imageVector = if (isPhishing) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = prediction.prediction.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Confidence: ${(prediction.probability * 100).toInt()}%", fontWeight = FontWeight.Bold)
            Text("URL: ${prediction.url}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(Modifier.height(8.dp))
            Text("Analyst Notes:", style = MaterialTheme.typography.labelLarge)
            Text(prediction.analystNotes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Screen displaying previous scan results queried from the local Room SQLite database.
 */
@Composable
fun HistoryScreen(viewModel: PhishGuardViewModel) {
    // Obtains a stream of data from the repository and converts it to Compose state.
    val history by viewModel.scanHistory.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scan History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            // Maintenance function to clear all rows from the local database.
            if (history.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearHistory() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear All History", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scan history found.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            // Memory Optimisation: LazyColumn only renders visible items, similar to a RecyclerView.
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { scan ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(scan.prediction, fontWeight = FontWeight.Bold, color = if(scan.prediction == "Phishing") Color.Red else Color.Green)
                                Text(dateFormat.format(Date(scan.timestamp)), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(scan.url, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Accessibility Layer: Interacts with hardware components to provide physical threat alerts.
 */
private fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Handles API level differences for hardware vibration support.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }
}

@Composable
fun CircularProgressIndicator(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp
    )
}
