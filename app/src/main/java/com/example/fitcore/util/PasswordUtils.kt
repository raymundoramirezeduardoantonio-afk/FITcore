package com.example.fitcore.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.nio.charset.StandardCharsets

/**
 * Nuevo: `s1$` + sal hex (16 bytes) + `$` + SHA-256 hex de (sal || password UTF-8).
 * Legado: 64 hex de SHA-256(password) sin sal.
 */
object PasswordUtils {
    private const val PREFIX = "s1"
    private const val SALT_BYTES = 16
    private val LEGACY = Regex("^[0-9a-fA-F]{64}$")
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        val digest = sha256(salt + password.toByteArray(StandardCharsets.UTF_8))
        return PREFIX + "$" + toHex(salt) + "$" + toHex(digest)
    }

    fun isLegacyHash(stored: String): Boolean = LEGACY.matches(stored)

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size == 3 && parts[0] == PREFIX) {
            val salt = decodeHex(parts[1], SALT_BYTES) ?: return false
            val expected = decodeHex(parts[2], 32) ?: return false
            val actual = sha256(salt + password.toByteArray(StandardCharsets.UTF_8))
            return MessageDigest.isEqual(actual, expected)
        }
        if (!isLegacyHash(stored)) return false
        val expected = decodeHex(stored, 32) ?: return false
        val actual = sha256(password.toByteArray(StandardCharsets.UTF_8))
        return MessageDigest.isEqual(actual, expected)
    }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun toHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun decodeHex(hex: String, expectedBytes: Int): ByteArray? {
        if (hex.length != expectedBytes * 2) return null
        val out = ByteArray(expectedBytes)
        for (i in 0 until expectedBytes) {
            val byte = hex.substring(i * 2, i * 2 + 2).toIntOrNull(16) ?: return null
            out[i] = byte.toByte()
        }
        return out
    }
}
