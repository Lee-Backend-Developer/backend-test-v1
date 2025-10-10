package im.bigs.pg.domain.payment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentStatusTest {
    @Test
    fun `APPROVED 문자열을 상태로 변환`() {
        val status = PaymentStatus.fromRaw("APPROVED")
        assertEquals(PaymentStatus.APPROVED, status)
    }

    @Test
    fun `CANCELLED 문자열도 CANCELED 로 인식`() {
        val status = PaymentStatus.fromRaw("Cancelled")
        assertEquals(PaymentStatus.CANCELED, status)
    }

    @Test
    fun `알 수 없는 상태는 null`() {
        val status = PaymentStatus.fromRaw("UNKNOWN")
        assertNull(status)
    }
}
