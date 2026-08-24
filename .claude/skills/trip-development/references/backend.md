# Spring Backend

## 구조

- Java 21, Spring Boot 4.1.x (`build.gradle` 기준, 정확한 버전은 그때그때 확인)
- **JPA·MySQL은 아직 도입 전이다.** 현재 `build.gradle`에는 `spring-boot-starter-webmvc`만
  있다. MySQL은 로컬에서 docker compose로 함께 띄우기로 했지만, 영속성
  스택(JPA 등) 도입 시점과 방식은 팀 확인 없이 정하지 않는다.
- 도메인형 패키지 (`com.samsam55.trip.{도메인}` 아래 `controller`/`service`/`repository`/`dto`/`entity`)
- `Controller → Service → Repository`
- 트랜잭션은 Service에서만 관리

```
com.samsam55.trip
├── global
│   ├── config
│   ├── common
│   ├── exception
│   └── util
│
├── member
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
│
├── trip
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── entity
```

Controller는 요청·`@Valid`·응답만 담당한다.
Service는 권한·상태 전이·비즈니스 검증·트랜잭션을 담당한다.
Repository는 영속성 접근만 담당한다.

## DTO

- Entity 직접 반환 금지
- Request/Response 분리, DTO는 `record`로 작성
- 정적 팩토리 메소드(`from`, `of`)로 변환
- 내용이 같아도 재사용하지 않고 용도별로 분리한다
- Request DTO에 Bean Validation 어노테이션(`@NotNull`, `@Positive`, `@Size` 등) 부착
- 컨트롤러 파라미터에 `@Valid` 필수
- 단순 필드 검증(형식, null 여부)은 DTO에서, 비즈니스 규칙 검증은 Service에서 처리
- 네이밍: `{도메인}{동작}RequestDto` / `{도메인}{동작}ResponseDto`
  - 예: `TripRoomCreateRequestDto`, `TripRoomCreateResponseDto`, `ScheduleCardDetailResponseDto`

## 예외

- 상위 예외 클래스 하나: `ApplicationException`
- `GlobalExceptionHandler`에서 전역적으로 에러를 응답으로 변환한다
- `ErrorType` enum으로 도메인별 에러를 관리한다 (`httpStatus`, `code`, `message`)
  - `code`는 enum 상수 이름을 그대로 쓴다 (예: `TRIP_NOT_FOUND`)
  - 응답 바디 형식은 [contracts.md](contracts.md)를 따른다

## Swagger

- 도메인 API를 컨트롤러에 바로 쓰지 않고, `<도메인>Api` 인터페이스로 분리해서 작성한다
- 해당 인터페이스를 컨트롤러가 구현한다
- 컨트롤러마다 `@Tag`로 도메인 그룹핑, 메서드마다 `@Operation`으로 설명 부착
  - 예시 데이터와 상황별 에러 응답도 함께 명시한다

## 네이밍

자바 컨벤션을 따른다. 예시는 이 프로젝트(여행 일정) 도메인 기준.

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스, 인터페이스 | PascalCase | `TripRoomService` |
| 변수, 메서드명 | camelCase | `addScheduleCard()` |
| 상수 | SCREAMING_SNAKE_CASE | `MAX_CANDIDATES_PER_CARD` |
| boolean 변수/메서드 | is/has 접두어 | `isConfirmed`, `hasVoted()` |
| 컬렉션 | 복수형 | `scheduleCards`, `candidates` |
| URL(REST 엔드포인트) | kebab-case, 복수 명사 | `/trip-rooms`, `/schedule-cards` |
| DB 테이블/컬럼 | snake_case + 복수 | `schedule_cards` |

## 테스트

- JUnit 5 + Mockito
- 테스트 메서드명은 한글
- **이 프로젝트는 구현을 먼저 하고, 그 기능에 대한 테스트를 작성하는 순서를
  따른다** (TDD로 테스트를 먼저 쓰지 않는다 — 팀이 그렇게 정했다).
- 권한, 검증 실패, 경계값, 동시 요청처럼 실수하기 쉬운 지점을 우선 검증한다.
- JPA·MySQL이 도입되면 `@DataJpaTest` 등 DB 연결이 필요한 테스트는
  개발자 로컬 MySQL과 격리된 별도 컨테이너(Testcontainers 등) 사용을
  검토한다 — 도입 전이라 현재는 해당 없음.

## 금지

- 필드 주입(`@Autowired` on field), 생성자 주입만 사용
- `System.out.println()`, `e.printStackTrace()` — 예외는 `ApplicationException`으로
  감싸고 slf4j `ERROR` 레벨로 로깅한다
- Controller의 비즈니스 로직
- 예외 무시
- 트랜잭션 전 성공 처리
- 근거 없는 캐시와 비동기화
