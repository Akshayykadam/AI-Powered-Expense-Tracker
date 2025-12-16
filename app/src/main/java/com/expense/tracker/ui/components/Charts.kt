package com.expense.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expense.tracker.domain.model.Category
import com.expense.tracker.ui.theme.*
import kotlin.math.roundToInt

/**
 * Data classes for charts
 */
data class HourlyData(val hour: Int, val debit: Double, val credit: Double)
data class DailyData(val dayOfWeek: String, val amount: Double, val isToday: Boolean = false)
data class MonthlyData(val month: String, val income: Double, val expense: Double)
data class CategoryData(val category: Category, val amount: Double, val percentage: Float)

/**
 * Premium animated Y-axis label formatter
 */
private fun formatYAxisValue(value: Double): String {
    return when {
        value >= 100000 -> "₹${(value / 100000).roundToInt()}L"
        value >= 1000 -> "₹${(value / 1000).roundToInt()}K"
        value > 0 -> "₹${value.roundToInt()}"
        else -> ""
    }
}

/**
 * Premium Hourly Bar Chart with gradient bars and animations
 */
@Composable
fun HourlyBarChart(
    data: List<HourlyData>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { maxOf(it.debit, it.credit) }?.coerceAtLeast(100.0) ?: 100.0
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    // Animate bar heights
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "bar_animation"
    )
    LaunchedEffect(Unit) { animationPlayed = true }
    
    // Gradient colors
    val debitGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A5A))
    )
    val creditGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF51CF66), Color(0xFF40C057))
    )
    
    Row(modifier = modifier.fillMaxWidth()) {
        // Y-axis labels
        Column(
            modifier = Modifier.width(42.dp).height(160.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(1.0, 0.5, 0.0).forEach { ratio ->
                Text(
                    text = if (ratio > 0) formatYAxisValue(maxValue * ratio) else "₹0",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor.copy(alpha = 0.7f),
                    fontSize = 9.sp
                )
            }
        }
        
        // Chart with bars
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(start = 4.dp)
            ) {
                if (data.isEmpty()) return@Canvas
                
                val barWidth = size.width / data.size * 0.7f
                val spacing = size.width / data.size
                val chartHeight = size.height - 8f
                
                // Draw subtle gradient background
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Gray.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    ),
                    size = Size(size.width, chartHeight)
                )
                
                // Draw horizontal grid lines with fade
                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { ratio ->
                    val y = chartHeight * (1 - ratio)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                
                data.forEachIndexed { index, hourData ->
                    val x = spacing * index + (spacing - barWidth) / 2
                    
                    // Debit bar with gradient
                    if (hourData.debit > 0) {
                        val barHeight = (hourData.debit / maxValue * chartHeight * animationProgress).toFloat()
                        drawRoundRect(
                            brush = debitGradient,
                            topLeft = Offset(x, chartHeight - barHeight),
                            size = Size(barWidth * 0.45f, barHeight),
                            cornerRadius = CornerRadius(4f)
                        )
                    }
                    
                    // Credit bar with gradient (offset)
                    if (hourData.credit > 0) {
                        val barHeight = (hourData.credit / maxValue * chartHeight * animationProgress).toFloat()
                        drawRoundRect(
                            brush = creditGradient,
                            topLeft = Offset(x + barWidth * 0.5f, chartHeight - barHeight),
                            size = Size(barWidth * 0.45f, barHeight),
                            cornerRadius = CornerRadius(4f)
                        )
                    }
                }
            }
            
            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12am", "6am", "12pm", "6pm", "11pm").forEach { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF51CF66), label = "Received")
                Spacer(modifier = Modifier.width(20.dp))
                LegendItem(color = Color(0xFFFF6B6B), label = "Spent")
            }
        }
    }
}

/**
 * Premium Daily Bar Chart with gradient and animations
 */
/**
 * Premium Daily Bar Chart with modern aesthetics
 */
