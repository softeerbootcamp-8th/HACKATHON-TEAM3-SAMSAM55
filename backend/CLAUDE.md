# SAMSAM55 Backend

SAMSAM55의 Spring 백엔드다. 이 문서는 백엔드의 상시 규칙만 정의한다.
상세 규칙은 작업에 맞춰 `.claude/skills/trip-development/`를 참고한다.

## 기술과 구조

- Java 21, Spring Boot 4.1.x, JUnit 5 + Mockito를 기본 스택으로 사용한다.
- **영속성 계층(JPA·MySQL)은 아직 도입 전이다.** `build.gradle`에는
  `spring-boot-starter-webmvc`만 있다. 로컬 MySQL은 docker compose로
  함께 띄우기로 했지만, JPA 등 도입 시점과 방식은 팀 확인 없이 정하지 않는다.
- 도메인형 패키지, `Controller → Service → Repository` 구조를 따른다
- 트랜잭션은 Service에서만 관리한다.

## 필수 규칙

- 관련 코드·테스트·API 명세를 먼저 읽고 최소 범위만 수정한다.
- Entity를 요청·응답으로 직접 사용하지 않는다.
- Request/Response DTO를 분리하고 `record`로 작성한다 (정적 팩토리 `from`/`of`).
- 필드 형식 검증은 DTO(Bean Validation)에서, 비즈니스 규칙 검증은 Service에서 한다.
- `ApplicationException`, 도메인별 `ErrorType`, `GlobalExceptionHandler`를 사용한다.
- 생성자 주입을 사용한다.
- API 응답 형식(`success`/`data`/`error`)과 에러 코드는 팀 확인 없이 임의 변경하지 않는다.
- 기능을 구현한 뒤 그 기능의 테스트를 작성한다 (TDD로 테스트를 먼저 쓰지 않는다 — 팀 결정).

## 금지

- Controller의 비즈니스 로직 또는 트랜잭션
- `System.out.println()`, `e.printStackTrace()`, 필드 주입(`@Autowired` on field)
- 팀 확인 없는 의존성·API·DB 계약 변경
- `DROP`, `TRUNCATE`, `docker compose down -v`
- `git push --force`, `git reset --hard`, `git clean`
- 요청받지 않은 대규모 리팩터링

## 검증과 완료 보고

- Gradle Wrapper(`./gradlew test`, `./gradlew build`)로 빌드·테스트를 실행한다.
- 변경한 기능의 단위 테스트를 실행한다.
- 완료 시 변경 내용, 계약 영향, 테스트 결과, 미검증 사항을 보고한다.

## 상세 규칙 라우팅

- 백엔드 구현: `.claude/skills/trip-development/references/backend.md`
- API 계약: `.claude/skills/trip-development/references/contracts.md`
- 협업·Git·환경·안전: `.claude/skills/trip-development/references/workflow.md`
