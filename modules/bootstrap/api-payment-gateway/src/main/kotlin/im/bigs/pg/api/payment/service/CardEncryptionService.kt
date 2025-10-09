package im.bigs.pg.api.payment.service

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.api.payment.config.EncryptionConfig
import im.bigs.pg.api.payment.dto.CardInfo
import im.bigs.pg.api.payment.util.encryptToBase64Url
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * 카드 정보 암호화 서비스.
 * 카드 정보를 안전하게 암호화하여 외부 PG사에 전송할 수 있는 형태로 변환합니다.
 */
@Service
class CardEncryptionService(
    private val objectMapper: ObjectMapper,
    private val encryptionConfig: EncryptionConfig,
) {
    private val logger = LoggerFactory.getLogger(CardEncryptionService::class.java)

    /**
     * 카드 정보를 암호화합니다.
     *
     * @param cardInfo 암호화할 카드 정보
     * @param amount 결제 금액
     * @return 암호화된 카드 정보와 IV
     */
    fun encryptCardInfo(cardInfo: CardInfo, amount: BigDecimal): EncryptedCardResult {
        return try {
            val cardData = CardData(
                cardNumber = "${cardInfo.cardBin}-${cardInfo.cardLast4}",
                birthDate = cardInfo.birthDate,
                expiry = cardInfo.expiry,
                password = cardInfo.password,
                amount = amount,
            )

            val plaintext = objectMapper.writeValueAsString(cardData)
            val encrypted = encryptToBase64Url(
                encryptionConfig.apiKey,
                encryptionConfig.ivBase64Url,
                plaintext,
            )

            logger.debug("카드 정보 암호화 완료. 길이: ${encrypted.length}")

            EncryptedCardResult(
                encryptedData = encrypted,
                iv = encryptionConfig.ivBase64Url,
            )
        } catch (e: Exception) {
            logger.error("카드 정보 암호화 실패", e)
            throw CardEncryptionException("카드 정보 암호화에 실패했습니다.", e)
        }
    }
}

/**
 * 암호화된 카드 정보 결과
 */
data class EncryptedCardResult(
    val encryptedData: String,
    val iv: String,
)

/**
 * 암호화용 카드 데이터
 */
private data class CardData(
    val cardNumber: String,
    val birthDate: String,
    val expiry: String,
    val password: String,
    val amount: BigDecimal,
)

/**
 * 카드 암호화 예외
 */
class CardEncryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
