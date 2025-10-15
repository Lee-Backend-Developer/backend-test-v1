package im.bigs.pg.api.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import im.bigs.pg.api.payment.dto.CardInfo
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.service.CardEncryptionService
import im.bigs.pg.api.payment.service.EncryptedCardResult
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(RestDocumentationExtension::class)
class PaymentControllerTest @Autowired constructor(
    private val objectMapper: ObjectMapper,
) {
    private lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var paymentUseCase: PaymentUseCase

    @MockkBean
    lateinit var queryPaymentsUseCase: QueryPaymentsUseCase

    @MockkBean
    lateinit var cardEncryptionService: CardEncryptionService

    @BeforeEach
    fun setUp(
        webApplicationContext: WebApplicationContext,
        restDocumentation: RestDocumentationContextProvider,
    ) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(documentationConfiguration(restDocumentation))
            .build()
    }

    @Test
    fun `POST api v1 payments returns created payment information`() {
        val request = CreatePaymentRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardInfo = CardInfo(
                cardBin = "123456",
                cardLast4 = "7890",
                birthDate = "19900101",
                expiry = "1227",
                password = "12",
            ),
            productName = "테스트 상품",
        )

        val approvalTime = LocalDateTime.of(2024, 5, 1, 10, 30)
        val payment = Payment(
            id = 42L,
            partnerId = request.partnerId,
            amount = request.amount,
            appliedFeeRate = BigDecimal("0.0300"),
            feeAmount = BigDecimal("300"),
            netAmount = BigDecimal("9700"),
            cardBin = request.cardInfo.cardBin,
            cardLast4 = request.cardInfo.cardLast4,
            approvalCode = "APR-001",
            approvedAt = approvalTime,
            status = PaymentStatus.APPROVED,
            createdAt = approvalTime,
            updatedAt = approvalTime,
        )

        every { cardEncryptionService.encryptCardInfo(any(), any()) } returns EncryptedCardResult(
            encryptedData = "enc-data",
            iv = "iv-data",
        )
        every { paymentUseCase.pay(any()) } returns payment

        mockMvc.post("/api/v1/payments") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(payment.id) }
                jsonPath("$.amount") { value(payment.amount.toInt()) }
                jsonPath("$.appliedFeeRate") { value(payment.appliedFeeRate.toDouble()) }
                jsonPath("$.feeAmount") { value(payment.feeAmount.toInt()) }
                jsonPath("$.netAmount") { value(payment.netAmount.toInt()) }
                jsonPath("$.cardLast4") { value(payment.cardLast4) }
                jsonPath("$.approvalCode") { value(payment.approvalCode) }
                jsonPath("$.status") { value(payment.status.name) }
            }
            .andDo {
                handle(
                    document(
                        "payment-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                            fieldWithPath("partnerId").type(JsonFieldType.NUMBER).description("제휴사 ID"),
                            fieldWithPath("amount").type(JsonFieldType.NUMBER).description("결제 금액"),
                            fieldWithPath("cardInfo").type(JsonFieldType.OBJECT).description("카드 정보"),
                            fieldWithPath("cardInfo.cardBin").type(JsonFieldType.STRING).description("카드 BIN (앞 6자리)"),
                            fieldWithPath("cardInfo.cardLast4").type(JsonFieldType.STRING).description("카드 뒤 4자리"),
                            fieldWithPath("cardInfo.birthDate").type(JsonFieldType.STRING).description("생년월일 (YYYYMMDD)"),
                            fieldWithPath("cardInfo.expiry").type(JsonFieldType.STRING).description("유효기간 (MMYY)"),
                            fieldWithPath("cardInfo.password").type(JsonFieldType.STRING).description("카드 비밀번호 앞 2자리"),
                            fieldWithPath("productName").type(JsonFieldType.STRING).description("상품명")
                        ),
                        responseFields(
                            fieldWithPath("id").type(JsonFieldType.NUMBER).description("결제 ID"),
                            fieldWithPath("partnerId").type(JsonFieldType.NUMBER).description("제휴사 ID"),
                            fieldWithPath("amount").type(JsonFieldType.NUMBER).description("결제 금액"),
                            fieldWithPath("appliedFeeRate").type(JsonFieldType.NUMBER).description("적용된 수수료율"),
                            fieldWithPath("feeAmount").type(JsonFieldType.NUMBER).description("수수료 금액"),
                            fieldWithPath("netAmount").type(JsonFieldType.NUMBER).description("정산 금액"),
                            fieldWithPath("cardLast4").type(JsonFieldType.STRING).description("카드 뒤 4자리"),
                            fieldWithPath("approvalCode").type(JsonFieldType.STRING).description("승인 번호"),
                            fieldWithPath("approvedAt").type(JsonFieldType.STRING).description("승인 일시"),
                            fieldWithPath("status").type(JsonFieldType.STRING).description("결제 상태"),
                            fieldWithPath("createdAt").type(JsonFieldType.STRING).description("생성 일시")
                        )
                    )
                )
            }
    }

    @Test
    fun `GET api v1 payments returns paginated payments with summary`() {
        val approvalTime = LocalDateTime.of(2024, 5, 2, 11, 0)
        val payment = Payment(
            id = 100L,
            partnerId = 2L,
            amount = BigDecimal("5000"),
            appliedFeeRate = BigDecimal("0.0200"),
            feeAmount = BigDecimal("100"),
            netAmount = BigDecimal("4900"),
            cardBin = "654321",
            cardLast4 = "3210",
            approvalCode = "APR-002",
            approvedAt = approvalTime,
            status = PaymentStatus.APPROVED,
            createdAt = approvalTime,
            updatedAt = approvalTime,
        )

        val queryResult = QueryResult(
            items = listOf(payment),
            summary = PaymentSummary(
                count = 1,
                totalAmount = payment.amount,
                totalNetAmount = payment.netAmount,
            ),
            nextCursor = "next-cursor",
            hasNext = true,
        )

        every { queryPaymentsUseCase.query(any<QueryFilter>()) } returns queryResult

        mockMvc.get("/api/v1/payments") {
            param("partnerId", "2")
            param("status", "APPROVED")
            param("limit", "20")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.items[0].id") { value(payment.id) }
                jsonPath("$.items[0].amount") { value(payment.amount.toInt()) }
                jsonPath("$.summary.count") { value(1) }
                jsonPath("$.summary.totalAmount") { value(payment.amount.toInt()) }
                jsonPath("$.summary.totalNetAmount") { value(payment.netAmount.toInt()) }
                jsonPath("$.nextCursor") { value("next-cursor") }
                jsonPath("$.hasNext") { value(true) }
            }
            .andDo {
                handle(
                    document(
                        "payment-query",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                            parameterWithName("partnerId").description("제휴사 ID (선택)").optional(),
                            parameterWithName("status").description("결제 상태 (선택)").optional(),
                            parameterWithName("from").description("조회 시작 일시 (선택, ISO-8601)").optional(),
                            parameterWithName("to").description("조회 종료 일시 (선택, ISO-8601)").optional(),
                            parameterWithName("cursor").description("커서 (선택, 다음 페이지 조회용)").optional(),
                            parameterWithName("limit").description("페이지 크기 (선택, 기본값 20)").optional()
                        ),
                        responseFields(
                            fieldWithPath("items[]").type(JsonFieldType.ARRAY).description("결제 내역 목록"),
                            fieldWithPath("items[].id").type(JsonFieldType.NUMBER).description("결제 ID"),
                            fieldWithPath("items[].partnerId").type(JsonFieldType.NUMBER).description("제휴사 ID"),
                            fieldWithPath("items[].amount").type(JsonFieldType.NUMBER).description("결제 금액"),
                            fieldWithPath("items[].appliedFeeRate").type(JsonFieldType.NUMBER).description("적용된 수수료율"),
                            fieldWithPath("items[].feeAmount").type(JsonFieldType.NUMBER).description("수수료 금액"),
                            fieldWithPath("items[].netAmount").type(JsonFieldType.NUMBER).description("정산 금액"),
                            fieldWithPath("items[].cardLast4").type(JsonFieldType.STRING).description("카드 뒤 4자리"),
                            fieldWithPath("items[].approvalCode").type(JsonFieldType.STRING).description("승인 번호"),
                            fieldWithPath("items[].approvedAt").type(JsonFieldType.STRING).description("승인 일시"),
                            fieldWithPath("items[].status").type(JsonFieldType.STRING).description("결제 상태"),
                            fieldWithPath("items[].createdAt").type(JsonFieldType.STRING).description("생성 일시"),
                            fieldWithPath("summary").type(JsonFieldType.OBJECT).description("통계 정보"),
                            fieldWithPath("summary.count").type(JsonFieldType.NUMBER).description("총 건수"),
                            fieldWithPath("summary.totalAmount").type(JsonFieldType.NUMBER).description("총 결제 금액"),
                            fieldWithPath("summary.totalNetAmount").type(JsonFieldType.NUMBER).description("총 정산 금액"),
                            fieldWithPath("nextCursor").type(JsonFieldType.STRING).description("다음 페이지 커서").optional(),
                            fieldWithPath("hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부")
                        )
                    )
                )
            }
    }
}
