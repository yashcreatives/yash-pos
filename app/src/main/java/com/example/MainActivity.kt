package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.PosRepository
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = PosRepository.getInstance(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RestaurantPosApp(repository = repository)
                }
            }
        }
    }
}

@Composable
fun RestaurantPosApp(repository: PosRepository) {
    // Shared ViewModels created with repository
    val authViewModel: AuthViewModel = viewModel(factory = PosViewModelFactory(repository))
    val posViewModel: PosViewModel = viewModel(factory = PosViewModelFactory(repository))
    val tableViewModel: TableViewModel = viewModel(factory = PosViewModelFactory(repository))
    val kdsViewModel: KdsViewModel = viewModel(factory = PosViewModelFactory(repository))
    val dashboardViewModel: DashboardViewModel = viewModel(factory = PosViewModelFactory(repository))
    val expenseTallyViewModel: ExpenseTallyViewModel = viewModel(factory = PosViewModelFactory(repository))
    val inventoryViewModel: InventoryViewModel = viewModel(factory = PosViewModelFactory(repository))
    val reportsViewModel: ReportsViewModel = viewModel(factory = PosViewModelFactory(repository))
    val billHistoryViewModel: BillHistoryViewModel = viewModel(factory = PosViewModelFactory(repository))
    val customerViewModel: CustomerViewModel = viewModel(factory = PosViewModelFactory(repository))
    val menuViewModel: MenuManagementViewModel = viewModel(factory = PosViewModelFactory(repository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = PosViewModelFactory(repository))

    val authState by authViewModel.uiState.collectAsState()

    if (!authState.isLoggedIn) {
        LoginScreen(authViewModel = authViewModel)
    } else {
        MainAppScreen(
            authViewModel = authViewModel,
            posViewModel = posViewModel,
            tableViewModel = tableViewModel,
            kdsViewModel = kdsViewModel,
            dashboardViewModel = dashboardViewModel,
            expenseTallyViewModel = expenseTallyViewModel,
            inventoryViewModel = inventoryViewModel,
            reportsViewModel = reportsViewModel,
            billHistoryViewModel = billHistoryViewModel,
            customerViewModel = customerViewModel,
            menuViewModel = menuViewModel,
            settingsViewModel = settingsViewModel
        )
    }
}
