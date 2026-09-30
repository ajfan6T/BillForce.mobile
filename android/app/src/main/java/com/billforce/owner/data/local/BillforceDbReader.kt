package com.billforce.owner.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.billforce.owner.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reads the Billforce SQLite database file directly.
 *
 * Billforce stores data in a local SQLite file. This reader opens it in
 * read-only mode and queries the main tables: sales, invoices, stock, accounts.
 *
 * Table structure is inferred from the Billforce desktop application schema.
 * Falls back gracefully if tables/columns don't exist (version differences).
 */
class BillforceDbReader(private val context: Context) {

    companion object {
        // Table & column names used by Billforce desktop app
        private const val TABLE_SALES          = "sales"
        private const val TABLE_INVOICES       = "invoices"
        private const val TABLE_INVOICE_ITEMS  = "invoice_items"
        private const val TABLE_CUSTOMERS      = "customers"
        private const val TABLE_STOCK          = "stock"
        private const val TABLE_STOCK_ITEMS    = "items"
        private const val TABLE_ACCOUNTS       = "accounts"
        private const val TABLE_TRANSACTIONS   = "transactions"
        private const val TABLE_SETTINGS       = "settings"
        private const val TABLE_EXPENSES       = "expenses"

        private val DATE_FMT  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val LABEL_FMT = SimpleDateFormat("EEE", Locale.getDefault())
    }

