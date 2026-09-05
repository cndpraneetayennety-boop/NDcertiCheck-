package com.example

import com.example.model.CertificateFieldType
import com.example.model.DiffType
import com.example.model.DiscrepancySeverity
import com.example.model.LetterTemplateType
import com.example.util.DiffEngine
import com.example.util.LetterGenerator
import com.example.util.VaultSecurity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DiffEngineTest {

    @Test
    fun `test character by character exact match`() {
        val result = DiffEngine.auditField(
            CertificateFieldType.STUDENT_NAME,
            certValue = "MICHAEL THOMPSON",
            officialValue = "MICHAEL THOMPSON"
        )

        assertTrue(result.isMatch)
        assertEquals(1.0, result.similarityScore, 0.001)
        assertEquals(DiscrepancySeverity.NONE, result.severity)
        assertTrue(result.diffs.all { it.type == DiffType.MATCH })
    }

    @Test
    fun `test character substitution detection`() {
        // "MICHEAL" on certificate vs "MICHAEL" in official registry
        val result = DiffEngine.auditField(
            CertificateFieldType.STUDENT_NAME,
            certValue = "MICHEAL THOMPSON",
            officialValue = "MICHAEL THOMPSON"
        )

        assertFalse(result.isMatch)
        assertTrue(result.similarityScore > 0.8)
        val substitutions = result.diffs.filter { it.type == DiffType.SUBSTITUTION }
        assertTrue(substitutions.isNotEmpty())
    }

    @Test
    fun `test transposed roll number detection`() {
        val result = DiffEngine.auditField(
            CertificateFieldType.ROLL_NUMBER,
            certValue = "NIT/2020/CS/4829",
            officialValue = "NIT/2020/CS/4892"
        )

        assertFalse(result.isMatch)
        assertTrue(result.severity == DiscrepancySeverity.CRITICAL_ERROR)
    }

    @Test
    fun `test vault security pin hashing and verification`() {
        val pin = "4892"
        val salt = VaultSecurity.generateSalt()
        val hash = VaultSecurity.hashPin(pin, salt)

        assertTrue(VaultSecurity.verifyPin(pin, hash, salt))
        assertFalse(VaultSecurity.verifyPin("1234", hash, salt))
    }

    @Test
    fun `test rectifying letter generation`() {
        val samplePreset = com.example.util.OcrEngine.samplePresets.first()
        val discrepancies = listOf(
            DiffEngine.auditField(CertificateFieldType.STUDENT_NAME, samplePreset.scannedData.studentName, samplePreset.officialRecord.studentName),
            DiffEngine.auditField(CertificateFieldType.ROLL_NUMBER, samplePreset.scannedData.rollNumber, samplePreset.officialRecord.rollNumber)
        )

        val audit = com.example.model.CertificateAudit(
            id = 1,
            certificateTitle = "B.Tech Degree",
            candidateName = samplePreset.scannedData.studentName,
            officialName = samplePreset.officialRecord.studentName,
            overallSimilarity = 0.92,
            discrepancies = discrepancies,
            auditDate = System.currentTimeMillis(),
            rawOcrText = samplePreset.scannedData.rawOcrText,
            status = com.example.model.AuditStatus.RECTIFICATION_REQUIRED
        )

        val letter = LetterGenerator.generateLetter(
            audit = audit,
            templateType = LetterTemplateType.ACADEMIC_CORRECTION,
            recipientAuthority = "The Controller of Examinations"
        )

        assertNotNull(letter)
        assertTrue(letter.contains("The Controller of Examinations"))
        assertTrue(letter.contains("MICHEAL T. THOMPSON"))
        assertTrue(letter.contains("MICHAEL T. THOMPSON"))
        assertTrue(letter.contains("Subject:"))
    }
}
