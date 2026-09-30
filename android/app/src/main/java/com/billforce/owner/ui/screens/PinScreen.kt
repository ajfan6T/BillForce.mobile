package com.billforce.owner.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billforce.owner.ui.theme.*

@Composable
fun PinScreen(
    shopName: String,
    pinInput: String,
    hasError: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onForgot: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF0D3D36))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative orb
        Box(
            Modifier
                .size(350.dp)
                .offset(x = (-80).dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x200D9488), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Logo + App name
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Teal40),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ReceiptLong, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.height(16.dp))

            Text("Billforce", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black, color = Color.White))

            Text("Owner Dashboard", style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(0.5f))

            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = Teal40.copy(alpha = 0.15f)
            ) {
                Text(
                    shopName.ifEmpty { "My Shop" },
                    style = MaterialTheme.typography.labelMedium,
                    color = Teal80,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(48.dp))

            // PIN dots
            AnimatedContent(
                targetState = hasError,
                transitionSpec = {
                    slideInHorizontally { -30 } + fadeIn() togetherWith
                    slideOutHorizontally { 30 } + fadeOut()
                },
                label = "pin-dots"
            ) { error ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        val filled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        error  -> RedDanger
                                        filled -> Teal80
                                        else   -> Color.White.copy(alpha = 0.25f)
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = hasError) {
                Text("Wrong PIN. Try again.",
                    color = RedDanger,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(40.dp))

            // ── Numpad ────────────────────────────────────────────────────────
            val digits = listOf(
                listOf("1","2","3"),
                listOf("4","5","6"),
                listOf("7","8","9"),
                listOf("","0","⌫")
            )

            digits.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    row.forEach { key ->
                        PinKey(
                            label = key,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when (key) {
                                    "⌫" -> onBackspace()
                                    ""  -> { /* empty slot */ }
                                    else -> onDigit(key)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            TextButton(onClick = onForgot) {
                Text("Forgot PIN / Change Database",
                    color = Color.White.copy(0.4f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    val isEmpty = label.isEmpty()
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(
                when {
                    isEmpty -> Color.Transparent
                    label == "⌫" -> Color.White.copy(alpha = 0.08f)
                    else -> Color.White.copy(alpha = 0.10f)
                }
            )
            .then(
                if (!isEmpty) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label == "⌫") {
            Icon(Icons.Outlined.Backspace, null,
                tint = Color.White.copy(0.7f),
                modifier = Modifier.size(24.dp))
        } else if (label.isNotEmpty()) {
            Text(
                label,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
