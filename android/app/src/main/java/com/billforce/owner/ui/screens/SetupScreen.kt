package com.billforce.owner.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billforce.owner.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(onConnect: (path: String, pin: String) -> Unit) {
    val context = LocalContext.current

    var dbPath     by remember { mutableStateOf("") }
    var pin        by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin    by remember { mutableStateOf(false) }
    var error      by remember { mutableStateOf("") }
    var step       by remember { mutableIntStateOf(0) } // 0=pick file, 1=set PIN

    // File picker (SAF)
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist URI permission
            context.contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            // Resolve to real path via cache copy approach
            dbPath = it.toString()
            error  = ""
            step   = 1
        }
    }

    // Manage-all-files permission (Android 11+)
    val allFilesPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF0D3D36))
                )
            )
    ) {
        // Decorative orb
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 100.dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0x2A0D9488), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Teal40),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Bill",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Text(
                "force",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = Teal80
                ),
                modifier = Modifier.offset(y = (-8).dp)
            )

            Text(
                "Owner Dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(48.dp))

            // ── Step indicator ───────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDot(active = step == 0, done = step > 0, label = "Connect")
                Box(
                    Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(if (step > 0) Teal40 else Color.White.copy(0.2f))
                )
                StepDot(active = step == 1, done = false, label = "Set PIN")
            }

            Spacer(Modifier.height(32.dp))

            // ── Card ─────────────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(24.dp)) {
                    AnimatedContent(targetState = step, label = "setup-step") { st ->
                        when (st) {
                            0 -> ConnectStep(
                                onPickFile = {
                                    // Request permission on Android 11+
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                        !Environment.isExternalStorageManager()
                                    ) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        allFilesPermLauncher.launch(intent)
                                    }
                                    filePicker.launch(arrayOf("*/*"))
                                }
                            )
                            1 -> PinSetupStep(
                                dbPath     = dbPath,
                                pin        = pin,
                                confirmPin = confirmPin,
                                showPin    = showPin,
                                error      = error,
                                onPinChange = { pin = it.take(4).filter { c -> c.isDigit() } },
                                onConfirmChange = { confirmPin = it.take(4).filter { c -> c.isDigit() } },
                                onToggleShow = { showPin = !showPin },
                                onBack = { step = 0; pin = ""; confirmPin = "" },
                                onConnect = {
                                    if (pin.length < 4) { error = "PIN must be 4 digits"; return@PinSetupStep }
                                    if (pin != confirmPin) { error = "PINs do not match"; return@PinSetupStep }
                                    error = ""
                                    onConnect(dbPath, pin)
                                }
                            )
                        }
                    }
                }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(32.dp))

            // Info chips
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                InfoChip("100% Offline")
                Spacer(Modifier.width(8.dp))
                InfoChip("Read-only")
                Spacer(Modifier.width(8.dp))
                InfoChip("Secure PIN")
            }
        }
    }
}

@Composable
private fun ConnectStep(onPickFile: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Outlined.FolderOpen,
            contentDescription = null,
            tint = Teal80,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Connect Billforce Database",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Select your Billforce .db file from your phone storage or PC shared folder.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(24.dp))

        // Hint box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0D9488).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, Teal40.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Where is the file?",
                    style = MaterialTheme.typography.labelMedium,
                    color = Teal80, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                HintRow("Copy the .db file from your PC")
                HintRow("Or connect to a shared network folder")
                HintRow("Default location: C:\\billforce\\data.db")
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Browse & Select .db File", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupStep(
    dbPath: String,
    pin: String, confirmPin: String, showPin: Boolean, error: String,
    onPinChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onToggleShow: () -> Unit,
    onBack: () -> Unit,
    onConnect: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Lock, null, tint = Teal80, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Set Owner PIN",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Create a 4-digit PIN to secure your dashboard.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        // File info strip
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White.copy(alpha = 0.06f)
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = GreenSuccess, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    dbPath.take(40) + if (dbPath.length > 40) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        val visual = if (showPin) VisualTransformation.None else PasswordVisualTransformation()

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("4-digit PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visual,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal40,
                unfocusedBorderColor = Color.White.copy(0.2f),
                focusedLabelColor = Teal80,
                unfocusedLabelColor = Color.White.copy(0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            trailingIcon = {
                IconButton(onClick = onToggleShow) {
                    Icon(
                        if (showPin) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        null, tint = Color.White.copy(0.5f)
                    )
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPin,
            onValueChange = onConfirmChange,
            label = { Text("Confirm PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = visual,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = error.isNotEmpty(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal40,
                unfocusedBorderColor = Color.White.copy(0.2f),
                focusedLabelColor = Teal80,
                unfocusedLabelColor = Color.White.copy(0.4f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                errorBorderColor = RedDanger
            )
        )

        if (error.isNotEmpty()) {
            Text(error, color = RedDanger, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.White.copy(0.2f))
            ) {
                Text("Back", color = Color.White.copy(0.7f))
            }
            Button(
                onClick = onConnect,
                modifier = Modifier.weight(2f),
                enabled = pin.length == 4 && confirmPin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = Teal40),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Connect & Open", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepDot(active: Boolean, done: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(when { done -> GreenSuccess; active -> Teal40; else -> Color.White.copy(0.15f) }),
            contentAlignment = Alignment.Center
        ) {
            if (done) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            else Text(if (active) "1" else "2",
                color = if (active) Color.White else Color.White.copy(0.5f),
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = if (active || done) Color.White else Color.White.copy(0.4f),
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun HintRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(4.dp).clip(CircleShape).background(Teal80))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
