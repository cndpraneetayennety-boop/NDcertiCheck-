package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audits")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val certificateTitle: String,
    val studentName: String,
    val officialName: String,
    val rollNumber: String,
    val institution: String,
    val overallSimilarity: Double,
    val discrepancyCount: Int,
    val status: String,
    val rawOcrText: String,
    val discrepanciesJson: String,
    val auditTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "rectifying_letters")
data class LetterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val auditId: Long,
    val studentName: String,
    val rollNumber: String,
    val institution: String,
    val authorityTitle: String,
    val letterType: String,
    val subject: String,
    val letterBody: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_security")
data class VaultSecurityEntity(
    @PrimaryKey
    val id: Int = 1,
    val pinHash: String,
    val salt: String,
    val isPinSet: Boolean,
    val autoLockSeconds: Int = 180, // default 3 minutes
    val lastUnlockedAt: Long = 0
)
