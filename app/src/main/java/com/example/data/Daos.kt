package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Query("SELECT * FROM audits ORDER BY auditTimestamp DESC")
    fun getAllAudits(): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audits WHERE id = :id LIMIT 1")
    suspend fun getAuditById(id: Long): AuditEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AuditEntity): Long

    @Query("DELETE FROM audits WHERE id = :id")
    suspend fun deleteAudit(id: Long)

    @Query("DELETE FROM audits")
    suspend fun clearAllAudits()
}

@Dao
interface LetterDao {
    @Query("SELECT * FROM rectifying_letters ORDER BY createdAt DESC")
    fun getAllLetters(): Flow<List<LetterEntity>>

    @Query("SELECT * FROM rectifying_letters WHERE auditId = :auditId ORDER BY createdAt DESC")
    fun getLettersForAudit(auditId: Long): Flow<List<LetterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLetter(letter: LetterEntity): Long

    @Query("DELETE FROM rectifying_letters WHERE id = :id")
    suspend fun deleteLetter(id: Long)
}

@Dao
interface VaultSecurityDao {
    @Query("SELECT * FROM vault_security WHERE id = 1 LIMIT 1")
    fun getSecurityConfig(): Flow<VaultSecurityEntity?>

    @Query("SELECT * FROM vault_security WHERE id = 1 LIMIT 1")
    suspend fun getSecurityConfigSync(): VaultSecurityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSecurityConfig(config: VaultSecurityEntity)

    @Query("DELETE FROM vault_security")
    suspend fun clearSecurityConfig()
}
