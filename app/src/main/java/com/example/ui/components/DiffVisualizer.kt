package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DiffChar
import com.example.model.DiffType
import com.example.model.DiscrepancySeverity
import com.example.model.FieldDiscrepancy
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
fun FieldDiffCard(
    discrepancy: FieldDiscrepancy,
    modifier: Modifier = Modifier
) {
    val isMatch = discrepancy.isMatch
    val severity = discrepancy.severity

    val borderColor = when (severity) {
        DiscrepancySeverity.NONE -> VerifiedGreen.copy(alpha = 0.4f)
        DiscrepancySeverity.MINOR_TYPO -> WarningAmber.copy(alpha = 0.6f)
        DiscrepancySeverity.SIGNIFICANT_MISMATCH,
        DiscrepancySeverity.CRITICAL_ERROR -> DiscrepancyRed.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("field_diff_card_${discrepancy.field.name.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Field name + Severity pill + Similarity Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = discrepancy.field.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SeverityBadge(severity = severity)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${(discrepancy.similarityScore * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isMatch) VerifiedGreen else if (severity == DiscrepancySeverity.MINOR_TYPO) WarningAmber else DiscrepancyRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Certificate Extracted (Actual Detected)
            Text(
                text = "Printed on Certificate:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            CharacterDiffRow(
                diffs = discrepancy.diffs,
                showCertificatePerspective = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Official Registry Record (Expected)
            Text(
                text = "Official University Registry:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OfficialReferenceRow(
                officialText = discrepancy.officialValue,
                diffs = discrepancy.diffs,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Description / Recommendation
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when (severity) {
                    DiscrepancySeverity.NONE -> VerifiedGreenContainer.copy(alpha = 0.5f)
                    DiscrepancySeverity.MINOR_TYPO -> WarningAmberContainer.copy(alpha = 0.5f)
                    else -> DiscrepancyRedContainer.copy(alpha = 0.5f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (severity) {
                            DiscrepancySeverity.NONE -> Icons.Default.CheckCircle
                            DiscrepancySeverity.MINOR_TYPO -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = severity.label,
                        tint = when (severity) {
                            DiscrepancySeverity.NONE -> OnVerifiedGreen
                            DiscrepancySeverity.MINOR_TYPO -> OnWarningAmber
                            else -> OnDiscrepancyRed
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = discrepancy.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (severity) {
                            DiscrepancySeverity.NONE -> OnVerifiedGreen
                            DiscrepancySeverity.MINOR_TYPO -> OnWarningAmber
                            else -> OnDiscrepancyRed
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: DiscrepancySeverity) {
    val (bg, fg) = when (severity) {
        DiscrepancySeverity.NONE -> VerifiedGreenContainer to OnVerifiedGreen
        DiscrepancySeverity.MINOR_TYPO -> WarningAmberContainer to OnWarningAmber
        DiscrepancySeverity.SIGNIFICANT_MISMATCH -> DiscrepancyRedContainer to OnDiscrepancyRed
        DiscrepancySeverity.CRITICAL_ERROR -> DiscrepancyRed to Color.White
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CharacterDiffRow(
    diffs: List<DiffChar>,
    showCertificatePerspective: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (diffs.isEmpty()) {
            Text(
                text = "(Empty)",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            diffs.forEach { diff ->
                when (diff.type) {
                    DiffType.MATCH -> {
                        CharBox(
                            char = diff.char,
                            bgColor = Color.Transparent,
                            textColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DiffType.SUBSTITUTION -> {
                        CharBox(
                            char = diff.char,
                            bgColor = DiscrepancyRed,
                            textColor = Color.White,
                            isHighlighted = true,
                            subscript = diff.expectedChar?.toString()
                        )
                    }
                    DiffType.INSERTION -> {
                        CharBox(
                            char = diff.char,
                            bgColor = WarningAmber,
                            textColor = Color.White,
                            isHighlighted = true,
                            subscript = "+"
                        )
                    }
                    DiffType.DELETION -> {
                        // In certificate perspective, deletion means character was omitted
                        CharBox(
                            char = diff.expectedChar ?: '_',
                            bgColor = DiscrepancyRedContainer,
                            textColor = OnDiscrepancyRed,
                            isStrikethrough = true,
                            subscript = "-"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfficialReferenceRow(
    officialText: String,
    diffs: List<DiffChar>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (officialText.isEmpty()) {
            Text(
                text = "(Empty)",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            officialText.forEach { char ->
                CharBox(
                    char = char,
                    bgColor = Color.Transparent,
                    textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
fun CharBox(
    char: Char,
    bgColor: Color,
    textColor: Color,
    isHighlighted: Boolean = false,
    isStrikethrough: Boolean = false,
    subscript: String? = null
) {
    val displayChar = if (char == ' ') "␣" else char.toString()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(
                if (isHighlighted) Modifier.border(1.dp, textColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                else Modifier
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayChar,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = textColor
            )
            if (subscript != null) {
                Text(
                    text = subscript,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}
