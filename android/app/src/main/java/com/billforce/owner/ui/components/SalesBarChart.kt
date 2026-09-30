package com.billforce.owner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billforce.owner.data.model.DailyEntry
import com.billforce.owner.ui.theme.Teal40
import com.billforce.owner.ui.theme.TealDark
import com.billforce.owner.ui.theme.Teal80
import java.text.NumberFormat

/**
 * A simple vertical bar chart for weekly sales data.
 * Renders directly with Compose Canvas boxes — no third-party chart library needed.
 */
@Composable
fun SalesBarChart(
    entries: List<DailyEntry>,
    currFmt: NumberFormat,
    modifier: Modifier = Modifier
) {
    val maxVal = entries.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                Text("Last 7 Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(
                    "Total: ${currFmt.format(entries.sumOf { it.amount })}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { entry ->
                    val fraction = (entry.amount / maxVal).toFloat().coerceIn(0.04f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Amount label on top
                        if (entry.amount > 0) {
                            Text(
                                compactAmount(entry.amount),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        // Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Teal80, TealDark)
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day labels
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entries.forEach { entry ->
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun compactAmount(amount: Double): String {
    return when {
        amount >= 100_000 -> "₹${(amount / 100_000).toInt()}L"
        amount >= 1_000   -> "₹${(amount / 1_000).toInt()}K"
        else              -> "₹${amount.toInt()}"
    }
}
