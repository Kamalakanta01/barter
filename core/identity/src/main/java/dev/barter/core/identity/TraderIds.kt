package dev.barter.core.identity

import dev.barter.core.domain.TraderId
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Derives a stable pseudonymous identity from a phone number.
 *
 * The number is only ever used as HMAC input, so it is not recoverable from the resulting id, and
 * two different numbers produce unrelated ids. [deviceSecret] is what stops anyone who knows your
 * number from computing your id without holding your device's secret.
 */
object TraderIds {

    private const val ALGORITHM = "HmacSHA256"

    fun fromPhoneNumber(phoneNumber: String, deviceSecret: ByteArray): TraderId =
        TraderId(hmac(deviceSecret, phoneNumber.normalize()).toHex())

    /** Fallback for devices where the SIM reports no usable number; see [LocalTraderId]. */
    fun local(): TraderId = TraderId("0".repeat(ID_HEX_LENGTH))

    private fun hmac(key: ByteArray, message: String): ByteArray =
        Mac.getInstance(ALGORITHM).run {
            init(SecretKeySpec(key, ALGORITHM))
            doFinal(message.toByteArray(Charsets.UTF_8))
        }

    private fun String.normalize(): String {
        val compact = trim().filterNot { it.isWhitespace() || it == '-' || it == '(' || it == ')' }
        return if (compact.startsWith("+")) compact else "+$compact"
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    const val ID_HEX_LENGTH = 64
}
