package com.expense.tracker.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.ui.components.*
import com.expense.tracker.domain.model.Transaction

/**
 * Time period selection for the home screen
 */
enum class TimePeriod(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val context = LocalContext.current
    
    // ... (rest of HomeScreen start)
    // Selected time period (Week by default)
    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    
    // State for transaction details dialog
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    // Track if user skipped download
    var skippedDownload by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onSmsPermissionGranted(context.contentResolver)
        } else {
            viewModel.updateSmsPermissionStatus(false)
        }
    }
    
    // Check permission on launch
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        viewModel.updateSmsPermissionStatus(hasPermission)
        
        // FIRST: Check model status and initialize AI (this will trigger SMS processing if ready)
        viewModel.checkModelStatus(context)
        
        // If AI isn't ready yet but permission is granted, the caller will see "AI not ready" error
        // checkModelStatus will call onSmsPermissionGranted once AI is initialized
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Expense Tracker",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                windowInsets = WindowInsets(0.dp),
                actions = {
                    if (uiState.hasSmsPermission) {
                        // Mode Toggle (Small)
                        ProcessingModeSmallToggle(
                            mode = uiState.processingMode,
                            onClick = { viewModel.toggleProcessingMode() }
                        )
                        
                        IconButton(
                            onClick = { 
                                viewModel.refreshSms(context)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                    
                }
            )
        }
    ) { padding ->
        // ... (rest of Scaffold content)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SMS Permission Banner
            if (!uiState.hasSmsPermission) {
                item {
                    SmsPermissionBanner(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    )
                }
            }
            
            // AI Model Download Card REMOVED as per user request (Moved to Settings)

            
            // Success message REMOVED as per user request
            
            // Processing Mode Selector REMOVED (Moved to AppBar)

            // Time Period Selector
            item {
                TimePeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it }
                )
            }
            
            // Summary Cards - Inflow and Outflow
            item {
                AnimatedContent(
                    targetState = selectedPeriod,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "summary_animation"
                ) { period ->
                    SummaryCards(
                        inflow = uiState.todayInflow,
                        outflow = uiState.todayOutflow,
                        periodLabel = period.label
                    )
                }
            }
            
            // Chart based on selected period (no hourly chart)
            if (selectedPeriod != TimePeriod.TODAY) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when (selectedPeriod) {
                                    TimePeriod.TODAY -> ""
                                    TimePeriod.WEEK -> "Daily Spending"
                                    TimePeriod.MONTH -> "Weekly Spending"
                                    TimePeriod.YEAR -> "Monthly Spending"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Build chart data from real transactions
                            // Build chart data from ALL transactions (filtered by period)
                            val allTransactions = uiState.allTransactions
                            
                            // Weekly data - Current Week Only
                            val dailyData = remember(allTransactions, selectedPeriod) {
                                if (selectedPeriod != TimePeriod.WEEK) return@remember emptyList<DailyData>()
                                
                                val cal = java.util.Calendar.getInstance()
                                // Set to start of current week (Sunday)
                                cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                cal.set(java.util.Calendar.MINUTE, 0)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                val startOfWeek = cal.timeInMillis
                                
                                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                val dailyTotals = mutableMapOf<Int, Double>()
                                
                                allTransactions.forEach { txn ->
                                    if (txn.type == com.expense.tracker.domain.model.TransactionType.DEBIT && txn.timestamp >= startOfWeek) {
                                        cal.timeInMillis = txn.timestamp
                                        // Double check it's the right week (in case of future dates?) - assuming no future txns
                                        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
                                        dailyTotals[dayOfWeek] = (dailyTotals[dayOfWeek] ?: 0.0) + txn.amount
                                    }
                                }
                                
                                val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
                                dayNames.mapIndexed { index, name ->
                                    DailyData(name, dailyTotals[index] ?: 0.0, index == today)
                                }
                            }
                            
                            // Monthly data - Current Month Only
                            val monthlyBarData = remember(allTransactions, selectedPeriod) {
                                if (selectedPeriod != TimePeriod.MONTH) return@remember emptyList<DailyData>()
                                
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                cal.set(java.util.Calendar.MINUTE, 0)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                val startOfMonth = cal.timeInMillis
                                
                                val weekTotals = mutableMapOf<Int, Double>()
                                
                                allTransactions.forEach { txn ->
                                    if (txn.type == com.expense.tracker.domain.model.TransactionType.DEBIT && txn.timestamp >= startOfMonth) {
                                        cal.timeInMillis = txn.timestamp
                                        val weekOfMonth = cal.get(java.util.Calendar.WEEK_OF_MONTH)
                                        weekTotals[weekOfMonth] = (weekTotals[weekOfMonth] ?: 0.0) + txn.amount
                                    }
                                }
                                
                                val currentWeek = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_MONTH)
                                (1..5).map { week ->
                                    DailyData("W$week", weekTotals[week] ?: 0.0, week == currentWeek)
                                }
                            }
                            
                            // Yearly data - Current Year Only
                            val yearlyData = remember(allTransactions, selectedPeriod) {
                                if (selectedPeriod != TimePeriod.YEAR) return@remember emptyList<DailyData>()
                                
                                val cal = java.util.Calendar.getInstance()
                                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                cal.set(java.util.Calendar.MINUTE, 0)
                                cal.set(java.util.Calendar.SECOND, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                val startOfYear = cal.timeInMillis
                                
                                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                                val monthTotals = mutableMapOf<Int, Double>()
                                
                                allTransactions.forEach { txn ->
                                    if (txn.type == com.expense.tracker.domain.model.TransactionType.DEBIT && txn.timestamp >= startOfYear) {
                                        cal.timeInMillis = txn.timestamp
                                        val month = cal.get(java.util.Calendar.MONTH)
                                        monthTotals[month] = (monthTotals[month] ?: 0.0) + txn.amount
                                    }
                                }
                                
                                val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                                monthNames.mapIndexed { index, name ->
                                    DailyData(name, monthTotals[index] ?: 0.0, index == currentMonth)
                                }
                            }
                            
                            when (selectedPeriod) {
                                TimePeriod.TODAY -> { /* No chart for today */ }
                                TimePeriod.WEEK -> DailyBarChart(data = dailyData)
                                TimePeriod.MONTH -> DailyBarChart(data = monthlyBarData)
                                TimePeriod.YEAR -> TrendLineChart(data = yearlyData)
                            }
                        }
                    }
                }
            }
            
            // Statistics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        label = "Transactions",
                        value = uiState.totalTransactionCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    StatCard(
                        label = "Uncategorized",
                        value = uiState.unclassifiedCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Recent Transactions
            if (uiState.recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(uiState.recentTransactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction }
                    )
                }
            }
            
            
            // Loading indicator
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            // Empty state
            if (!uiState.isLoading && uiState.recentTransactions.isEmpty() && uiState.hasSmsPermission) {
                item { EmptyState() }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    
    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }
    
    // Details Dialog
    if (selectedTransaction != null) {
        TransactionDetailsDialog(
            transaction = selectedTransaction!!,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
private fun ProcessingModeSmallToggle(
    mode: String,
    onClick: () -> Unit
) {
    val isRules = mode == "RULES"
    val icon = if (isRules) Icons.Default.Memory else Icons.Default.AutoAwesome
    val tint = if (isRules) MaterialTheme.colorScheme.tertiary else Color(0xFF4CAF50)
    
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = "Toggle Processing Mode",
            tint = tint
        )
    }
}


@Composable
private fun DownloadSuccessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI Model Ready • Smart categorization enabled",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        TimePeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TimePeriod.entries.size
                )
            ) {
                Text(period.label)
            }
        }
    }
}

@Composable
private fun SummaryCards(
    inflow: Double,
    outflow: Double,
    periodLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BalanceCard(
            title = "Received",
            amount = inflow,
            type = BalanceType.INFLOW,
            modifier = Modifier.weight(1f)
        )
        BalanceCard(
            title = "Spent",
            amount = outflow,
            type = BalanceType.OUTFLOW,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SmsPermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "SMS Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Read bank SMS to auto-track expenses. Data stays on device.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Sms,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Bank SMS messages will appear here",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



