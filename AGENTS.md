# Repository Guidelines

## 프로젝트 구조 및 모듈 구성
- 레포는 헥사고널 아키텍처 멀티모듈입니다. 비즈니스 규칙은 `modules/domain`에 두고 프레임워크 의존성을 차단하세요.
- `modules/application`은 유스케이스와 서비스 오케스트레이션을 담당하며, 트랜잭션 경계와 수수료 정책 계산을 여기서 제어합니다.
- `modules/infrastructure/persistence`에는 Spring Data JPA 어댑터와 조회 쿼리가 위치합니다. 커서 페이지네이션·통계 로직을 변경하면 이 모듈의 테스트도 갱신하세요.
- PG 연동과 목 구현은 `modules/external/pg-client`에, 실행 가능한 Spring Boot API와 설정은 `modules/bootstrap/api-payment-gateway`에 있습니다.
- 스키마 및 초기 데이터는 `sql/scheme.sql`에서 관리하므로 테이블·인덱스 계약이 바뀌면 스크립트와 코드를 함께 갱신하세요.

## 빌드 · 테스트 · 개발 명령어
- `./gradlew build` – 전체 컴파일과 테스트 실행; 제출 전 필수.
- `./gradlew test` – JUnit 5 + MockK 단위·통합 테스트만 수행.
- `./gradlew :modules:bootstrap:api-payment-gateway:bootRun` – 기본 H2 환경에서 API를 기동해 수동 검증.
- `./gradlew ktlintCheck` / `ktlintFormat` – Kotlin 포매터 규칙 검사 및 자동 수정.
- `./gradlew clean` – 모듈 구조를 크게 손본 뒤 캐시를 초기화할 때 사용.

## 코딩 스타일 및 네이밍 규칙
Kotlin 4칸 들여쓰기, 후행 쉼표 비사용, 공개 API 가시성 명시를 기본으로 합니다. 클래스·객체는 PascalCase, 함수·프로퍼티는 camelCase, 상수는 UPPER_SNAKE_CASE를 사용하세요. 패키지는 `im.payment.<layer>` 패턴을 유지하고, `modules/domain`에서는 data class·sealed 계층으로 도메인 규칙을 표현합니다. 프레임워크 애노테이션은 애플리케이션 이후 계층으로 한정하고, 복잡한 계산은 간결한 KDoc으로 맥락을 남깁니다.

## 테스트 가이드라인
각 모듈의 `src/test/kotlin`에 테스트를 배치하고 클래스 이름은 `<대상>Test`, 테스트 메서드는 백틱을 활용한 설명형 이름을 씁니다. 수수료 정책과 정산 계산은 도메인 테스트로 경계값을 다루고, 리포지토리 쿼리·프로젝션 변경 시 통합 테스트를 추가하세요. 외부 PG 시나리오는 목 어댑터로 고정하여 결정적 결과를 확보합니다. 커밋 전 `./gradlew test`를 실행하고 H2 기준 SQL과 코드가 일치하는지 확인하세요.

## 커밋 및 PR 가이드라인
Git 로그에서 사용 중인 `type: summary` 포맷을 유지하세요. 예: `feat: apply partner fee policy`. 제목은 60자 내외, 본문은 72자 폭으로 줄바꿈하며 변경 이유와 영향을 기술합니다. 테스트·스키마·문서 변경은 의존 코드를 포함한 동일 커밋으로 묶습니다. PR에는 관련 이슈 링크, 핵심 변화, `./gradlew test` 결과, 스키마·설정 영향, REST API 수정 시 요청·응답 예시를 포함하세요.

## 보안 및 구성 유의사항
카드 정보는 README에 안내된 대로 마스킹하거나 저장하지 말고, 외부 API 자격 증명은 프로필·환경 변수로 주입하세요. H2 외 DB를 사용할 경우 `application.yml` 대신 별도 프로필을 추가해 로컬과 운영 구성을 분리합니다.
