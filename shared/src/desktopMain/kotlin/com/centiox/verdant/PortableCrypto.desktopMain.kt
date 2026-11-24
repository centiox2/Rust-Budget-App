package com.centiox.verdant

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.*

private fun key(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, 600_000, 256)
    return try {
        SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES",
        )
    } finally {
        spec.clearPassword()
    }
}

actual fun protectBackup(json: String, password: String): String {
    require(password.length in 12..256) { "Use a backup password between 12 and 256 characters." }
    require(json.encodeToByteArray().size <= 5_000_000) { "Backup is too large." }
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
    cipher.updateAAD("Verdant portable backup v1".toByteArray())
    fun b(value: ByteArray) = Base64.getEncoder().encodeToString(value)
    return buildJsonObject {
            put("verdantEncrypted", 1)
            put("salt", b(salt))
            put("iv", b(iv))
            put("ciphertext", b(cipher.doFinal(json.toByteArray(Charsets.UTF_8))))
        }
        .toString()
}

actual fun unprotectBackup(envelope: String, password: String): String {
    require(envelope.length <= 8_000_000 && password.length in 1..256) {
        "Invalid encrypted backup."
    }
    try {
        val obj = Json.parseToJsonElement(envelope).jsonObject
        require(obj["verdantEncrypted"]?.jsonPrimitive?.int == 1)
        fun b(name: String) = Base64.getDecoder().decode(obj.getValue(name).jsonPrimitive.content)
        val salt = b("salt")
        val iv = b("iv")
        val bytes = b("ciphertext")
        require(salt.size == 16 && iv.size == 12 && bytes.size in 16..5_000_016)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD("Verdant portable backup v1".toByteArray())
        return cipher.doFinal(bytes).toString(Charsets.UTF_8)
    } catch (_: Exception) {
        throw IllegalArgumentException(
            "Incorrect password or damaged backup. Your ledger is unchanged."
        )
    }
}

actual fun calculateAmount(expression: String): Long {
    require(expression.length in 1..128) { "Enter an expression up to 128 characters." }
    val text = expression.replace('×', '*').replace('÷', '/').replace('−', '-').replace(',', '.')
    class Parser {
        var index = 0

        fun space() {
            while (index < text.length && text[index].isWhitespace()) index++
        }

        fun take(c: Char): Boolean {
            space()
            if (index < text.length && text[index] == c) {
                index++
                return true
            }
            return false
        }

        fun factor(): java.math.BigDecimal {
            if (take('+')) return factor()
            if (take('-')) return factor().negate()
            if (take('(')) {
                val v = sum()
                require(take(')')) { "Close the parentheses." }
                return v
            }
            space()
            val start = index
            while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
            require(index > start) { "Enter a number." }
            val number =
                text.substring(start, index).toBigDecimalOrNull() ?: error("Invalid number.")
            require(number.precision() <= 30 && number.scale() <= 8) {
                "Use smaller numbers with at most eight decimal places."
            }
            return number
        }

        fun product(): java.math.BigDecimal {
            var value = factor()
            while (true) value =
                when {
                    take('*') -> value.multiply(factor(), java.math.MathContext.DECIMAL128)
                    take('/') -> {
                        val divisor = factor()
                        require(divisor.signum() != 0) { "Cannot divide by zero." }
                        value.divide(divisor, java.math.MathContext.DECIMAL128)
                    }
                    else -> return value
                }
        }

        fun sum(): java.math.BigDecimal {
            var value = product()
            while (true) value =
                when {
                    take('+') -> value.add(product())
                    take('-') -> value.subtract(product())
                    else -> return value
                }
        }
    }
    val parser = Parser()
    val result = parser.sum()
    parser.space()
    require(parser.index == text.length) { "Use numbers, +, −, ×, ÷, and parentheses." }
    val cents =
        try {
            result.setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            error("Result is too large.")
        }
    require(cents in 1..MAX_AMOUNT) { "Result must be positive and at most 1 billion." }
    return cents
}
