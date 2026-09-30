package com.billforce.owner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billforce.owner.data.model.*
import com.billforce.owner.ui.components.SalesBarChart
import com.billforce.owner.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    summary: DashboardSummary,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val currFmt = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(summary.shopName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text("Owner Dashboard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    Box(
                        Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ReceiptLong, null,
                            tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Outlined.Refresh, "Refresh")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Refresh Data") },
                                leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                                onClick = { showMenu = false; onRefresh() }
                            )
                            DropdownMenuItem(
                                text = { Text("Disconnect") },
                                leadingIcon = { Icon(Icons.Outlined.LinkOff, null) },
                                onClick = { showMenu = false; onDisconnect() }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Today's Sales Hero Card ──────────────────────────────────
                TodayHeroCard(summary, currFmt)

                // ── Quick Stats Row ──────────────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Month Sales",
                        value = currFmt.format(summary.monthSales),
                        icon = Icons.Outlined.TrendingUp,
                        tint = GreenSuccess
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Month Profit",
                        value = currFmt.format(summary.monthProfit),
                        icon = Icons.Outlined.ShowChart,
                        tint = if (summary.monthProfit >= 0) GreenSuccess else RedDanger
                    )
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Receivable",
                        value = currFmt.format(summary.totalDue),
                        icon = Icons.Outlined.AccountBalance,
                        tint = OrangeWarning
                    )
                    QuickStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Payable",
                        value = currFmt.format(summary.totalPayable),
                        icon = Icons.Outlined.Payment,
                        tint = RedDanger
                    )
                }

                // ── Weekly Sales Chart ───────────────────────────────────────
                if (summary.weeklySales.isNotEmpty()) {
                    SalesBarChart(
                        entries = summary.weeklySales,
                        currFmt = currFmt
                    )
                }

                // ── Stock Alerts ─────────────────────────────────────────────
                StockSection(summary)

                // ── Recent Invoices ──────────────────────────────────────────
                if (summary.recentInvoices.isNotEmpty()) {
                    RecentInvoicesSection(summary.recentInvoices, currFmt)
                }

                // ── Last sync time ───────────────────────────────────────────
                if (summary.lastSyncTime > 0) {
                    Text(
                        "Last synced: ${formatSyncTime(summary.lastSyncTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Today Hero Card ────────────────────────────────────────────────────────────
@Composable
private fun TodayHeroCard(summary: DashboardSummary, currFmt: NumberFormat) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0D3D36), Color(0xFF0F766E))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's Sales",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(0.6f))
                    // Live indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(7.dp).clip(CircleShape).background(GreenSuccess)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Live", style = MaterialTheme.typography.labelSmall,
                            color = GreenSuccess)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    currFmt.format(summary.todaySales),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "${summary.todayInvoiceCount} invoice${if (summary.todayInvoiceCount != 1) "s" else ""} today",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.55f)
                )

                Spacer(Modifier.height(20.dp))

                // Cash In / Cash Out strip
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        label = "Cash In",
                        value = currFmt.format(summary.todayCashIn),
                        color = Teal80
                    )
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        label = "Month Bills",
                        value = summary.monthInvoiceCount.toString() + " bills",
                        color = Teal80
                    )
                    MiniStat(
                        modifier = Modifier.weight(1f),
                        label = "Stock Items",
                        value = summary.totalStockItems.toString(),
                        color = Teal80
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, label: String, value: String, color: Color) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(0.45f))
        Text(value, style = MaterialTheme.typography.titleSmall,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Quick Stat Card ────────────────────────────────────────────────────────────
@Composable
private fun QuickStatCard(
    modifier: Modifier, label: String, value: String,
    icon: ImageVector, tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ── Stock Section ──────────────────────────────────────────────────────────────
@Composable
private fun StockSection(summary: DashboardSummary) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stock Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StockBadge("${summary.lowStockItems} Low", OrangeWarning)
                    StockBadge("${summary.outOfStockItems} Out", RedDanger)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Stock gauge
            val total   = summary.totalStockItems.coerceAtLeast(1).toFloat()
            val okFrac  = ((total - summary.lowStockItems - summary.outOfStockItems) / total).coerceIn(0f, 1f)
            val lowFrac = (summary.lowStockItems / total).coerceIn(0f, 1f)
            val outFrac = (summary.outOfStockItems / total).coerceIn(0f, 1f)

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
            ) {
                if (okFrac > 0) Box(Modifier.weight(okFrac).fillMaxHeight().background(GreenSuccess))
                if (lowFrac > 0) Box(Modifier.weight(lowFrac).fillMaxHeight().background(OrangeWarning))
                if (outFrac > 0) Box(Modifier.weight(outFrac).fillMaxHeight().background(RedDanger))
            }

            Spacer(Modifier.height(8.dp))
            Text("${summary.totalStockItems} total items  ·  ${summary.lowStockItems} low  ·  ${summary.outOfStockItems} out of stock",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Alert list
            if (summary.stockAlerts.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text("Alerts", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                summary.stockAlerts.take(5).forEach { alert ->
                    StockAlertRow(alert)
                }
            }
        }
    }
}

@Composable
private fun StockBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

@Composable
private fun StockAlertRow(alert: StockAlert) {
    val (color, icon) = when (alert.alertLevel) {
        AlertLevel.OUT_OF_STOCK -> RedDanger to Icons.Outlined.Block
        AlertLevel.CRITICAL     -> RedDanger to Icons.Outlined.Warning
        AlertLevel.LOW          -> OrangeWarning to Icons.Outlined.Info
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(alert.itemName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface)
        Text("${alert.currentStock} ${alert.unit}",
            style = MaterialTheme.typography.bodySmall,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}

// ── Recent Invoices Section ────────────────────────────────────────────────────
@Composable
private fun RecentInvoicesSection(
    invoices: List<InvoiceSummary>,
    currFmt: NumberFormat
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Recent Invoices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            invoices.forEach { inv ->
                InvoiceRow(inv, currFmt)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun InvoiceRow(inv: InvoiceSummary, currFmt: NumberFormat) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(inv.customerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text("#${inv.invoiceNo}  ·  ${inv.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(currFmt.format(inv.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold)
            val (label, color) = when (inv.status) {
                InvoiceStatus.PAID    -> "Paid"    to GreenSuccess
                InvoiceStatus.DUE     -> "Due"     to RedDanger
                InvoiceStatus.PARTIAL -> "Partial" to OrangeWarning
            }
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = color.copy(alpha = 0.1f)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = color, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}

private fun formatSyncTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000  -> "just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        else -> "${diff / 3_600_000} hr ago"
    }
}
