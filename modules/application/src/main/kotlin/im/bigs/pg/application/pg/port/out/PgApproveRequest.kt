package im.bigs.pg.application.pg.port.out

import java.math.BigDecimal

/** PG 승인 요청 최소 정보. */
data class PgApproveRequest(
    val partnerId: Long,
    val amount: BigDecimal,
    val cardBin: String?,
    val cardLast4: String?,
    val productName: String?,
    val encryptedCard: EncryptedCardPayload,
)

/** 외부 PG에 전달할 암호화된 카드 데이터. */
data class EncryptedCardPayload(
    val enc: String,
    val iv: String,
)
