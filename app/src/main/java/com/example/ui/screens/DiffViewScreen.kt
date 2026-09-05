package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditStatus
import com.example.model.DiscrepancySeverity
import com.example.ui.AppScreen
import com.example.ui.AuditViewModel
import com.example.ui.components.FieldDiffCard
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

@Composable
fun DiffViewScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val audit by viewModel.activeAudit.collectAsState()
    var savedFeedback by remember { mutableStateOf(false) }

    if (audit == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No active audit. Please run an audit first.")
        }
        return
    }

    val activeAudit = audit!!
    val discrepancies = activeAudit.discrepancies
    val mismatchCount = discrepancies.count { !it.isMatch }
    val criticalCount = discrepancies.count { it.severity == DiscrepancySeverity.CRITICAL_ERROR }
    val matchCount = discrepancies.count { it.isMatch }
    val similarityPercent = (activeAudit.overallSimilarity * 100).toInt()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Score & Status Summary Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (activeAudit.status) {
                        AuditStatus.VERIFIED_PASS -> VerifiedGreenContainer
                        AuditStatus.DISCREPANCIES_FOUND -> WarningAmberContainer
                        AuditStatus.RECTIFICATION_REQUIRED -> DiscrepancyRedContainer
                    }
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeAudit.status.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = when (activeAudit.status) {
                                    AuditStatus.VERIFIED_PASS -> OnVerifiedGreen
                                    AuditStatus.DISCREPANCIES_FOUND -> OnWarningAmber
                                    AuditStatus.RECTIFICATION_REQUIRED -> OnDiscrepancyRed
                                }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Audited: ${activeAudit.candidateName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Similarity gauge pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (activeAudit.status) {
                                AuditStatus.VERIFIED_PASS -> VerifiedGreen
                                AuditStatus.DISCREPANCIES_FOUND -> WarningAmber
                                AuditStatus.RECTIFICATION_REQUIRED -> DiscrepancyRed
                            }
                        ) {
                            Text(
                                text = "$similarityPercent%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { activeAudit.overallSimilarity.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when (activeAudit.status) {
                            AuditStatus.VERIFIED_PASS -> VerifiedGreen
                            AuditStatus.DISCREPANCIES_FOUND -> WarningAmber
                            AuditStatus.RECTIFICATION_REQUIRED -> DiscrepancyRed
                        },
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stat Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatMetric(label = "Matches", count = matchCount, color = VerifiedGreen)
                        StatMetric(label = "Discrepancies", count = mismatchCount, color = if (mismatchCount > 0) WarningAmber else VerifiedGreen)
                        StatMetric(label = "Critical Errors", count = criticalCount, color = if (criticalCount > 0) DiscrepancyRed else VerifiedGreen)
                    }
                }
            }
        }

        // Action Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.navigateTo(AppScreen.RECTIFY_LETTER)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_letter_from_diff_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepNavyPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Draft Rectifying Letter", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.saveAuditToVault()
                        savedFeedback = true
                    },
                    modifier = Modifier.testTag("save_audit_to_vault_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (savedFeedback) "Saved!" else "Save to Vault", fontSize = 13.sp)
                }
            }
        }

        item {
            Text(
                text = "Character-by-Character Field Audits:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // List of Field Diff Cards
        items(discrepancies) { disc ->
            FieldDiffCard(discrepancy = disc)
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StatMetric(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