    /**
     * Opens a Billforce SQLite DB from [dbFilePath] in read-only mode
     * and extracts dashboard metrics.
     * Throws [IllegalArgumentException] if the file is not a valid SQLite DB.
     */
    fun readDashboard(dbFilePath: String): DashboardSummary {
        val file = File(dbFilePath)
        if (!file.exists()) throw IllegalArgumentException("Database file not found: $dbFilePath")

        // Copy to app cache for safe read-only access (avoids permission issues on Android 11+)
        val cacheDb = File(context.cacheDir, "billforce_ro.db")
        file.copyTo(cacheDb, overwrite = true)

        val db = try {
            SQLiteDatabase.openDatabase(cacheDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            throw IllegalArgumentException("Not a valid SQLite database: ${e.message}")
        }

        return db.use { buildDashboard(it, dbFilePath) }
    }

    private fun buildDashboard(db: SQLiteDatabase, dbPath: String): DashboardSummary {
        val tables = getTableNames(db)

        val shopName  = readSetting(db, "shop_name", tables)  ?: readSetting(db, "company_name", tables) ?: "My Shop"
        val ownerName = readSetting(db, "owner_name", tables) ?: readSetting(db, "username", tables)     ?: "Owner"

        val todayStr  = DATE_FMT.format(Date())

        // ── Today's sales ─────────────────────────────────────────────────────
        val (todaySales, todayCount) = querySalesForDate(db, tables, todayStr)

        // ── Month sales ────────────────────────────────────────────────────────
        val monthPrefix = todayStr.substring(0, 7) // "yyyy-MM"
        val (monthSales, monthCount) = querySalesForMonth(db, tables, monthPrefix)

        // ── Accounts due/payable ───────────────────────────────────────────────
        val totalDue     = queryTotalDue(db, tables)
        val totalPayable = queryTotalPayable(db, tables)

        // ── Stock ─────────────────────────────────────────────────────────────
        val (totalItems, lowItems, outItems, alerts) = queryStock(db, tables)

        // ── Recent invoices ────────────────────────────────────────────────────
        val recentInvoices = queryRecentInvoices(db, tables, 10)

        // ── Weekly chart data (last 7 days) ────────────────────────────────────
        val weeklySales = queryWeeklySales(db, tables)

        // ── Month expenses & profit ────────────────────────────────────────────
        val monthExpenses = queryMonthExpenses(db, tables, monthPrefix)
        val monthProfit   = monthSales - monthExpenses

        return DashboardSummary(
            shopName            = shopName,
            ownerName           = ownerName,
            todaySales          = todaySales,
            todayInvoiceCount   = todayCount,
            monthSales          = monthSales,
            monthInvoiceCount   = monthCount,
            monthExpenses       = monthExpenses,
            monthProfit         = monthProfit,
            totalDue            = totalDue,
            totalPayable        = totalPayable,
            totalStockItems     = totalItems,
            lowStockItems       = lowItems,
            outOfStockItems     = outItems,
            recentInvoices      = recentInvoices,
            stockAlerts         = alerts,
            weeklySales         = weeklySales,
            lastSyncTime        = System.currentTimeMillis(),
            dbPath              = dbPath
        )
    }

    // ── Helper: list tables ────────────────────────────────────────────────────
    private fun getTableNames(db: SQLiteDatabase): Set<String> {
        val tables = mutableSetOf<String>()
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) tables.add(c.getString(0).lowercase())
        }
        return tables
    }

    // ── Helper: read settings ──────────────────────────────────────────────────
    private fun readSetting(db: SQLiteDatabase, key: String, tables: Set<String>): String? {
        if (TABLE_SETTINGS !in tables) return null
        return try {
            db.rawQuery(
                "SELECT value FROM $TABLE_SETTINGS WHERE key=? LIMIT 1",
                arrayOf(key)
            ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null }
    }

    // ── Sales for a specific date ──────────────────────────────────────────────
    private fun querySalesForDate(
        db: SQLiteDatabase, tables: Set<String>, dateStr: String
    ): Pair<Double, Int> {
        // Try 'invoices' table first, then 'sales'
        val (table, amtCol, dateCol, statusCol) = resolveInvoiceTable(tables)
            ?: return Pair(0.0, 0)
        return try {
            val filter = if (statusCol != null) " AND LOWER($statusCol) != 'cancelled'" else ""
            db.rawQuery(
                "SELECT COALESCE(SUM($amtCol),0), COUNT(*) FROM $table " +
                "WHERE DATE($dateCol)=?$filter",
                arrayOf(dateStr)
            ).use { c ->
                if (c.moveToFirst()) Pair(c.getDouble(0), c.getInt(1))
                else Pair(0.0, 0)
            }
        } catch (_: Exception) { Pair(0.0, 0) }
    }

    private fun querySalesForMonth(
        db: SQLiteDatabase, tables: Set<String>, monthPrefix: String
    ): Pair<Double, Int> {
        val (table, amtCol, dateCol, statusCol) = resolveInvoiceTable(tables)
            ?: return Pair(0.0, 0)
        return try {
            val filter = if (statusCol != null) " AND LOWER($statusCol) != 'cancelled'" else ""
            db.rawQuery(
                "SELECT COALESCE(SUM($amtCol),0), COUNT(*) FROM $table " +
                "WHERE strftime('%Y-%m',$dateCol)=?$filter",
                arrayOf(monthPrefix)
            ).use { c ->
                if (c.moveToFirst()) Pair(c.getDouble(0), c.getInt(1))
                else Pair(0.0, 0)
            }
        } catch (_: Exception) { Pair(0.0, 0) }
    }

    // ── Resolve invoice table schema ───────────────────────────────────────────
    data class InvoiceTableInfo(
        val table: String, val amountCol: String, val dateCol: String, val statusCol: String?
    )

    private fun resolveInvoiceTable(tables: Set<String>): InvoiceTableInfo? {
        // Common schemas: invoices, sales, bills
        val candidates = listOf(TABLE_INVOICES, TABLE_SALES, "bills", "vouchers")
        for (tbl in candidates) {
            if (tbl in tables) {
                val cols = getColumnNames(tbl)
                val amtCol = cols.firstOrNull { it in setOf("total", "grand_total", "amount", "total_amount", "net_amount") }
                    ?: continue
                val dateCol = cols.firstOrNull { it in setOf("date", "invoice_date", "bill_date", "created_at", "sale_date") }
                    ?: continue
                val statusCol = cols.firstOrNull { it in setOf("status", "payment_status", "bill_status") }
                return InvoiceTableInfo(tbl, amtCol, dateCol, statusCol)
            }
        }
        return null
    }

    // Temporary db holder for column queries
    private var _tempDb: SQLiteDatabase? = null
    private fun getColumnNames(table: String): Set<String> {
        return try {
            val cols = mutableSetOf<String>()
            _tempDb?.rawQuery("PRAGMA table_info($table)", null)?.use { c ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) cols.add(c.getString(nameIdx).lowercase())
            }
            cols
        } catch (_: Exception) { emptySet() }
    }

    private fun buildDashboard(db: SQLiteDatabase, dbPath: String, placeholder: Boolean = false): DashboardSummary {
        _tempDb = db
        return buildDashboard(db, dbPath)
    }

    // ── Accounts ───────────────────────────────────────────────────────────────
    private fun queryTotalDue(db: SQLiteDatabase, tables: Set<String>): Double {
        val tbl = listOf("ledger", TABLE_ACCOUNTS, TABLE_CUSTOMERS, "receivables")
            .firstOrNull { it in tables } ?: return 0.0
        return try {
            val cols = getColumnNames(tbl)
            val balCol = cols.firstOrNull { it in setOf("balance", "due_amount", "outstanding", "receivable") }
                ?: return 0.0
            db.rawQuery("SELECT COALESCE(SUM(CASE WHEN $balCol > 0 THEN $balCol ELSE 0 END),0) FROM $tbl", null)
                .use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        } catch (_: Exception) { 0.0 }
    }

    private fun queryTotalPayable(db: SQLiteDatabase, tables: Set<String>): Double {
        val tbl = listOf("payables", "suppliers", "vendor_ledger")
            .firstOrNull { it in tables } ?: return 0.0
        return try {
            val cols = getColumnNames(tbl)
            val balCol = cols.firstOrNull { it in setOf("balance", "payable_amount", "due", "outstanding") }
                ?: return 0.0
            db.rawQuery("SELECT COALESCE(SUM(CASE WHEN $balCol > 0 THEN $balCol ELSE 0 END),0) FROM $tbl", null)
                .use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        } catch (_: Exception) { 0.0 }
    }

    // ── Stock ──────────────────────────────────────────────────────────────────
    private data class StockResult(
        val total: Int, val low: Int, val out: Int, val alerts: List<StockAlert>
    )

    private fun queryStock(db: SQLiteDatabase, tables: Set<String>): StockResult {
        val tbl = listOf(TABLE_STOCK_ITEMS, TABLE_STOCK, "products", "inventory")
            .firstOrNull { it in tables } ?: return StockResult(0, 0, 0, emptyList())

        return try {
            val cols = getColumnNames(tbl)
            val nameCol   = cols.firstOrNull { it in setOf("name", "item_name", "product_name") } ?: return StockResult(0, 0, 0, emptyList())
            val qtyCol    = cols.firstOrNull { it in setOf("quantity", "stock", "qty", "current_stock", "closing_stock") } ?: return StockResult(0, 0, 0, emptyList())
            val minCol    = cols.firstOrNull { it in setOf("min_stock", "minimum_stock", "reorder_level", "min_qty") }
            val unitCol   = cols.firstOrNull { it in setOf("unit", "uom", "unit_of_measure") }

            val alerts    = mutableListOf<StockAlert>()
            var total     = 0; var low = 0; var out = 0

            val selectCols = listOf(nameCol, qtyCol, minCol, unitCol).filterNotNull().joinToString(",")
            db.rawQuery("SELECT $selectCols FROM $tbl ORDER BY $qtyCol ASC", null).use { c ->
                val niIdx = c.getColumnIndex(nameCol)
                val qiIdx = c.getColumnIndex(qtyCol)
                val miIdx = if (minCol != null) c.getColumnIndex(minCol) else -1
                val uiIdx = if (unitCol != null) c.getColumnIndex(unitCol) else -1

                while (c.moveToNext()) {
                    total++
                    val qty  = c.getDouble(qiIdx)
                    val min  = if (miIdx >= 0) c.getDouble(miIdx) else 5.0
                    val name = c.getString(niIdx) ?: "Unknown"
                    val unit = if (uiIdx >= 0) c.getString(uiIdx) ?: "pcs" else "pcs"

                    val level = when {
                        qty <= 0   -> { out++; AlertLevel.OUT_OF_STOCK }
                        qty <= min -> { low++; if (qty <= min / 2) AlertLevel.CRITICAL else AlertLevel.LOW }
                        else       -> null
                    }
                    if (level != null) {
                        alerts.add(StockAlert(name, qty, min, unit, level))
                    }
                }
            }
            StockResult(total, low, out, alerts.take(20))
        } catch (_: Exception) { StockResult(0, 0, 0, emptyList()) }
    }

    // ── Recent invoices ────────────────────────────────────────────────────────
    private fun queryRecentInvoices(
        db: SQLiteDatabase, tables: Set<String>, limit: Int
    ): List<InvoiceSummary> {
        val info = resolveInvoiceTable(tables) ?: return emptyList()
        return try {
            val cols = getColumnNames(info.table)
            val invNoCol = cols.firstOrNull { it in setOf("invoice_no", "bill_no", "voucher_no", "id") } ?: "rowid"
            val custCol  = cols.firstOrNull { it in setOf("customer_name", "party_name", "client_name", "customer") }

            val selectParts = mutableListOf(invNoCol, info.amountCol, info.dateCol)
            if (custCol != null) selectParts.add(custCol)
            if (info.statusCol != null) selectParts.add(info.statusCol)

            val result = mutableListOf<InvoiceSummary>()
            db.rawQuery(
                "SELECT ${selectParts.joinToString(",")} FROM ${info.table} " +
                "ORDER BY ${info.dateCol} DESC LIMIT $limit",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    val status = if (info.statusCol != null) {
                        when (c.getString(c.getColumnIndex(info.statusCol)).lowercase()) {
                            "paid"    -> InvoiceStatus.PAID
                            "partial" -> InvoiceStatus.PARTIAL
                            else      -> InvoiceStatus.DUE
                        }
                    } else InvoiceStatus.PAID

                    result.add(InvoiceSummary(
                        invoiceNo    = c.getString(c.getColumnIndex(invNoCol)) ?: "-",
                        customerName = if (custCol != null) c.getString(c.getColumnIndex(custCol)) ?: "Walk-in" else "Customer",
                        amount       = c.getDouble(c.getColumnIndex(info.amountCol)),
                        date         = c.getString(c.getColumnIndex(info.dateCol)) ?: "",
                        status       = status
                    ))
                }
            }
            result
        } catch (_: Exception) { emptyList() }
    }

    // ── Weekly chart sales (last 7 days) ──────────────────────────────────────
    private fun queryWeeklySales(db: SQLiteDatabase, tables: Set<String>): List<DailyEntry> {
        val info = resolveInvoiceTable(tables) ?: return getDemoWeekly()
        val entries = mutableListOf<DailyEntry>()
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = DATE_FMT.format(cal.time)
            val label   = LABEL_FMT.format(cal.time)

            val amount = try {
                db.rawQuery(
                    "SELECT COALESCE(SUM(${info.amountCol}),0) FROM ${info.table} WHERE DATE(${info.dateCol})=?",
                    arrayOf(dateStr)
                ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
            } catch (_: Exception) { 0.0 }

            entries.add(DailyEntry(label, amount, dateStr))
        }
        return entries
    }

    // ── Month expenses ─────────────────────────────────────────────────────────
    private fun queryMonthExpenses(
        db: SQLiteDatabase, tables: Set<String>, monthPrefix: String
    ): Double {
        val tbl = listOf(TABLE_EXPENSES, "expense", "expenditure")
            .firstOrNull { it in tables } ?: return 0.0
        return try {
            val cols = getColumnNames(tbl)
            val amtCol  = cols.firstOrNull { it in setOf("amount", "total", "expense_amount") } ?: return 0.0
            val dateCol = cols.firstOrNull { it in setOf("date", "expense_date", "created_at") } ?: return 0.0
            db.rawQuery(
                "SELECT COALESCE(SUM($amtCol),0) FROM $tbl WHERE strftime('%Y-%m',$dateCol)=?",
                arrayOf(monthPrefix)
            ).use { c -> if (c.moveToFirst()) c.getDouble(0) else 0.0 }
        } catch (_: Exception) { 0.0 }
    }

    // ── Demo fallback for weekly chart ─────────────────────────────────────────
    private fun getDemoWeekly(): List<DailyEntry> {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return labels.mapIndexed { i, l -> DailyEntry(l, 0.0) }
    }
}
