package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.CertificateFieldType
import com.example.ui.AppScreen
import com.example.ui.AuditViewModel
import com.example.ui.theme.BrightCyanAccent
import com.example.ui.theme.DeepNavyPrimary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenContainer
import com.example.util.OcrEngine

@Composable
fun ScanInputScreen(
    viewModel: AuditViewModel,
    modifier: Modifier = Modifier
) {
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val scannedData by viewModel.scannedData.collectAsState()
    val officialRecord by viewModel.officialRecord.collectAsState()
    val imageUri by viewModel.certificateImageUri.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Extracted Fields, 1: Raw OCR Stream
    var isOfficialExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onImageSelected(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DeepNavyPrimary
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Academic Certificate Auditor",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Automated OCR extraction & character-by-character discrepancy detection against official registry",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrightCyanAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Preset Document Selector Chips
        item {
            Column {
                Text(
                    text = "Select Document to Audit:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(OcrEngine.samplePresets) { preset ->
                        val isSelected = selectedPreset?.id == preset.id && imageUri == null
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPreset(preset) },
                            label = { Text(preset.title) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("preset_chip_${preset.id}")
                        )
                    }
                }
            }
        }

        // Certificate Preview & OCR Scanner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Document Scan & OCR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("upload_certificate_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Document Visual Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isScanning) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Scanning & Extracting Certificate Data...")
                            }
                        } else if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Scanned Certificate",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            val drawableRes = when (selectedPreset?.drawableResName) {
                                "cert_degree_sample" -> R.drawable.cert_degree_sample
                                "cert_marksheet_sample" -> R.drawable.cert_marksheet_sample
                                else -> null
                            }

                            if (drawableRes != null) {
                                Image(
                                    painter = painterResource(id = drawableRes),
                                    contentDescription = "Certificate Preset",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = VerifiedGreen,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = selectedPreset?.title ?: "Standard Certificate",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedPreset?.subtitle ?: "Ready for character-by-character audit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle Tabs: Extracted Entities vs Raw OCR Stream
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Extracted Fields") }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Raw OCR Text") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScannedFieldRow(
                                label = "Candidate Name",
                                value = scannedData.studentName,
                                onValueChange = { viewModel.updateScannedField(CertificateFieldType.STUDENT_NAME, it) },
                                tag = "scanned_name"
                            )
                            ScannedFieldRow(
                                label = "Father / Guardian",
                                value = scannedData.fatherName,
                                onValueChange = { viewModel.updateScannedField(CertificateFieldType.FATHER_NAME, it) },
                                tag = "scanned_father"
                            )
                            ScannedFieldRow(
                                label = "Roll / Reg Number",
                                value = scannedData.rollNumber,
                                onValueChange = { viewModel.updateScannedField(CertificateFieldType.ROLL_NUMBER, it) },
                                tag = "scanned_roll"
                            )
                            ScannedFieldRow(
                                label = "Degree Title",
                                value = scannedData.degreeTitle,
                                onValueChange = { viewModel.updateScannedField(CertificateFieldType.DEGREE_TITLE, it) },
                                tag = "scanned_degree"
                            )
                            ScannedFieldRow(
                                label = "Grade / CGPA",
                                value = scannedData.gradeCgpa,
                                onValueChange = { viewModel.updateScannedField(CertificateFieldType.GRADE_CGPA, it) },
                                tag = "scanned_grade"
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = scannedData.rawOcrText,
                            onValueChange = { viewModel.updateRawOcrText(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("raw_ocr_input"),
                            label = { Text("Raw OCR Document Stream") },
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }

        // Official Student Registry Reference Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isOfficialExpanded = !isOfficialExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = VerifiedGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Official Student Registry",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ground-truth benchmark from University records",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isOfficialExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isOfficialExpanded) "Collapse" else "Expand"
                        )
                    }

                    if (isOfficialExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScannedFieldRow(
                                label = "Official Registered Name",
                                value = officialRecord.studentName,
                                onValueChange = { viewModel.updateOfficialField(CertificateFieldType.STUDENT_NAME, it) },
                                tag = "official_name"
                            )
                            ScannedFieldRow(
                                label = "Official Father Name",
                                value = officialRecord.fatherName,
                                onValueChange = { viewModel.updateOfficialField(CertificateFieldType.FATHER_NAME, it) },
                                tag = "official_father"
                            )
                            ScannedFieldRow(
                                label = "Official Roll No.",
                                value = officialRecord.rollNumber,
                                onValueChange = { viewModel.updateOfficialField(CertificateFieldType.ROLL_NUMBER, it) },
                                tag = "official_roll"
                            )
                            ScannedFieldRow(
                                label = "Official Degree Title",
                                value = officialRecord.degreeTitle,
                                onValueChange = { viewModel.updateOfficialField(CertificateFieldType.DEGREE_TITLE, it) },
                                tag = "official_degree"
                            )
                            ScannedFieldRow(
                                label = "Official Grade / CGPA",
                                value = officialRecord.gradeCgpa,
                                onValueChange = { viewModel.updateOfficialField(CertificateFieldType.GRADE_CGPA, it) },
                                tag = "official_grade"
                            )
                        }
                    }
                }
            }
        }

        // Primary Action: Run Audit
        item {
            Button(
                onClick = {
                    viewModel.runDiscrepancyAudit()
                    viewModel.navigateTo(AppScreen.DIFF_VIEW)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("run_audit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepNavyPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Run Discrepancy Audit & Diff",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ScannedFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(8.dp)
    )
}
