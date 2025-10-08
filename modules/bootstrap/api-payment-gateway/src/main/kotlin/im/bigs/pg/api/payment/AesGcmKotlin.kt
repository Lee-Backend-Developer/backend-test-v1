package im.bigs.pg.api.payment

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

fun main() {
    val apiKey = "11111111-1111-4111-8111-111111111111"
    val ivB64Url = "AAAAAAAAAAAAAAAA"
    val plaintext = """
  {
    "cardNumber": "1111-1111-1111-1111",
    "birthDate": "19900101",
    "expiry": "1227",
    "password": "12",
    "amount": 10000
  }
  """.trimIndent()

    val enc = encryptToBase64Url(apiKey, ivB64Url, plaintext)
    println(enc)
}