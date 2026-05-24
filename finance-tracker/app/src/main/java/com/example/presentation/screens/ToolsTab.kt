package com.example.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.LoanEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsTab(
    viewModel: FinanceViewModel
) {
    val context = LocalContext.current

    var selectedSubTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Savings", "Debts", "Calendar", "Configure")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER ---
        Text(
            text = "Smart Vault",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // --- SUB TABS ---
        ScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty() && selectedSubTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                        color = ElectricPurple
                    )
                }
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSubTab == index) ElectricPurple else TextSecondaryDark
                        )
                    }
                )
            }
        }

        // --- SUB CONTENT RENDERS ---
        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedSubTab) {
                0 -> SavingsSubSection(viewModel)
                1 -> DebtsSubSection(viewModel)
                2 -> CalendarBillsSubSection(viewModel)
                3 -> ConfigureSettingsSubSection(viewModel)
            }
        }
    }
}

// ==========================================
// SUB SECTION 1: SAVINGS GOALS
// ==========================================
@Composable
fun SavingsSubSection(viewModel: FinanceViewModel) {
    val goals by viewModel.savingsGoals.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active Savings Targets", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddGoalDialog = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Goal", tint = ElectricPurple)
            }
        }

        if (goals.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No goals logged yet. Define a savings target to begin!", fontSize = 11.sp, color = TextSecondaryDark)
            }
        } else {
            goals.forEach { goal ->
                val progressRatio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val daysRemaining = ((goal.deadline - System.currentTimeMillis()) / (86400000L).coerceAtLeast(1L)).toInt()
                
                var showAddMoneyDialog by remember { mutableStateOf(false) }
                var depositInputAmt by remember { mutableStateOf("") }
                var showDetailsDialog by remember { mutableStateOf(false) }

                SwipeableGoalRow(
                    onDelete = { viewModel.deleteSavingsGoal(goal) },
                    onEdit = { goalToEdit = goal }
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth().clickable { showDetailsDialog = true }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(goal.emoji, fontSize = 28.sp)
                                Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showAddMoneyDialog = true }) {
                                    Icon(Icons.Default.Savings, contentDescription = "Deposit Cash", tint = MintGreen)
                                }
                            }
                        }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            String.format("৳%,.0f saved", goal.currentAmount),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            String.format("Target: ৳%,.0f", goal.targetAmount),
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = progressRatio.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MintGreen,
                        trackColor = Color(0x1BFFFFFF)
                    )

                    if (showAddMoneyDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddMoneyDialog = false },
                            title = { Text("Deposit money", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("How much money would you like to save into ${goal.name}?", color = TextSecondaryDark, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = depositInputAmt,
                                        onValueChange = { depositInputAmt = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = ElectricPurple
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val amt = depositInputAmt.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            viewModel.updateSavingsGoal(goal.copy(currentAmount = goal.currentAmount + amt))
                                        }
                                        showAddMoneyDialog = false
                                        depositInputAmt = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                                ) {
                                    Text("Save BDT")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showAddMoneyDialog = false }) { Text("Cancel", color = TextSecondaryDark) }
                            },
                            containerColor = Color(0xFF1E293B)
                        )
                    }

                    if (showDetailsDialog) {
                        AlertDialog(
                            onDismissRequest = { showDetailsDialog = false },
                            title = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(goal.emoji, fontSize = 24.sp)
                                    Text(goal.name, color = Color.White)
                                }
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (goal.description.isNotEmpty()) {
                                        Text("Description:", color = TextSecondaryDark, fontSize = 12.sp)
                                        Text(goal.description, color = Color.White, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    
                                    val statusText = if (goal.currentAmount >= goal.targetAmount) "Goal Completed! 🎉" else if (daysRemaining > 0) "$daysRemaining days left" else "Deadline reached"
                                    val statusColor = if (goal.currentAmount >= goal.targetAmount) MintGreen else if (daysRemaining > 0) Color.White else CoralRed
                                    
                                    Text("Status:", color = TextSecondaryDark, fontSize = 12.sp)
                                    Text(statusText, color = statusColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text("Progress:", color = TextSecondaryDark, fontSize = 12.sp)
                                    Text(
                                        String.format("৳%,.0f / ৳%,.0f", goal.currentAmount, goal.targetAmount),
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { showDetailsDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                                ) {
                                    Text("Close")
                                }
                            },
                            containerColor = Color(0xFF1E293B)
                        )
                    }
                }
                }
            }
        }
    }

    if (showAddGoalDialog || goalToEdit != null) {
        var name by remember { mutableStateOf(goalToEdit?.name ?: "") }
        var emoji by remember { mutableStateOf(goalToEdit?.emoji ?: "💻") }
        var targetAmt by remember { mutableStateOf(goalToEdit?.targetAmount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
        var initialAmt by remember { mutableStateOf(goalToEdit?.currentAmount?.let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
        var description by remember { mutableStateOf(goalToEdit?.description ?: "") }
        val remainingDaysOpt = goalToEdit?.let { ((it.deadline - System.currentTimeMillis()) / 86400000L).toInt().coerceAtLeast(1) }
        var deadlineDays by remember { mutableStateOf(remainingDaysOpt?.toString() ?: "180") }

        AlertDialog(
            onDismissRequest = { 
                showAddGoalDialog = false
                goalToEdit = null
            },
            title = { Text(if (goalToEdit != null) "Edit savings goal" else "Add savings goal", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji icon", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = targetAmt,
                        onValueChange = { targetAmt = it },
                        label = { Text("Target Amount in ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = initialAmt,
                        onValueChange = { initialAmt = it },
                        label = { Text("Already Saved in ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = deadlineDays,
                        onValueChange = { deadlineDays = it },
                        label = { Text("Target Days To Reach", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (Optional)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tar = targetAmt.toDoubleOrNull() ?: 0.0
                        val ini = initialAmt.toDoubleOrNull() ?: 0.0
                        val days = deadlineDays.toLongOrNull() ?: 180L
                        val newDeadline = System.currentTimeMillis() + (86400000L * days)
                        if (name.isNotEmpty() && tar > 0) {
                            if (goalToEdit != null) {
                                viewModel.updateSavingsGoal(
                                    goalToEdit!!.copy(
                                        name = name,
                                        emoji = emoji,
                                        targetAmount = tar,
                                        currentAmount = ini,
                                        description = description,
                                        deadline = newDeadline
                                    )
                                )
                            } else {
                                viewModel.addSavingsGoal(name, emoji, tar, ini, newDeadline, description)
                            }
                        }
                        showAddGoalDialog = false
                        goalToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text(if (goalToEdit != null) "Update Goal" else "Add Goal")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableGoalRow(
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                return@rememberSwipeToDismissBoxState true
            } else if (it == SwipeToDismissBoxValue.StartToEnd) {
                onEdit()
                return@rememberSwipeToDismissBoxState false
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CoralRed),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 24.dp))
                }
            } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricPurple),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.padding(start = 24.dp))
                }
            }
        },
        content = {
            content()
        }
    )
}

// ==========================================
// SUB SECTION 2: LOANS AND DEBTS (তুমি দিয়েছো / তুমি নিয়েছো)
// ==========================================
@Composable
fun DebtsSubSection(viewModel: FinanceViewModel) {
    val loans by viewModel.loans.collectAsState()
    var showAddLoanDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lent & Borrowed Ledger", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddLoanDialog = true }) {
                Icon(Icons.Default.AddCard, contentDescription = "Add debt", tint = ElectricPurple)
            }
        }

        // Section Totals
        val totalLent = loans.filter { it.type == "LENT" && it.status != "SETTLED" }.sumOf { it.amount - it.paidAmount }
        val totalBorrowed = loans.filter { it.type == "BORROWED" && it.status != "SETTLED" }.sumOf { it.amount - it.paidAmount }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassCard(modifier = Modifier.weight(1f), backgroundColor = Color(0x1F00E676)) {
                Text("Lent (তুমি পাবে)", fontSize = 10.sp, color = TextSecondaryDark)
                Text(String.format("৳%,.0f", totalLent), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
            }
            GlassCard(modifier = Modifier.weight(1f), backgroundColor = Color(0x1FFF6B6B)) {
                Text("Borrowed (তুমি দেবে)", fontSize = 10.sp, color = TextSecondaryDark)
                Text(String.format("৳%,.0f", totalBorrowed), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoralRed)
            }
        }

        if (loans.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Your debt diary is currently clean.", fontSize = 11.sp, color = TextSecondaryDark)
            }
        } else {
            loans.forEach { loan ->
                var showRefundDialog by remember { mutableStateOf(false) }
                var payAmtStr by remember { mutableStateOf("") }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = if (loan.status == "SETTLED") Color(0x0F00E676) else Color(0x11111827)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (loan.type == "LENT") Color(0x3300E676) else Color(0x33FF6B6B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (loan.type == "LENT") "🔼" else "🔽", fontSize = 12.sp)
                            }

                            Column {
                                Text(loan.personName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = if (loan.status == "SETTLED") "Settled ✅" else loan.notes,
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark,
                                    maxLines = 1
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("৳%,.0f", loan.amount),
                                color = if (loan.type == "LENT") IncomeGreen else CoralRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (loan.status != "SETTLED") {
                                Text(
                                    text = String.format("Due: ৳%,.0f", loan.amount - loan.paidAmount),
                                    fontSize = 10.sp,
                                    color = TextSecondaryDark,
                                    modifier = Modifier.clickable { showRefundDialog = true }
                                )
                            }
                        }
                    }

                    if (showRefundDialog) {
                        AlertDialog(
                            onDismissRequest = { showRefundDialog = false },
                            title = { Text("Log installment payment", color = Color.White) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Amount received / paid back:", color = TextSecondaryDark, fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = payAmtStr,
                                        onValueChange = { payAmtStr = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                                    )
                                }
                            },
                            confirmButton = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = {
                                        viewModel.updateLoanPayment(loan, 0.0, true)
                                        showRefundDialog = false
                                    }) {
                                        Text("Force Settle", color = IncomeGreen)
                                    }
                                    Button(
                                        onClick = {
                                            val amt = payAmtStr.toDoubleOrNull() ?: 0.0
                                            if (amt > 0) {
                                                viewModel.updateLoanPayment(loan, amt, false)
                                            }
                                            showRefundDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                                    ) {
                                        Text("Save")
                                    }
                                }
                            },
                            containerColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }

    if (showAddLoanDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("LENT") } // LENT or BORROWED
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = { Text("Register Loan Entry", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { type = "LENT" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (type == "LENT") IncomeGreen else Color(0xFF334155))
                        ) {
                            Text("I Lent GET 💸")
                        }
                        Button(
                            onClick = { type = "BORROWED" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (type == "BORROWED") CoralRed else Color(0xFF334155))
                        ) {
                            Text("I Borrow GIVE 💳")
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Person Name") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ৳") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Loan context/notes") }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amount.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty() && amt > 0) {
                            viewModel.addLoan(name, amt, type, note, null)
                        }
                        showAddLoanDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Record Statement")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// SUB SECTION 3: CALENDAR VIEW
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarBillsSubSection(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showAddBillDialog by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val todayDate = calendar.get(Calendar.DAY_OF_MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Daily Outflow Heatmap", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        // Draw heat matrix grid for 30 days
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111827))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..7).forEach { col ->
                            val dayIdx = row * 7 + col
                            if (dayIdx <= 30) {
                                // Match transactions for this particular day of the month
                                val spendsForDay = transactions.filter {
                                    val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                    tCal.get(Calendar.DAY_OF_MONTH) == dayIdx && it.type == "EXPENSE"
                                }.sumOf { it.amount }

                                val colorHex = when {
                                    spendsForDay >= 15000 -> Color(0xFFFF5252)
                                    spendsForDay >= 5000 -> Color(0xFFFF9100)
                                    spendsForDay >= 1000 -> Color(0xFFFFD600)
                                    spendsForDay > 0 -> Color(0xFF00E676)
                                    else -> Color(0xFF1E293B)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colorHex),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$dayIdx",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = if (spendsForDay > 0) Color.Black else Color.White
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                Text("<1k Low spent", fontSize = 9.sp, color = TextSecondaryDark)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5252)))
                Text("15k+ Heavy spent", fontSize = 9.sp, color = TextSecondaryDark)
            }
        }

        // Active bills listing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Next Bills", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddBillDialog = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Bill", tint = ElectricPurple)
            }
        }
        
        if (bills.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("No upcoming bills registered.", fontSize = 11.sp, color = TextSecondaryDark)
            }
        } else {
            bills.forEach { bill ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bill.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(bill.dueDate))
                            Text("Due Date: $dateStr • Auto-Process", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                        Button(
                            onClick = { viewModel.markBillPaid(bill) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Pay ৳", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddBillDialog) {
        var billName by remember { mutableStateOf("") }
        var billAmount by remember { mutableStateOf("") }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDueDate by remember { mutableStateOf<Long?>(null) }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("Add Recurring Bill", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        label = { Text("Bill Name (e.g. Netflix)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { billAmount = it },
                        label = { Text("Amount ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { showDatePicker = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateText = selectedDueDate?.let {
                            java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                        } ?: "Select Due Date"
                        Text(dateText, color = if (selectedDueDate != null) Color.White else TextSecondaryDark)
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = TextSecondaryDark)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = billAmount.toDoubleOrNull() ?: 0.0
                        val date = selectedDueDate
                        val walletId = wallets.firstOrNull()?.id ?: 1L
                        val categoryId = categories.firstOrNull()?.id ?: 1L
                        
                        if (billName.isNotEmpty() && amt > 0 && date != null) {
                            viewModel.addBill(billName, amt, date, categoryId, walletId)
                            showAddBillDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Add Bill")
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ==========================================
// SUB SECTION 4: SETTINGS & BACKUP/EXPORTS
// ==========================================
@Composable
fun ConfigureSettingsSubSection(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val currentCurrency by viewModel.primaryCurrency.collectAsState()
    val isNotifEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentUserName by viewModel.userName.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Application Configuration", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        // PROFILE NAME
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showNameDialog = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "User", tint = ElectricPurple)
                    Column {
                        Text("Profile Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Name shown on the home dashboard greeting", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
                Text(
                    text = if (currentUserName.isEmpty()) "Not set" else currentUserName,
                    fontWeight = FontWeight.Bold,
                    color = if (currentUserName.isEmpty()) Color.Gray else ElectricPurple,
                    fontSize = 14.sp
                )
            }
        }

        // BASE CURRENCY
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCurrencyDialog = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = "Loc", tint = ElectricPurple)
                    Column {
                        Text("Base Display Currency", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Current system currency representation", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
                Text(currentCurrency, fontWeight = FontWeight.Bold, color = ElectricPurple, fontSize = 14.sp)
            }
        }

        // CSV GENERATION EXPORTS
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val uri = viewModel.exportToCSV(context)
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Finance Statement"))
                    } else {
                        Toast.makeText(context, "Export generation error.", Toast.LENGTH_SHORT).show()
                    }
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "CSV", tint = MintGreen)
                    Column {
                        Text("Export to CSV", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Prepare CSV ledger backup & Share directly", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondaryDark)
            }
        }

        // NOTIFICATION TOGGLES
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Notif", tint = CoralRed)
                    Column {
                        Text("Daily Budget Reminders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Exceeded thresholds direct warning alerts", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
                Switch(
                    checked = isNotifEnabled,
                    onCheckedChange = { viewModel.updateNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricPurple)
                )
            }
        }

        // ABOUT APP INFO
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = "About", tint = Color.White)
                Column {
                    Text("Finance Tracker Client App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Version 1.0.0 • SQLite SQLiteCipher Local Sandbox", fontSize = 11.sp, color = TextSecondaryDark)
                }
            }
        }

        if (showCurrencyDialog) {
            AlertDialog(
                onDismissRequest = { showCurrencyDialog = false },
                title = { Text("Choose Base currency", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BDT", "USD", "EUR", "INR", "AED", "GBP").forEach { cur ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updatePrimaryCurrency(cur)
                                        showCurrencyDialog = false
                                    }
                                    .padding(vertical = 10.dp)
                             ) {
                                Text(cur, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {},
                containerColor = Color(0xFF1E293B)
            )
        }

        if (showNameDialog) {
            var tempName by remember { mutableStateOf(currentUserName) }
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("Update Profile Name", color = Color.White) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Introduce yourself for a personalized homepage dashboard greeting.", color = TextSecondaryDark, fontSize = 12.sp)
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            placeholder = { Text("Enter your name", color = Color.Gray) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricPurple,
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = ElectricPurple
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateUserName(tempName.trim())
                            showNameDialog = false
                        }
                    ) {
                        Text("Save", color = ElectricPurple, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }
    }
}
