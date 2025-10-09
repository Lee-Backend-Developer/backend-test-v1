package im.bigs.pg.api.payment.dto

import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class CreatePaymentRequest(
    val partnerId: Long,
    @field:Min(1)
    val amount: BigDecimal,             // 가격
    val cardInfo: CardInfo,     // 카드
    val productName: String,    // 결제 할 제품
)

data class CardInfo(
    val cardBin: String,        // 카드 번호
    val cardLast4: String,      // 카드 번호 뒤에서 4자리
    val birthDate: String,      // 생년월일
    val expiry: String,         // 만료 일
    val password: String,       // 카드 비밀번호
)
