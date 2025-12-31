package com.expense.tracker.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDebugLogs by remember { mutableStateOf(false) }
    
    // Check AI status and Updates on load
    LaunchedEffect(Unit) {
        viewModel.checkAiStatus(context)
        viewModel.checkForUpdates()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsSection(title = "AI Engine")
            }
            

            
            item {
                AIStatusCard(isReady = uiState.isModelDownloaded)
            }
            
            // Data Section
            item {
                SettingsSection(title = "Data")
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Receipt,
                    title = "Transactions",
                    subtitle = "${uiState.transactionCount} transactions stored",
                    onClick = { }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear All Data",
                    subtitle = "Delete all transactions",
                    onClick = { showClearDataDialog = true }
                )
            }
            
            // About Section
            item {
                SettingsSection(title = "About")
            }
            
            item {
                var versionTapCount by remember { mutableIntStateOf(0) }
                
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = uiState.appVersion,
                    onClick = { 
                        versionTapCount++
                        if (versionTapCount >= 5) {
                            versionTapCount = 0
                            showDebugLogs = true
                        }
                    }
                )
            }
            
            item {
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Privacy",
                    subtitle = "All data stays on device",
                    onClick = { }
                )
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            
            // UPDATE Section
            item {
                if (uiState.updateAvailable || uiState.isCheckingForUpdate || uiState.isDownloading) {
                    SettingsSection(title = "Updates")
                    UpdateStatusCard(
                        uiState = uiState,
                        onDownloadClick = { viewModel.downloadUpdate(context) }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
    
    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Clear All Data?") },
            text = { 
                Text("This will delete all ${uiState.transactionCount} transactions. After clearing, tap Refresh on Home to re-scan SMS.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Debug Logs Dialog
    if (showDebugLogs) {
        val clipboard = LocalContext.current.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        DebugLogDialog(
            log = uiState.debugLog,
            onDismiss = { showDebugLogs = false },
            onCopy = {
                val clip = android.content.ClipData.newPlainText("Debug Log", uiState.debugLog)
                clipboard.setPrimaryClip(clip)
                showDebugLogs = false
            }
        )
    }
}

@Composable
private fun AIStatusCard(isReady: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = if (isReady) {
                        listOf(CreditGreen.copy(alpha = 0.15f), CreditGreen.copy(alpha = 0.05f))
                    } else {
                        listOf(DebitRed.copy(alpha = 0.15f), DebitRed.copy(alpha = 0.05f))
                    }
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        if (isReady) CreditGreen.copy(alpha = 0.4f) else DebitRed.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isReady) Icons.Default.SmartToy else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (isReady) CreditGreen else DebitRed,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isReady) "AI Ready" else "AI Offline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isReady) CreditGreen else DebitRed
                )
                Text(
                    text = if (isReady) "Gemini API + Smart Categorization" else "Add GEMINI_API_KEY to local.properties",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = PurplePrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                text = title,
                fontWeight = FontWeight.Medium
            ) 
        },
        supportingContent = { 
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurplePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun DebugLogDialog(
    log: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Debug Log") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                item {
                    Text(
                        text = log.ifEmpty { "No logs yet" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
@Composable
fun UpdateStatusCard(
    uiState: SettingsUiState,
    onDownloadClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val isDownloading = uiState.isDownloading
    var expanded by remember { mutableStateOf(false) }
    
    // Process markdown lines
    val notesLines = remember(uiState.releaseNotes) {
        uiState.releaseNotes.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    val shouldTruncate = notesLines.size > 4
    val visibleLines = if (expanded || !shouldTruncate) notesLines else notesLines.take(4)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PurplePrimary.copy(alpha = 0.15f),
                        AccentCyan.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(PurplePrimary.copy(alpha = 0.4f), Color.Transparent)
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when {
                            isDownloading -> "Downloading Update..."
                            uiState.updateAvailable -> "Update Available"
                            else -> "Checking for Update..."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                    if (uiState.updateAvailable && !isDownloading) {
                        Text(
                            text = "Version ${uiState.latestVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (uiState.updateAvailable) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (notesLines.isNotEmpty()) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurplePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        visibleLines.forEach { line ->
                            MarkdownLine(line)
                        }
                    }
                    
                    if (shouldTruncate) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (expanded) "See Less" else "See More",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { expanded = !expanded }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { uiState.downloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Text(
                        text = "${(uiState.downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                } else {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Text("Download & Install")
                    }
                }
            } else if (uiState.isCheckingForUpdate) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
            
            if (uiState.updateError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.updateError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MarkdownLine(line: String) {
    val isHeader = line.startsWith("#")
    val isBullet = line.startsWith("-") || line.startsWith("*")
    
    val cleanLine = when {
        isHeader -> line.trimStart('#', ' ')
        isBullet -> line.trimStart('-', '*', ' ')
        else -> line
    }
    
    val annotatedString = parseBoldText(cleanLine)
    
    Row(modifier = Modifier.fillMaxWidth()) {
        if (isBullet) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodySmall,
                color = PurplePrimary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        
        Text(
            text = annotatedString,
            style = if (isHeader) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) 
                   else MaterialTheme.typography.bodySmall,
            color = if (isHeader) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper to parse **bold** text
private fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    val parts = text.split("**")
    return androidx.compose.ui.text.buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) { // Odd indices are inside ** **
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = PurplePrimary))
                append(part)
                pop()
            } else {
                append(part)
            }
        }
    }
}
