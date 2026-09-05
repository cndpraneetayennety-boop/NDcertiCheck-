package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuditRepository
import com.example.data.VaultSecurityEntity
import com.example.model.AuditStatus
import com.example.model.CertificateAudit
import com.example.model.CertificateFieldType
import com.example.model.DiscrepancySeverity
import com.example.model.FieldDiscrepancy
import com.example.model.LetterTemplateType
import com.example.model.OfficialStudentRecord
import com.example.model.RectifyingLetterData
import com.example.model.SampleCertificatePreset
import com.example.model.ScannedCertificateData
import com.example.util.DiffEngine
import com.example.util.LetterGenerator
import com.example.util.OcrEngine
import com.example.util.VaultSecurity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    SCAN_INPUT("Audit & OCR"),
    DIFF_VIEW("Discrepancy Diff"),
    RECTIFY_LETTER("Rectifying Letter"),
    PRIVACY_VAULT("Privacy Vault")
}

class AuditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuditRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AuditRepository(db)
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.SCAN_INPUT)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Active Sample Preset
    private val _selectedPreset = MutableStateFlow<SampleCertificatePreset?>(OcrEngine.samplePresets.first())
    val selectedPreset: StateFlow<SampleCertificatePreset?> = _selectedPreset.asStateFlow()

    // Scanned Certificate state
    private val _scannedData = MutableStateFlow(OcrEngine.samplePresets.first().scannedData)
    val scannedData: StateFlow<ScannedCertificateData> = _scannedData.asStateFlow()

    // Official Student Record state
    private val _officialRecord = MutableStateFlow(OcrEngine.samplePresets.first().officialRecord)
    val officialRecord: StateFlow<OfficialStudentRecord> = _officialRecord.asStateFlow()

    // Selected image URI or drawable reference
    private val _certificateImageUri = MutableStateFlow<Uri?>(null)
    val certificateImageUri: StateFlow<Uri?> = _certificateImageUri.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // Active Audit Result
    private val _activeAudit = MutableStateFlow<CertificateAudit?>(null)
    val activeAudit: StateFlow<CertificateAudit?> = _activeAudit.asStateFlow()

    // Letter Generator state
    private val _selectedLetterTemplate = MutableStateFlow(LetterTemplateType.ACADEMIC_CORRECTION)
    val selectedLetterTemplate: StateFlow<LetterTemplateType> = _selectedLetterTemplate.asStateFlow()

    private val _letterRecipientAuthority = MutableStateFlow("The Controller of Examinations / Registrar")
    val letterRecipientAuthority: StateFlow<String> = _letterRecipientAuthority.asStateFlow()

    private val _studentContact = MutableStateFlow("+1 (555) 234-8901")
    val studentContact: StateFlow<String> = _studentContact.asStateFlow()

    private val _studentAddress = MutableStateFlow("128 University Avenue, Suite 4B")
    val studentAddress: StateFlow<String> = _studentAddress.asStateFlow()

    private val _generatedLetterContent = MutableStateFlow("")
    val generatedLetterContent: StateFlow<String> = _generatedLetterContent.asStateFlow()

    private val _letterSaveSuccess = MutableStateFlow(false)
    val letterSaveSuccess: StateFlow<Boolean> = _letterSaveSuccess.asStateFlow()

    // Privacy Vault Security
    private val _vaultState = MutableStateFlow(VaultSecurity.VaultState.LOCKED)
    val vaultState: StateFlow<VaultSecurity.VaultState> = _vaultState.asStateFlow()

    private val _pinErrorMessage = MutableStateFlow<String?>(null)
    val pinErrorMessage: StateFlow<String?> = _pinErrorMessage.asStateFlow()

    private val _isSettingUpPin = MutableStateFlow(false)
    val isSettingUpPin: StateFlow<Boolean> = _isSettingUpPin.asStateFlow()

    private val _tempSetupPin = MutableStateFlow("")

    val savedAudits: StateFlow<List<CertificateAudit>> = repository.allAudits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLetters: StateFlow<List<RectifyingLetterData>> = repository.allLetters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run initial audit on the first preset
        runDiscrepancyAudit()
        checkVaultSecurityStatus()
    }

    private fun checkVaultSecurityStatus() {
        viewModelScope.launch {
            val config = repository.getSecurityConfigSync()
            if (config == null || !config.isPinSet) {
                _vaultState.value = VaultSecurity.VaultState.NOT_INITIALIZED
            } else {
                _vaultState.value = VaultSecurity.VaultState.LOCKED
            }
        }
    }

    fun selectPreset(preset: SampleCertificatePreset) {
        _selectedPreset.value = preset
        _scannedData.value = preset.scannedData
        _officialRecord.value = preset.officialRecord
        _certificateImageUri.value = null
        runDiscrepancyAudit()
    }

    fun onImageSelected(uri: Uri) {
        _certificateImageUri.value = uri
        _selectedPreset.value = null
        _isScanning.value = true

        viewModelScope.launch {
            val extracted = OcrEngine.processCertificateImage(getApplication(), uri)
            _scannedData.value = extracted
            _isScanning.value = false
            runDiscrepancyAudit()
        }
    }

    fun updateScannedField(field: CertificateFieldType, value: String) {
        val current = _scannedData.value
        _scannedData.value = when (field) {
            CertificateFieldType.STUDENT_NAME -> current.copy(studentName = value)
            CertificateFieldType.FATHER_NAME -> current.copy(fatherName = value)
            CertificateFieldType.ROLL_NUMBER -> current.copy(rollNumber = value)
            CertificateFieldType.DEGREE_TITLE -> current.copy(degreeTitle = value)
            CertificateFieldType.INSTITUTION -> current.copy(institution = value)
            CertificateFieldType.PASSING_YEAR -> current.copy(passingYear = value)
            CertificateFieldType.GRADE_CGPA -> current.copy(gradeCgpa = value)
            CertificateFieldType.SERIAL_NUMBER -> current.copy(serialNumber = value)
            CertificateFieldType.ISSUE_DATE -> current.copy(issueDate = value)
        }
        runDiscrepancyAudit()
    }

    fun updateOfficialField(field: CertificateFieldType, value: String) {
        val current = _officialRecord.value
        _officialRecord.value = when (field) {
            CertificateFieldType.STUDENT_NAME -> current.copy(studentName = value)
            CertificateFieldType.FATHER_NAME -> current.copy(fatherName = value)
            CertificateFieldType.ROLL_NUMBER -> current.copy(rollNumber = value)
            CertificateFieldType.DEGREE_TITLE -> current.copy(degreeTitle = value)
            CertificateFieldType.INSTITUTION -> current.copy(institution = value)
            CertificateFieldType.PASSING_YEAR -> current.copy(passingYear = value)
            CertificateFieldType.GRADE_CGPA -> current.copy(gradeCgpa = value)
            CertificateFieldType.SERIAL_NUMBER -> current.copy(serialNumber = value)
            CertificateFieldType.ISSUE_DATE -> current.copy(issueDate = value)
        }
        runDiscrepancyAudit()
    }

    fun updateRawOcrText(text: String) {
        _scannedData.value = _scannedData.value.copy(rawOcrText = text)
    }

    fun runDiscrepancyAudit() {
        val scanned = _scannedData.value
        val official = _officialRecord.value

        val discrepancies = listOf(
            DiffEngine.auditField(CertificateFieldType.STUDENT_NAME, scanned.studentName, official.studentName),
            DiffEngine.auditField(CertificateFieldType.FATHER_NAME, scanned.fatherName, official.fatherName),
            DiffEngine.auditField(CertificateFieldType.ROLL_NUMBER, scanned.rollNumber, official.rollNumber),
            DiffEngine.auditField(CertificateFieldType.DEGREE_TITLE, scanned.degreeTitle, official.degreeTitle),
            DiffEngine.auditField(CertificateFieldType.INSTITUTION, scanned.institution, official.institution),
            DiffEngine.auditField(CertificateFieldType.PASSING_YEAR, scanned.passingYear, official.passingYear),
            DiffEngine.auditField(CertificateFieldType.GRADE_CGPA, scanned.gradeCgpa, official.gradeCgpa),
            DiffEngine.auditField(CertificateFieldType.SERIAL_NUMBER, scanned.serialNumber, official.serialNumber),
            DiffEngine.auditField(CertificateFieldType.ISSUE_DATE, scanned.issueDate, official.issueDate)
        )

        val totalSimilarity = if (discrepancies.isNotEmpty()) {
            discrepancies.map { it.similarityScore }.average()
        } else 1.0

        val hasCritical = discrepancies.any { it.severity == DiscrepancySeverity.CRITICAL_ERROR }
        val hasDiscrepancy = discrepancies.any { !it.isMatch }

        val status = when {
            !hasDiscrepancy -> AuditStatus.VERIFIED_PASS
            hasCritical || discrepancies.count { !it.isMatch } >= 2 -> AuditStatus.RECTIFICATION_REQUIRED
            else -> AuditStatus.DISCREPANCIES_FOUND
        }

        val audit = CertificateAudit(
            id = System.currentTimeMillis(),
            certificateTitle = scanned.degreeTitle.ifBlank { "Academic Certificate" },
            candidateName = scanned.studentName,
            officialName = official.studentName,
            overallSimilarity = totalSimilarity,
            discrepancies = discrepancies,
            auditDate = System.currentTimeMillis(),
            rawOcrText = scanned.rawOcrText,
            imageUriOrRes = _selectedPreset.value?.drawableResName ?: _certificateImageUri.value?.toString(),
            status = status
        )

        _activeAudit.value = audit
        refreshLetterPreview(audit)
    }

    fun setLetterTemplate(template: LetterTemplateType) {
        _selectedLetterTemplate.value = template
        _activeAudit.value?.let { refreshLetterPreview(it) }
    }

    fun updateLetterRecipient(authority: String) {
        _letterRecipientAuthority.value = authority
        _activeAudit.value?.let { refreshLetterPreview(it) }
    }

    fun updateStudentContact(contact: String, address: String) {
        _studentContact.value = contact
        _studentAddress.value = address
        _activeAudit.value?.let { refreshLetterPreview(it) }
    }

    private fun refreshLetterPreview(audit: CertificateAudit) {
        val letter = LetterGenerator.generateLetter(
            audit = audit,
            templateType = _selectedLetterTemplate.value,
            recipientAuthority = _letterRecipientAuthority.value,
            institutionName = audit.discrepancies.firstOrNull { it.field == CertificateFieldType.INSTITUTION }?.officialValue
                ?: "",
            studentContact = _studentContact.value,
            studentAddress = _studentAddress.value
        )
        _generatedLetterContent.value = letter
    }

    fun saveAuditToVault() {
        val audit = _activeAudit.value ?: return
        viewModelScope.launch {
            repository.saveAudit(audit)
        }
    }

    fun saveLetterToVault() {
        val audit = _activeAudit.value ?: return
        val letterData = RectifyingLetterData(
            auditId = audit.id,
            studentName = audit.officialName,
            rollNumber = audit.discrepancies.firstOrNull { it.field == CertificateFieldType.ROLL_NUMBER }?.officialValue
                ?: "",
            institution = audit.discrepancies.firstOrNull { it.field == CertificateFieldType.INSTITUTION }?.officialValue
                ?: "",
            authorityTitle = _letterRecipientAuthority.value,
            letterType = _selectedLetterTemplate.value,
            subject = "Application for Rectification of Certificate Data Discrepancy",
            letterBody = _generatedLetterContent.value,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveLetter(letterData)
            _letterSaveSuccess.value = true
        }
    }

    fun clearLetterSaveSuccess() {
        _letterSaveSuccess.value = false
    }

    fun deleteAudit(id: Long) {
        viewModelScope.launch {
            repository.deleteAudit(id)
        }
    }

    fun deleteLetter(id: Long) {
        viewModelScope.launch {
            repository.deleteLetter(id)
        }
    }

    fun clearAllAudits() {
        viewModelScope.launch {
            repository.clearAllAudits()
        }
    }

    // Vault Security Actions
    fun submitVaultPin(pin: String) {
        _pinErrorMessage.value = null
        viewModelScope.launch {
            val config = repository.getSecurityConfigSync()
            if (config == null || !config.isPinSet) {
                _vaultState.value = VaultSecurity.VaultState.NOT_INITIALIZED
                return@launch
            }

            val isValid = VaultSecurity.verifyPin(pin, config.pinHash, config.salt)
            if (isValid) {
                _vaultState.value = VaultSecurity.VaultState.UNLOCKED
                _pinErrorMessage.value = null
            } else {
                _pinErrorMessage.value = "Incorrect PIN. Please try again."
            }
        }
    }

    fun startPinSetup() {
        _isSettingUpPin.value = true
        _tempSetupPin.value = ""
        _pinErrorMessage.value = null
    }

    fun setInitialPin(pin: String) {
        if (_tempSetupPin.value.isEmpty()) {
            _tempSetupPin.value = pin
            _pinErrorMessage.value = null
        } else {
            if (_tempSetupPin.value == pin) {
                // Confirmed!
                viewModelScope.launch {
                    val salt = VaultSecurity.generateSalt()
                    val hash = VaultSecurity.hashPin(pin, salt)
                    val entity = VaultSecurityEntity(
                        id = 1,
                        pinHash = hash,
                        salt = salt,
                        isPinSet = true,
                        autoLockSeconds = 180,
                        lastUnlockedAt = System.currentTimeMillis()
                    )
                    repository.saveSecurityConfig(entity)
                    _vaultState.value = VaultSecurity.VaultState.UNLOCKED
                    _isSettingUpPin.value = false
                    _tempSetupPin.value = ""
                    _pinErrorMessage.value = null
                }
            } else {
                _pinErrorMessage.value = "PIN confirmation did not match. Please re-enter."
                _tempSetupPin.value = ""
            }
        }
    }

    fun lockVault() {
        _vaultState.value = VaultSecurity.VaultState.LOCKED
        _pinErrorMessage.value = null
    }

    fun resetVaultPinSecurity() {
        viewModelScope.launch {
            repository.clearSecurityConfig()
            _vaultState.value = VaultSecurity.VaultState.NOT_INITIALIZED
            _isSettingUpPin.value = false
            _tempSetupPin.value = ""
        }
    }
}
