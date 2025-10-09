package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * TestPg REST 연동용 PG 클라이언트.
 */
@Component
class PgClient(
    builder: WebClient.Builder,
    @Value("${'$'}{pg.client.base-url:https://api-test-pg.bigs.im}") baseUrl: String,
    @Value("${'$'}{pg.client.api-key:11111111-1111-4111-8111-111111111111}") private val apiKey: String,
) : PgClientOutPort {
    private val logger = LoggerFactory.getLogger(PgClient::class.java)
    private val webClient: WebClient = builder.baseUrl(baseUrl.trimEnd('/')).build()

    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 1L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val response = webClient.post()
            .uri(PAY_ENDPOINT)
            .header(API_KEY_HEADER, apiKey)
            .bodyValue(TestPgApproveRequest(enc = request.encryptedCard.enc))
            .retrieve()
            .onStatus({ !it.is2xxSuccessful }) { clientResponse ->
                clientResponse.bodyToMono(String::class.java)
                    .defaultIfEmpty("(empty body)")
                    .flatMap { body ->
                        val message = "TestPg approval failed: HTTP ${clientResponse.statusCode().value()} $body"
                        Mono.error(CardForgeryException(message))
                    }
            }
            .bodyToMono(TestPgApproveResponse::class.java)
            .block() ?: throw CardForgeryException("TestPg approval returned no body")

        if (!response.isSuccessful()) {
            val message = response.resultMessage ?: "TestPg rejected card"
            throw CardForgeryException(message)
        }

        val approvalCode = response.approvalCode
            ?: throw CardForgeryException("TestPg response missing approvalCode")

        val approvedAt = response.parseApprovedAt() ?: LocalDateTime.now(ZoneOffset.UTC)

        val status = response.status?.let { runCatching { PaymentStatus.valueOf(it) }.getOrNull() }
            ?: PaymentStatus.APPROVED

        return PgApproveResult(
            approvalCode = approvalCode,
            approvedAt = approvedAt,
            status = status,
        )
    }

    companion object {
        private const val API_KEY_HEADER = "API-KEY"
        private const val PAY_ENDPOINT = "/api/v1/pay/credit-card"
    }
}

private data class TestPgApproveRequest(
    val enc: String,
)

private data class TestPgApproveResponse(
    val approvalCode: String? = null,
    val approvedAt: String? = null,
    val status: String? = null,
    val resultCode: String? = null,
    val resultMessage: String? = null,
) {
    fun isSuccessful(): Boolean = resultCode == null || resultCode == SUCCESS_CODE

    fun parseApprovedAt(): LocalDateTime? = approvedAt?.let { raw ->
        try {
            OffsetDateTime.parse(raw).toLocalDateTime()
        } catch (ex: Exception) {
            try {
                LocalDateTime.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                null
            }
        }
    }

    companion object {
        private const val SUCCESS_CODE = "0000"
    }
}

class CardForgeryException(message: String) : RuntimeException(message) {
    override fun fillInStackTrace(): Throwable = this
}
