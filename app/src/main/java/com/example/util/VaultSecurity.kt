package com.example.util

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

object VaultSecurity {

    enum class VaultState {
        NOT_INITIALIZED, // No PIN configured yet
        LOCKED,          // PIN configured, currently locked
        UNLOCKED         // Vault opened and accessible
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$pin:$salt"
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun verifyPin(pin: String, storedHash: String, salt: String): Boolean {
        val computedHash = hashPin(pin, salt)
        return computedHash == storedHash
    }
}
