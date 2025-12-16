package com.expense.tracker.ui.screens.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.domain.model.Category
import com.expense.tracker.ui.components.InsightCard
import com.expense.tracker.ui.components.PeriodSwitcher
import com.expense.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Insights",
                        fontWeight = FontWeight.Bold
                    ) 
                }
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
            // Period Switcher
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PeriodSwitcher(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.onPeriodSelected(it) }
                )
            }
            
            // Total Spending
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Total Spending",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%,.2f", uiState.totalSpending)}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Category Pie Chart
            if (uiState.categoryTotals.isNotEmpty()) {
                item {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SimplePieChart(
                        data = uiState.categoryTotals,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                
                // Category breakdown list
                item {
                    CategoryBreakdown(
                        categoryTotals = uiState.categoryTotals,
                        total = uiState.totalSpending
                    )
                }
            }
            
            // AI Insight Section
            if (uiState.isAiConfigured) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uiState.observation != null || uiState.isGeneratingInsight) {
                        InsightCard(
                            observation = uiState.observation,
                            comparison = uiState.comparison,
                            suggestion = uiState.suggestion,
                            isLoading = uiState.isGeneratingInsight,
                            onRegenerate = { viewModel.generateInsight() },
                            onDismiss = { }
                        )
                    } else {
                        Button(
                            onClick = { viewModel.generateInsight() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Insight")
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🤖",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI Insights",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Download AI model from Home screen for smart insights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SimplePieChart(
    data: Map<Category, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0.0) return
    
    val sortedData = data.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
    
    Canvas(modifier = modifier) {
        val canvasSize = minOf(size.width, size.height)
        val radius = canvasSize / 2.5f
        val center = Offset(size.width / 2, size.height / 2)
        
        var startAngle = -90f
        
        sortedData.forEach { (category, value) ->
            val sweepAngle = (value / total * 360).toFloat()
            val color = getCategoryChartColor(category)
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
        
        // Inner circle for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.5f,
            center = center
        )
    }
}

@Composable
private fun CategoryBreakdown(
    categoryTotals: Map<Category, Double>,
    total: Double
) {
    val sortedTotals = categoryTotals.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sortedTotals.forEach { (category, amount) ->
            val percentage = if (total > 0) (amount / total * 100) else 0.0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = getCategoryChartColor(category),
                        shape = MaterialTheme.shapes.extraSmall
                    ) { }
                    
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "₹${String.format("%,.0f", amount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCategoryChartColor(category: Category): Color {
    return when (category) {
        Category.FOOD_DINING -> CategoryFood
        Category.GROCERY -> Color(0xFF66BB6A)
        Category.FUEL, Category.TRAVEL -> CategoryTravel
        Category.FASHION -> CategoryShopping
        Category.UTILITIES -> CategoryUtilities
        Category.RENT -> CategoryRent
        Category.SUBSCRIPTIONS -> CategorySubscriptions
        Category.ENTERTAINMENT -> CategoryEntertainment
        Category.MEDICAL -> CategoryHealthcare
        Category.UPI_TRANSFER, Category.BANK_TRANSFER -> CategoryTransfers
        Category.INCOME, Category.INVESTMENTS -> CategoryIncome
        Category.EMI_LOAN, Category.CREDIT_CARD -> Color(0xFFEF5350)
        Category.INSURANCE -> Color(0xFF5C6BC0)
        Category.EDUCATION -> Color(0xFF26A69A)
        Category.OTHER -> CategoryOther
    }
}
