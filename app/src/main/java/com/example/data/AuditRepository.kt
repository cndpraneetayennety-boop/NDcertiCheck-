package com.example.data

import com.example.model.AuditStatus
import com.example.model.CertificateAudit
import com.example.model.CertificateFieldType
import com.example.model.DiffChar
import com.example.model.DiffType
import com.example.model.DiscrepancySeverity
import com.example.model.FieldDiscrepancy
import com.example.model.LetterTemplateType
import com.example.model.RectifyingLetterData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

class AuditRepository(private val db: AppDatabase) {

    val allAudits: Flow<List<CertificateAudit>> = db.auditDao().getAllAudits().map { list ->
        list.map { entity -> entityToAudit(entity) }
    }

    val allLetters: Flow<List<RectifyingLetterData>> = db.letterDao().getAllLetters().map { list ->
        list.map { entity ->
            RectifyingLetterData(
                id = entity.id,
                auditId = entity.auditId,
                studentName = entity.studentName,
                rollNumber = entity.rollNumber,
                institution = entity.institution,
                authorityTitle = entity.authorityTitle,
                letterType = runCatching { LetterTemplateType.valueOf(entity.letterType) }
                    .getOrDefault(LetterTemplateType.ACADEMIC_CORRECTION),
                subject = entity.subject,
                letterBody = entity.letterBody,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun saveAudit(audit: CertificateAudit): Long {
        val entity = auditToEntity(audit)
        return db.auditDao().insertAudit(entity)
    }

    suspend fun deleteAudit(id: Long) {
        db.auditDao().deleteAudit(id)
    }

    suspend fun clearAllAudits() {
        db.auditDao().clearAllAudits()
    }

    suspend fun saveLetter(letter: RectifyingLetterData): Long {
        val entity = LetterEntity(
            auditId = letter.auditId,
            studentName = letter.studentName,
            rollNumber = letter.rollNumber,
            institution = letter.institution,
            authorityTitle = letter.authorityTitle,
            letterType = letter.letterType.name,
            subject = letter.subject,
            letterBody = letter.letterBody,
            createdAt = letter.createdAt
        )
        return db.letterDao().insertLetter(entity)
    }

    suspend fun deleteLetter(id: Long) {
        db.letterDao().deleteLetter(id)
    }

    // Vault Security
    val securityConfig: Flow<VaultSecurityEntity?> = db.vaultSecurityDao().getSecurityConfig()

    suspend fun getSecurityConfigSync(): VaultSecurityEntity? {
        return db.vaultSecurityDao().getSecurityConfigSync()
    }

    suspend fun saveSecurityConfig(config: VaultSecurityEntity) {
        db.vaultSecurityDao().saveSecurityConfig(config)
    }

    suspend fun clearSecurityConfig() {
        db.vaultSecurityDao().clearSecurityConfig()
    }

    private fun auditToEntity(audit: CertificateAudit): AuditEntity {
        val array = JSONArray()
        for (disc in audit.discrepancies) {
            val obj = JSONObject()
            obj.put("field", disc.field.name)
            obj.put("certificateValue", disc.certificateValue)
            obj.put("officialValue", disc.officialValue)
            obj.put("similarityScore", disc.similarityScore)
            obj.put("isMatch", disc.isMatch)
            obj.put("severity", disc.severity.name)
            obj.put("description", disc.description)
            array.put(obj)
        }

        return AuditEntity(
            id = audit.id,
            certificateTitle = audit.certificateTitle,
            studentName = audit.candidateName,
            officialName = audit.officialName,
            rollNumber = audit.discrepancies.firstOrNull { it.field == CertificateFieldType.ROLL_NUMBER }?.certificateValue
                ?: "",
            institution = audit.discrepancies.firstOrNull { it.field == CertificateFieldType.INSTITUTION }?.certificateValue
                ?: "",
            overallSimilarity = audit.overallSimilarity,
            discrepancyCount = audit.discrepancies.count { !it.isMatch },
            status = audit.status.name,
            rawOcrText = audit.rawOcrText,
            discrepanciesJson = array.toString(),
            auditTimestamp = audit.auditDate
        )
    }

    private fun entityToAudit(entity: AuditEntity): CertificateAudit {
        val discrepancies = mutableListOf<FieldDiscrepancy>()
        try {
            val array = JSONArray(entity.discrepanciesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val field = CertificateFieldType.valueOf(obj.getString("field"))
                val certVal = obj.getString("certificateValue")
                val offVal = obj.getString("officialValue")
                val diffs = com.example.util.DiffEngine.alignCharacters(offVal, certVal)
                val sim = obj.optDouble("similarityScore", 1.0)
                val isMatch = obj.optBoolean("isMatch", true)
                val severity = runCatching { DiscrepancySeverity.valueOf(obj.getString("severity")) }
                    .getOrDefault(if (isMatch) DiscrepancySeverity.NONE else DiscrepancySeverity.MINOR_TYPO)
                val desc = obj.optString("description", "")
                discrepancies.add(
                    FieldDiscrepancy(
                        field = field,
                        certificateValue = certVal,
                        officialValue = offVal,
                        diffs = diffs,
                        similarityScore = sim,
                        isMatch = isMatch,
                        severity = severity,
                        description = desc
                    )
                )
            }
        } catch (_: Exception) {
        }

        val status = runCatching { AuditStatus.valueOf(entity.status) }
            .getOrDefault(AuditStatus.DISCREPANCIES_FOUND)

        return CertificateAudit(
            id = entity.id,
            certificateTitle = entity.certificateTitle,
            candidateName = entity.studentName,
            officialName = entity.officialName,
            overallSimilarity = entity.overallSimilarity,
            discrepancies = discrepancies,
            auditDate = entity.auditTimestamp,
            rawOcrText = entity.rawOcrText,
            status = status
        )
    }
}
