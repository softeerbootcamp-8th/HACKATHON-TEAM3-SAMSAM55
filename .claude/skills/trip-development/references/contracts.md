# API Contracts

## 응답 형식

**팀 확정 규칙:** 성공/실패 모두 같은 3개 필드(`success`, `data`, `error`)를
가진 동일한 응답 구조를 쓴다. 필드를 생략하지 않고, 쓰이지 않는 쪽은
`null`로 채운다 (레퍼런스로 쓰던 다른 팀 프로젝트처럼 `@JsonInclude(NON_NULL)`로
필드를 숨기지 않는다).

성공 응답:

    {
      "success": true,
      "data": { "id": 1, "name": "홍길동" },
      "error": null
    }

데이터 없는 성공 응답도 `data`는 그대로 두고 값만 비운다(`null` 또는 `{}`,
엔드포인트마다 자연스러운 쪽으로 팀과 확인):

    {
      "success": true,
      "data": null,
      "error": null
    }

에러 응답:

    {
      "success": false,
      "data": null,
      "error": {
        "code": "TRIP_NOT_FOUND",
        "message": "여행을 찾을 수 없습니다."
      }
    }

- `error.code`는 `ErrorType` enum 상수 이름을 그대로 문자열로 노출한다
  (SCREAMING_SNAKE_CASE). HTTP status는 `ErrorType`에 함께 정의하지만
  응답 바디에는 넣지 않고 응답의 HTTP status line으로만 쓴다.
- 구현 쪽 규칙(`ApplicationException`, `GlobalExceptionHandler`, `ErrorType`)은
  [backend.md](backend.md)를 본다.

## 미확정 사항

다음은 팀 합의 없이 임의로 결정하지 않는다.

- API prefix (`/api/v1` 등 버전 규칙)와 인증 방식
- validation 실패처럼 필드별 에러가 여러 개일 때의 표현 방식
  (현재 `error`는 `code`/`message` 단일 객체만 정의됨)
- 페이지네이션 방식 (offset vs cursor)
- 영속성 스택(JPA·MySQL) 도입 시점 — 현재 `build.gradle`에는 아직 없음

## 계약 변경

1. 확정 명세인지 확인한다.
2. 프론트 화면, 백엔드 endpoint, 테스트 영향을 찾는다.
3. 하위 호환성과 배포 순서를 정한다.
4. 서버·클라이언트·문서를 함께 수정한다.
5. 통합 흐름을 검증한다.
