package im.bigs.pg.api.payment.util

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

fun encryptToBase64Url(apiKey: String, ivB64Url: String, plaintext: String): String {
    val keyBytes = MessageDigest.getInstance("SHA-256")
        .digest(apiKey.toByteArray(StandardCharsets.UTF_8))
    val secretKey = SecretKeySpec(keyBytes, "AES")

    val iv = Base64.getUrlDecoder().decode(padBase64Url(ivB64Url))
    require(iv.size == 12) { "IV must be 12 bytes for GCM" }

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

    val cipherWithTag = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(cipherWithTag)
}

private fun padBase64Url(value: String): String =
    value.padEnd(value.length + (4 - value.length % 4) % 4, '=')
