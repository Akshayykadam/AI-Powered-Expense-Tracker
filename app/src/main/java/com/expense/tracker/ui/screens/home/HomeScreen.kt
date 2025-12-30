package com.expense.tracker.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.ui.components.*
import com.expense.tracker.ui.theme.*
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
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    
    var selectedPeriod by remember { mutableStateOf(TimePeriod.WEEK) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
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
    
    // Initialize on launch
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        viewModel.updateSmsPermissionStatus(hasPermission)
        viewModel.checkModelStatus(context)
        
        // Fetch initial AI insight
        viewModel.fetchAIInsight()
    }
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Modern gradient top bar with centered content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                PurplePrimary.copy(alpha = if (isDarkMode) 0.3f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title group (Left)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Expense Tracker",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1
                        )
                    }
                    
                    // Actions group (Right)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // AI Status Badge
                        AIStatusBadge(isReady = uiState.isModelDownloaded)
                        
                        // Refresh
                        if (uiState.hasSmsPermission) {
                            Spacer(modifier = Modifier.width(10.dp))
                            IconButton(
                                onClick = { viewModel.refreshSms(context) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Extra spacing below header
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            // SMS Permission Banner (if needed)
            if (!uiState.hasSmsPermission) {
                item {
                    SmsPermissionBanner(
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    )
                }
            }
            
            // AI Insight Widget 🔥
            item {
                AIInsightWidget(
                    insight = uiState.aiInsight,
                    isLoading = uiState.isLoadingInsight,
                    onRefresh = { viewModel.fetchAIInsight() }
                )
            }
            
            // Time Period Selector
            item {
                ModernTimePeriodSelector(
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BalanceCard(
                            title = "Received",
                            amount = uiState.todayInflow,
                            type = BalanceType.INFLOW,
                            modifier = Modifier.weight(1f)
                        )
                        BalanceCard(
                            title = "Spent",
                            amount = uiState.todayOutflow,
                            type = BalanceType.OUTFLOW,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Chart (for non-today periods)
            if (selectedPeriod != TimePeriod.TODAY) {
                item {
                    ChartCard(
                        selectedPeriod = selectedPeriod,
                        allTransactions = uiState.allTransactions
                    )
                }
            }
            
            // Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatCard(
                        icon = Icons.Default.Receipt,
                        label = "Transactions",
                        value = uiState.totalTransactionCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatCard(
                        icon = Icons.Default.Help,
                        label = "Uncategorized",
                        value = uiState.unclassifiedCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Recent Transactions Header
            if (uiState.recentTransactions.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See All →",
                            style = MaterialTheme.typography.labelMedium,
                            color = PurplePrimary
                        )
                    }
                }
                
                items(uiState.recentTransactions.take(10)) { transaction ->
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
                        CircularProgressIndicator(color = PurplePrimary)
                    }
                }
            }
            
            // Empty state
            if (!uiState.isLoading && uiState.recentTransactions.isEmpty() && uiState.hasSmsPermission) {
                item { ModernEmptyState() }
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
private fun ModernTimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimePeriod.entries.forEach { period ->
            val isSelected = selectedPeriod == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) PurplePrimary else Color.Transparent
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = period.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 8.dp)
                        .then(
                            if (!isSelected) Modifier.clickable { onPeriodSelected(period) }
                            else Modifier
                        )
                )
            }
        }
    }
}

@Composable
private fun ModernStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurplePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PurplePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    selectedPeriod: TimePeriod,
    allTransactions: List<Transaction>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when (selectedPeriod) {
                    TimePeriod.TODAY -> ""
                    TimePeriod.WEEK -> "Daily Spending"
                    TimePeriod.MONTH -> "Weekly Spending"
                    TimePeriod.YEAR -> "Monthly Trends"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Chart data calculation
            val dailyData = remember(allTransactions, selectedPeriod) {
                if (selectedPeriod != TimePeriod.WEEK) return@remember emptyList<DailyData>()
                
                val cal = java.util.Calendar.getInstance()
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
                        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK) - 1
                        dailyTotals[dayOfWeek] = (dailyTotals[dayOfWeek] ?: 0.0) + txn.amount
                    }
                }
                
                val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) - 1
                dayNames.mapIndexed { index, name ->
                    DailyData(name, dailyTotals[index] ?: 0.0, index == today)
                }
            }
            
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
            
            val yearlyData = remember(allTransactions, selectedPeriod) {
                if (selectedPeriod != TimePeriod.YEAR) return@remember emptyList<DailyData>()
                
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_YEAR, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val startOfYear = cal.timeInMillis
                
                val monthNames = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
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
                TimePeriod.TODAY -> { /* No chart */ }
                TimePeriod.WEEK -> DailyBarChart(data = dailyData)
                TimePeriod.MONTH -> DailyBarChart(data = monthlyBarData)
                TimePeriod.YEAR -> TrendLineChart(data = yearlyData)
            }
        }
    }
}

@Composable
private fun SmsPermissionBanner(
    onRequestPermission: () -> Unit
) {
    GradientGlassCard(
        gradientColors = listOf(
            PurplePrimary.copy(alpha = 0.2f),
            AccentPink.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Sms,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "SMS Permission Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Auto-track expenses from bank SMS. Data stays on device.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ModernEmptyState() {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bank SMS messages will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


