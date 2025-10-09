package im.bigs.pg.api.payment.handler

import im.bigs.pg.api.payment.PaymentController
import im.bigs.pg.external.pg.CardForgeryException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [PaymentController::class])
class PaymentExceptionHandler {
    private val logger = LoggerFactory.getLogger(PaymentExceptionHandler::class.java)

    @ExceptionHandler(CardForgeryException::class)
    fun handleCardForgery(ex: CardForgeryException): ResponseEntity<ErrorResponse> {
        logger.warn("PG approval rejected: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = ex.message ?: "PG approval rejected"))
    }
}

data class ErrorResponse(
    val message: String,
)
