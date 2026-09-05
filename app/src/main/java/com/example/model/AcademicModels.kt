package com.example.model

enum class CertificateFieldType(val displayName: String) {
    STUDENT_NAME("Candidate / Student Name"),
    FATHER_NAME("Father / Guardian Name"),
    ROLL_NUMBER("Roll / Registration No."),
    DEGREE_TITLE("Degree / Certificate Title"),
    INSTITUTION("University / Institution"),
    PASSING_YEAR("Year of Passing"),
    GRADE_CGPA("Grade / CGPA / Division"),
    SERIAL_NUMBER("Certificate Serial No."),
    ISSUE_DATE("Date of Issue")
}

enum class DiffType {
    MATCH,
    SUBSTITUTION,
    INSERTION,
    DELETION
}

data class DiffChar(
    val char: Char,
    val type: DiffType,
    val expectedChar: Char? = null
)

enum class DiscrepancySeverity(val label: String) {
    NONE("Match"),
    MINOR_TYPO("Minor Typo"),
    SIGNIFICANT_MISMATCH("Significant Discrepancy"),
    CRITICAL_ERROR("Critical Discrepancy")
}

data class FieldDiscrepancy(
    val field: CertificateFieldType,
    val certificateValue: String,
    val officialValue: String,
    val diffs: List<DiffChar>,
    val similarityScore: Double, // 0.0 to 1.0
    val isMatch: Boolean,
    val severity: DiscrepancySeverity,
    val description: String
)

enum class AuditStatus(val label: String) {
    VERIFIED_PASS("Verified - Zero Discrepancies"),
    DISCREPANCIES_FOUND("Discrepancies Detected"),
    RECTIFICATION_REQUIRED("Rectification Letter Required")
}

data class CertificateAudit(
    val id: Long = 0,
    val certificateTitle: String,
    val candidateName: String,
    val officialName: String,
    val overallSimilarity: Double,
    val discrepancies: List<FieldDiscrepancy>,
    val auditDate: Long = System.currentTimeMillis(),
    val rawOcrText: String,
    val imageUriOrRes: String? = null,
    val status: AuditStatus,
    val notes: String = ""
)

data class OfficialStudentRecord(
    val studentName: String,
    val fatherName: String,
    val rollNumber: String,
    val degreeTitle: String,
    val institution: String,
    val passingYear: String,
    val gradeCgpa: String,
    val serialNumber: String,
    val issueDate: String
)

data class ScannedCertificateData(
    val studentName: String,
    val fatherName: String,
    val rollNumber: String,
    val degreeTitle: String,
    val institution: String,
    val passingYear: String,
    val gradeCgpa: String,
    val serialNumber: String,
    val issueDate: String,
    val rawOcrText: String
)

data class RectifyingLetterData(
    val id: Long = 0,
    val auditId: Long,
    val studentName: String,
    val rollNumber: String,
    val institution: String,
    val authorityTitle: String = "The Controller of Examinations / Registrar",
    val letterType: LetterTemplateType = LetterTemplateType.ACADEMIC_CORRECTION,
    val subject: String,
    val letterBody: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class LetterTemplateType(val title: String, val subtitle: String) {
    ACADEMIC_CORRECTION(
        "Standard Academic Correction",
        "Official application to University Registrar for typographical correction"
    ),
    URGENT_EMPLOYMENT_VISA(
        "Urgent Verification & NOC",
        "Expedited clearance letter for background checks, visa, or employment"
    ),
    MARKSHEET_ERRATA(
        "Marksheet & Ledger Errata",
        "Petition for grade, CGPA, or marks calculation recalculation"
    ),
    DUAL_NAME_AFFIDAVIT(
        "Name Discrepancy Clarification",
        "Legal explanatory affidavit explaining spelling or middle-initial variations"
    )
}

data class SampleCertificatePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val institution: String,
    val drawableResName: String?,
    val scannedData: ScannedCertificateData,
    val officialRecord: OfficialStudentRecord,
    val highlightedDiscrepanciesSummary: String
)
