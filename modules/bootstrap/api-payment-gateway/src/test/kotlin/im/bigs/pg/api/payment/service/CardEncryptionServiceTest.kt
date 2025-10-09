package im.bigs.pg.api.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.api.payment.config.EncryptionConfig
import im.bigs.pg.api.payment.dto.CardInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CardEncryptionServiceTest {

    private val objectMapper = ObjectMapper()
    private val encryptionConfig = EncryptionConfig(
        apiKey = "11111111-1111-4111-8111-111111111111",
        ivBase64Url = "AAAAAAAAAAAAAAAA",
    )
    private val cardEncryptionService = CardEncryptionService(objectMapper, encryptionConfig)

    @Test
    fun `카드 정보 암호화 성공`() {
        // given
        val cardInfo = CardInfo(
            cardBin = "1234567890123456",
            cardLast4 = "1234",
            birthDate = "19900101",
            expiry = "1225",
            password = "1234",
        )
        val amount = BigDecimal("10000")

        // when
        val result = cardEncryptionService.encryptCardInfo(cardInfo, amount)

        // then
        assertNotNull(result.encryptedData)
        assertNotNull(result.iv)
        assertEquals(encryptionConfig.ivBase64Url, result.iv)
        assertTrue(result.encryptedData.isNotEmpty())
    }

    @Test
    fun `잘못된 IV로 인한 암호화 실패`() {
        // given
        val invalidConfig = EncryptionConfig(
            apiKey = "11111111-1111-4111-8111-111111111111",
            ivBase64Url = "invalid-iv", // 잘못된 IV
        )
        val invalidService = CardEncryptionService(objectMapper, invalidConfig)

        val cardInfo = CardInfo(
            cardBin = "1234567890123456",
            cardLast4 = "1234",
            birthDate = "19900101",
            expiry = "1225",
            password = "1234",
        )
        val amount = BigDecimal("10000")

        // when & then
        assertThrows<CardEncryptionException> {
            invalidService.encryptCardInfo(cardInfo, amount)
        }
    }
}
