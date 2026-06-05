package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.FinanceDatabase
import com.example.data.local.preferences.UserPreferencesManager
import com.example.data.repository.FinanceRepository
import com.example.presentation.viewmodel.FinanceViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testViewModelInitialization() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val application = context as Application

    // Build in-memory database to isolate testing
    val database = Room.inMemoryDatabaseBuilder(
      context,
      FinanceDatabase::class.java
    ).allowMainThreadQueries().build()

    val repository = FinanceRepository(database.financeDao())
    val userPreferencesManager = UserPreferencesManager(context)

    // Instantiate View Model
    val viewModel = FinanceViewModel(application, repository, userPreferencesManager)
    
    assertNotNull(viewModel)
    
    database.close()
  }
}
