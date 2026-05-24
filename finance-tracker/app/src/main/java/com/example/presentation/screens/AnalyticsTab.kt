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

        // --- SECTION 2: DONUT EXPENSE SPLIT BREAKDOWN ---
        Text(
            text = "Spend Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        val expensesByCategory = remember(transactions, categories) {
            categories.filter { it.type == "EXPENSE" }.map { cat ->
                val amt = transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
                cat to amt
            }.filter { it.second > 0 }.sortedByDescending { it.second }
        }

        if (expensesByCategory.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Log expenses to render donut split chart.", fontSize = 12.sp, color = TextSecondaryDark)
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Draw Donut
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val totalSum = expensesByCategory.sumOf { it.second }
                            
                            expensesByCategory.forEach { (cat, amt) ->
                                val sweep = (amt / totalSum * 360f).toFloat()
                                val colorVal = try {
                                    Color(android.graphics.Color.parseColor(cat.color))
                                } catch (e: Exception) {
                                    ElectricPurple
                                }
                                drawArc(
                                    color = colorVal,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx())
                                )
                                startAngle += sweep
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Outflow", fontSize = 10.sp, color = TextSecondaryDark)
                            Text(
                                String.format("৳%,.0f", expensesByCategory.sumOf { it.second }),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Legend Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        expensesByCategory.forEach { (cat, amt) ->
                            val colorVal = try {
                                Color(android.graphics.Color.parseColor(cat.color))
                            } catch (e: Exception) {
                                ElectricPurple
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(colorVal)
                                    )
                                    Text(
                                        text = "${cat.icon} ${cat.name}",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = String.format("৳%,.0f", amt),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: REVENUE COMPARISON BAR CHART ---
        Text(
            text = "Inflow vs Outflow",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("This Month summary", fontSize = 11.sp, color = TextSecondaryDark)
                    Icon(imageVector = Icons.Default.QueryStats, contentDescription = "Graph", tint = ElectricPurple)
                }

                // Render Bar Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val maxAmt = Math.max(100.0, Math.max(totals.first, totals.second))
                        val incomeHeightRatio = (totals.first / maxAmt).toFloat()
                        val expenseHeightRatio = (totals.second / maxAmt).toFloat()

                        val borderSpace = 50f
                        val centerBaseY = size.height - 30f
                        
                        // Draw grid lines
                        drawLine(Color(0x1F8892A4), Offset(borderSpace, centerBaseY), Offset(size.width - borderSpace, centerBaseY))
                        drawLine(Color(0x0A8892A4), Offset(borderSpace, centerBaseY * 0.5f), Offset(size.width - borderSpace, centerBaseY * 0.5f))

                        // Inflow Bar
                        val barW = 32.dp.toPx()
                        val barSpacing = 40.dp.toPx()
                        val bar1X = size.width / 2f - barW - barSpacing / 2f
                        val bar2X = size.width / 2f + barSpacing / 2f

                        // Draw Inflow Green Bar
                        drawRoundRect(
                            color = IncomeGreen,
                            topLeft = Offset(bar1X, centerBaseY - (incomeHeightRatio * (size.height - 60f))),
                            size = Size(barW, Math.max(10f, incomeHeightRatio * (size.height - 60f)))
                        )

                        // Draw Outflow Red Bar
                        drawRoundRect(
                            color = CoralRed,
                            topLeft = Offset(bar2X, centerBaseY - (expenseHeightRatio * (size.height - 60f))),
                            size = Size(barW, Math.max(10f, expenseHeightRatio * (size.height - 60f)))
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IncomeGreen))
                        Text("Inflow", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CoralRed))
                        Text("Outflow", fontSize = 11.sp, color = TextSecondaryDark)
                    }
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
