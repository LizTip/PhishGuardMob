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
 */
class MainActivity : ComponentActivity() {

    private val viewModel: PhishGuardViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PhishGuardRepository(database.scanResultDao())
        PhishGuardViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            PhishGuard2Theme {
                MainAppContainer(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedUrl ->
                val url = extractUrl(sharedUrl)
                if (url.isNotEmpty()) {
                    viewModel.performScan(url)
                }
            }
        }
    }

    private fun extractUrl(text: String): String {
        return text.split("\\s+".toRegex()).find { 
            it.startsWith("http://", true) || it.startsWith("https://", true) 
        } ?: text
    }
}

@Composable
fun MainAppContainer(viewModel: PhishGuardViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
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
                enabled = !isLoading && urlInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(size = 24.dp, color = Color.White)
                } else {
                    Text("Scan Now")
                }
            }
            
            // Clear current result button
            if (scanResult != null || error != null) {
                OutlinedButton(
                    onClick = { viewModel.resetScanState() },
                    modifier = Modifier.weight(0.4f)
                ) {
                    Text("Clear")
                }
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        scanResult?.let { result ->
            LaunchedEffect(result) {
                if (result.prediction.equals("Phishing", ignoreCase = true)) {
                    triggerVibration(context)
                }
            }
            ResultElevatedCard(result)
        }
    }
}

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

@Composable
fun HistoryScreen(viewModel: PhishGuardViewModel) {
    val history by viewModel.scanHistory.collectAsState(initial = emptyList())
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Scan History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
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

private fun triggerVibration(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

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
