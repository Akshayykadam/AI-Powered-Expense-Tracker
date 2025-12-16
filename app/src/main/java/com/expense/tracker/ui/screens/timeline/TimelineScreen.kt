package com.expense.tracker.ui.screens.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.domain.model.TransactionType
import com.expense.tracker.ui.components.BalanceCard
import com.expense.tracker.ui.components.BalanceType
import com.expense.tracker.ui.components.Period
import com.expense.tracker.ui.components.PeriodSwitcher
import com.expense.tracker.ui.components.TransactionCard
import com.expense.tracker.ui.components.TransactionDetailsDialog
import com.expense.tracker.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

enum class TransactionFilter { ALL, INFLOW, OUTFLOW }

// Monthly breakdown data
data class MonthlyBreakdown(
    val month: String,
    val inflow: Double,
    val outflow: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableStateOf(TransactionFilter.ALL) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    // Filter transactions based on selection
    val filteredTransactions = remember(uiState.transactions, selectedFilter, selectedMonth) {
        var result = uiState.transactions
        
        // Filter by type
        result = when (selectedFilter) {
            TransactionFilter.ALL -> result
            TransactionFilter.INFLOW -> result.filter { it.type == TransactionType.CREDIT }
            TransactionFilter.OUTFLOW -> result.filter { it.type == TransactionType.DEBIT }
        }
        
        // Filter by month if selected
        if (selectedMonth != null) {
            result = result.filter { transaction ->
                val month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(transaction.timestamp))
                month == selectedMonth
            }
        }
        
        result
    }
    
    // Monthly breakdown data - computed from actual transactions
    val monthlyBreakdown = remember(uiState.transactions) {
        val calendar = java.util.Calendar.getInstance()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthlyInflow = mutableMapOf<Int, Double>()
        val monthlyOutflow = mutableMapOf<Int, Double>()
        
        uiState.transactions.forEach { txn ->
            calendar.timeInMillis = txn.timestamp
            val month = calendar.get(java.util.Calendar.MONTH)
            
            if (txn.type == TransactionType.CREDIT) {
                monthlyInflow[month] = (monthlyInflow[month] ?: 0.0) + txn.amount
            } else {
                monthlyOutflow[month] = (monthlyOutflow[month] ?: 0.0) + txn.amount
            }
        }
        
        monthNames.mapIndexedNotNull { index, name ->
            val inflow = monthlyInflow[index] ?: 0.0
            val outflow = monthlyOutflow[index] ?: 0.0
            if (inflow > 0 || outflow > 0) {
                MonthlyBreakdown(name, inflow, outflow)
            } else null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Timeline",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period Switcher
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PeriodSwitcher(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { 
                        viewModel.onPeriodSelected(it)
                        selectedFilter = TransactionFilter.ALL
                        selectedMonth = null
                    }
                )
            }
            
            // Period Label
            item {
                Text(
                    text = uiState.periodLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Summary Cards - Clickable to filter
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        title = if (selectedFilter == TransactionFilter.INFLOW) "✓ Received" else "Received",
                        amount = uiState.totalInflow,
                        type = BalanceType.INFLOW,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedFilter = if (selectedFilter == TransactionFilter.INFLOW) 
                                    TransactionFilter.ALL else TransactionFilter.INFLOW
                            }
                    )
                    BalanceCard(
                        title = if (selectedFilter == TransactionFilter.OUTFLOW) "✓ Spent" else "Spent",
                        amount = uiState.totalOutflow,
                        type = BalanceType.OUTFLOW,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedFilter = if (selectedFilter == TransactionFilter.OUTFLOW) 
                                    TransactionFilter.ALL else TransactionFilter.OUTFLOW
                            }
                    )
                }
            }
            
            // Monthly Breakdown (for Year view)
            if (uiState.selectedPeriod == Period.YEAR) {
                item {
                    Text(
                        text = "Monthly Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            monthlyBreakdown.forEach { month ->
                                MonthlyBreakdownRow(
                                    breakdown = month,
                                    isSelected = selectedMonth == month.month,
                                    onClick = {
                                        selectedMonth = if (selectedMonth == month.month) null else month.month
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Filter indicator
            if (selectedFilter != TransactionFilter.ALL || selectedMonth != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterText = buildString {
                            if (selectedMonth != null) append("$selectedMonth ")
                            if (selectedFilter != TransactionFilter.ALL) {
                                append(selectedFilter.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        }.trim()
                        Text(
                            text = "Showing: $filterText",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { 
                            selectedFilter = TransactionFilter.ALL
                            selectedMonth = null
                        }) {
                            Text("Clear")
                        }
                    }
                }
            }
            
            // Transactions Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transactions (${filteredTransactions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Loading
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
            
            // Transaction List grouped by date
            val groupedTransactions = filteredTransactions.groupBy { transaction ->
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(Date(transaction.timestamp))
            }
            
            groupedTransactions.forEach { (date, transactions) ->
                item {
                    DateHeader(date = date)
                }
                
                items(transactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction }
                    )
                }
            }
            
            // Empty State
            if (!uiState.isLoading && filteredTransactions.isEmpty()) {
                item {
                    EmptyTimelineState(
                        message = if (selectedFilter != TransactionFilter.ALL || selectedMonth != null) 
                            "No matching transactions" 
                        else "No transactions found"
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
private fun MonthlyBreakdownRow(
    breakdown: MonthlyBreakdown,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = breakdown.month,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(40.dp)
        )
        
        // Inflow
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${formatCompact(breakdown.inflow)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF51CF66),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "In",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Outflow
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${formatCompact(breakdown.outflow)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Out",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Net
        val net = breakdown.inflow - breakdown.outflow
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (net >= 0) "+" else ""}₹${formatCompact(kotlin.math.abs(net))}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (net >= 0) Color(0xFF51CF66) else Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Net",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatCompact(amount: Double): String {
    return when {
        amount >= 100000 -> String.format("%.1fL", amount / 100000)
        amount >= 1000 -> String.format("%.1fK", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}

@Composable
private fun DateHeader(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptyTimelineState(message: String = "No transactions found") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📊",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
