package com.billforce.owner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billforce.owner.ui.screens.DashboardScreen
import com.billforce.owner.ui.screens.ErrorScreen
import com.billforce.owner.ui.screens.LoadingScreen
import com.billforce.owner.ui.screens.PinScreen
import com.billforce.owner.ui.screens.SetupScreen
import com.billforce.owner.viewmodel.AppState
import com.billforce.owner.viewmodel.AppViewModel

@Composable
fun BillforceNavGraph(appViewModel: AppViewModel) {
    val state by appViewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is AppState.Setup -> SetupScreen(
            onConnect = { path, pin -> appViewModel.connectDatabase(path, pin) }
        )

        is AppState.PinEntry -> PinScreen(
            shopName   = appViewModel.dbConfig.collectAsStateWithLifecycle().value.shopName,
            pinInput   = appViewModel.pinInput.collectAsStateWithLifecycle().value,
            hasError   = appViewModel.pinError.collectAsStateWithLifecycle().value,
            onDigit    = { appViewModel.onPinDigit(it) },
            onBackspace = { appViewModel.onPinBackspace() },
            onForgot   = { appViewModel.disconnect() }
        )

        is AppState.Dashboard -> DashboardScreen(
            summary    = s.summary,
            onRefresh  = { appViewModel.loadDashboard() },
            onDisconnect = { appViewModel.disconnect() }
        )

        is AppState.Refreshing -> DashboardScreen(
            summary      = s.summary,
            isRefreshing = true,
            onRefresh    = { appViewModel.loadDashboard() },
            onDisconnect = { appViewModel.disconnect() }
        )

        is AppState.Error -> {
            if (s.message == "Connecting..." || s.message == "Loading...") {
                LoadingScreen(message = s.message)
            } else {
                ErrorScreen(
                    message  = s.message,
                    canRetry = s.canRetry,
                    onRetry  = { appViewModel.loadDashboard() },
                    onReset  = { appViewModel.disconnect() }
                )
            }
        }
    }
}
