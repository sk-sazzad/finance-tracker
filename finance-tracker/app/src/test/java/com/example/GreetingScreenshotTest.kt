package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.FinanceDatabase
import com.example.data.local.preferences.UserPreferencesManager
import com.example.data.repository.FinanceRepository
import com.example.presentation.screens.*
import com.example.presentation.viewmodel.FinanceViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var database: FinanceDatabase
  private lateinit var repository: FinanceRepository
  private lateinit var userPreferencesManager: UserPreferencesManager
  private lateinit var viewModel: FinanceViewModel

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as Application

    database = Room.inMemoryDatabaseBuilder(
      context,
      FinanceDatabase::class.java
    ).allowMainThreadQueries().build()

    repository = FinanceRepository(database.financeDao())
    userPreferencesManager = UserPreferencesManager(context)
    viewModel = FinanceViewModel(application, repository, userPreferencesManager)
  }

  @After
  fun tearDown() {
    // Let JVM Naturally discard in-memory db instance to avoid closing it
    // while initialization coroutines are still running.
  }

  @Test
  fun dashboard_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardTab(
          viewModel = viewModel,
          onNavigateToTransactions = {},
          onNavigateToAnalytics = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard.png")
  }

  @Test
  fun transactions_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        TransactionsTab(viewModel = viewModel)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transactions.png")
  }

  @Test
  fun budget_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        BudgetTab(viewModel = viewModel)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/budget.png")
  }

  @Test
  fun analytics_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        AnalyticsTab(viewModel = viewModel)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/analytics.png")
  }

  @Test
  fun tools_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ToolsTab(viewModel = viewModel)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/tools.png")
  }
}
