package com.expense.tracker.ui.screens.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.ui.theme.PurplePrimary
import com.expense.tracker.ui.theme.AccentPink
import com.expense.tracker.domain.model.Transaction
import com.expense.tracker.domain.model.TransactionType
import com.expense.tracker.ui.components.TransactionCard
import com.expense.tracker.ui.components.TransactionDetailsDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Calendar",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Navigation
            item {
                Spacer(modifier = Modifier.height(16.dp))
                MonthNavigator(
                    currentMonth = uiState.currentMonth,
                    currentYear = uiState.currentYear,
                    onPreviousMonth = { viewModel.previousMonth() },
                    onNextMonth = { viewModel.nextMonth() }
                )
            }
            
            // Calendar Grid with Heat Map
            item {
                CalendarGrid(
                    year = uiState.currentYear,
                    month = uiState.currentMonth,
                    dailySpending = uiState.dailySpending,
                    selectedDate = uiState.selectedDate,
                    onDateSelected = { viewModel.selectDate(it) },
                    maxSpending = uiState.maxDailySpending
                )
            }
            
            // Selected Day Summary
            if (uiState.selectedDate != null) {
                item {
                    SelectedDaySummary(
                        date = uiState.selectedDate!!,
                        totalSpent = uiState.selectedDaySpent,
                        totalReceived = uiState.selectedDayReceived,
                        transactionCount = uiState.selectedDayTransactions.size
                    )
                }
                
                // Transactions for selected day
                items(uiState.selectedDayTransactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = { selectedTransaction = transaction }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        
        if (selectedTransaction != null) {
            TransactionDetailsDialog(
                transaction = selectedTransaction!!,
                onDismiss = { selectedTransaction = null }
            )
        }
    }
}

@Composable
private fun MonthNavigator(
    currentMonth: Int,
    currentYear: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthNames[currentMonth],
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentYear.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onNextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    dailySpending: Map<Int, Double>,
    selectedDate: Int?,
    onDateSelected: (Int) -> Unit,
    maxSpending: Double
) {
    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                dayHeaders.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar days
            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > daysInMonth) break
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        if ((week == 0 && dayOfWeek < firstDayOfWeek) || dayCounter > daysInMonth) {
                            // Empty cell
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val day = dayCounter
                            val spending = dailySpending[day] ?: 0.0
                            val isSelected = selectedDate == day
                            
                            CalendarDay(
                                day = day,
                                spending = spending,
                                maxSpending = maxSpending,
                                isSelected = isSelected,
                                onClick = { onDateSelected(day) },
                                modifier = Modifier.weight(1f)
                            )
                            dayCounter++
                        }
                    }
                }
            }
            
            // Legend
            Spacer(modifier = Modifier.height(12.dp))
            HeatMapLegend()
        }
    }
}

@Composable
private fun CalendarDay(
    day: Int,
    spending: Double,
    maxSpending: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate heat intensity (0.0 to 1.0)
    val intensity = if (maxSpending > 0) (spending / maxSpending).coerceIn(0.0, 1.0) else 0.0
    
    // Heat map colors - GenZ Theme
    val heatColor = when {
        spending == 0.0 -> Color.Transparent
        intensity < 0.25 -> PurplePrimary.copy(alpha = 0.2f)
        intensity < 0.5 -> PurplePrimary.copy(alpha = 0.5f)
        intensity < 0.75 -> AccentPink.copy(alpha = 0.6f)
        else -> AccentPink.copy(alpha = 0.9f)
    }
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else heatColor,
        animationSpec = tween(200),
        label = "day_color"
    )
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else if (intensity > 0.5 && spending > 0) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (spending > 0 && !isSelected) {
                Text(
                    text = formatCompactAmount(spending),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HeatMapLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        
        listOf(
            PurplePrimary.copy(alpha = 0.2f),
            PurplePrimary.copy(alpha = 0.5f),
            AccentPink.copy(alpha = 0.6f),
            AccentPink.copy(alpha = 0.9f)
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SelectedDaySummary(
    date: Int,
    totalSpent: Double,
    totalReceived: Double,
    transactionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Day $date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem(
                    label = "Spent",
                    amount = totalSpent,
                    color = Color(0xFFE53935)
                )
                SummaryItem(
                    label = "Received",
                    amount = totalReceived,
                    color = Color(0xFF43A047)
                )
                SummaryItem(
                    label = "Transactions",
                    amount = transactionCount.toDouble(),
                    color = MaterialTheme.colorScheme.primary,
                    isCount = true
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Double,
    color: Color,
    isCount: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = if (isCount) amount.toInt().toString() else "₹${formatCompactAmount(amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatCompactAmount(amount: Double): String {
    return when {
        amount >= 100000 -> String.format("%.1fL", amount / 100000)
        amount >= 1000 -> String.format("%.1fK", amount / 1000)
        else -> String.format("%.0f", amount)
    }
}
