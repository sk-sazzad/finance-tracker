package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.CategoryEntity
import com.example.data.local.entities.WalletEntity
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsTab(
    viewModel: FinanceViewModel
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val transactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.filterType.collectAsState()
    
    val selectedWalletIds by viewModel.selectedWalletIds.collectAsState()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsState()
    val minAmount by viewModel.minAmount.collectAsState()
    val maxAmount by viewModel.maxAmount.collectAsState()
    val startDate by viewModel.startDate.collectAsState()
    val endDate by viewModel.endDate.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddCategoryDialogType by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = ElectricPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- TITLE ---
            Text(
                text = "My Ledger",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholder = { Text("Search category, tags, or notes...", color = TextSecondaryDark) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF111827),
                    focusedBorderColor = ElectricPurple,
                    unfocusedBorderColor = Color(0x33FFFFFF)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // --- ADVANCED FILTERS ---
            var showFilters by remember { mutableStateOf(false) }
            val activeFilterCount = (if(selectedWalletIds.isNotEmpty()) 1 else 0) +
                                    (if(selectedCategoryIds.isNotEmpty()) 1 else 0) +
                                    (if(minAmount != null || maxAmount != null) 1 else 0) +
                                    (if(startDate != null || endDate != null) 1 else 0)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { showFilters = !showFilters },
                    colors = ButtonDefaults.buttonColors(containerColor = if (showFilters || activeFilterCount > 0) Color(0xFF1E293B) else Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🔽 Filters" + if(activeFilterCount > 0) " ($activeFilterCount)" else "", color = if (activeFilterCount > 0) ElectricPurple else Color.White, fontSize = 13.sp)
                }
                
                if (activeFilterCount > 0) {
                    TextButton(onClick = { viewModel.clearAllFilters(); showFilters = false }, contentPadding = PaddingValues(0.dp)) {
                        Text("Clear All", color = CoralRed, fontSize = 13.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showFilters) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF111827)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date Range
                    var showStartDatePicker by remember { mutableStateOf(false) }
                    var showEndDatePicker by remember { mutableStateOf(false) }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Date Range", fontSize = 12.sp, color = TextSecondaryDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B)).clickable { showStartDatePicker = true }.padding(12.dp)) {
                                Text(if (startDate != null) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(startDate!!)) else "From Date", color = Color.White, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B)).clickable { showEndDatePicker = true }.padding(12.dp)) {
                                Text(if (endDate != null) SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(endDate!!)) else "To Date", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }

                    if (showStartDatePicker) {
                        val state = rememberDatePickerState(initialSelectedDateMillis = startDate ?: System.currentTimeMillis())
                        DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = { TextButton(onClick = { viewModel.setDateRange(state.selectedDateMillis, endDate); showStartDatePicker = false }) { Text("OK") } }) {
                            DatePicker(state)
                        }
                    }
                    if (showEndDatePicker) {
                        val state = rememberDatePickerState(initialSelectedDateMillis = endDate ?: System.currentTimeMillis())
                        DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = { TextButton(onClick = { viewModel.setDateRange(startDate, state.selectedDateMillis); showEndDatePicker = false }) { Text("OK") } }) {
                            DatePicker(state)
                        }
                    }

                    // Amount Range
                    var minAmtStr by remember { mutableStateOf(minAmount?.toString() ?: "") }
                    var maxAmtStr by remember { mutableStateOf(maxAmount?.toString() ?: "") }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Amount Range", fontSize = 12.sp, color = TextSecondaryDark)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = minAmtStr, onValueChange = { minAmtStr = it }, modifier = Modifier.weight(1f), label = { Text("Min ৳", fontSize=10.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White))
                            OutlinedTextField(value = maxAmtStr, onValueChange = { maxAmtStr = it }, modifier = Modifier.weight(1f), label = { Text("Max ৳", fontSize=10.sp) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { viewModel.setAmountRange(minAmtStr.toDoubleOrNull(), maxAmtStr.toDoubleOrNull()); focusManager.clearFocus() }), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color.White))
                            Button(onClick = { viewModel.setAmountRange(minAmtStr.toDoubleOrNull(), maxAmtStr.toDoubleOrNull()) }, colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("Apply", fontSize = 12.sp)
                            }
                        }
                    }

                    // Wallet Filter
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Wallet", fontSize = 12.sp, color = TextSecondaryDark)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(wallets) { w ->
                                val sel = selectedWalletIds.contains(w.id)
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if(sel) Color(0x336C63FF) else Color(0xFF1E293B)).border(1.dp, if(sel) ElectricPurple else Color.Transparent, RoundedCornerShape(8.dp)).clickable { viewModel.toggleWalletFilter(w.id) }.padding(horizontal=10.dp, vertical=6.dp)) {
                                    Text("${w.icon} ${w.name}", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Category Filter
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Category", fontSize = 12.sp, color = TextSecondaryDark)
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { c ->
                                val sel = selectedCategoryIds.contains(c.id)
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if(sel) Color(0x336C63FF) else Color(0xFF1E293B)).border(1.dp, if(sel) ElectricPurple else Color.Transparent, RoundedCornerShape(8.dp)).clickable { viewModel.toggleCategoryFilter(c.id) }.padding(horizontal=10.dp, vertical=6.dp)) {
                                    Text("${c.icon} ${c.name}", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- FILTER CHIPS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Brush.horizontalGradient(listOf(GradientPurpleStart, GradientPurpleEnd))
                                else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                            )
                            .clickable { viewModel.setFilterType(type) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // --- TRANSACTION LISTING ---
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔍", fontSize = 48.sp)
                        Text("No match found", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Clear search filter or log your first transaction", fontSize = 11.sp, color = TextSecondaryDark)
                        Button(
                            onClick = { viewModel.clearAllFilters() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF))
                        ) {
                            Text("Clear Filters", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Group and list transactions based on Date Headers
                    val grouped = transactions.groupBy {
                        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                        val today = Calendar.getInstance()
                        val diffDays = (today.timeInMillis - cal.timeInMillis)/(1000*3600*24)
                        when {
                            diffDays == 0L -> "Today"
                            diffDays == 1L -> "Yesterday"
                            else -> SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(it.date))
                        }
                    }

                    grouped.forEach { (dateHeader, itemsList) ->
                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0A0E1A))
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPurple
                                )
                                val dailyInc = itemsList.filter { it.type == "INCOME" }.sumOf { it.amount }
                                val dailyExp = itemsList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (dailyExp > 0) Text("-৳${String.format("%,.0f", dailyExp)}", color = CoralRed, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                    if (dailyInc > 0) Text("+৳${String.format("%,.0f", dailyInc)}", color = IncomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        items(itemsList) { tx ->
                            val category = categories.firstOrNull { it.id == tx.categoryId }
                            val wallet = wallets.firstOrNull { it.id == tx.walletId }
                            
                            SwipeableTransactionRow(
                                onDelete = { viewModel.deleteTransaction(tx) },
                                onEdit = { transactionToEdit = tx }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF111827))
                                        .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                        .clickable { transactionToEdit = tx },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val accentColor = when (tx.type) {
                                            "INCOME" -> IncomeGreen
                                            "EXPENSE" -> CoralRed
                                            else -> Color(0xFF0EA5E9)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(accentColor)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(10.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    category?.color?.let { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.2f) }
                                                        ?: Color(0x33FFFFFF)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (tx.type == "TRANSFER") "↔" else category?.icon ?: "💸", fontSize = 20.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                            Text(
                                                text = if (tx.note.isNotBlank()) tx.note else (category?.name ?: "Transfer"),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${wallet?.name ?: "Cash"} • ${SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(Date(tx.date))}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryDark
                                                )
                                                if (tx.type == "TRANSFER") {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0x330EA5E9))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("↔ Transfer", fontSize = 8.sp, color = Color(0xFF0EA5E9))
                                                    }
                                                }
                                            }
                                            if (tx.tags.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    tx.tags.split(",").forEach { tag ->
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0x1A00D4AA))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(tag.trim(), fontSize = 8.sp, color = MintGreen)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            val amountText = when(tx.type) {
                                                "INCOME" -> "+৳%,.0f".format(tx.amount)
                                                "EXPENSE" -> "-৳%,.0f".format(tx.amount)
                                                else -> "৳%,.0f".format(tx.amount)
                                            }
                                            val amtColor = when(tx.type) {
                                                "INCOME" -> IncomeGreen
                                                "EXPENSE" -> CoralRed
                                                else -> Color(0xFF0EA5E9)
                                            }
                                            Text(
                                                text = amountText,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = amtColor
                                            )
                                            Text(
                                                text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.date)),
                                                fontSize = 10.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Edit", tint = Color(0x80FFFFFF), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD/EDIT TRANSACTION BOTTOM SHEET ---
    if (showAddSheet || transactionToEdit != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddSheet = false
                transactionToEdit = null
            },
            containerColor = Color(0xFF0F172A),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            AddTransactionSheetContent(
                categories = categories,
                wallets = wallets,
                transactionToEdit = transactionToEdit,
                onSave = { amount, type, categoryId, walletId, toWalletId, date, isRecurring, note, tags ->
                    if (transactionToEdit != null) {
                        viewModel.updateTransaction(
                            newTx = transactionToEdit!!.copy(
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                walletId = walletId,
                                toWalletId = toWalletId,
                                date = date,
                                note = note,
                                tags = tags,
                                isRecurring = isRecurring
                            ),
                            oldTx = transactionToEdit!!
                        )
                    } else {
                        viewModel.addTransaction(
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            walletId = walletId,
                            toWalletId = toWalletId,
                            date = date,
                            note = note,
                            tags = tags,
                            isRecurring = isRecurring
                        )
                    }
                    showAddSheet = false
                    transactionToEdit = null
                },
                onAddCategory = { type ->
                    showAddCategoryDialogType = type
                }
            )
        }
    }

    if (showAddCategoryDialogType != null) {
        var catName by remember { mutableStateOf("") }
        var catEmoji by remember { mutableStateOf("📦") }
        
        AlertDialog(
            onDismissRequest = { showAddCategoryDialogType = null },
            title = { Text("Add Custom Category", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Type: ${showAddCategoryDialogType}", color = TextSecondaryDark, fontSize = 12.sp)
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                    OutlinedTextField(
                        value = catEmoji,
                        onValueChange = { catEmoji = it },
                        label = { Text("Emoji/Icon", color = TextSecondaryDark) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotEmpty() && catEmoji.isNotEmpty()) {
                            viewModel.addCategory(
                                name = catName,
                                icon = catEmoji,
                                color = "#8B5CF6", // default brand color
                                type = showAddCategoryDialogType!!
                            )
                            showAddCategoryDialogType = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialogType = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionRow(
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
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CoralRed),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                }
            } else if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricPurple),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.padding(start = 16.dp))
                }
            }
        },
        content = {
            content()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheetContent(
    categories: List<CategoryEntity>,
    wallets: List<WalletEntity>,
    transactionToEdit: TransactionEntity? = null,
    onSave: (amount: Double, type: String, categoryId: Long, walletId: Long, toWalletId: Long?, date: Long, isRecurring: Boolean, note: String, tags: String) -> Unit,
    onAddCategory: (type: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") } // "EXPENSE" or "INCOME" or "TRANSFER"
    var selectedCategoryId by remember { mutableStateOf<Long?>(transactionToEdit?.categoryId) }
    var selectedWalletId by remember { mutableStateOf<Long?>(transactionToEdit?.walletId) }
    var selectedToWalletId by remember { mutableStateOf<Long?>(transactionToEdit?.toWalletId) }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var tagsInput by remember { mutableStateOf(transactionToEdit?.tags ?: "") }
    var isRecurring by remember { mutableStateOf(transactionToEdit?.isRecurring ?: false) }
    var selectedDate by remember { mutableStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var showCalculator by remember { mutableStateOf(false) }

    val filteredCats = categories.filter { it.type == "ALL" || it.type == selectedType }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (transactionToEdit != null) "Edit Statement" else "New Statement",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        // --- TYPE TABS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { selectedType = "EXPENSE"; validationError = null },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") CoralRed else Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Expense", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { selectedType = "INCOME"; validationError = null },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "INCOME") IncomeGreen else Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Income", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { selectedType = "TRANSFER"; validationError = null },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "TRANSFER") Color(0xFF0EA5E9) else Color(0xFF1E293B)
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // --- AMOUNT INPUT & CALC ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                modifier = Modifier.weight(1f),
                label = { Text("Amount (৳)", color = TextSecondaryDark) },
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
                shape = RoundedCornerShape(12.dp)
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .clickable { showCalculator = !showCalculator },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator Mode",
                    tint = ElectricPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showCalculator) {
            CustomCalculatorNumpad(
                onValueChange = { amountStr = it },
                onSubmit = { showCalculator = false }
            )
        }

        // --- DATE SELECTOR ---
        var showDatePickerDialog by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF111827))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .clickable { showDatePickerDialog = true }
                .padding(16.dp)
        ) {
            Text("📅 ${SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))}", color = Color.White)
        }
        
        if (showDatePickerDialog) {
            val dateState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { selectedDate = it }
                        showDatePickerDialog = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(dateState)
            }
        }

        if (selectedType != "TRANSFER") {
            // --- CATEGORIES GRID selector ---
            Text("Category", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredCats.forEach { cat ->
                val isSelected = selectedCategoryId == cat.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) ElectricPurple
                            else Color(0xFF111827)
                        )
                        .border(1.dp, if (isSelected) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { selectedCategoryId = cat.id }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.icon)
                        Text(cat.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            // Add custom category button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onAddCategory(selectedType) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Add Custom", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        }

        // --- WALLET SELECTOR ---
        Text(if (selectedType == "TRANSFER") "From Wallet" else "Paying Method / Source", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            wallets.forEach { w ->
                val isSelected = selectedWalletId == w.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MintGreen
                            else Color(0xFF111827)
                        )
                        .border(1.dp, if (isSelected) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .clickable { selectedWalletId = w.id }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "${w.icon} ${w.name}",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (selectedType == "TRANSFER") {
            Text("To Wallet", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                wallets.forEach { w ->
                    val isSelected = selectedToWalletId == w.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF0EA5E9) else Color(0xFF111827))
                            .border(1.dp, if (isSelected) Color.Transparent else Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                            .clickable {
                                selectedToWalletId = w.id
                                validationError = null
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "${w.icon} ${w.name}",
                            color = if (isSelected) Color.White else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        if (validationError != null) {
            Text(validationError!!, color = CoralRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        // --- NOTES FIELD ---
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note / Description", color = TextSecondaryDark) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ElectricPurple
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 2
        )

        // --- TAGS INPUT ---
        OutlinedTextField(
            value = tagsInput,
            onValueChange = { tagsInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tags (comma separated e.g. food,lunch)", color = TextSecondaryDark) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = ElectricPurple
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // --- RECURRING SWITCH ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recurring Transaction", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Process automatically each month", color = TextSecondaryDark, fontSize = 10.sp)
            }
            Switch(
                checked = isRecurring,
                onCheckedChange = { isRecurring = it },
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricPurple)
            )
        }

        // --- SAVE BUTTON ---
        Button(
            onClick = {
                val amt = amountStr.toDoubleOrNull() ?: 0.0
                val catId = selectedCategoryId ?: (if (filteredCats.isNotEmpty()) filteredCats[0].id else -1L)
                val walId = selectedWalletId ?: (if (wallets.isNotEmpty()) wallets[0].id else -1L)
                
                if (selectedType == "TRANSFER") {
                    if (selectedWalletId == null || selectedToWalletId == null) {
                        validationError = "Please select both wallets"
                        return@Button
                    }
                    if (selectedWalletId == selectedToWalletId) {
                        validationError = "Cannot transfer to the same wallet"
                        return@Button
                    }
                }

                if (amt > 0) {
                    onSave(amt, selectedType, catId, walId, selectedToWalletId, selectedDate, isRecurring, note, tagsInput)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "TRANSFER") Color(0xFF0EA5E9) else ElectricPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (transactionToEdit != null) "Update Ledger Statement" else "Save Ledger Statement", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
