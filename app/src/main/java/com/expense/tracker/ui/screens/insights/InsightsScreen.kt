package com.expense.tracker.ui.screens.insights

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expense.tracker.ui.components.GlassCard
import com.expense.tracker.ui.components.PeriodSwitcher
import com.expense.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Spending, 1 = Income
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                ) {
                    Text(
                        text = "Analytics & Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                PeriodSwitcher(
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodSelected = { viewModel.onPeriodSelected(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Hero Chart Section
            item {
                InsightsHeroSection(uiState = uiState)
            }
        }
    }
}

@Composable
fun InsightsHeroSection(uiState: InsightsUiState) {
    // Mode is always Payment Mode now as per user request
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Data for Payment Mode
        val dataMap = uiState.paymentModeTotals
        val total = dataMap.values.sum()
        val sortedData = dataMap.toList().sortedByDescending { it.second }
        
        // Glassy Colors (Payment Mode) - Vibrant but with transparency for glass effect
        val colors = listOf(
            PurplePrimary.copy(alpha = 0.8f), 
            AccentPink.copy(alpha = 0.8f), 
            AccentCyan.copy(alpha = 0.8f), 
            AccentGold.copy(alpha = 0.8f),
            Color(0xFF9C27B0).copy(alpha = 0.8f)
        )

        // HERO CHART (Bigger & Glassy)
        val animatedProgress = remember { androidx.compose.animation.core.Animatable(0f) }
        
        LaunchedEffect(sortedData) {
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }

        Box(
            modifier = Modifier.size(280.dp), // Slightly larger
            contentAlignment = Alignment.Center
        ) {
            // Background glow for glass effect

            if (sortedData.isNotEmpty()) {
                DonutChart(
                    data = sortedData,
                    total = total,
                    colors = colors,
                    modifier = Modifier.size(260.dp),
                    thickness = 40.dp,
                    animationProgress = animatedProgress.value
                )
            }
            
            // Center Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${String.format("%,.0f", total)}", // Full amount
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Detailed Legend List
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Payment Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (sortedData.isEmpty()) {
                    Text(
                         text = "No data for this period",
                         modifier = Modifier.align(Alignment.CenterHorizontally),
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sortedData.forEachIndexed { index, (name, amount) ->
                        val color = colors.getOrElse(index) { Color.Gray }
                        val percentage = if (total > 0) (amount / total * 100) else 0.0
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "(${String.format("%.1f", percentage)}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "₹${String.format("%,.0f", amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Mini progress bar below each item
                        LinearProgressIndicator(
                            progress = { percentage.toFloat() / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp) // Slightly thicker for emphasis
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = color.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    data: List<Pair<String, Double>>,
    total: Double,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 20.dp,
    animationProgress: Float = 1f
) {
    var startAngle = -90f
    
    Canvas(modifier = modifier) {
        val totalSweep = 360f * animationProgress
        
        data.forEachIndexed { index, (_, amount) ->
            val sweepAngle = ((amount / total) * 360f).toFloat() * animationProgress
            val color = colors.getOrElse(index) { Color.Gray }
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt),
                size = Size(size.width, size.height),
                topLeft = Offset(0f, 0f)
            )
            
            startAngle += sweepAngle
        }
    }
}


@Composable
fun MerchantRow(name: String, amount: Double, total: Double, color: Color) {
    val percentage = if (total > 0) (amount / total) else 0.0
    
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1
                    )
                    Text(
                        text = "₹${String.format("%,.0f", amount)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                LinearProgressIndicator(
                    progress = { percentage.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun SegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onIndexChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        items.forEachIndexed { index, text ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onIndexChanged(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
