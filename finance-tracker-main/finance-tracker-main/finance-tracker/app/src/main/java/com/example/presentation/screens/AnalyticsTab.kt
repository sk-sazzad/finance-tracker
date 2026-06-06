package com.example.presentation.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun AnalyticsTab(
    viewModel: FinanceViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val healthScore by viewModel.getFinancialHealthScore().collectAsState()
    val totals by viewModel.getThisMonthTotals().collectAsState()
    val prediction by viewModel.getAISpendingPrediction().collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- TITLE ---
        Text(
            text = "Insights & Trends",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // --- SECTION 1: HEALTH BREAKDOWN RADAR ---
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color(0x11FFFFFF), style = Stroke(width = 8.dp.toPx()))
                        drawArc(
                            color = ElectricPurple,
                            startAngle = -90f,
                            sweepAngle = (healthScore / 100f) * 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }
                    Text(
                        text = "$healthScore",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Aggregated Financial Health", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Stable standing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Scores calculated across 5 custom parameters: Savings Rate, Budget, Debt, Reserves & Streak Consistency.",
                        fontSize = 10.sp,
                        color = TextSecondaryDark,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // --- NEW SECTION 1: SAVINGS RATE CARD ---
        val savingsRate = remember(totals) {
            val inc = totals.first
            val exp = totals.second
            if (inc > 0) ((inc - exp) / inc * 100).coerceAtLeast(0.0).coerceAtMost(100.0) else 0.0
        }
        val rateColor = when {
            savingsRate >= 20 -> IncomeGreen
            savingsRate >= 10 -> Color(0xFFFFB300)
            else -> CoralRed
        }
        
        Text(
            text = "Savings Rate This Month",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${savingsRate.toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                LinearProgressIndicator(
                    progress = (savingsRate / 100).toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = rateColor,
                    trackColor = Color(0x33FFFFFF)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Saved: ৳${String.format("%,.0f", totals.first - totals.second)}", fontSize = 11.sp, color = TextSecondaryDark)
                    Text("Income: ৳${String.format("%,.0f", totals.first)}", fontSize = 11.sp, color = TextSecondaryDark)
                }
                
                val remarkStr = when {
                    savingsRate >= 30 -> "🌟 Excellent saving habit!"
                    savingsRate >= 20 -> "✅ Good saving rate"
                    savingsRate >= 10 -> "⚠️ Try to save more"
                    else -> "🔴 Critical — spending too much"
                }
                Text(remarkStr, fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // --- NEW SECTION 2: 6-MONTH OVERVIEW ---
        Text(
            text = "6-Month Overview",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        val sixMonthData = remember(transactions) {
            val res = mutableListOf<Triple<String, Double, Double>>() // Month(String), Income, Expense
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            for (i in 5 downTo 0) {
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.MONTH, -i)
                val m = cal.get(java.util.Calendar.MONTH)
                val y = cal.get(java.util.Calendar.YEAR)
                
                val matchedTxs = transactions.filter {
                    val txCal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                    txCal.get(java.util.Calendar.MONTH) == m && txCal.get(java.util.Calendar.YEAR) == y
                }
                val inc = matchedTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                val exp = matchedTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                res.add(Triple(monthNames[m], inc, exp))
            }
            res
        }
        
        fun formatBriefAmt(amt: Double): String {
            return when {
                amt >= 100000 -> String.format("%.1fL", amt / 100000)
                amt >= 1000 -> String.format("%.0fk", amt / 1000)
                else -> String.format("৳%.0f", amt)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val textMeasurer = rememberTextMeasurer()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxVal = sixMonthData.flatMap { listOf(it.second, it.third) }.maxOrNull()?.coerceAtLeast(1000.0) ?: 1000.0
                        val barMaxHeight = size.height - 40.dp.toPx()
                        
                        // Grid lines
                        val gridLines = listOf(0.25f, 0.5f, 0.75f)
                        gridLines.forEach { fac ->
                            val y = barMaxHeight - (barMaxHeight * fac) + 20.dp.toPx()
                            drawLine(Color(0x1F8892A4), Offset(0f, y), Offset(size.width, y))
                        }
                        
                        val colWidth = size.width / 6f
                        val barW = 12.dp.toPx()
                        val barGap = 4.dp.toPx()
                        
                        sixMonthData.forEachIndexed { i, data ->
                            val centerX = (i * colWidth) + (colWidth / 2f)
                            val incH = (data.second / maxVal).toFloat() * barMaxHeight
                            val expH = (data.third / maxVal).toFloat() * barMaxHeight
                            
                            val incX = centerX - barW - (barGap / 2f)
                            val expX = centerX + (barGap / 2f)
                            
                            val baseY = size.height - 20.dp.toPx()
                            
                            drawRoundRect(
                                color = IncomeGreen,
                                topLeft = Offset(incX, baseY - incH),
                                size = Size(barW, incH.coerceAtLeast(4f)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                            drawRoundRect(
                                color = CoralRed,
                                topLeft = Offset(expX, baseY - expH),
                                size = Size(barW, expH.coerceAtLeast(4f)),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                            )
                            
                            // Amount labels
                            if (data.second > 0) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = formatBriefAmt(data.second),
                                    topLeft = Offset(incX - 6.dp.toPx(), baseY - incH - 12.dp.toPx()),
                                    style = TextStyle(color = IncomeGreen, fontSize = 8.sp)
                                )
                            }
                            if (data.third > 0) {
                                drawText(
                                    textMeasurer = textMeasurer,
                                    text = formatBriefAmt(data.third),
                                    topLeft = Offset(expX - 2.dp.toPx(), baseY - expH - 12.dp.toPx()),
                                    style = TextStyle(color = CoralRed, fontSize = 8.sp)
                                )
                            }
                            
                            // X-axis label
                            drawText(
                                textMeasurer = textMeasurer,
                                text = data.first,
                                topLeft = Offset(centerX - 10.dp.toPx(), size.height - 12.dp.toPx()),
                                style = TextStyle(color = TextSecondaryDark, fontSize = 9.sp)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                        Text("Income", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CoralRed))
                        Text("Expense", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
            }
        }

        // --- NEW SECTION 3: INFLOW VS OUTFLOW (IMPROVED) ---
        Text(
            text = "Inflow vs Outflow This Month",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    val textMeasurer = rememberTextMeasurer()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxAmt = Math.max(100.0, Math.max(totals.first, totals.second))
                        val incRatio = (totals.first / maxAmt).toFloat()
                        val expRatio = (totals.second / maxAmt).toFloat()
                        
                        val borderSpace = 50f
                        val centerBaseY = size.height - 20f
                        val chartH = size.height - 50f
                        
                        drawLine(Color(0x1F8892A4), Offset(borderSpace, centerBaseY), Offset(size.width - borderSpace, centerBaseY))
                        drawLine(Color(0x0A8892A4), Offset(borderSpace, centerBaseY * 0.5f), Offset(size.width - borderSpace, centerBaseY * 0.5f))
                        
                        val barW = 32.dp.toPx()
                        val barSpacing = 40.dp.toPx()
                        val bar1X = size.width / 2f - barW - barSpacing / 2f
                        val bar2X = size.width / 2f + barSpacing / 2f

                        val incH = (incRatio * chartH).coerceAtLeast(10f)
                        val expH = (expRatio * chartH).coerceAtLeast(10f)
                        
                        drawRoundRect(
                            color = IncomeGreen,
                            topLeft = Offset(bar1X, centerBaseY - incH),
                            size = Size(barW, incH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        drawRoundRect(
                            color = CoralRed,
                            topLeft = Offset(bar2X, centerBaseY - expH),
                            size = Size(barW, expH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        
                        drawText(
                            textMeasurer = textMeasurer,
                            text = formatBriefAmt(totals.first),
                            topLeft = Offset(bar1X - 4.dp.toPx(), centerBaseY - incH - 16.dp.toPx()),
                            style = TextStyle(color = IncomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                        
                        drawText(
                            textMeasurer = textMeasurer,
                            text = formatBriefAmt(totals.second),
                            topLeft = Offset(bar2X - 4.dp.toPx(), centerBaseY - expH - 16.dp.toPx()),
                            style = TextStyle(color = CoralRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                
                val net = totals.first - totals.second
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Income: ৳${String.format("%,.0f", totals.first)}", fontSize = 11.sp, color = IncomeGreen)
                    Text("Expense: ৳${String.format("%,.0f", totals.second)}", fontSize = 11.sp, color = CoralRed)
                    Text("Net: ৳${String.format("%,.0f", net)}", fontSize = 11.sp, color = if (net >= 0) IncomeGreen else CoralRed)
                }
            }
        }

        // --- NEW SECTION 4: WEEK-WISE TREND ---
        Text(
            text = "This Week vs Last Week",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        val weekData = remember(transactions) {
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val thisWeekStart = now - (7 * dayMs)
            val lastWeekStart = now - (14 * dayMs)
            
            val thisWeekExp = transactions.filter { it.type == "EXPENSE" && it.date in thisWeekStart..now }.sumOf { it.amount }
            val lastWeekExp = transactions.filter { it.type == "EXPENSE" && it.date in lastWeekStart..thisWeekStart }.sumOf { it.amount }
            
            // Calc daily for this week (oldest to newest)
            val dailyExp = mutableListOf<Double>()
            for (i in 6 downTo 0) {
                val dStart = now - ((i + 1) * dayMs)
                val dEnd = now - (i * dayMs)
                dailyExp.add(transactions.filter { it.type == "EXPENSE" && it.date in dStart..dEnd }.sumOf { it.amount })
            }
            
            Triple(thisWeekExp, lastWeekExp, dailyExp)
        }
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val maxW = Math.max(100.0, Math.max(weekData.first, weekData.second))
                val thisWFrac = (weekData.first / maxW).toFloat()
                val lastWFrac = (weekData.second / maxW).toFloat()
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("This Week", fontSize = 12.sp, color = Color.White)
                        Text(String.format("৳%,.0f", weekData.first), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = thisWFrac.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = ElectricPurple, trackColor = Color.Transparent
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Last Week", fontSize = 12.sp, color = TextSecondaryDark)
                        Text(String.format("৳%,.0f", weekData.second), fontSize = 12.sp, color = TextSecondaryDark)
                    }
                    LinearProgressIndicator(
                        progress = lastWFrac.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0x99FFFFFF), trackColor = Color.Transparent
                    )
                }
                
                val diff = weekData.first - weekData.second
                val compText = when {
                    diff < 0 -> "📉 ৳${String.format("%,.0f", -diff)} less than last week"
                    diff > 0 -> "📈 ৳${String.format("%,.0f", diff)} more than last week"
                    else -> "Same spending as last week"
                }
                val compColor = when {
                    diff < 0 -> IncomeGreen
                    diff > 0 -> CoralRed
                    else -> TextSecondaryDark
                }
                Text(compText, fontSize = 12.sp, color = compColor, fontWeight = FontWeight.Medium)
                
                // Mini 7-day chart
                Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                    val textMeasurer = rememberTextMeasurer()
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val days = listOf("M", "T", "W", "T", "F", "S", "S") // just visual representation
                        val dExp = weekData.third
                        val dMax = dExp.maxOrNull()?.coerceAtLeast(10.0) ?: 10.0
                        
                        val colW = size.width / 7f
                        val bW = 16.dp.toPx()
                        
                        dExp.forEachIndexed { i, amt ->
                            val cX = (i * colW) + (colW / 2f)
                            val bH = ((amt / dMax) * (size.height - 20.dp.toPx())).toFloat().coerceAtLeast(4f)
                            val color = if (amt == dMax && amt > 0) CoralRed else ElectricPurple
                            
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(cX - bW/2f, size.height - 20.dp.toPx() - bH),
                                size = Size(bW, bH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                            )
                            
                            drawText(
                                textMeasurer = textMeasurer,
                                text = days[i],
                                topLeft = Offset(cX - 4.dp.toPx(), size.height - 14.dp.toPx()),
                                style = TextStyle(color = TextSecondaryDark, fontSize = 9.sp)
                            )
                        }
                    }
                }
            }
        }

        // --- NEW SECTION 5: TOP 5 CATEGORY ---
        Text(
            text = "Top Spending Categories",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        val topCategories = remember(transactions, categories) {
            val totalExp = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            if (totalExp <= 0) emptyList() else {
                categories.filter { it.type == "EXPENSE" }.map { cat ->
                    val amt = transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
                    Triple(cat, amt, (amt / totalExp * 100))
                }.filter { it.second > 0 }.sortedByDescending { it.second }.take(5)
            }
        }

        if (topCategories.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No categorized expenses yet.", fontSize = 12.sp, color = TextSecondaryDark)
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    topCategories.forEachIndexed { idx, (cat, amt, pct) ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.color))
                        } catch (e: Exception) { ElectricPurple }

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                                Text("${idx+1}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${cat.icon} ${cat.name}", fontSize = 13.sp, color = Color.White)
                                    Text("৳${String.format("%,.0f", amt)}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = (pct / 100).toFloat(),
                                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = catColor, trackColor = Color(0x22FFFFFF)
                                    )
                                    Text("${pct.toInt()}%", fontSize = 10.sp, color = TextSecondaryDark)
                                }
                            }
                        }
                    }
                    Text("Based on all-time transactions", fontSize = 10.sp, color = TextSecondaryDark, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        // --- SECTION 4: AI FORECAST / LINEAR REGRESSION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF0D9488))))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = Color.White)
                    Text("AI Predictive Analytics Output", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }

                Text(
                    text = String.format("Current Month Forecast spending: ৳%,.0f", prediction.first),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = String.format("Confidence Rate: %,.1f%% (Determined via Least Squares Regression calculation)", prediction.second),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                val isOnTrack = prediction.first <= (totals.first * 0.8).coerceAtLeast(20000.0)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOnTrack) Color(0x3300FF88) else Color(0x33FF3333))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isOnTrack) "STATUS: ON TARGET" else "STATUS: APPROACHING EXCESSES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}
