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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val budgets by viewModel.monthlyBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val selectedMonth by viewModel.currentMonth.collectAsState()
    val selectedYear by viewModel.currentYear.collectAsState()

    val expenseCategories = categories.filter { it.type == "EXPENSE" }

    var selectedCategoryIdForEdit by remember { mutableStateOf<Long?>(null) }
    var editAmountStr by remember { mutableStateOf("") }
    
    var showBottomSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Launch notification check on changes
    LaunchedEffect(transactions, budgets, selectedMonth, selectedYear) {
        budgets.forEach { budget ->
            val cat = categories.firstOrNull { it.id == budget.categoryId }
            val spent = transactions.filter {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                it.categoryId == budget.categoryId &&
                it.type == "EXPENSE" &&
                cal.get(java.util.Calendar.MONTH) + 1 == selectedMonth &&
                cal.get(java.util.Calendar.YEAR) == selectedYear
            }.sumOf { it.amount }
            if (spent > budget.amount && budget.amount > 0) {
                com.example.notifications.NotificationHelper.sendBudgetWarningNotification(
                    context = context,
                    categoryId = budget.categoryId,
                    categoryName = cat?.name ?: "Category",
                    percentUsed = (spent / budget.amount * 100).toInt()
                )
            }
        }
    }

    // Autosuggest calculation
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
        // --- 1. MONTH NAVIGATOR HEADER ---
        val monthNames = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val currentSysCal = java.util.Calendar.getInstance()
        val currentSysMonth = currentSysCal.get(java.util.Calendar.MONTH) + 1
        val currentSysYear = currentSysCal.get(java.util.Calendar.YEAR)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (selectedMonth == 1) {
                        viewModel.setCurrentMonth(12)
                        viewModel.setCurrentYear(selectedYear - 1)
                    } else {
                        viewModel.setCurrentMonth(selectedMonth - 1)
                    }
                }
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous Month", tint = Color.White)
            }
            
            Text(
                text = "${monthNames[selectedMonth]} $selectedYear",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            val isCurrentMonthAndYear = selectedMonth == currentSysMonth && selectedYear == currentSysYear
            IconButton(
                onClick = {
                    if (!isCurrentMonthAndYear) {
                        if (selectedMonth == 12) {
                            viewModel.setCurrentMonth(1)
                            viewModel.setCurrentYear(selectedYear + 1)
                        } else {
                            viewModel.setCurrentMonth(selectedMonth + 1)
                        }
                    }
                },
                enabled = !isCurrentMonthAndYear
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward, 
                    contentDescription = "Next Month", 
                    tint = if (isCurrentMonthAndYear) TextSecondaryDark else Color.White
                )
            }
        }

        // --- 2. CIRCULAR TOTAL HERO PROGRESS ---
        val totalLimit = budgets.sumOf { it.amount }
        val totalSpent = expenseCategories.sumOf { cat ->
            transactions.filter { 
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                it.categoryId == cat.id && it.type == "EXPENSE" &&
                cal.get(java.util.Calendar.MONTH) + 1 == selectedMonth &&
                cal.get(java.util.Calendar.YEAR) == selectedYear
            }.sumOf { it.amount }
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
                    if (totalLimit == 0.0) {
                        Text(
                            "No budgets set for this month",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    } else {
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
                        val diff = totalLimit - totalSpent
                        Text(
                            "৳${String.format("%,.0f", diff)} left",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (diff >= 0) IncomeGreen else CoralRed
                        )
                    }
                }

                if (totalLimit > 0) {
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
        }

        // --- 3. SMART AUTO SUGGESTIONS DIALOG BANNER ---
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

        // --- 4. BUDGETED CATEGORIES ---
        val budgetedCats = expenseCategories.filter { cat -> budgets.any { it.categoryId == cat.id } }
        val unbudgetedCats = expenseCategories.filter { cat -> budgets.none { it.categoryId == cat.id } }

        if (budgetedCats.isNotEmpty()) {
            Text(
                text = "Budgeted",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                budgetedCats.forEach { cat ->
                    val b = budgets.first { it.categoryId == cat.id }
                    val currentLimit = b.amount
                    val spentOnCategory = transactions.filter { 
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                        it.categoryId == cat.id && it.type == "EXPENSE" &&
                        cal.get(java.util.Calendar.MONTH) + 1 == selectedMonth &&
                        cal.get(java.util.Calendar.YEAR) == selectedYear
                    }.sumOf { it.amount }
                    
                    val cPct = if (currentLimit > 0) (spentOnCategory / currentLimit).toFloat() else 0f
                    val statusColor = when {
                        cPct >= 0.9f -> CoralRed
                        cPct >= 0.7f -> Color(0xFFFFB300)
                        else -> IncomeGreen
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategoryIdForEdit = cat.id
                                editAmountStr = (b.amount.toString())
                                showBottomSheet = true
                            },
                        backgroundColor = if (cPct >= 1.0f) Color(0x22FF6B6B) else Color(0x11111827),
                        borderColor = if (cPct >= 1.0f) CoralRed else Color(0x10FFFFFF)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            text = String.format("Suggested: ৳%,.0f", autosuggestions[cat.id] ?: 3000.0),
                                            fontSize = 10.sp,
                                            color = TextSecondaryDark
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Spent: ৳${String.format("%,.0f", spentOnCategory)}", fontSize = 11.sp, color = statusColor)
                                Text("Budget: ৳${String.format("%,.0f", currentLimit)}", fontSize = 11.sp, color = Color.White)
                                val diff = currentLimit - spentOnCategory
                                Text(
                                    "Left: ৳${String.format("%,.0f", diff)}", 
                                    fontSize = 11.sp, 
                                    color = if (diff >= 0) IncomeGreen else CoralRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            LinearProgressIndicator(
                                progress = cPct.coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = statusColor,
                                trackColor = Color(0x1AFFFFFF)
                            )
                            
                            if (spentOnCategory > currentLimit) {
                                Text(
                                    "⚠️ Overspent by ৳${String.format("%,.0f", spentOnCategory - currentLimit)}",
                                    fontSize = 11.sp,
                                    color = CoralRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. UNBUDGETED CATEGORIES ---
        if (unbudgetedCats.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Unbudgeted",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text("Tap to set a budget limit", fontSize = 11.sp, color = TextSecondaryDark)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                unbudgetedCats.forEach { cat ->
                    val spentOnCategory = transactions.filter { 
                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
                        it.categoryId == cat.id && it.type == "EXPENSE" &&
                        cal.get(java.util.Calendar.MONTH) + 1 == selectedMonth &&
                        cal.get(java.util.Calendar.YEAR) == selectedYear
                    }.sumOf { it.amount }
                    
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategoryIdForEdit = cat.id
                                editAmountStr = ""
                                showBottomSheet = true
                            },
                        backgroundColor = Color(0x0A111827),
                        borderColor = Color(0x08FFFFFF)
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
                                        text = if (spentOnCategory > 0) "Spent this month: ৳${String.format("%,.0f", spentOnCategory)}" else "No spending yet",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    selectedCategoryIdForEdit = cat.id
                                    editAmountStr = ""
                                    showBottomSheet = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricPurple),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Set Budget", fontSize = 11.sp, color = ElectricPurple)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }

    // --- 6. BUDGET EDIT MODAL BOTTOM SHEET ---
    if (showBottomSheet && selectedCategoryIdForEdit != null) {
        val cat = expenseCategories.firstOrNull { it.id == selectedCategoryIdForEdit }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val monthNames = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val b = budgets.firstOrNull { it.categoryId == cat?.id }

        val spentOnCategory = transactions.filter {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = it.date }
            it.categoryId == cat?.id && it.type == "EXPENSE" &&
            cal.get(java.util.Calendar.MONTH) + 1 == selectedMonth &&
            cal.get(java.util.Calendar.YEAR) == selectedYear
        }.sumOf { it.amount }

        val suggested = autosuggestions[cat?.id] ?: 3000.0

        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false 
                selectedCategoryIdForEdit = null
            },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Set Budget — ${cat?.icon ?: ""} ${cat?.name ?: ""}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("For: ${monthNames[selectedMonth]} $selectedYear", color = TextSecondaryDark, fontSize = 14.sp)
                    Text("Already spent: ৳${String.format("%,.0f", spentOnCategory)}", color = TextSecondaryDark, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = editAmountStr,
                    onValueChange = { editAmountStr = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ElectricPurple
                    ),
                    placeholder = { Text("Enter limit amount in ৳") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Presets
                val presets = listOf("5000", "10000", "15000", "20000", "25000")
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets.size) { i ->
                        val preset = presets[i]
                        Surface(
                            color = Color(0x1AFFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { editAmountStr = preset }
                        ) {
                            Text("৳${String.format("%,.0f", preset.toDouble())}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(12.dp, 8.dp))
                        }
                    }
                }

                // AI Supported
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x0F6C63FF)).padding(12.dp)) {
                    Text("AI Suggested: ৳${String.format("%,.0f", suggested)}", color = ElectricPurple, fontSize = 13.sp)
                    TextButton(onClick = { editAmountStr = suggested.toLong().toString() }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                        Text("Use", color = ElectricPurple, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val amount = editAmountStr.toDoubleOrNull() ?: 0.0
                        cat?.let {
                            viewModel.saveBudget(it.id, amount)
                        }
                        showBottomSheet = false
                        selectedCategoryIdForEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Budget", fontWeight = FontWeight.Bold)
                }

                if (b != null) {
                    TextButton(
                        onClick = {
                            cat?.let {
                                viewModel.saveBudget(it.id, 0.0)
                            }
                            showBottomSheet = false
                            selectedCategoryIdForEdit = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove Budget", color = CoralRed)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
