package im.bigs.pg.api.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 암호화 관련 설정.
 * application.yml에서 암호화에 필요한 설정값들을 관리합니다.
 */
@Configuration
@ConfigurationProperties(prefix = "payment.encryption")
data class EncryptionConfig(
    /**
     * PG사 API 키
     */
    var apiKey: String = "11111111-1111-4111-8111-111111111111",

    /**
     * 초기화 벡터 (Base64 URL 인코딩)
     */
    var ivBase64Url: String = "AAAAAAAAAAAAAAAA",
) {
    init {
        require(apiKey.isNotBlank()) { "API 키는 비어있을 수 없습니다." }
        require(ivBase64Url.isNotBlank()) { "IV는 비어있을 수 없습니다." }
    }
}