@Composable
fun DailyBarChart(
    data: List<DailyData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    activeBarColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "bar_anim"
    )
    LaunchedEffect(Unit) { animationPlayed = true }
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Chart Area
        Row(
            modifier = Modifier.height(180.dp).fillMaxWidth()
        ) {
            // Y-Axis Labels
            Column(
                modifier = Modifier.fillMaxHeight().width(40.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatYAxisValue(maxValue), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
                Text(formatYAxisValue(maxValue * 0.5), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
                Text("0", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = labelColor)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Bars
            Canvas(
                modifier = Modifier.fillMaxHeight().weight(1f)
            ) {
                val barWidth = (size.width / (data.size * 2)).coerceAtMost(60f)
                val spacing = size.width / data.size
                val bottomY = size.height
                
                // Draw Grid Lines
                val midY = size.height / 2
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, midY),
                    end = Offset(size.width, midY),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height)
                )
                
                // Draw Bars
                data.forEachIndexed { index, item ->
                    val x = spacing * index + (spacing / 2) - (barWidth / 2)
                    val heightRatio = (item.amount / maxValue).toFloat().coerceIn(0f, 1f)
                    val barHeight = size.height * heightRatio * animationProgress
                    
                    val color = if (item.isToday) activeBarColor else barColor
                    val brush = Brush.verticalGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    )
                    
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, bottomY - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
        
        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { 
                Text(
                    text = it.dayOfWeek.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (it.isToday) activeBarColor else labelColor,
                    fontWeight = if (it.isToday) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Modern Line Chart for Trends (Yearly/Monthly)
 */
@Composable
fun TrendLineChart(
    data: List<DailyData>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    val maxValue = data.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "line_anim"
    )
    LaunchedEffect(Unit) { animationPlayed = true }

    Column(modifier = modifier) {
        Box(modifier = Modifier.height(180.dp).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spacePerPoint = width / (data.size - 1)
                
                val path = Path()
                
                // Draw Grid
                drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, height), Offset(width, height))
                
                // Build Path
                data.forEachIndexed { i, item ->
                    val x = i * spacePerPoint
                    val y = height - ((item.amount / maxValue) * height * animationProgress)
                    
                    if (i == 0) path.moveTo(x, y.toFloat())
                    else {
                        // Cubic Bezier for smooth curves
                        val prevX = (i - 1) * spacePerPoint
                        val prevY = height - ((data[i-1].amount / maxValue) * height * animationProgress)
                        val conX1 = prevX + spacePerPoint / 2
                        val conX2 = x - spacePerPoint / 2
                        path.cubicTo(conX1, prevY.toFloat(), conX2, y.toFloat(), x, y.toFloat())
                    }
                }

                // Draw Fill
                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(width, height)
                fillPath.lineTo(0f, height)
                fillPath.close()
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw Stroke
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                
                // Draw Points
                data.forEachIndexed { i, item ->
                    val x = i * spacePerPoint
                    val y = height - ((item.amount / maxValue) * height * animationProgress)
                    drawCircle(Color.White, radius = 6f, center = Offset(x, y.toFloat()))
                    drawCircle(lineColor, radius = 4f, center = Offset(x, y.toFloat()))
                }
            }
        }
        
        // X-Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Show first, middle, last label to avoid clutter
            val labels = listOfNotNull(
                data.firstOrNull(),
                data.getOrNull(data.size / 2),
                data.lastOrNull()
            ).distinct()
            
            labels.forEach { 
                Text(
                    text = it.dayOfWeek,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    data: List<CategoryData>,
    onCategoryClick: (Category) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Keep existing implementation but ensure imports are valid
    // For brevity, using the existing code structure if possible or rewriting
    val total = data.sumOf { it.amount }
    if (total == 0.0) return
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donut_animation"
    )
    LaunchedEffect(Unit) { animationPlayed = true }
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(data) {
                    detectTapGestures { onCategoryClick(data.firstOrNull()?.category ?: Category.OTHER) }
                }
        ) {
            val radius = size.minDimension / 2
            val innerRadius = radius * 0.6f
            val center = Offset(size.width / 2, size.height / 2)
            
            var startAngle = -90f
            
            data.forEach { categoryData ->
                val sweepAngle = (categoryData.amount / total * 360 * animatedProgress).toFloat()
                val color = getCategoryColor(categoryData.category)
                
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
            
            drawCircle(
                color = Color.White,
                radius = innerRadius,
                center = center
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "₹${formatIndianNumber(total.toLong())}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total Spent",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Legend item component
 */
@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

// Helper functions
fun getCategoryColor(category: Category): Color {
    return when (category) {
        Category.FOOD_DINING -> Color(0xFFFF6B6B)
        Category.GROCERY -> Color(0xFF51CF66)
        Category.FUEL -> Color(0xFFFFD43B)
        Category.MEDICAL -> Color(0xFFFF8787)
        Category.UTILITIES -> Color(0xFF748FFC)
        Category.RENT -> Color(0xFFFF922B)
        Category.FASHION -> Color(0xFFF06595)
        Category.ENTERTAINMENT -> Color(0xFFCC5DE8)
        Category.TRAVEL -> Color(0xFF20C997)
        Category.SUBSCRIPTIONS -> Color(0xFF845EF7)
        Category.EMI_LOAN, Category.CREDIT_CARD -> Color(0xFFFA5252)
        Category.INSURANCE -> Color(0xFF5C7CFA)
        Category.INVESTMENTS -> Color(0xFF12B886)
        Category.UPI_TRANSFER, Category.BANK_TRANSFER -> Color(0xFF339AF0)
        Category.INCOME -> Color(0xFF40C057)
        Category.EDUCATION -> Color(0xFF15AABF)
        Category.OTHER -> Color(0xFF868E96)
    }
}

fun formatIndianNumber(number: Long): String {
    val str = number.toString()
    if (str.length <= 3) return str
    
    val result = StringBuilder()
    var count = 0
    
    for (i in str.length - 1 downTo 0) {
        result.insert(0, str[i])
        count++
        if (count == 3 && i > 0) {
            result.insert(0, ',')
            count = 0
        } else if (count == 2 && i > 0 && result.count { it == ',' } >= 1) {
            result.insert(0, ',')
            count = 0
        }
    }
    
    return result.toString()
}
