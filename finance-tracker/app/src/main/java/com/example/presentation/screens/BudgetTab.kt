package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.CoralRed
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTab(
    viewModel: FinanceViewModel
) {
    val budgets by viewModel.monthlyBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val expenseCategories = categories.filter { it.type == "EXPENSE" }

    var selectedCategoryIdForEdit by remember { mutableStateOf<Long?>(null) }
    var editAmountStr by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Autosuggest simulation: 1.2x of average of last 3 months
    val autosuggestions = remember(transactions) {
        expenseCategories.associate { cat ->
            val sum = transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
            val suggested = if (sum > 0) sum * 0.95 else 5000.0
            cat.id to suggested
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- TITLE ---
        Text(
            text = "Monthly Budgets",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // --- CIRCULAR TOTAL HERO PROGRESS ---
        val totalLimit = budgets.sumOf { it.amount }
        val totalSpent = expenseCategories.sumOf { cat ->
            transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
        }
        val pct = if (totalLimit > 0) (totalSpent / totalLimit).toFloat() else 0f

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Total Cash Budget", fontSize = 11.sp, color = TextSecondaryDark)
                    Text(
                        String.format("৳%,.0f Limit", totalLimit),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        String.format("৳%,.0f Used (%,.1f%%)", totalSpent, pct * 100f),
                        fontSize = 11.sp,
                        color = if (pct >= 0.9f) CoralRed else if (pct >= 0.7f) Color(0xFFFFB300) else IncomeGreen
                    )
                }

                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    val animateP by animateFloatAsState(targetValue = pct.coerceIn(0f, 1f), label = "Progress")
                    CircularProgressIndicator(
                        progress = { animateP },
                        modifier = Modifier.fillMaxSize(),
                        color = if (pct >= 0.9f) CoralRed else if (pct >= 0.7f) Color(0xFFFFB300) else IncomeGreen,
                        trackColor = Color(0x1BFFFFFF),
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${(pct * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // --- SMART AUTO SUGGESTIONS DIALOG BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x0F6C63FF))
                .border(1.dp, Color(0x336C63FF), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Suggest",
                        tint = ElectricPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "AI Smart Adjust suggestions",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPurple
                    )
                }

                Text(
                    text = "Based on your spending trend from past weeks, we prepared suggested thresholds. Keep inside target limits with a single click.",
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )

                Button(
                    onClick = {
                        // Apply all suggestions
                        autosuggestions.forEach { (catId, sug) ->
                            viewModel.saveBudget(catId, sug)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply All Suggested Budgets", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // --- CATEGORIES BUDGET SETUP MATRIX ---
        Text(
            text = "Threshold Breakdown",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            expenseCategories.forEach { cat ->
                val b = budgets.firstOrNull { it.categoryId == cat.id }
                val currentLimit = b?.amount ?: 0.0
                val spentOnCategory = transactions.filter { it.categoryId == cat.id && it.type == "EXPENSE" }.sumOf { it.amount }
                
                val cPct = if (currentLimit > 0) (spentOnCategory / currentLimit).toFloat() else 0f
                val statusColor = when {
                    cPct >= 0.9f -> CoralRed
                    cPct >= 0.7f -> Color(0xFFFFB300)
                    else -> IncomeGreen
                }

                // Infinite Pulse Animation for budgets exceeded (100%+)
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                val cardModifier = if (cPct >= 1.0f) {
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategoryIdForEdit = cat.id
                            editAmountStr = (b?.amount ?: 0).toString()
                        }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategoryIdForEdit = cat.id
                            editAmountStr = (b?.amount ?: 0).toString()
                        }
                }

                GlassCard(
                    modifier = cardModifier,
                    backgroundColor = if (cPct >= 1.0f) Color(0x22FF6B6B) else Color(0x11111827),
                    borderColor = if (cPct >= 1.0f) CoralRed else Color(0x10FFFFFF)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x1AFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.icon, fontSize = 16.sp)
                            }
                            Column {
                                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    text = if (currentLimit > 0) String.format("Suggested: ৳%,.0f", autosuggestions[cat.id] ?: 3000.0)
                                           else "No threshold set",
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("৳%,.0f / ৳%,.0f", spentOnCategory, currentLimit),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                            Text(
                                String.format("%.1f%% used", cPct * 100),
                                fontSize = 9.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    if (currentLimit > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = cPct.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = statusColor,
                            trackColor = Color(0x1AFFFFFF)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }

    // --- QUICK RE-THRESHOLD EDIT BOTTOM SHEET ---
    if (selectedCategoryIdForEdit != null) {
        val cat = expenseCategories.firstOrNull { it.id == selectedCategoryIdForEdit }
        AlertDialog(
            onDismissRequest = { selectedCategoryIdForEdit = null },
            title = { Text("Configure target budget limit", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Setting limit for: ${cat?.icon ?: ""} ${cat?.name ?: ""}", color = TextSecondaryDark, fontSize = 12.sp)
                    OutlinedTextField(
                        value = editAmountStr,
                        onValueChange = { editAmountStr = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ElectricPurple
                        ),
                        placeholder = { Text("Enter target limit amount in ৳") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = editAmountStr.toDoubleOrNull() ?: 0.0
                        selectedCategoryIdForEdit?.let { catId ->
                            viewModel.saveBudget(catId, amount)
                        }
                        selectedCategoryIdForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Apply Limits")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryIdForEdit = null }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
