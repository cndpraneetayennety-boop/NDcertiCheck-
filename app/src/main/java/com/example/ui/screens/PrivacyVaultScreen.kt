package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditStatus
import com.example.model.CertificateAudit
import com.example.model.RectifyingLetterData
import com.example.ui.AppScreen
import com.example.ui.AuditViewModel
import com.example.ui.theme.DeepNavyPrimary
import com.example.ui.theme.DiscrepancyRed
import com.example.ui.theme.DiscrepancyRedContainer
import com.example.ui.theme.OnDiscrepancyRed
import com.example.ui.theme.OnVerifiedGreen
import com.example.ui.theme.OnWarningAmber
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer
import com.example.util.VaultSecurity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyVaultScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val vaultState by viewModel.vaultState.collectAsState()
    val pinError by viewModel.pinErrorMessage.collectAsState()
    val isSettingUpPin by viewModel.isSettingUpPin.collectAsState()
    val savedAudits by viewModel.savedAudits.collectAsState()
    val savedLetters by viewModel.savedLetters.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Audits, 1: Letters
    var enteredPin by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var firstEnteredPin by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    if (vaultState == VaultSecurity.VaultState.NOT_INITIALIZED || isSettingUpPin) {
        // PIN Setup View
        PinPadView(
            title = if (setupStep == 1) "Create Privacy Vault PIN" else "Confirm Your PIN",
            subtitle = if (setupStep == 1) "Set a 4-digit PIN to encrypt and secure student records locally" else "Re-enter the same 4-digit PIN to confirm",
            enteredPin = enteredPin,
            errorMessage = pinError,
            onDigitClick = { digit ->
                if (enteredPin.length < 4) {
                    val next = enteredPin + digit
                    enteredPin = next
                    if (next.length == 4) {
                        if (setupStep == 1) {
                            firstEnteredPin = next
                            enteredPin = ""
                            setupStep = 2
                            viewModel.setInitialPin(firstEnteredPin)
                        } else {
                            viewModel.setInitialPin(next)
                            enteredPin = ""
                            setupStep = 1
                        }
                    }
                }
            },
            onBackspace = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            },
            onClear = {
                enteredPin = ""
            },
            modifier = modifier
        )
    } else if (vaultState == VaultSecurity.VaultState.LOCKED) {
        // PIN Verification View
        Column(modifier = modifier.fillMaxSize()) {
            PinPadView(
                title = "Unlock Privacy Vault",
                subtitle = "Enter your 4-digit PIN to access encrypted academic audits & records",
                enteredPin = enteredPin,
                errorMessage = pinError,
                onDigitClick = { digit ->
                    if (enteredPin.length < 4) {
                        val next = enteredPin + digit
                        enteredPin = next
                        if (next.length == 4) {
                            viewModel.submitVaultPin(next)
                            enteredPin = ""
                        }
                    }
                },
                onBackspace = {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                    }
                },
                onClear = {
                    enteredPin = ""
                },
                modifier = Modifier.weight(1f)
            )

            // Reset PIN button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.testTag("reset_pin_button")
                ) {
                    Text("Reset Vault Security PIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    } else {
        // Vault UNLOCKED - Display Protected Audit Ledger & Saved Letters
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Vault Status Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DeepNavyPrimary
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(VerifiedGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = VerifiedGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Privacy Vault Unlocked",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Protected local on-device database",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.lockVault() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("lock_vault_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tab Selector: Audits vs Letters
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Audited Certificates (${savedAudits.size})") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Saved Letters (${savedLetters.size})") }
                    )
                }
            }

            if (activeTab == 0) {
                if (savedAudits.isEmpty()) {
                    item {
                        EmptyVaultState(
                            title = "No Certificate Audits in Vault",
                            subtitle = "Run an audit on the Audit tab and tap 'Save to Vault' to archive records here securely."
                        )
                    }
                } else {
                    items(savedAudits) { audit ->
                        SavedAuditCard(
                            audit = audit,
                            onDelete = { viewModel.deleteAudit(audit.id) },
                            onOpenDiff = {
                                viewModel.navigateTo(AppScreen.DIFF_VIEW)
                            }
                        )
                    }
                }
            } else {
                if (savedLetters.isEmpty()) {
                    item {
                        EmptyVaultState(
                            title = "No Rectifying Letters Saved",
                            subtitle = "Generate letters on the Rectifying Letter tab and save them for record keeping and submission."
                        )
                    }
                } else {
                    items(savedLetters) { letter ->
                        SavedLetterCard(
                            letter = letter,
                            onDelete = { viewModel.deleteLetter(letter.id) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Academic Rectifying Letter", letter.letterBody)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Letter copied!", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, letter.subject)
                                    putExtra(Intent.EXTRA_TEXT, letter.letterBody)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Letter"))
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Privacy Vault PIN?") },
            text = { Text("Resetting the PIN will allow you to create a new 4-digit PIN.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetVaultPinSecurity()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset PIN", color = DiscrepancyRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PinPadView(
    title: String,
    subtitle: String,
    enteredPin: String,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(DeepNavyPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = DeepNavyPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 PIN Dots Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) DeepNavyPrimary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = DiscrepancyRed,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Numeric Keypad Grid (1-9, Clear, 0, Backspace)
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in digits) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    for (item in row) {
                        KeypadButton(
                            text = item,
                            onClick = {
                                when (item) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    else -> onDigitClick(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("pin_key_$text")
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (text == "⌫") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
        }
    }
}

@Composable
fun SavedAuditCard(
    audit: CertificateAudit,
    onDelete: () -> Unit,
    onOpenDiff: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(audit.auditDate))
    val mismatchCount = audit.discrepancies.count { !it.isMatch }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiff),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = audit.candidateName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = audit.certificateTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when (audit.status) {
                        AuditStatus.VERIFIED_PASS -> VerifiedGreenContainer
                        AuditStatus.DISCREPANCIES_FOUND -> WarningAmberContainer
                        AuditStatus.RECTIFICATION_REQUIRED -> DiscrepancyRedContainer
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${(audit.overallSimilarity * 100).toInt()}% Match",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (audit.status) {
                            AuditStatus.VERIFIED_PASS -> OnVerifiedGreen
                            AuditStatus.DISCREPANCIES_FOUND -> OnWarningAmber
                            AuditStatus.RECTIFICATION_REQUIRED -> OnDiscrepancyRed
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audited: $formattedDate • $mismatchCount discrepancies",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Audit",
                        tint = DiscrepancyRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedLetterCard(
    letter: RectifyingLetterData,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(letter.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = letter.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = letter.letterType.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DiscrepancyRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Saved on $formattedDate • ${letter.authorityTitle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyVaultState(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
