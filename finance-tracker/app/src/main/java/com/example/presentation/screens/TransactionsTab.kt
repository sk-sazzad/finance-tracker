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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
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
    val transactions by viewModel.filteredTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val wallets by viewModel.wallets.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.filterType.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0A0E1A))
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricPurple
                                )
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
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF111827))
                                        .border(1.dp, Color(0x0FFFFFFF), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    category?.color?.let { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.15f) }
                                                        ?: Color(0x1AFFFFFF)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(category?.icon ?: "💸", fontSize = 18.sp)
                                        }

                                        Column {
                                            Text(
                                                text = tx.note.ifEmpty { category?.name ?: "Record" },
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = wallet?.name ?: "Cash",
                                                    fontSize = 11.sp,
                                                    color = TextSecondaryDark
                                                )
                                                if (tx.tags.isNotEmpty()) {
                                                    Text("•", fontSize = 11.sp, color = TextSecondaryDark)
                                                    tx.tags.split(",").forEach { tag ->
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0x1A00D4AA))
                                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(tag, fontSize = 8.sp, color = MintGreen)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (tx.type == "INCOME") String.format("+৳%,.0f", tx.amount) else String.format("-৳%,.0f", tx.amount),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.type == "INCOME") IncomeGreen else CoralRed
                                        )
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.date)),
                                            fontSize = 9.sp,
                                            color = TextSecondaryDark
                                        )
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
            containerColor = Color(0xFF0F172A)
        ) {
            AddTransactionSheetContent(
                categories = categories,
                wallets = wallets,
                transactionToEdit = transactionToEdit,
                onSave = { amount, type, categoryId, walletId, isRecurring, note, tags ->
                    if (transactionToEdit != null) {
                        viewModel.updateTransaction(
                            newTx = transactionToEdit!!.copy(
                                amount = amount,
                                type = type,
                                categoryId = categoryId,
                                walletId = walletId,
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
                            toWalletId = null,
                            date = System.currentTimeMillis(),
                            note = note,
                            tags = tags,
                            isRecurring = isRecurring
                        )
                    }
                    showAddSheet = false
                    transactionToEdit = null
                }
            )
        }
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
    onSave: (amount: Double, type: String, categoryId: Long, walletId: Long, isRecurring: Boolean, note: String, tags: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: "EXPENSE") } // "EXPENSE" or "INCOME"
    var selectedCategoryId by remember { mutableStateOf<Long?>(transactionToEdit?.categoryId) }
    var selectedWalletId by remember { mutableStateOf<Long?>(transactionToEdit?.walletId) }
    var note by remember { mutableStateOf(transactionToEdit?.note ?: "") }
    var tagsInput by remember { mutableStateOf(transactionToEdit?.tags ?: "") }
    var isRecurring by remember { mutableStateOf(transactionToEdit?.isRecurring ?: false) }

    var showCalculator by remember { mutableStateOf(false) }

    val filteredCats = categories.filter { it.type == "ALL" || it.type == selectedType }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                onClick = { selectedType = "EXPENSE" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") CoralRed else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Expense", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { selectedType = "INCOME" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "INCOME") IncomeGreen else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Income", color = Color.White, fontWeight = FontWeight.Bold)
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
        }

        // --- WALLET SELECTOR ---
        Text("Paying Method / Source", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
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

        // --- NOTES FIELD ---
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note / Description", color = TextSecondaryDark) },
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
                val catId = selectedCategoryId ?: (if (filteredCats.isNotEmpty()) filteredCats[0].id else 1L)
                val walId = selectedWalletId ?: (if (wallets.isNotEmpty()) wallets[0].id else 1L)
                if (amt > 0) {
                    onSave(amt, selectedType, catId, walId, isRecurring, note, tagsInput)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (transactionToEdit != null) "Update Ledger Statement" else "Save Ledger Statement", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
