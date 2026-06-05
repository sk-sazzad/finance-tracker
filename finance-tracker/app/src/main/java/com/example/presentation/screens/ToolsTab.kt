package com.example.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.UpdateChecker
import com.example.data.local.entities.LoanEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.SavingsDepositEntity
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
    val tabTitles = listOf("Savings", "Debts", "Tracker", "Configure")

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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabTitles.size) { index ->
                val title = tabTitles[index]
                val isSelected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) ElectricPurple else Color(0xFF1E293B))
                        .clickable { selectedSubTab = index }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🎯", fontSize = 24.sp)
                Column {
                    Text("Savings Goals", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Track your future targets", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { showAddGoalDialog = true }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Goal", tint = ElectricPurple)
            }
        }

        val totalSaved = goals.sumOf { it.currentAmount }
        val activeCount = goals.count { it.currentAmount < it.targetAmount }
        val achievedCount = goals.count { it.currentAmount >= it.targetAmount }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF00D4AA))))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Total Saved: ৳${String.format("%,.0f", totalSaved)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("$activeCount Active Goals", color = Color.White, fontSize = 12.sp)
                    Text("$achievedCount Achieved 🎉", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (goals.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎯", fontSize = 64.sp)
                Text("No Goals Yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Define a savings target to begin!", color = TextSecondaryDark, fontSize = 14.sp)
            }
        } else {
            val wallets by viewModel.wallets.collectAsState()
            
            goals.forEach { goal ->
                val progressRatio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                val daysRemaining = ((goal.deadline - System.currentTimeMillis()) / (86400000L).coerceAtLeast(1L)).toInt()
                val percentage = (progressRatio * 100).toInt()
                val deadlineStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(goal.deadline))
                
                var showAddMoneySheet by remember { mutableStateOf(false) }
                var showHistorySheet by remember { mutableStateOf(false) }

                SwipeableGoalRow(
                    onDelete = { viewModel.deleteSavingsGoal(goal) },
                    onEdit = { goalToEdit = goal }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF111827))
                    ) {
                        Row(modifier = Modifier.matchParentSize()) {
                            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(IncomeGreen))
                        }
                        Column(
                            modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Brush.horizontalGradient(listOf(ElectricPurple, MintGreen))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(goal.emoji, fontSize = 32.sp)
                                    }
                                    Column {
                                        Text(goal.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (goal.description.isNotBlank()) {
                                            Text(goal.description, fontSize = 12.sp, color = TextSecondaryDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                val badgeColor = when {
                                    daysRemaining < 7 -> CoralRed
                                    daysRemaining < 30 -> Color(0xFFFACC15)
                                    else -> IncomeGreen
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(badgeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("$daysRemaining days left", fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🗓 $deadlineStr", fontSize = 10.sp, color = TextSecondaryDark)
                                }
                            }

                            if (goal.currentAmount >= goal.targetAmount) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x3300E676), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🎉 Goal Achieved!", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            } else {
                                Text(
                                    text = "৳${String.format("%,.0f", goal.currentAmount)} / ৳${String.format("%,.0f", goal.targetAmount)}  •  $percentage%",
                                    fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x1BFFFFFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progressRatio.coerceIn(0f, 1f))
                                            .height(10.dp)
                                            .background(Brush.horizontalGradient(listOf(ElectricPurple, MintGreen)))
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddMoneySheet = true },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("💰 Add Money", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { showHistorySheet = true },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("📋 History", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                if (showAddMoneySheet) {
                    var depositInputAmt by remember { mutableStateOf("") }
                    var depositNote by remember { mutableStateOf("") }
                    var selectedWalletId by remember { mutableStateOf<Long?>(null) }
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(
                        onDismissRequest = { showAddMoneySheet = false },
                        containerColor = Color(0xFF1E293B)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Add to ${goal.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = depositInputAmt,
                                onValueChange = { depositInputAmt = it },
                                label = { Text("Amount ৳", color = TextSecondaryDark) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = depositNote,
                                onValueChange = { depositNote = it },
                                label = { Text("Note (optional)", color = TextSecondaryDark) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Select Wallet (Source)", color = TextSecondaryDark, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedWalletId == null) ElectricPurple else Color(0xFF111827))
                                        .border(1.dp, if (selectedWalletId == null) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                        .clickable { selectedWalletId = null }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text("Manual (No Deduction)", color = Color.White, fontSize = 11.sp)
                                }
                                wallets.forEach { w ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selectedWalletId == w.id) ElectricPurple else Color(0xFF111827))
                                            .border(1.dp, if (selectedWalletId == w.id) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable { selectedWalletId = w.id }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(w.name, color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }

                            if (selectedWalletId != null) {
                                val wName = wallets.firstOrNull { it.id == selectedWalletId }?.name ?: ""
                                val amtStr = depositInputAmt.ifEmpty { "0" }
                                Text("৳$amtStr will be deducted from $wName", color = CoralRed, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val amt = depositInputAmt.toDoubleOrNull() ?: 0.0
                                    if (amt > 0) {
                                        viewModel.addSavingsDeposit(goal.id, amt, selectedWalletId, depositNote)
                                        showAddMoneySheet = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save BDT", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                if (showHistorySheet) {
                    val deposits by viewModel.getDepositsForGoal(goal.id).collectAsState(initial = emptyList())
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(
                        onDismissRequest = { showHistorySheet = false },
                        containerColor = Color(0xFF1E293B)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("${goal.name} — Deposit History", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val totalDep = deposits.sumOf { it.amount }
                            Text("Total Deposited: ৳${String.format("%,.0f", totalDep)}", color = MintGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            
                            Divider(color = Color(0x33FFFFFF))

                            if (deposits.isEmpty()) {
                                Text("No deposits yet", color = TextSecondaryDark, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(deposits) { dep ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF0F172A)).padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                val dStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(dep.date))
                                                Text(dStr, color = Color.White, fontSize = 12.sp)
                                                if (dep.note.isNotEmpty()) {
                                                    Text(dep.note, color = TextSecondaryDark, fontSize = 10.sp)
                                                }
                                                if (dep.walletId != null) {
                                                    val wName = wallets.firstOrNull { it.id == dep.walletId }?.name ?: "Wallet"
                                                    Text("From: $wName", color = ElectricPurple, fontSize = 10.sp)
                                                }
                                            }
                                            Text("+ ৳${String.format("%,.0f", dep.amount)}", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
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
        var selectedDeadline by remember { mutableStateOf(goalToEdit?.deadline ?: (System.currentTimeMillis() + 86400000L * 180L)) }
        var showDatePickerDialog by remember { mutableStateOf(false) }

        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { 
                showAddGoalDialog = false
                goalToEdit = null
            },
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(if (goalToEdit != null) "Edit savings goal" else "Add savings goal", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Quick emoji picker
                Text("Select an icon:", color = TextSecondaryDark, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val emojis = listOf("🏠", "🚗", "💻", "✈️", "💍", "📱", "🎓", "💰", "🏋️", "🎮")
                    items(emojis) { e ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (emoji == e) ElectricPurple else Color(0xFF111827))
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(e, fontSize = 24.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetAmt,
                        onValueChange = { targetAmt = it },
                        label = { Text("Target ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = initialAmt,
                        onValueChange = { initialAmt = it },
                        label = { Text("Saved ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111827))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showDatePickerDialog = true }
                        .padding(16.dp)
                ) {
                    Text("🗓 Target Date: ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedDeadline))}", color = Color.White)
                }
                
                if (showDatePickerDialog) {
                    val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDeadline)
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dateState.selectedDateMillis?.let { selectedDeadline = it }
                                showDatePickerDialog = false
                            }) { Text("OK") }
                        }
                    ) {
                        DatePicker(dateState)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val tar = targetAmt.toDoubleOrNull() ?: 0.0
                        val ini = initialAmt.toDoubleOrNull() ?: 0.0
                        if (name.isNotEmpty() && tar > 0) {
                            if (goalToEdit != null) {
                                viewModel.updateSavingsGoal(
                                    goalToEdit!!.copy(
                                        name = name,
                                        emoji = emoji,
                                        targetAmount = tar,
                                        currentAmount = ini,
                                        description = description,
                                        deadline = selectedDeadline
                                    )
                                )
                            } else {
                                viewModel.addSavingsGoal(name, emoji, tar, ini, selectedDeadline, description)
                            }
                        }
                        showAddGoalDialog = false
                        goalToEdit = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (goalToEdit != null) "Update Goal" else "Add Goal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsSubSection(viewModel: FinanceViewModel) {
    val debtPersons by viewModel.debtPersons.collectAsState()
    val debtEntries by viewModel.debtEntries.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var expandedPersonId by remember { mutableStateOf<Long?>(null) }
    var showAddEntryDialogForPerson by remember { mutableStateOf<Long?>(null) }
    var showPayOverallDialogForPerson by remember { mutableStateOf<Long?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🤝", fontSize = 24.sp)
                Column {
                    Text("Lent & Borrowed Ledger", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Track your debts efficiently", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { showAddPersonDialog = true }) {
                Icon(Icons.Default.AddCard, contentDescription = "Add Person", tint = ElectricPurple)
            }
        }

        // Section Totals Calculate
        val lentPersons = debtPersons.filter { it.type == "LENT" }
        val borrowedPersons = debtPersons.filter { it.type == "BORROWED" }

        val totalLentStr = lentPersons.sumOf { person ->
            val personEntries = debtEntries.filter { it.personId == person.id }
            val owed = personEntries.sumOf { it.amount }
            val paid = personEntries.sumOf { it.paidAmount }
            owed - paid
        }

        val totalBorrowedStr = borrowedPersons.sumOf { person ->
            val personEntries = debtEntries.filter { it.personId == person.id }
            val owed = personEntries.sumOf { it.amount }
            val paid = personEntries.sumOf { it.paidAmount }
            owed - paid
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF00E676).copy(alpha = 0.2f), Color.Transparent))).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📤", fontSize = 16.sp)
                        Text("I Will Receive", fontSize = 12.sp, color = TextSecondaryDark)
                    }
                    Text(String.format("৳%,.0f", totalLentStr), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF6B6B).copy(alpha = 0.2f), Color.Transparent))).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📥", fontSize = 16.sp)
                        Text("I Will Pay", fontSize = 12.sp, color = TextSecondaryDark)
                    }
                    Text(String.format("৳%,.0f", totalBorrowedStr), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                }
            }
        }

        if (debtPersons.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🤝", fontSize = 64.sp)
                Text("No Debts Yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Your debt diary is currently clean.", color = TextSecondaryDark, fontSize = 14.sp)
            }
        } else {
            debtPersons.forEach { person ->
                val personEntries = debtEntries.filter { it.personId == person.id }
                val totalOwed = personEntries.sumOf { it.amount }
                val totalPaid = personEntries.sumOf { it.paidAmount }
                val netOutstanding = totalOwed - totalPaid
                
                val statusBadge = if (totalOwed == 0.0) "ACTIVE" else if (netOutstanding <= 0.0 && totalOwed > 0.0) "SETTLED" else if (totalPaid > 0.0) "PARTIALLY_PAID" else "ACTIVE"

                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (statusBadge == "SETTLED") Color(0xFF1E293B) else Color(0xFF111827))
                ) {
                    Row(modifier = Modifier.matchParentSize()) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(if (person.type == "LENT") IncomeGreen else CoralRed))
                    }
                    Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedPersonId = if (expandedPersonId == person.id) null else person.id },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (person.type == "LENT") IncomeGreen else CoralRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(person.personName.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                Column {
                                    Text(person.personName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (person.phoneNumber.isNotBlank()) {
                                        Text(person.phoneNumber, fontSize = 12.sp, color = TextSecondaryDark)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val badgeColor = when (statusBadge) {
                                        "SETTLED" -> IncomeGreen
                                        "PARTIALLY_PAID" -> Color(0xFFFACC15)
                                        else -> Color(0xFFF97316)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.2f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(statusBadge.replace("_", " "), fontSize = 8.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = String.format("৳%,.0f", netOutstanding),
                                    color = if (person.type == "LENT") IncomeGreen else CoralRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                val rotation = androidx.compose.animation.core.animateFloatAsState(targetValue = if (expandedPersonId == person.id) 180f else 0f)
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp).rotate(rotation.value)
                                )
                            }
                        }

                        // Expanded View
                        if (expandedPersonId == person.id) {
                            Divider(color = Color(0x33FFFFFF))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text("৳${String.format("%,.0f", totalOwed)}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Paid", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text("৳${String.format("%,.0f", totalPaid)}", fontSize = 12.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Left", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text("৳${String.format("%,.0f", netOutstanding)}", fontSize = 12.sp, color = CoralRed, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Debt Entries", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (netOutstanding > 0) {
                                        Button(
                                            onClick = { showPayOverallDialogForPerson = person.id },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                                        ) {
                                            Text("Pay Overall", fontSize = 10.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { showAddEntryDialogForPerson = person.id },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (person.type == "LENT") IncomeGreen else CoralRed)
                                    ) {
                                        Text("+ Entry", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            if (personEntries.isEmpty()) {
                                Text("No entries added yet.", color = TextSecondaryDark, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }

                            personEntries.forEach { entry ->
                                var showPartialPayDialog by remember { mutableStateOf(false) }
                                val outst = entry.amount - entry.paidAmount
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (entry.status == "PAID") IncomeGreen else CoralRed))
                                        Column {
                                            Text(entry.reason, color = Color.White, fontSize = 13.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                            val entryDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.date))
                                            Text(entryDate, color = TextSecondaryDark, fontSize = 10.sp)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(String.format("৳%,.0f", entry.amount), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        if (entry.status == "PAID") {
                                            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(IncomeGreen.copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                Text("PAID", color = IncomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text("Due: ৳${String.format("%,.0f", outst)}", color = CoralRed, fontSize = 10.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                                                Box(
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, ElectricPurple, RoundedCornerShape(12.dp)).clickable { showPartialPayDialog = true }.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("💸 Partial", fontSize = 10.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(IncomeGreen).clickable { viewModel.payEntryAmount(entry, outst) }.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("✅ Full Pay", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showPartialPayDialog) {
                                    var partialAmtStr by remember { mutableStateOf("") }
                                    @OptIn(ExperimentalMaterial3Api::class)
                                    ModalBottomSheet(
                                        onDismissRequest = { showPartialPayDialog = false },
                                        containerColor = Color(0xFF1E293B)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text("Partial Payment", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = partialAmtStr,
                                                onValueChange = { partialAmtStr = it },
                                                label = { Text("Amount ৳", color = TextSecondaryDark) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    val amt = partialAmtStr.toDoubleOrNull() ?: 0.0
                                                    if (amt > 0) {
                                                        viewModel.payEntryAmount(entry, amt)
                                                    }
                                                    showPartialPayDialog = false
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                                                shape = RoundedCornerShape(14.dp)
                                            ) {
                                                Text("Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(32.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("LENT") }

        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Person", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "LENT" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "LENT") IncomeGreen else Color(0xFF334155)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("📤 I Lent GET", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { type = "BORROWED" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "BORROWED") CoralRed else Color(0xFF334155)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("📥 I Borrow GIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, 
                    label = { Text("Person Name", color = TextSecondaryDark) }, 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, 
                    label = { Text("Phone (Optional)", color = TextSecondaryDark) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            viewModel.addDebtPerson(name, phone, type)
                        }
                        showAddPersonDialog = false
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Person", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddEntryDialogForPerson != null) {
        var amountStr by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }
        var selectedEntryDate by remember { mutableStateOf(System.currentTimeMillis()) }
        var showEntryDatePicker by remember { mutableStateOf(false) }
        
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showAddEntryDialogForPerson = null },
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Debt Entry", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it }, 
                    label = { Text("Amount ৳", color = TextSecondaryDark) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it }, 
                    label = { Text("Reason/Context", color = TextSecondaryDark) }, 
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF111827))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showEntryDatePicker = true }
                        .padding(16.dp)
                ) {
                    Text("📅 ${java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(selectedEntryDate))}", color = Color.White)
                }
                
                if (showEntryDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedEntryDate)
                    DatePickerDialog(
                        onDismissRequest = { showEntryDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedEntryDate = it }
                                showEntryDatePicker = false
                            }) { Text("OK", color = ElectricPurple) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEntryDatePicker = false }) {
                                Text("Cancel", color = TextSecondaryDark)
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
                
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && reason.isNotEmpty()) {
                            viewModel.addDebtEntry(showAddEntryDialogForPerson!!, amt, reason, selectedEntryDate)
                        }
                        showAddEntryDialogForPerson = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPayOverallDialogForPerson != null) {
        val personId = showPayOverallDialogForPerson!!
        val person = debtPersons.firstOrNull { it.id == personId }
        val outstanding = if (person != null) {
            debtEntries.filter { it.personId == person.id }.sumOf { it.amount - it.paidAmount }.coerceAtLeast(0.0)
        } else 0.0

        if (person != null) {
            var overallPayStr by remember { mutableStateOf("") }
            var selectedWalletId by remember { mutableStateOf<Long?>(wallets.firstOrNull()?.id) }
            var markSettled by remember { mutableStateOf(true) }
            var errorMsg by remember { mutableStateOf<String?>(null) }
            
            // Initialization
            LaunchedEffect(personId) {
                overallPayStr = outstanding.toString()
                markSettled = true
            }
            
            ModalBottomSheet(
                onDismissRequest = { showPayOverallDialogForPerson = null },
                containerColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Record Payment — ${person.personName}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Outstanding: ৳${String.format("%,.0f", outstanding)}", color = TextSecondaryDark, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = overallPayStr,
                        onValueChange = { 
                            overallPayStr = it
                            if (it.toDoubleOrNull() != outstanding) markSettled = false
                        },
                        label = { Text("Amount ৳", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = markSettled,
                            onCheckedChange = { 
                                markSettled = it
                                if (it) overallPayStr = outstanding.toString()
                            },
                        )
                        Text("Settle Fully", color = Color.White)
                    }

                    Text("Select Wallet", color = TextSecondaryDark, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(wallets) { w ->
                            val isSelected = selectedWalletId == w.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF111827))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) ElectricPurple else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { 
                                        selectedWalletId = w.id 
                                        errorMsg = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("${w.name} (৳${String.format("%,.0f", w.balance)})", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = CoralRed, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val amt = overallPayStr.toDoubleOrNull() ?: 0.0
                            if (selectedWalletId == null) {
                                errorMsg = "Please select a wallet"
                            } else if (amt > 0) {
                                viewModel.payDebtOverallWithWallet(person, amt, selectedWalletId!!, markSettled)
                                val wName = wallets.firstOrNull { it.id == selectedWalletId }?.name ?: "Wallet"
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Payment recorded from $wName")
                                }
                                showPayOverallDialogForPerson = null
                            } else {
                                errorMsg = "Invalid amount"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Payment", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
    } // Close Column

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState)
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

    val currentCal = Calendar.getInstance()
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableStateOf<Int?>(null) }

    var showBillSheet by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<com.example.data.local.entities.BillEntity?>(null) }
    var billToDelete by remember { mutableStateOf<com.example.data.local.entities.BillEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedCalTab by remember { mutableStateOf(0) }

    val tempCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, selectedYear)
        set(Calendar.MONTH, selectedMonth)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
    val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(tempCal.time)

    val txInMonth = transactions.filter {
        val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
        tCal.get(Calendar.MONTH) == selectedMonth && tCal.get(Calendar.YEAR) == selectedYear
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selectedCalTab == 0) ElectricPurple else Color.Transparent).clickable { selectedCalTab = 0 }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("📅 Calendar", color = if (selectedCalTab == 0) Color.White else TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selectedCalTab == 1) ElectricPurple else Color.Transparent).clickable { selectedCalTab = 1 }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🧾 Bills", color = if (selectedCalTab == 1) Color.White else TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (selectedCalTab == 0) {
            // Section 1: Month Navigator Header
            Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF1E293B)).clickable {
                    if (selectedMonth == 0) {
                        selectedMonth = 11
                        selectedYear -= 1
                    } else {
                        selectedMonth -= 1
                    }
                    selectedDay = null
                }.padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = Color.White)
            }
            Text("📅 $monthName $selectedYear", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val isCurrentMonth = selectedMonth == currentCal.get(Calendar.MONTH) && selectedYear == currentCal.get(Calendar.YEAR)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (isCurrentMonth) Color(0xFF111827) else Color(0xFF1E293B)).clickable(enabled = !isCurrentMonth) {
                    if (!isCurrentMonth) {
                        if (selectedMonth == 11) {
                            selectedMonth = 0
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                        selectedDay = null
                    }
                }.padding(8.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = if (isCurrentMonth) Color.Gray else Color.White)
            }
        }

        // Section 2: Proper Calendar Grid
        val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            weekdays.forEach { day ->
                Text(day, color = TextSecondaryDark, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        val totalCells = daysInMonth + firstDayOfWeek
        val rows = (totalCells + 6) / 7

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (c in 0 until 7) {
                        val index = r * 7 + c
                        if (index < firstDayOfWeek || index >= totalCells) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val dayNum = index - firstDayOfWeek + 1
                            val txInDay = txInMonth.filter {
                                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH) == dayNum
                            }
                            val inc = txInDay.filter { it.type == "INCOME" }.sumOf { it.amount }
                            val exp = txInDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                            val net = exp - inc

                            val isSelected = dayNum == selectedDay
                            val cellColor = if (isSelected) ElectricPurple
                                else if (txInDay.isEmpty()) Color(0xFF1E293B)
                                else if (inc > exp) Color(0xFF6C63FF)
                                else if (net > -1000) IncomeGreen
                                else if (net > -5000) Color(0xFFFFD600)
                                else if (net > -15000) Color(0xFFFF9100)
                                else CoralRed

                            val isToday = dayNum == currentCal.get(Calendar.DAY_OF_MONTH) && selectedMonth == currentCal.get(Calendar.MONTH) && selectedYear == currentCal.get(Calendar.YEAR)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellColor)
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedDay = dayNum },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    color = if (isSelected || txInDay.isEmpty() || cellColor == Color(0xFF6C63FF) || cellColor == CoralRed || cellColor == Color(0xFFFF9100)) Color.White else Color.Black,
                                    fontSize = if (isToday) 16.sp else 14.sp,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Color Legend Pills
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val legend = listOf(
                Pair("Income", Color(0xFF6C63FF)),
                Pair("Low Exp", IncomeGreen),
                Pair("Med Exp", Color(0xFFFFD600)),
                Pair("High Exp", Color(0xFFFF9100)),
                Pair("Max Exp", CoralRed)
            )
            items(legend) { (text, color) ->
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 3: Day Detail Panel
        if (selectedDay != null) {
            val txInSelectedDay = txInMonth.filter {
                Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.DAY_OF_MONTH) == selectedDay
            }

            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E293B)).padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📅", fontSize = 20.sp)
                        Text("Transactions on $selectedDay $monthName", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    val dayInc = txInSelectedDay.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val dayExp = txInSelectedDay.filter { it.type == "EXPENSE" }.sumOf { it.amount }

                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF111827)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Income", color = TextSecondaryDark, fontSize = 10.sp)
                            Text("৳${String.format("%,.0f", dayInc)}", color = IncomeGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Divider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Expense", color = TextSecondaryDark, fontSize = 10.sp)
                            Text("৳${String.format("%,.0f", dayExp)}", color = CoralRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0x33FFFFFF))

                    if (txInSelectedDay.isEmpty()) {
                        Text("No transactions on this day", color = TextSecondaryDark, fontSize = 12.sp)
                    } else {
                        txInSelectedDay.forEach { tx ->
                            val cat = categories.find { it.id == tx.categoryId }
                            val wallet = wallets.find { it.id == tx.walletId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat?.icon ?: "💰", fontSize = 18.sp)
                                    Column {
                                        Text(cat?.name ?: "Unknown", color = Color.White, fontSize = 13.sp)
                                        Text(wallet?.name ?: "Cash", color = TextSecondaryDark, fontSize = 10.sp)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(tx.date))
                                    Text(
                                        text = if (tx.type == "INCOME") "+ ৳${String.format("%,.0f", tx.amount)}" else "- ৳${String.format("%,.0f", tx.amount)}",
                                        color = if (tx.type == "INCOME") IncomeGreen else CoralRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(timeStr, color = TextSecondaryDark, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Month Summary Bar
        val monthInc = txInMonth.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthExp = txInMonth.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val monthNet = monthInc - monthExp

        val topCatId = txInMonth.filter { it.type == "EXPENSE" }
            .groupBy { it.categoryId }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .maxByOrNull { it.value }?.key

        val topCat = categories.find { it.id == topCatId }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Income", color = TextSecondaryDark, fontSize = 12.sp)
                    Text("৳${String.format("%,.0f", monthInc)}", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Expense", color = TextSecondaryDark, fontSize = 12.sp)
                    Text("৳${String.format("%,.0f", monthExp)}", color = CoralRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                HorizontalDivider(color = Color(0x33FFFFFF))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Net Savings", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "৳${String.format("%,.0f", monthNet)}",
                        color = if (monthNet >= 0) IncomeGreen else CoralRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                if (topCat != null) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Most Spent Category:", color = TextSecondaryDark, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(topCat.icon, fontSize = 14.sp)
                            Text(topCat.name, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        } else {
        // Section 5: Bills Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🧾", fontSize = 24.sp)
                Column {
                    Text("Next Bills", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Upcoming subscriptions & dues", color = TextSecondaryDark, fontSize = 12.sp)
                }
            }
            IconButton(onClick = {
                billToEdit = null
                showBillSheet = true
            }) {
                Icon(Icons.Default.AddCircle, contentDescription = "Add Bill", tint = ElectricPurple)
            }
        }

        if (bills.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🧾", fontSize = 64.sp)
                Text("No Upcoming Bills", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Your registered bills will appear here.", color = TextSecondaryDark, fontSize = 14.sp)
            }
        } else {
            bills.forEach { bill ->
                val isPaidThisMonth = bill.lastPaid != null && Calendar.getInstance().apply { timeInMillis = bill.lastPaid }.get(Calendar.MONTH) == currentCal.get(Calendar.MONTH) && Calendar.getInstance().apply { timeInMillis = bill.lastPaid }.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR)
                val daysRemaining = ((bill.dueDate - System.currentTimeMillis()) / 86400000L).coerceAtLeast(0L).toInt()
                val urgencyColor = if (isPaidThisMonth) IncomeGreen else if (daysRemaining <= 3) CoralRed else if (daysRemaining <= 7) Color(0xFFFFD600) else IncomeGreen

                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827))) {
                    Row(modifier = Modifier.matchParentSize()) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(urgencyColor))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bill.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            val dateStr = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(bill.dueDate))
                            Text("🗓 $dateStr", fontSize = 11.sp, color = TextSecondaryDark)
                            val wName = wallets.firstOrNull { it.id == bill.walletId }?.name ?: "Wallet"
                            Text("Wallet: $wName", fontSize = 10.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { 
                                    billToEdit = bill
                                    showBillSheet = true
                                })
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(20.dp).clickable { billToDelete = bill })
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("৳${String.format("%,.0f", bill.amount)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                            if (isPaidThisMonth) {
                                Box(modifier = Modifier.background(Color(0x3300E676), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("Paid ✅", color = IncomeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Box(modifier = Modifier.background(urgencyColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text(if (daysRemaining == 0) "Due Today" else "In $daysRemaining days", color = urgencyColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(ElectricPurple).clickable { 
                                        viewModel.markBillPaid(bill)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Bill paid!") }
                                    }.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("✅ Pay Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (billToDelete != null) {
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete Bill", color = Color.White) },
            text = { Text("Are you sure you want to delete this bill?", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(billToDelete!!)
                        billToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    if (showBillSheet) {
        var billName by remember { mutableStateOf(billToEdit?.name ?: "") }
        var billAmount by remember { mutableStateOf(if (billToEdit != null) billToEdit!!.amount.toString() else "") }
        var showDatePicker by remember { mutableStateOf(false) }
        var selectedDueDate by remember { mutableStateOf<Long?>(billToEdit?.dueDate) }
        var selectedWalletId by remember { mutableStateOf(billToEdit?.walletId ?: wallets.firstOrNull()?.id) }
        var selectedCategoryId by remember { mutableStateOf(billToEdit?.categoryId ?: categories.firstOrNull()?.id) }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDueDate ?: System.currentTimeMillis())
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

        ModalBottomSheet(
            onDismissRequest = { 
                showBillSheet = false
                billToEdit = null
            },
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(if (billToEdit != null) "Edit Bill" else "Add Bill", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = billName,
                    onValueChange = { billName = it },
                    label = { Text("Bill Name", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it },
                    label = { Text("Amount ৳", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
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

                Text("Select Wallet", color = TextSecondaryDark, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { w ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedWalletId == w.id) ElectricPurple else Color(0xFF111827))
                                .clickable { selectedWalletId = w.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(w.name, color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Text("Select Category", color = TextSecondaryDark, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { c ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedCategoryId == c.id) ElectricPurple else Color(0xFF111827))
                                .clickable { selectedCategoryId = c.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("${c.icon} ${c.name}", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        val amt = billAmount.toDoubleOrNull() ?: 0.0
                        if (billName.isNotEmpty() && amt > 0 && selectedDueDate != null && selectedWalletId != null && selectedCategoryId != null) {
                            if (billToEdit != null) {
                                viewModel.deleteBill(billToEdit!!)
                            }
                            viewModel.addBill(billName, amt, selectedDueDate!!, selectedCategoryId!!, selectedWalletId!!)
                            showBillSheet = false
                            billToEdit = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Bill", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(hostState = snackbarHostState)
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
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️", fontSize = 24.sp)
            Column {
                Text("Application Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Customize your experience", color = TextSecondaryDark, fontSize = 12.sp)
            }
        }

        // PROFILE NAME
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827)).clickable { showNameDialog = true }
        ) {
            Row(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(ElectricPurple)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "User", tint = ElectricPurple)
                    Column {
                        Text("Profile Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827)).clickable { showCurrencyDialog = true }
        ) {
            Row(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(ElectricPurple)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Language, contentDescription = "Loc", tint = ElectricPurple)
                    Column {
                        Text("Base Display Currency", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Current system currency representation", fontSize = 11.sp, color = TextSecondaryDark)
                    }
                }
                Text(currentCurrency, fontWeight = FontWeight.Bold, color = ElectricPurple, fontSize = 14.sp)
            }
        }

        // NOTIFICATION TOGGLES
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827))) {
            Row(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(ElectricPurple)) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Notif", tint = ElectricPurple)
                    Column {
                        Text("Daily Budget Reminders", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🗄️", fontSize = 24.sp)
            Column {
                Text("Export & Backup", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Manage your financial data", color = TextSecondaryDark, fontSize = 12.sp)
            }
        }

        // CSV GENERATION EXPORTS
        Button(
            onClick = {
                val uri = viewModel.exportToCSV(context)
                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Finance Statement"))
                } else {
                    Toast.makeText(context, "Export generation error.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, contentDescription = "CSV", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Ledger to CSV", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠️", fontSize = 24.sp)
            Column {
                Text("Danger Zone", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Irreversible actions", color = TextSecondaryDark, fontSize = 12.sp)
            }
        }

        Button(
            onClick = { showClearConfirmDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Delete, contentDescription = "Reset", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clean Slate (Wipe Data)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("⚠️ Wipe All Data?", color = Color.White) },
                text = { Text("This will permanently delete ALL transactions, wallets, budgets, goals and debts. This cannot be undone.", color = TextSecondaryDark) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllData()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) { Text("Yes, Wipe Everything") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondaryDark)
                    }
                },
                containerColor = Color(0xFF1E293B)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UPDATE CHECKER SYSTEM
        val updateState by viewModel.updateInfo.collectAsState()
        var isCheckingUpdate by remember { mutableStateOf(false) }
        var noUpdateFound by remember { mutableStateOf(false) }
        val updateScope = rememberCoroutineScope()

        Button(
            onClick = {
                isCheckingUpdate = true
                noUpdateFound = false
                updateScope.launch {
                    val info = UpdateChecker.checkForUpdate(context)
                    isCheckingUpdate = false
                    if (info.hasUpdate) {
                        viewModel.checkForUpdates(context)
                    } else {
                        noUpdateFound = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0EA5E9)
            )
        ) {
            if (isCheckingUpdate) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking...", color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Text("🔄 Check for Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (noUpdateFound) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "✅ You're on the latest version!",
                color = IncomeGreen,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        // ABOUT INFO
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // App logo mini
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Finance Tracker", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Smart Money. Simple Life.", color = TextSecondaryDark, fontSize = 12.sp)
                Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))
                Text("Version 1.0.0", color = TextSecondaryDark, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Developed by", color = TextSecondaryDark, fontSize = 11.sp)
                Text("SK Sazzad", color = ElectricPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x1100D4AA))
                        .border(1.dp, Color(0x3300D4AA), RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sazzad.site"))
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🌐", fontSize = 14.sp)
                    Text(
                        "sazzad.site",
                        color = Color(0xFF00D4AA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Open Website",
                        tint = Color(0xFF00D4AA),
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                Text("© 2026 All Rights Reserved", color = TextSecondaryDark, fontSize = 10.sp)
            }
        }

        if (showCurrencyDialog) {
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showCurrencyDialog = false },
                containerColor = Color(0xFF1E293B)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select Base Currency", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Your entire app will display totals in this currency.", color = TextSecondaryDark, fontSize = 12.sp)
                    
                    val currencies = listOf("BDT", "USD", "EUR", "INR", "AED", "GBP")
                    
                    currencies.chunked(3).forEach { rowCurrencies ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowCurrencies.forEach { cur ->
                                val isSelected = currentCurrency == cur
                                Button(
                                    onClick = {
                                        viewModel.updatePrimaryCurrency(cur)
                                        showCurrencyDialog = false
                                    },
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) ElectricPurple else Color(0xFF334155)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(cur, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showNameDialog) {
            var tempName by remember { mutableStateOf(currentUserName) }
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showNameDialog = false },
                containerColor = Color(0xFF1E293B)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Update Profile Name", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                    
                    Button(
                        onClick = {
                            viewModel.updateUserName(tempName.trim())
                            showNameDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Profile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
