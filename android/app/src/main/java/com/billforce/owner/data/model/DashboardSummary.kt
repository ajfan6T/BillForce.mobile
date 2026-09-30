package com.billforce.owner.data.model

import java.time.LocalDate

/**
 * Summary data for the owner dashboard.
 * Populated from the local Billforce SQLite database.
 */
data class DashboardSummary(
    val shopName: String = "My Shop",
    val ownerName: String = "Owner",

    // Today's stats
    val todaySales: Double = 0.0,
    val todayInvoiceCount: Int = 0,
    val todayCashIn: Double = 0.0,
    val todayCashOut: Double = 0.0,

    // Month stats
    val monthSales: Double = 0.0,
    val monthInvoiceCount: Int = 0,
    val monthExpenses: Double = 0.0,
    val monthProfit: Double = 0.0,

    // Accounts receivable
    val totalDue: Double = 0.0,       // customers owe the shop
    val totalPayable: Double = 0.0,   // shop owes suppliers

    // Stock
    val totalStockItems: Int = 0,
    val lowStockItems: Int = 0,
    val outOfStockItems: Int = 0,

    // GST
    val pendingGst: Double = 0.0,
    val gstPeriod: String = "",

    // Charts: last 7 days
    val weeklySales: List<DailyEntry> = emptyList(),

    val recentInvoices: List<InvoiceSummary> = emptyList(),
    val stockAlerts: List<StockAlert> = emptyList(),

    val lastSyncTime: Long = 0L,
    val dbPath: String = ""
)

data class DailyEntry(
    val label: String,        // "Mon", "Tue", etc.
    val amount: Double,
    val date: String = ""
)

data class InvoiceSummary(
    val invoiceNo: String,
    val customerName: String,
    val amount: Double,
    val date: String,
    val status: InvoiceStatus
)

enum class InvoiceStatus { PAID, DUE, PARTIAL }

data class StockAlert(
    val itemName: String,
    val currentStock: Double,
    val minStock: Double,
    val unit: String,
    val alertLevel: AlertLevel
)

enum class AlertLevel { LOW, CRITICAL, OUT_OF_STOCK }
