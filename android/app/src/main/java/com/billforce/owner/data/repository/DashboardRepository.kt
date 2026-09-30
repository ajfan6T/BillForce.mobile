package com.billforce.owner.data.repository

import android.content.Context
import com.billforce.owner.data.local.BillforceDbReader
import com.billforce.owner.data.model.DashboardSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class DbResult {
    data class Success(val summary: DashboardSummary) : DbResult()
    data class Error(val message: String) : DbResult()
}

/**
 * Orchestrates reading from the Billforce SQLite database.
 */
class DashboardRepository(private val context: Context) {

    private val reader = BillforceDbReader(context)

    suspend fun loadDashboard(dbPath: String): DbResult = withContext(Dispatchers.IO) {
        try {
            val summary = reader.readDashboard(dbPath)
            DbResult.Success(summary)
        } catch (e: IllegalArgumentException) {
            DbResult.Error(e.message ?: "Invalid database file")
        } catch (e: Exception) {
            DbResult.Error("Failed to read database: ${e.message}")
        }
    }
}
