package com.billforce.owner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.billforce.owner.ui.navigation.BillforceNavGraph
import com.billforce.owner.ui.theme.BillforceOwnerTheme
import com.billforce.owner.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appViewModel: AppViewModel = viewModel(
                factory = AppViewModel.Factory(applicationContext)
            )
            splashScreen.setKeepOnScreenCondition { appViewModel.isLoading.value }

            BillforceOwnerTheme {
                BillforceNavGraph(appViewModel = appViewModel)
            }
        }
    }
}
