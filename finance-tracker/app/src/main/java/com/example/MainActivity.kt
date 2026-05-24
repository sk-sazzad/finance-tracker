package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.db.FinanceDatabase
import com.example.data.local.preferences.UserPreferencesManager
import com.example.data.repository.FinanceRepository
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.presentation.viewmodel.FinanceViewModelFactory
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core singletons instantiation
        val database = FinanceDatabase.getDatabase(this)
        val repository = FinanceRepository(database.financeDao())
        val userPreferencesManager = UserPreferencesManager(this)
        val factory = FinanceViewModelFactory(application, repository, userPreferencesManager)
        
        val viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]

        setContent {
            val themePreference by viewModel.appTheme.collectAsState()
            val isDarkSystem = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themePreference) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isDarkSystem
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                var currentTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (isDarkTheme) DarkBackground else MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Rounded.Home, contentDescription = "Dashboard") },
                                label = { Text("Home") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricPurple,
                                    selectedTextColor = ElectricPurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Filled.ListAlt, contentDescription = "Ledger") },
                                label = { Text("Statements") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricPurple,
                                    selectedTextColor = ElectricPurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = { Icon(Icons.Rounded.Category, contentDescription = "Budgets") },
                                label = { Text("Budgets") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricPurple,
                                    selectedTextColor = ElectricPurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Insights") },
                                label = { Text("Insights") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricPurple,
                                    selectedTextColor = ElectricPurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { currentTab = 4 },
                                icon = { Icon(Icons.Rounded.Settings, contentDescription = "Vault Config") },
                                label = { Text("Vault") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricPurple,
                                    selectedTextColor = ElectricPurple,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "tabTransition"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> DashboardTab(
                                    viewModel = viewModel,
                                    onNavigateToTransactions = { currentTab = 1 },
                                    onNavigateToAnalytics = { currentTab = 3 }
                                )
                                1 -> TransactionsTab(viewModel = viewModel)
                                2 -> BudgetTab(viewModel = viewModel)
                                3 -> AnalyticsTab(viewModel = viewModel)
                                4 -> ToolsTab(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
