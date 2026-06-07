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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.UpdateChecker
import com.example.data.local.entities.LoanEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.SavingsDepositEntity
import com.example.data.local.entities.DebtEntryEntity
import com.example.data.local.entities.DebtPersonEntity
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.*
import androidx.activity.compose.BackHandler
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsTab(
    viewModel: FinanceViewModel
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
                val daysRemainingRaw = ((goal.deadline - System.currentTimeMillis()) / 86400000L).toInt()
                val isDeadlinePassed = daysRemainingRaw < 0
                val daysRemaining = daysRemainingRaw
                val percentage = (progressRatio * 100).toInt()
                val deadlineStr = formatDate(goal.deadline, "MMM dd, yyyy")
                
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
                                        if (isDeadlinePassed) {
                                            Text(
                                                "⚠️ Deadline passed",
                                                fontSize = 10.sp,
                                                color = CoralRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else if (daysRemaining == 0) {
                                            Text(
                                                "⚠️ Due today!",
                                                fontSize = 10.sp,
                                                color = CoralRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                "$daysRemaining days left",
                                                fontSize = 10.sp,
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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
                        containerColor = Color(0xFF0F172A),
                        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 4.dp)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x33FFFFFF))
                            )
                        }
                    ) {
                        val keyController = LocalSoftwareKeyboardController.current
                        val fManager = LocalFocusManager.current
                        
                        BackHandler {
                            fManager.clearFocus()
                            keyController?.hide()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sheet header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Add to ${goal.name}",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showAddMoneySheet = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                            OutlinedTextField(
                                value = depositInputAmt,
                                onValueChange = { depositInputAmt = it },
                                label = { Text("Amount ৳", color = TextSecondaryDark, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricPurple,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = ElectricPurple,
                                    focusedLabelColor = ElectricPurple,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            OutlinedTextField(
                                value = depositNote,
                                onValueChange = { depositNote = it },
                                label = { Text("Note (optional)", color = TextSecondaryDark, fontSize = 13.sp) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ElectricPurple,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = ElectricPurple,
                                    focusedLabelColor = ElectricPurple,
                                    unfocusedLabelColor = TextSecondaryDark,
                                    focusedContainerColor = Color(0xFF1E293B),
                                    unfocusedContainerColor = Color(0xFF1E293B)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                "Select Wallet (Source)",
                                color = TextSecondaryDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isManualSelected = selectedWalletId == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isManualSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                        .border(
                                            width = if (isManualSelected) 1.5.dp else 1.dp,
                                            color = if (isManualSelected) ElectricPurple else Color(0x22FFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedWalletId = null }
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text("Manual (No Deduction)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                wallets.forEach { w ->
                                    val isSelected = selectedWalletId == w.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) ElectricPurple else Color(0x22FFFFFF),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedWalletId = w.id }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(w.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                            ) {
                                Text("Save BDT", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showHistorySheet) {
                    val deposits by viewModel.getDepositsForGoal(goal.id).collectAsState(initial = emptyList())
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(
                        onDismissRequest = { showHistorySheet = false },
                        containerColor = Color(0xFF0F172A),
                        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 4.dp)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0x33FFFFFF))
                            )
                        }
                    ) {
                        val keyController = LocalSoftwareKeyboardController.current
                        val fManager = LocalFocusManager.current
                        
                        BackHandler {
                            fManager.clearFocus()
                            keyController?.hide()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Sheet header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${goal.name} — History",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { showHistorySheet = false },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            val totalDep = deposits.sumOf { it.amount }
                            Text("Total Deposited: ৳${String.format("%,.0f", totalDep)}", color = MintGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            
                            HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                            if (deposits.isEmpty()) {
                                Text("No deposits yet", color = TextSecondaryDark, fontSize = 12.sp, modifier = Modifier.padding(vertical = 16.dp))
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(deposits) { dep ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B)).padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                val dStr = formatDate(dep.date, "MMM dd, yyyy")
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
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            val keyController = LocalSoftwareKeyboardController.current
            val fManager = LocalFocusManager.current
            
            BackHandler {
                fManager.clearFocus()
                keyController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (goalToEdit != null) "Edit Savings Goal" else "Add Savings Goal",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { 
                            showAddGoalDialog = false
                            goalToEdit = null
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name", color = TextSecondaryDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Quick emoji picker
                Text(
                    "Select an icon:",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val emojis = listOf("🏠", "🚗", "💻", "✈️", "💍", "📱", "🎓", "💰", "🏋️", "🎮")
                    items(emojis) { e ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (emoji == e) ElectricPurple else Color(0xFF1E293B))
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
                        label = { Text("Target ৳", color = TextSecondaryDark, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Right) }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ElectricPurple,
                            focusedLabelColor = ElectricPurple,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (goalToEdit == null) {
                        OutlinedTextField(
                            value = initialAmt,
                            onValueChange = { initialAmt = it },
                            label = { Text("Saved ৳", color = TextSecondaryDark, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricPurple,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = ElectricPurple,
                                focusedLabelColor = ElectricPurple,
                                unfocusedLabelColor = TextSecondaryDark,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1A00D4AA))
                                .padding(12.dp)
                        ) {
                            Text(
                                "💰 Already saved: ৳${String.format("%,.0f", goalToEdit!!.currentAmount)}  •  Use '+ Add Money' to update",
                                color = Color(0xFF00D4AA),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showDatePickerDialog = true }
                        .padding(16.dp)
                ) {
                    Text("🗓 Target Date: ${formatDate(selectedDeadline, "dd MMMM yyyy")}", color = Color.White)
                }
                
                if (showDatePickerDialog) {
                    val dateState = rememberDatePickerState(initialSelectedDateMillis = if (selectedDeadline > 0) timestampToUtcMidnight(selectedDeadline) else todayUtcMidnight())
                    DatePickerDialog(
                        onDismissRequest = { showDatePickerDialog = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dateState.selectedDateMillis?.let { selectedDeadline = utcMidnightToLocalTimestamp(it) }
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
                    label = { Text("Description (Optional)", color = TextSecondaryDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
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
                                        currentAmount = goalToEdit!!.currentAmount,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text(if (goalToEdit != null) "Update Goal" else "Add Goal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val debtPersons by viewModel.debtPersons.collectAsState()
    val debtEntries by viewModel.debtEntries.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var expandedPersonId by remember { mutableStateOf<Long?>(null) }
    var showAddEntryDialogForPerson by remember { mutableStateOf<Long?>(null) }
    var showPayOverallDialogForPerson by remember { mutableStateOf<Long?>(null) }
    var showPayEntrySheet by remember { mutableStateOf<DebtEntryEntity?>(null) }
    var showEditPersonSheet by remember { mutableStateOf<DebtPersonEntity?>(null) }
    var showDeletePersonDialog by remember { mutableStateOf<DebtPersonEntity?>(null) }

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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { showEditPersonSheet = person }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { showDeletePersonDialog = person }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                  Text(
                                      text = String.format("৳%,.0f", netOutstanding),
                                      color = if (person.type == "LENT") IncomeGreen else CoralRed,
                                      fontWeight = FontWeight.Bold,
                                      fontSize = 18.sp
                                  )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
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
                            HorizontalDivider(color = Color(0x33FFFFFF))

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
                                HorizontalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Paid", fontSize = 10.sp, color = TextSecondaryDark)
                                    Text("৳${String.format("%,.0f", totalPaid)}", fontSize = 12.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
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
                                            val entryDate = formatDate(entry.date, "MMM dd, yyyy")
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
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, ElectricPurple, RoundedCornerShape(12.dp)).clickable { showPayEntrySheet = entry }.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("💸 Partial", fontSize = 10.sp, color = ElectricPurple, fontWeight = FontWeight.Bold)
                                                }
                                                Box(
                                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(IncomeGreen).clickable { showPayEntrySheet = entry }.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("✅ Full Pay", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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
        }
    }

    if (showAddPersonDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("LENT") }

        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            val keyController = LocalSoftwareKeyboardController.current
            val fManager = LocalFocusManager.current
            
            BackHandler {
                fManager.clearFocus()
                keyController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Person",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showAddPersonDialog = false },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)
                
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { type = "LENT" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "LENT") IncomeGreen else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = if (type != "LENT") BorderStroke(1.dp, Color(0x33FFFFFF)) else null
                    ) {
                        Text("📤 I Lent GET", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (type == "LENT") Color.Black else Color.White)
                    }
                    Button(
                        onClick = { type = "BORROWED" },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (type == "BORROWED") CoralRed else Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = if (type != "BORROWED") BorderStroke(1.dp, Color(0x33FFFFFF)) else null
                    ) {
                        Text("📥 I Borrow GIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (type == "BORROWED") Color.Black else Color.White)
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, 
                    label = { Text("Person Name", color = TextSecondaryDark, fontSize = 13.sp) }, 
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it }, 
                    label = { Text("Phone (Optional)", color = TextSecondaryDark, fontSize = 13.sp) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done), 
                    keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            viewModel.addDebtPerson(name, phone, type)
                        }
                        showAddPersonDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Add Person", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
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
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            val keyController = LocalSoftwareKeyboardController.current
            val fManager = LocalFocusManager.current
            
            BackHandler {
                fManager.clearFocus()
                keyController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Debt Entry",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showAddEntryDialogForPerson = null },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it }, 
                    label = { Text("Amount ৳", color = TextSecondaryDark, fontSize = 13.sp) }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), 
                    keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it }, 
                    label = { Text("Reason/Context", color = TextSecondaryDark, fontSize = 13.sp) }, 
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                var selectedWalletId by remember { mutableStateOf<Long?>(null) }
                
                // Wallet selector section
                Text("From/To Wallet (optional)", color = TextSecondaryDark, fontSize = 12.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { wallet ->
                        val isSelected = selectedWalletId == wallet.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) ElectricPurple else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { selectedWalletId = if (isSelected) null else wallet.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(wallet.icon, fontSize = 18.sp)
                                Text(wallet.name, color = Color.White, fontSize = 11.sp)
                                Text("৳${String.format("%.0f", wallet.balance)}", color = TextSecondaryDark, fontSize = 10.sp)
                            }
                        }
                    }
                }
                
                val person = debtPersons.firstOrNull { it.id == showAddEntryDialogForPerson }
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                if (selectedWalletId != null && person != null && amt > 0) {
                    val action = if (person.type == "LENT") "deducted from" else "added to"
                    Text(
                        "৳${amt} will be $action ${wallets.firstOrNull { it.id == selectedWalletId }?.name}",
                        color = Color(0xFF00D4AA),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showEntryDatePicker = true }
                        .padding(16.dp)
                ) {
                    Text("📅 ${formatDate(selectedEntryDate, "dd MMMM yyyy")}", color = Color.White)
                }
                
                if (showEntryDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = if (selectedEntryDate > 0) timestampToUtcMidnight(selectedEntryDate) else todayUtcMidnight())
                    DatePickerDialog(
                        onDismissRequest = { showEntryDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { selectedEntryDate = utcMidnightToLocalTimestamp(it) }
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
                        if (amt > 0 && reason.isNotEmpty()) {
                            viewModel.addDebtEntry(
                                personId = showAddEntryDialogForPerson!!,
                                amount = amt,
                                reason = reason,
                                date = selectedEntryDate,
                                walletId = selectedWalletId,
                                personType = person?.type ?: "LENT"
                            )
                        }
                        showAddEntryDialogForPerson = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Add Entry", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
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
                containerColor = Color(0xFF0F172A),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                }
            ) {
                val keyController = LocalSoftwareKeyboardController.current
                val fManager = LocalFocusManager.current
                
                BackHandler {
                    fManager.clearFocus()
                    keyController?.hide()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sheet header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Record Payment — ${person.personName}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showPayOverallDialogForPerson = null },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                    Text("Outstanding to Settle: ৳${String.format("%,.0f", outstanding)}", color = TextSecondaryDark, fontSize = 14.sp)
                    
                    OutlinedTextField(
                        value = overallPayStr,
                        onValueChange = { 
                            overallPayStr = it
                            if (it.toDoubleOrNull() != outstanding) markSettled = false
                        },
                        label = { Text("Amount ৳", color = TextSecondaryDark, fontSize = 13.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ElectricPurple,
                            focusedLabelColor = ElectricPurple,
                            unfocusedLabelColor = TextSecondaryDark,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = markSettled,
                            onCheckedChange = { 
                                markSettled = it
                                if (it) overallPayStr = outstanding.toString()
                            },
                            colors = CheckboxDefaults.colors(checkedColor = ElectricPurple)
                        )
                        Text("Settle Fully", color = Color.White)
                    }

                    Text(
                        "Select Wallet",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(wallets) { w ->
                            val isSelected = selectedWalletId == w.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) ElectricPurple else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { 
                                        selectedWalletId = w.id 
                                        errorMsg = null
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("${w.name} (৳${String.format("%,.0f", w.balance)})", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Confirm Payment", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    val currentPayEntry = showPayEntrySheet
    if (currentPayEntry != null) {
        val entry = currentPayEntry
        val outstanding = entry.amount - entry.paidAmount
        var payAmount by remember(entry.id) { mutableStateOf(outstanding.toString()) }
        var selectedWalletId by remember { mutableStateOf<Long?>(null) }

        ModalBottomSheet(
            onDismissRequest = { showPayEntrySheet = null },
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            val keyController = LocalSoftwareKeyboardController.current
            val fManager = LocalFocusManager.current
            
            BackHandler {
                fManager.clearFocus()
                keyController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Record Payment",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showPayEntrySheet = null },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                Text("Outstanding: ৳${String.format("%.2f", outstanding)}", color = TextSecondaryDark, fontSize = 14.sp)
                Text("Reason: ${entry.reason}", color = TextSecondaryDark, fontSize = 13.sp)

                // Amount field
                OutlinedTextField(
                    value = payAmount,
                    onValueChange = { payAmount = it },
                    label = { Text("Amount", color = TextSecondaryDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Full", "Half", "Custom").forEach { label ->
                        val amt = when(label) {
                            "Full" -> outstanding
                            "Half" -> outstanding / 2
                            else -> null
                        }
                        OutlinedButton(
                            onClick = { if (amt != null) payAmount = String.format("%.2f", amt) },
                            border = BorderStroke(1.dp, ElectricPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(label, color = ElectricPurple, fontSize = 11.sp)
                        }
                    }
                }

                // Wallet selector
                Text(
                    "Pay from Wallet",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { wallet ->
                        val isSelected = selectedWalletId == wallet.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(1.dp, if (isSelected) ElectricPurple else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { selectedWalletId = wallet.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(wallet.icon, fontSize = 18.sp)
                                Text(wallet.name, color = Color.White, fontSize = 11.sp)
                                Text("৳${String.format("%.0f", wallet.balance)}", color = TextSecondaryDark, fontSize = 10.sp)
                            }
                        }
                    }
                }

                if (selectedWalletId == null) {
                    Text("⚠️ Please select a wallet", color = CoralRed, fontSize = 12.sp)
                }

                // Confirm button
                Button(
                    onClick = {
                        val amt = payAmount.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && selectedWalletId != null) {
                            val person = debtPersons.firstOrNull { it.id == entry.personId }
                            viewModel.payEntryAmountWithWallet(entry, amt, selectedWalletId!!, person?.type ?: "BORROWED")
                            val wName = wallets.firstOrNull { it.id == selectedWalletId }?.name ?: "Wallet"
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Payment recorded from $wName")
                            }
                            showPayEntrySheet = null
                        }
                    },
                    enabled = selectedWalletId != null && (payAmount.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Confirm Payment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }
    
    showEditPersonSheet?.let { person ->
        var editName by remember { mutableStateOf(person.personName) }
        var editPhone by remember { mutableStateOf(person.phoneNumber) }
        var editType by remember { mutableStateOf(person.type) }
    
        ModalBottomSheet(
            onDismissRequest = { showEditPersonSheet = null },
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("✏️ Edit Person", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = editName, onValueChange = { editName = it }, 
                    label = { Text("Name", color = TextSecondaryDark) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple, unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
                OutlinedTextField(
                    value = editPhone, onValueChange = { editPhone = it }, 
                    label = { Text("Phone (optional)", color = TextSecondaryDark) }, 
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple, unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF1E293B), unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )
                
                Text("Type", color = TextSecondaryDark, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("LENT" to "📤 I Lent", "BORROWED" to "📥 I Borrowed").forEach { (value, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (editType == value)
                                        if (value == "LENT") Color(0x2200D4AA) else Color(0x22FF6B6B)
                                    else Color(0xFF1E293B)
                                )
                                .border(
                                    1.5.dp,
                                    if (editType == value)
                                        if (value == "LENT") IncomeGreen else CoralRed
                                    else Color(0x22FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { editType = value }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (editType == value) Color.White else TextSecondaryDark,
                                fontSize = 13.sp,
                                fontWeight = if (editType == value) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.updateDebtPerson(
                            person.copy(
                                personName = editName,
                                phoneNumber = editPhone,
                                type = editType
                            )
                        )
                        showEditPersonSheet = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) { Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    
    showDeletePersonDialog?.let { person ->
        AlertDialog(
            onDismissRequest = { showDeletePersonDialog = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Delete ${person.personName}?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will delete the person and ALL their debt entries permanently. This cannot be undone.", color = TextSecondaryDark) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebtPerson(person)
                        showDeletePersonDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePersonDialog = null }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    val monthName = formatDate(tempCal.timeInMillis, "MMMM")

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
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selectedCalTab == 2) ElectricPurple else Color.Transparent).clickable { selectedCalTab = 2 }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🔁 Recurring", color = if (selectedCalTab == 2) Color.White else TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Color.White)
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
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = if (isCurrentMonth) Color.Gray else Color.White)
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
                        HorizontalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0x33FFFFFF))
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
                                    val timeStr = formatDate(tx.date, "hh:mm a")
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
        } else if (selectedCalTab == 1) {
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
                val daysRemainingRaw = ((bill.dueDate - System.currentTimeMillis()) / 86400000L).toInt()
                val isBillOverdue = daysRemainingRaw < 0 && !isPaidThisMonth
                val daysRemaining = daysRemainingRaw.coerceAtLeast(0).toInt()
                val overdueDays = if (isBillOverdue) (-daysRemainingRaw) else 0
                val urgencyColor = when {
                    isPaidThisMonth -> IncomeGreen
                    isBillOverdue -> CoralRed
                    daysRemaining <= 3 -> CoralRed
                    daysRemaining <= 7 -> Color(0xFFFFD600)
                    else -> IncomeGreen
                }

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
                            val dateStr = formatDate(bill.dueDate, "dd MMM yyyy")
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
                                    when {
                                        isPaidThisMonth -> Text(
                                            "✅ Paid",
                                            color = IncomeGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        isBillOverdue -> Text(
                                            "🔴 $overdueDays days overdue",
                                            color = CoralRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        daysRemaining == 0 -> Text(
                                            "⚠️ Due Today!",
                                            color = CoralRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        else -> Text(
                                            "In $daysRemaining days",
                                            color = urgencyColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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
        } else if (selectedCalTab == 2) {
            RecurringSubSection(viewModel)
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
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDueDate?.let { timestampToUtcMidnight(it) } ?: todayUtcMidnight())
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedDueDate = datePickerState.selectedDateMillis?.let { utcMidnightToLocalTimestamp(it) }
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
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                )
            }
        ) {
            val keyController = LocalSoftwareKeyboardController.current
            val fManager = LocalFocusManager.current
            
            BackHandler {
                fManager.clearFocus()
                keyController?.hide()
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (billToEdit != null) "Edit Bill" else "Add Bill",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { 
                            showBillSheet = false
                            billToEdit = null
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                OutlinedTextField(
                    value = billName,
                    onValueChange = { billName = it },
                    label = { Text("Bill Name", color = TextSecondaryDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { fManager.moveFocus(FocusDirection.Down) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it },
                    label = { Text("Amount ৳", color = TextSecondaryDark, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricPurple,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = ElectricPurple,
                        focusedLabelColor = ElectricPurple,
                        unfocusedLabelColor = TextSecondaryDark,
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateText = selectedDueDate?.let {
                        formatDate(it, "dd MMM yyyy")
                    } ?: "Select Due Date"
                    Text(dateText, color = if (selectedDueDate != null) Color.White else TextSecondaryDark, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = ElectricPurple)
                }

                Text(
                    "Select Wallet",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { w ->
                        val isSelected = selectedWalletId == w.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ElectricPurple else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedWalletId = w.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(w.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text(
                    "Select Category",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { c ->
                        val isSelected = selectedCategoryId == c.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) ElectricPurple else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategoryId = c.id }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text("${c.icon} ${c.name}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Button(
                    onClick = {
                        val amt = billAmount.toDoubleOrNull() ?: 0.0
                        if (billName.isNotEmpty() && amt > 0 && selectedDueDate != null && selectedWalletId != null && selectedCategoryId != null) {
                            if (billToEdit != null) {
                                viewModel.updateBill(
                                    billToEdit!!.copy(
                                        name = billName,
                                        amount = amt,
                                        dueDate = selectedDueDate!!,
                                        categoryId = selectedCategoryId!!,
                                        walletId = selectedWalletId!!
                                    )
                                )
                            } else {
                                viewModel.addBill(billName, amt, selectedDueDate!!, selectedCategoryId!!, selectedWalletId!!)
                            }
                            showBillSheet = false
                            billToEdit = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Save Bill", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureSettingsSubSection(viewModel: FinanceViewModel) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val currentCurrency by viewModel.primaryCurrency.collectAsState()
    val isNotifEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentUserName by viewModel.userName.collectAsState()
    val categories by viewModel.categories.collectAsState()
    
    val pinEnabled by viewModel.pinEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    var showPinSetupSheet by remember { mutableStateOf(false) }
    var showCategoryManagerSheet by remember { mutableStateOf(false) }

    val appTheme by viewModel.appTheme.collectAsState()

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
        
        // MANAGE CATEGORIES
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCategoryManagerSheet = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x336C63FF)),
                        contentAlignment = Alignment.Center
                    ) { Text("🏷️", fontSize = 20.sp) }
                    Column {
                        Text("Manage Categories", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Edit or delete expense/income categories", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondaryDark)
            }
        }
        
        if (showCategoryManagerSheet) {
            var showDeleteConfirm by remember { mutableStateOf<com.example.data.local.entities.CategoryEntity?>(null) }
            
            ModalBottomSheet(
                onDismissRequest = { showCategoryManagerSheet = false },
                containerColor = Color(0xFF0F172A),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                val keyboardControllerLocal = LocalSoftwareKeyboardController.current
                val focusManagerLocal = LocalFocusManager.current
                BackHandler { focusManagerLocal.clearFocus(); keyboardControllerLocal?.hide() }
        
                var editingCategory by remember { mutableStateOf<com.example.data.local.entities.CategoryEntity?>(null) }
                var editName by remember { mutableStateOf("") }
                var editIcon by remember { mutableStateOf("") }
                val iconOptions = listOf("🍔","🚗","💊","🏠","📱","🎮","✈️","👗","💅","🎓","💡","🛒","💰","🎁","🏋️","🎵","🐾","⚽","🍕","☕")
        
                Column(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("🏷️ Categories", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showCategoryManagerSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondaryDark)
                        }
                    }
                    HorizontalDivider(color = Color(0x1AFFFFFF))
        
                    // Category list
                    categories.forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.icon, fontSize = 22.sp)
                                Column {
                                    Text(cat.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    Text(cat.type, color = TextSecondaryDark, fontSize = 11.sp)
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    editingCategory = cat
                                    editName = cat.name
                                    editIcon = cat.icon
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ElectricPurple, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { showDeleteConfirm = cat }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
        
                    // Inline edit fields
                    editingCategory?.let { cat ->
                        HorizontalDivider(color = Color(0x1AFFFFFF))
                        Text("Editing: ${cat.name}", color = ElectricPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Category Name", color = TextSecondaryDark) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricPurple,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
        
                        Text("Icon", color = TextSecondaryDark, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(iconOptions) { icon ->
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                        .background(if (editIcon == icon) Color(0x336C63FF) else Color(0xFF1E293B))
                                        .border(1.dp, if (editIcon == icon) ElectricPurple else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable { editIcon = icon },
                                    contentAlignment = Alignment.Center
                                ) { Text(icon, fontSize = 18.sp) }
                            }
                        }
        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { editingCategory = null },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Cancel", color = TextSecondaryDark) }
                            Button(
                                onClick = {
                                    viewModel.editCategory(cat.copy(name = editName, icon = editIcon))
                                    editingCategory = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                        }
                    }
        
                    Spacer(Modifier.height(16.dp))
                }
            }
        
            // Delete confirmation
            showDeleteConfirm?.let { cat ->
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    containerColor = Color(0xFF1E293B),
                    title = { Text("Delete \"${cat.name}\"?", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = { Text("Transactions with this category will be reassigned to the first available category.", color = TextSecondaryDark, fontSize = 13.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteCategory(cat, categories.firstOrNull { it.id != cat.id }?.id)
                                showDeleteConfirm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Delete", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) {
                            Text("Cancel", color = TextSecondaryDark)
                        }
                    }
                )
            }
        }
        
        // SECURITY / PIN
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF111827))) {
            Row(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(ElectricPurple)) }
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (pinEnabled) "🔒" else "🔓", fontSize = 20.sp)
                        Column {
                            Text("App PIN Lock", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (pinEnabled) "PIN is enabled" else "PIN is disabled", color = TextSecondaryDark, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                // Turning OFF — clear PIN and disable
                                viewModel.updateSecurityPin("")
                                viewModel.setPinEnabled(false)
                            } else {
                                // Turning ON — show setup sheet, but DON'T enable yet
                                // PIN will be enabled only after successful save
                                showPinSetupSheet = true
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricPurple)
                    )
                }
                if (pinEnabled) {
                    OutlinedButton(
                        onClick = { showPinSetupSheet = true },
                        border = BorderStroke(1.dp, ElectricPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Change PIN", color = ElectricPurple, fontSize = 13.sp) }
                }
            }
        }

        if (showPinSetupSheet) {
            var pinInput by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }
            var pinError by remember { mutableStateOf<String?>(null) }

            ModalBottomSheet(
                onDismissRequest = { showPinSetupSheet = false },
                containerColor = Color(0xFF0F172A),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("🔒 Set PIN", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Enter a 4-digit PIN to lock your app", color = TextSecondaryDark, fontSize = 13.sp)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("Enter PIN", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple, unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Confirm PIN", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple, unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    if (pinError != null) {
                        Text(pinError!!, color = CoralRed, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            when {
                                pinInput.length != 4 -> pinError = "PIN must be 4 digits"
                                pinInput != confirmPin -> pinError = "PINs do not match"
                                else -> {
                                    viewModel.updateSecurityPin(pinInput)
                                    // Also explicitly enable PIN after saving
                                    viewModel.setPinEnabled(true)
                                    showPinSetupSheet = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                    ) { Text("Save PIN", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // APP THEME
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x33FFD700)), contentAlignment = Alignment.Center) {
                        Text("🎨", fontSize = 20.sp)
                    }
                    Text("App Theme", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DARK" to "🌑 Dark", "LIGHT" to "☀️ Light", "SYSTEM" to "📱 System").forEach { (value, label) ->
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (appTheme == value) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(1.5.dp, if (appTheme == value) ElectricPurple else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { viewModel.setAppTheme(value) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (appTheme == value) ElectricPurple else TextSecondaryDark, fontSize = 12.sp, fontWeight = if (appTheme == value) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
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

        // Show dialog inline in ToolsTab when update found via manual check
        updateState?.let { info ->
            UpdateAvailableDialog(
                info = info,
                onDownload = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                    context.startActivity(intent)
                    updateScope.launch {
                        viewModel.markUpdateAsSeen(context, info.apkUpdatedAt)
                    }
                    viewModel.dismissUpdate()
                },
                onDismiss = { viewModel.dismissUpdate() }
            )
        }

        Button(
            onClick = {
                isCheckingUpdate = true
                noUpdateFound = false
                updateScope.launch {
                    val info = UpdateChecker.checkForUpdate(context)
                    isCheckingUpdate = false
                    if (info.hasUpdate) {
                        // Directly set the update info in ViewModel — don't call checkForUpdates again
                        viewModel.setUpdateInfo(info)
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
                HorizontalDivider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 8.dp))
                val pkgContext = LocalContext.current
                val appVersionName = remember(pkgContext) {
                    try {
                        pkgContext.packageManager.getPackageInfo(pkgContext.packageName, 0).versionName ?: "1.0"
                    } catch (e: Exception) { "1.0" }
                }
                Text("Version $appVersionName", color = TextSecondaryDark, fontSize = 11.sp)
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
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
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
                containerColor = Color(0xFF0F172A),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                }
            ) {
                val keyController = LocalSoftwareKeyboardController.current
                val fManager = LocalFocusManager.current
                
                BackHandler {
                    fManager.clearFocus()
                    keyController?.hide()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sheet header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Select Base Currency",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showCurrencyDialog = false },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                    Text("Your entire app will display totals in this currency.", color = TextSecondaryDark, fontSize = 13.sp)
                    
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
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) ElectricPurple else Color(0xFF1E293B)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (!isSelected) BorderStroke(1.dp, Color(0x22FFFFFF)) else null
                                ) {
                                    Text(cur, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showNameDialog) {
            var tempName by remember { mutableStateOf(currentUserName) }
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showNameDialog = false },
                containerColor = Color(0xFF0F172A),
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 4.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x33FFFFFF))
                    )
                }
            ) {
                val keyController = LocalSoftwareKeyboardController.current
                val fManager = LocalFocusManager.current
                
                BackHandler {
                    fManager.clearFocus()
                    keyController?.hide()
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sheet Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Update Profile Name",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showNameDialog = false },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                    Text("Introduce yourself for a personalized homepage dashboard greeting.", color = TextSecondaryDark, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        placeholder = { Text("Enter your name", color = TextSecondaryDark) },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { fManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = ElectricPurple,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Button(
                        onClick = {
                            viewModel.updateUserName(tempName.trim())
                            showNameDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Profile", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringSubSection(viewModel: FinanceViewModel) {
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔁 Recurring", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${recurringTransactions.count { it.isActive }} active", color = TextSecondaryDark, fontSize = 12.sp)
        }

        if (recurringTransactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔁", fontSize = 48.sp)
                    Text("No recurring transactions", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Add a transaction and toggle 'Recurring' to see it here", color = TextSecondaryDark, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            recurringTransactions.forEach { rec ->
                val category = categories.firstOrNull { it.id == rec.categoryId }
                val wallet = wallets.firstOrNull { it.id == rec.walletId }

                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        containerColor = Color(0xFF1E293B),
                        title = { Text("Stop Recurring?", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = { Text("This will permanently delete this recurring transaction. It will no longer auto-process.", color = TextSecondaryDark) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteRecurringTransaction(rec)
                                    showDeleteConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Stop & Delete", color = Color.White) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("Cancel", color = TextSecondaryDark)
                            }
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111827))
                ) {
                    Row(modifier = Modifier.matchParentSize()) {
                        Box(
                            modifier = Modifier.width(4.dp).fillMaxHeight()
                                .background(if (rec.isActive) ElectricPurple else Color(0x33FFFFFF))
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x336C63FF)),
                                contentAlignment = Alignment.Center
                            ) { Text(category?.icon ?: "🔁", fontSize = 20.sp) }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    rec.note.ifBlank { category?.name ?: "Recurring" },
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${rec.frequency} • ${wallet?.name ?: ""}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "Next: ${formatDate(rec.nextDueDate)}",
                                    color = Color(0xFF6C63FF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${if (rec.type == "EXPENSE") "-" else "+"}৳${String.format("%,.0f", rec.amount)}",
                                color = if (rec.type == "EXPENSE") CoralRed else IncomeGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Toggle active/inactive
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (rec.isActive) Color(0x2200D4AA) else Color(0x22FFFFFF))
                                        .clickable { viewModel.toggleRecurringActive(rec) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (rec.isActive) "Active" else "Paused",
                                        color = if (rec.isActive) IncomeGreen else TextSecondaryDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // Delete button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FF6B6B))
                                        .clickable { showDeleteConfirm = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Stop", color = CoralRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
