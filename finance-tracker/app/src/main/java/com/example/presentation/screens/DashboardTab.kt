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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
    val badges by viewModel.badges.collectAsState()

    val scrollState = rememberScrollState()
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var walletToDelete by remember { mutableStateOf<WalletEntity?>(null) }
    var walletToEdit by remember { mutableStateOf<WalletEntity?>(null) }
    var isBalanceVisible by remember { mutableStateOf(false) }

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
                    // Generate gradient from wallet's stored color field
                    val baseColor = try {
                        Color(android.graphics.Color.parseColor(wallet.color))
                    } catch (e: Exception) {
                        Color(0xFF6C63FF)
                    }
                    
                    // Create a darker shade for gradient end
                    val darkerColor = baseColor.copy(
                        red = (baseColor.red * 0.65f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.65f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.65f).coerceIn(0f, 1f)
                    )
                    
                    // Create a lighter/shifted shade for gradient start
                    val lighterColor = baseColor.copy(
                        red = ((baseColor.red + 0.15f) * 1.1f).coerceIn(0f, 1f),
                        green = ((baseColor.green + 0.1f) * 1.05f).coerceIn(0f, 1f),
                        blue = ((baseColor.blue + 0.2f) * 1.1f).coerceIn(0f, 1f)
                    )
                    
                    val colorBrush = Brush.linearGradient(
                        colors = listOf(lighterColor, baseColor, darkerColor),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
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
                                if (isBalanceVisible) {
                                    Text(
                                        String.format("৳%,.0f", wallet.balance),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "••••",
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
                                    progress = { pc.coerceIn(0f, 1f) },
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
                        val isPaidThisMonth = bill.lastPaid?.let {
                            val paidCal = Calendar.getInstance().apply { timeInMillis = it }
                            paidCal.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)
                        } ?: false

                        val daysRemainingRaw = ((bill.dueDate - System.currentTimeMillis()) / 86400000L).toInt()
                        val isBillOverdue = daysRemainingRaw < 0 && !isPaidThisMonth
                        val daysRemaining = daysRemainingRaw.coerceAtLeast(0)
                        val overdueDays = if (isBillOverdue) (-daysRemainingRaw) else 0

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

                            when {
                                isPaidThisMonth -> Text("✅ Paid", color = IncomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                isBillOverdue -> Text("🔴 $overdueDays days overdue", color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                daysRemaining == 0 -> Text("⚠️ Due Today!", color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                daysRemaining <= 3 -> Text("⚠️ In $daysRemaining days", color = CoralRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                else -> Text("In $daysRemaining days", color = TextSecondaryDark, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- BADGES ---
        if (badges.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆 Achievements", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("${badges.size} earned", color = TextSecondaryDark, fontSize = 12.sp)
                    }
        
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(badges) { badge ->
                            val badgeEmoji = when (badge.badgeKey) {
                                "FIRST_TRANSACTION" -> "💸"
                                "STREAK_7" -> "🔥"
                                "STREAK_30" -> "⚡"
                                "GOAL_GETTER" -> "🎯"
                                "DEBT_FREE" -> "🕊️"
                                "SAVER" -> "💰"
                                "BUDGET_MASTER" -> "📊"
                                else -> "⭐"
                            }
                            val badgeName = when (badge.badgeKey) {
                                "FIRST_TRANSACTION" -> "First Step"
                                "STREAK_7" -> "7-Day Streak"
                                "STREAK_30" -> "30-Day Streak"
                                "GOAL_GETTER" -> "Goal Getter"
                                "DEBT_FREE" -> "Debt Free"
                                "SAVER" -> "Super Saver"
                                "BUDGET_MASTER" -> "Budget Master"
                                else -> badge.badgeKey
                            }
        
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0x336C63FF), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Text(badgeEmoji, fontSize = 28.sp)
                                Text(
                                    badgeName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    formatDate(badge.earnedDate),
                                    color = TextSecondaryDark,
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center
                                )
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Initial Balance", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

                val walletColorPalette = listOf(
                    "#6C63FF", // Purple
                    "#00D4AA", // Teal
                    "#FF6B6B", // Red
                    "#FFB300", // Amber
                    "#0EA5E9", // Blue
                    "#10B981", // Green
                    "#F59E0B", // Orange
                    "#EF4444", // Coral
                    "#8B5CF6", // Violet
                    "#EC4899"  // Pink
                )
                var selectedColor by remember { 
                    mutableStateOf(walletColorPalette[wallets.size % walletColorPalette.size]) 
                }
                
                Text("Color", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(walletColorPalette) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = color }
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }
                }

                // Currency selector
                var selectedCurrency by remember { mutableStateOf("BDT") }
                val currencies = listOf("BDT", "USD", "EUR", "GBP", "INR")

                Text("Currency", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies) { cur ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCurrency == cur) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(1.dp, if (selectedCurrency == cur) ElectricPurple else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { selectedCurrency = cur }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(cur, color = if (selectedCurrency == cur) ElectricPurple else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Include in total toggle
                var includeInTotal by remember { mutableStateOf(true) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Include in Total Balance", color = Color.White, fontSize = 14.sp)
                        Text("Count this wallet in dashboard total", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                    Switch(
                        checked = includeInTotal,
                        onCheckedChange = { includeInTotal = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricPurple)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val bal = initialBalance.toDoubleOrNull() ?: 0.0
                        if (walletName.isNotEmpty()) {
                            viewModel.addWallet(walletName, walletIcon, selectedColor, bal, selectedCurrency, includeInTotal)
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = walletBalance,
                    onValueChange = { walletBalance = it },
                    label = { Text("Balance", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

                val walletColorPalette = listOf(
                    "#6C63FF", // Purple
                    "#00D4AA", // Teal
                    "#FF6B6B", // Red
                    "#FFB300", // Amber
                    "#0EA5E9", // Blue
                    "#10B981", // Green
                    "#F59E0B", // Orange
                    "#EF4444", // Coral
                    "#8B5CF6", // Violet
                    "#EC4899"  // Pink
                )
                var selectedColor by remember { mutableStateOf(walletToEdit!!.color) }

                Text("Color", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(walletColorPalette) { color ->
                        val isSelected = selectedColor == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(if (isSelected) 3.dp else 0.dp, Color.White, CircleShape)
                                .clickable { selectedColor = color }
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pre-fill from existing wallet
                var editCurrency by remember { mutableStateOf(walletToEdit!!.currency) }
                var editIncludeInTotal by remember { mutableStateOf(walletToEdit!!.includeInTotal) }
                val currencies = listOf("BDT", "USD", "EUR", "GBP", "INR")

                Text("Currency", color = TextSecondaryDark, fontSize = 14.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currencies) { cur ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (editCurrency == cur) Color(0x336C63FF) else Color(0xFF1E293B))
                                .border(1.dp, if (editCurrency == cur) ElectricPurple else Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { editCurrency = cur }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(cur, color = if (editCurrency == cur) ElectricPurple else Color.White, fontSize = 13.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Include in Total Balance", color = Color.White, fontSize = 14.sp)
                        Text("Count this wallet in dashboard total", color = TextSecondaryDark, fontSize = 11.sp)
                    }
                    Switch(
                        checked = editIncludeInTotal,
                        onCheckedChange = { editIncludeInTotal = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricPurple)
                    )
                }

                Button(
                    onClick = {
                        val bal = walletBalance.toDoubleOrNull() ?: walletToEdit!!.balance
                        if (walletName.isNotEmpty()) {
                            viewModel.editWallet(
                                walletToEdit!!.copy(
                                    name = walletName,
                                    icon = walletIcon,
                                    balance = bal,
                                    color = selectedColor,
                                    currency = editCurrency,
                                    includeInTotal = editIncludeInTotal
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
