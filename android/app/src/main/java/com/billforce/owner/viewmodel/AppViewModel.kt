package com.billforce.owner.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.billforce.owner.data.model.DashboardSummary
import com.billforce.owner.data.model.DbConfig
import com.billforce.owner.data.repository.DashboardRepository
import com.billforce.owner.data.repository.DbResult
import com.billforce.owner.data.repository.PrefsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI state ───────────────────────────────────────────────────────────────────
sealed class AppState {
    /** First launch — pick DB file */
    object Setup : AppState()
    /** DB connected, waiting for owner PIN */
    object PinEntry : AppState()
    /** Authenticated, show dashboard */
    data class Dashboard(val summary: DashboardSummary) : AppState()
    /** Refreshing data */
    data class Refreshing(val summary: DashboardSummary) : AppState()
    /** Error occurred */
    data class Error(val message: String, val canRetry: Boolean = true) : AppState()
}

class AppViewModel(private val context: Context) : ViewModel() {

    private val prefs     = PrefsRepository(context)
    private val dashRepo  = DashboardRepository(context)

    // Loading flag for splash screen
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _state = MutableStateFlow<AppState>(AppState.Setup)
    val state: StateFlow<AppState> = _state.asStateFlow()

    private val _dbConfig = MutableStateFlow(DbConfig())
    val dbConfig: StateFlow<DbConfig> = _dbConfig.asStateFlow()

    // PIN entry
    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.dbConfigFlow.collect { config ->
                _dbConfig.value = config
                if (_isLoading.value) {
                    // Initial startup
                    if (config.isConnected && config.dbFilePath.isNotEmpty()) {
                        _state.value = if (config.ownerPin.isNotEmpty()) AppState.PinEntry else AppState.Setup
                    } else {
                        _state.value = AppState.Setup
                    }
                    _isLoading.value = false
                }
            }
        }
    }

    // ── Connect to database file ───────────────────────────────────────────────
    fun connectDatabase(dbFilePath: String, pin: String) {
        viewModelScope.launch {
            _state.value = AppState.Error("Connecting...", canRetry = false)
            val result = dashRepo.loadDashboard(dbFilePath)
            when (result) {
                is DbResult.Success -> {
                    val shopName = result.summary.shopName
                    val config = DbConfig(
                        dbFilePath       = dbFilePath,
                        shopName         = shopName,
                        ownerPin         = pin.padStart(4, '0').take(4),
                        isConnected      = true,
                        lastConnectedAt  = System.currentTimeMillis()
                    )
                    prefs.saveDbConfig(config)
                    _dbConfig.value = config
                    _state.value    = AppState.Dashboard(result.summary)
                }
                is DbResult.Error -> {
                    _state.value = AppState.Error(result.message)
                }
            }
        }
    }

    // ── PIN entry ──────────────────────────────────────────────────────────────
    fun onPinDigit(digit: String) {
        if (_pinInput.value.length < 4) {
            _pinInput.value += digit
            _pinError.value = false
            if (_pinInput.value.length == 4) verifyPin()
        }
    }

    fun onPinBackspace() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _pinError.value = false
        }
    }

    private fun verifyPin() {
        val entered = _pinInput.value
        val stored  = _dbConfig.value.ownerPin
        if (entered == stored) {
            _pinInput.value = ""
            loadDashboard()
        } else {
            _pinError.value = true
            viewModelScope.launch {
                delay(600)
                _pinInput.value = ""
            }
        }
    }

    // ── Load / Refresh dashboard ───────────────────────────────────────────────
    fun loadDashboard() {
        val dbPath = _dbConfig.value.dbFilePath
        if (dbPath.isEmpty()) { _state.value = AppState.Setup; return }

        viewModelScope.launch {
            val current = _state.value
            _state.value = if (current is AppState.Dashboard)
                AppState.Refreshing(current.summary)
            else
                AppState.Error("Loading...", canRetry = false)

            when (val result = dashRepo.loadDashboard(dbPath)) {
                is DbResult.Success -> _state.value = AppState.Dashboard(result.summary)
                is DbResult.Error   -> _state.value = AppState.Error(result.message)
            }
        }
    }

    // ── Disconnect / reset ─────────────────────────────────────────────────────
    fun disconnect() {
        viewModelScope.launch {
            prefs.clearConnection()
            _state.value = AppState.Setup
        }
    }

    // ── ViewModel Factory ──────────────────────────────────────────────────────
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(context.applicationContext) as T
        }
    }
}
