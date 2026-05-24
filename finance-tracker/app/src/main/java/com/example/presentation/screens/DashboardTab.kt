package com.example.presentation.screens

import android.widget.Space
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import com.example.data.local.entities.*
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardTab(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val budgets by viewModel.monthlyBudgets.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val healthScore by viewModel.getFinancialHealthScore().collectAsState()
    val totals by viewModel.getThisMonthTotals().collectAsState()
    val streakCount by viewModel.streakCount.collectAsState()
    val currencySymbol by viewModel.primaryCurrency.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val scrollState = rememberScrollState()
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var walletToDelete by remember { mutableStateOf<WalletEntity?>(null) }
    var walletToEdit by remember { mutableStateOf<WalletEntity?>(null) }
    var isBalanceVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- TOP BAR: Greeting & Avatar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(GradientPurpleStart, GradientPurpleEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👦",
                        fontSize = 24.sp
                    )
                }
                Column {
                    val displayName = if (userName.isNullOrBlank()) "User" else userName
                    Text(
                        text = "Hello, $displayName!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Keep your finances simple",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
            }
        }

        // --- HERO CARD: Total Balance ---
        val totalBalance = wallets.filter { it.includeInTotal }.sumOf { it.balance }
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0x1F1E293B),
            borderColor = Color(0x2EFFFFFF)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Active Balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
                IconButton(
                    onClick = { isBalanceVisible = !isBalanceVisible },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Balance",
                        tint = TextSecondaryDark
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (isBalanceVisible) {
                AnimatedCountUpText(
                    targetValue = totalBalance,
                    currencySymbol = if (currencySymbol == "BDT") "৳" else currencySymbol
                )
            } else {
                Text(
                    text = "••••••",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Mini-Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0F00E676))
                        .border(1.dp, Color(0x1E00E676), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToTransactions() }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Income",
                            tint = IncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Monthly Income", fontSize = 10.sp, color = TextSecondaryDark)
                            if (isBalanceVisible) {
                                Text(
                                    String.format("৳%,.0f", totals.first),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            } else {
                                Text("••••••", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                        }
                    }
                }

                // Expense Mini-Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0FFFF6B6))
                        .border(1.dp, Color(0x1EFF6B6B), RoundedCornerShape(12.dp))
                        .clickable { onNavigateToTransactions() }
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Expense",
                            tint = CoralRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text("Monthly Spent", fontSize = 10.sp, color = TextSecondaryDark)
                            if (isBalanceVisible) {
                                Text(
                                    String.format("৳%,.0f", totals.second),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                            } else {
                                Text("••••••", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                            }
                        }
                    }
                }
            }
        }

        // --- STREAK / GAMIFICATION BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF9E2C), Color(0xFFFF4E50))))
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🔥",
                    fontSize = 32.sp
                )
                Column {
                    Text(
                        text = "Streak: $streakCount Days Active!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Log simple expenses daily to bulletproof your streaks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // --- FINANCIAL HEALTH WIDGET & DETAILS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1.1f),
                backgroundColor = Color(0x1F111827)
            ) {
                Text(
                    text = "Finance Health Score",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0x1AFFFFFF), style = Stroke(width = 6.dp.toPx()))
                            val specColor = when {
                                healthScore >= 80 -> IncomeGreen
                                healthScore >= 50 -> Color(0xFFFFB300)
                                else -> CoralRed
                            }
                            drawArc(
                                color = specColor,
                                startAngle = -90f,
                                sweepAngle = (healthScore / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$healthScore",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    Column {
                        val remark = when {
                            healthScore >= 80 -> "Excellent"
                            healthScore >= 50 -> "Good Standing"
                            else -> "Action Required"
                        }
                        val grade = when {
                            healthScore >= 90 -> "A+"
                            healthScore >= 80 -> "A"
                            healthScore >= 70 -> "B"
                            healthScore >= 50 -> "C"
                            else -> "D"
                        }
                        Text(remark, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Grade: $grade", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                }
            }

            // Quick Actions or Auto suggestions
            GlassCard(
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0x1F111827)
            ) {
                Text("Smart Tip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Tips",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (healthScore < 60) "Cut down eating out to increase score." else "Your savings rate looks pristine!",
                    fontSize = 11.sp,
                    color = TextSecondaryDark,
                    lineHeight = 14.sp
                )
            }
        }

        // --- HORIZONTAL WALLETS LIST ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${wallets.size} wallets",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Wallet",
                        tint = ElectricPurple,
                        modifier = Modifier.clickable { showAddWalletDialog = true }
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 12.dp)
            ) {
                items(wallets) { wallet ->
                    val colorBrush = when (wallet.name.lowercase()) {
                        "bkash" -> Brush.horizontalGradient(listOf(Color(0xFFE2125B), Color(0xFFFF4081)))
                        "nagad" -> Brush.horizontalGradient(listOf(Color(0xFFF74B26), Color(0xFFFF7A4F)))
                        "bank account" -> Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1E3A8A)))
                        else -> Brush.horizontalGradient(listOf(IncomeGreen, Color(0xFF00796B)))
                    }
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colorBrush)
                            .combinedClickable(
                                onClick = { walletToEdit = wallet },
                                onLongClick = { walletToDelete = wallet }
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = wallet.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(wallet.icon, fontSize = 16.sp)
                            }
                            Column {
                                Text("Balance", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                                Text(
                                    String.format("৳%,.0f", wallet.balance),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BUDGET IN-FOCUS (CATEGORY PROGRESSES) ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Budget Goals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            if (budgets.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No budget set for this month. Set up your categories inside Budget tab!", fontSize = 12.sp, color = TextSecondaryDark)
                }
            } else {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        budgets.take(3).forEach { budget ->
                            val category = categories.firstOrNull { it.id == budget.categoryId }
                            val totalSpentVal = transactions.filter { it.categoryId == budget.categoryId && it.type == "EXPENSE" }.sumOf { it.amount }
                            val pc = if (budget.amount > 0) (totalSpentVal / budget.amount).toFloat() else 0f
                            val progressColor = when {
                                pc >= 0.9f -> CoralRed
                                pc >= 0.7f -> Color(0xFFFFB300)
                                else -> IncomeGreen
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${category?.icon ?: "📁"} ${category?.name ?: "Category"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = String.format("৳%,.0f / ৳%,.0f", totalSpentVal, budget.amount),
                                        fontSize = 11.sp,
                                        color = progressColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = pc.coerceIn(0f, 1f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = progressColor,
                                    trackColor = Color(0x1AFFFFFF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- BILLS TIMELINE ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Bills Due Timeline",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    bills.take(3).forEach { bill ->
                        val daysRemaining = ((bill.dueDate - System.currentTimeMillis()) / 86400000L).toInt()
                        
                        val isPaidThisMonth = bill.lastPaid?.let {
                            val paidCal = Calendar.getInstance().apply { timeInMillis = it }
                            paidCal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
                        } ?: false

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
                                    Text("💡", fontSize = 16.sp)
                                }
                                Column {
                                    Text(bill.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(String.format("৳%,.0f", bill.amount), fontSize = 11.sp, color = TextSecondaryDark)
                                }
                            }

                            if (isPaidThisMonth) {
                                Text("Paid ✅", fontSize = 11.sp, color = IncomeGreen)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                daysRemaining <= 3 -> Color(0x33FF6B6B)
                                                daysRemaining <= 7 -> Color(0x33FFB300)
                                                else -> Color(0x3300E676)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            daysRemaining < 0 -> "Overdue"
                                            daysRemaining == 0 -> "Due Today"
                                            else -> "$daysRemaining days left"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            daysRemaining <= 3 -> CoralRed
                                            daysRemaining <= 7 -> Color(0xFFFFB300)
                                            else -> IncomeGreen
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- RECENT 5 TRANSACTIONS ---
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricPurple,
                    modifier = Modifier.clickable { onNavigateToTransactions() }
                )
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val recent = transactions.take(5)
                if (recent.isEmpty()) {
                    Text("No transactions logged yet.", fontSize = 12.sp, color = TextSecondaryDark)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        recent.forEach { tx ->
                            val category = categories.firstOrNull { it.id == tx.categoryId }
                            val wallet = wallets.firstOrNull { it.id == tx.walletId }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x1AFFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(category?.icon ?: "💸", fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(tx.note.ifEmpty { category?.name ?: "Expense" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${wallet?.name ?: "Cash"} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.date))}", fontSize = 10.sp, color = TextSecondaryDark)
                                    }
                                }

                                Text(
                                    text = if (tx.type == "INCOME") String.format("+৳%,.0f", tx.amount) else String.format("-৳%,.0f", tx.amount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "INCOME") IncomeGreen else CoralRed
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAddWalletDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        var walletName by remember { mutableStateOf("") }
        var walletIcon by remember { mutableStateOf("💳") }
        var initialBalance by remember { mutableStateOf("") }
        val emojis = listOf("💵", "💳", "📱", "🟠", "🏦", "💰")

        ModalBottomSheet(
            onDismissRequest = { showAddWalletDialog = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Wallet", style = MaterialTheme.typography.titleLarge, color = Color.White)

                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Wallet Name", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Icon", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (walletIcon == emoji) ElectricPurple else Color(0x1AFFFFFF))
                                .clickable { walletIcon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val bal = initialBalance.toDoubleOrNull() ?: 0.0
                        if (walletName.isNotEmpty()) {
                            viewModel.addWallet(walletName, walletIcon, "#6C63FF", bal, "BDT", true)
                        }
                        showAddWalletDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Save Wallet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (walletToDelete != null) {
        AlertDialog(
            onDismissRequest = { walletToDelete = null },
            title = { Text("Delete Wallet", color = Color.White) },
            text = { Text("Are you sure you want to delete '${walletToDelete?.name}'?", color = TextSecondaryDark) },
            confirmButton = {
                TextButton(onClick = {
                    walletToDelete?.let { viewModel.deleteWallet(it.id) }
                    walletToDelete = null
                }) {
                    Text("Delete", color = CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { walletToDelete = null }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    if (walletToEdit != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var walletName by remember { mutableStateOf(walletToEdit!!.name) }
        var walletIcon by remember { mutableStateOf(walletToEdit!!.icon) }
        var walletBalance by remember { mutableStateOf(walletToEdit!!.balance.toString()) }
        val emojis = listOf("💵", "💳", "📱", "🟠", "🏦", "💰")

        ModalBottomSheet(
            onDismissRequest = { walletToEdit = null },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Wallet", style = MaterialTheme.typography.titleLarge, color = Color.White)

                OutlinedTextField(
                    value = walletName,
                    onValueChange = { walletName = it },
                    label = { Text("Wallet Name", color = TextSecondaryDark) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = walletBalance,
                    onValueChange = { walletBalance = it },
                    label = { Text("Balance", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Icon", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(emojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (walletIcon == emoji) ElectricPurple else Color(0x1AFFFFFF))
                                .clickable { walletIcon = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val bal = walletBalance.toDoubleOrNull() ?: 0.0
                        if (walletName.isNotEmpty()) {
                            viewModel.editWallet(
                                walletToEdit!!.copy(
                                    name = walletName,
                                    icon = walletIcon,
                                    balance = bal
                                )
                            )
                        }
                        walletToEdit = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricPurple)
                ) {
                    Text("Update Wallet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
