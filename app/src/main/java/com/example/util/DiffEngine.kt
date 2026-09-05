package com.example.util

import com.example.model.CertificateFieldType
import com.example.model.DiffChar
import com.example.model.DiffType
import com.example.model.DiscrepancySeverity
import com.example.model.FieldDiscrepancy
import kotlin.math.max
import kotlin.math.min

object DiffEngine {

    /**
     * Performs character-by-character alignment comparing expected (official) vs actual (certificate) strings.
     */
    fun alignCharacters(official: String, certificate: String): List<DiffChar> {
        if (official.isEmpty() && certificate.isEmpty()) return emptyList()
        if (official.isEmpty()) {
            return certificate.map { DiffChar(it, DiffType.INSERTION, null) }
        }
        if (certificate.isEmpty()) {
            return official.map { DiffChar(' ', DiffType.DELETION, it) }
        }

        val m = official.length
        val n = certificate.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                if (official[i - 1] == certificate[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = min(
                        dp[i - 1][j - 1] + 1, // substitution
                        min(
                            dp[i - 1][j] + 1, // deletion from official
                            dp[i][j - 1] + 1  // insertion in certificate
                        )
                    )
                }
            }
        }

        // Backtrack to build the alignment
        val result = mutableListOf<DiffChar>()
        var i = m
        var j = n

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && official[i - 1] == certificate[j - 1]) {
                result.add(DiffChar(certificate[j - 1], DiffType.MATCH, official[i - 1]))
                i--
                j--
            } else if (i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1) {
                result.add(DiffChar(certificate[j - 1], DiffType.SUBSTITUTION, official[i - 1]))
                i--
                j--
            } else if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
                // Character was inserted in certificate
                result.add(DiffChar(certificate[j - 1], DiffType.INSERTION, null))
                j--
            } else if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                // Character in official was omitted in certificate
                result.add(DiffChar(official[i - 1], DiffType.DELETION, official[i - 1]))
                i--
            } else {
                if (j > 0) {
                    result.add(DiffChar(certificate[j - 1], DiffType.INSERTION, null))
                    j--
                } else {
                    result.add(DiffChar(official[i - 1], DiffType.DELETION, official[i - 1]))
                    i--
                }
            }
        }

        return result.reversed()
    }

    /**
     * Calculates normalized similarity score between 0.0 and 1.0 using Levenshtein distance.
     */
    fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.trim()
        val s2 = str2.trim()
        if (s1.equals(s2, ignoreCase = true)) return 1.0
        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0

        val distance = levenshteinDistance(s1, s2)
        return (1.0 - (distance.toDouble() / maxLen)).coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                dp[j] = if (s1[i - 1] == s2[j - 1]) {
                    prev
                } else {
                    min(prev + 1, min(dp[j] + 1, dp[j - 1] + 1))
                }
                prev = temp
            }
        }
        return dp[s2.length]
    }

    /**
     * Evaluates discrepancy between certificate value and official value for a specific field.
     */
    fun auditField(
        field: CertificateFieldType,
        certValue: String,
        officialValue: String
    ): FieldDiscrepancy {
        val trimmedCert = certValue.trim()
        val trimmedOff = officialValue.trim()
        val isExactMatch = trimmedCert == trimmedOff
        val isCaseInsensitiveMatch = trimmedCert.equals(trimmedOff, ignoreCase = true)

        val diffs = alignCharacters(trimmedOff, trimmedCert)
        val similarity = calculateSimilarity(trimmedOff, trimmedCert)

        val (severity, desc) = when {
            isExactMatch -> {
                DiscrepancySeverity.NONE to "Exact match with official registry."
            }
            isCaseInsensitiveMatch -> {
                DiscrepancySeverity.MINOR_TYPO to "Case capitalization variance detected."
            }
            field == CertificateFieldType.ROLL_NUMBER || field == CertificateFieldType.SERIAL_NUMBER || field == CertificateFieldType.GRADE_CGPA -> {
                DiscrepancySeverity.CRITICAL_ERROR to "Critical identifier discrepancy in ${field.displayName}! Found '$trimmedCert', registry records '$trimmedOff'."
            }
            similarity >= 0.85 -> {
                val typoCount = diffs.count { it.type != DiffType.MATCH }
                DiscrepancySeverity.MINOR_TYPO to "Minor typographical difference ($typoCount characters mismatch, ${(similarity * 100).toInt()}% match)."
            }
            similarity >= 0.50 -> {
                DiscrepancySeverity.SIGNIFICANT_MISMATCH to "Significant discrepancy detected ($trimmedCert vs $trimmedOff)."
            }
            else -> {
                DiscrepancySeverity.CRITICAL_ERROR to "Severe discrepancy! Found '$trimmedCert', registry expects '$trimmedOff'."
            }
        }

        return FieldDiscrepancy(
            field = field,
            certificateValue = certValue,
            officialValue = officialValue,
            diffs = diffs,
            similarityScore = similarity,
            isMatch = isExactMatch,
            severity = severity,
            description = desc
        )
    }
}
