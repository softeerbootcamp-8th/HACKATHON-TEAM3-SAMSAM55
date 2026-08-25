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

## 확정된 사항

- 인증 방식: Session Cookie. 다만 HOST와 PARTICIPANT는 방식이 다르다.
  - HOST: 로그인 기반 서버 세션만 사용. 서버 재시작으로 세션이 날아가면 재로그인으로
    복구한다(비밀번호가 있어 문제없음).
  - PARTICIPANT: 평소에는 서버 세션(`actorType=PARTICIPANT`, `participantId`,
    `tripId`)으로 빠르게 인증한다. 추가로 `participantId`를 서버 비밀키로 서명한
    쿠키를 `Max-Age` 30일로 발급해둔다(설정 안 하면 브라우저 종료 시 사라지는 진짜
    세션 쿠키가 되어버리므로 주의). 이 쿠키는 평소엔 쓰지 않고, **세션이 없을 때만**
    (서버 재시작, 세션 만료 등) 검증용으로 쓴다 — 서명을 확인하고 DB에서
    `participant`가 실제 존재하는지 확인한 뒤 그 값으로 세션을 다시 발급해
    재로그인 없이 자동 복구한다. 참여자는 비밀번호가 없어 재로그인 수단이 없기
    때문에 이 복구용 쿠키가 필요하다.
- 페이지네이션: 사용하지 않는다. 목록 조회 API는 전체 목록을 한 번에 반환한다
  (`data.items` 배열, `nextCursor`/`hasNext` 등 페이지네이션 필드 없음)

## 미확정 사항

다음은 팀 합의 없이 임의로 결정하지 않는다.

- API prefix (`/api/v1` 등 버전 규칙)
- validation 실패처럼 필드별 에러가 여러 개일 때의 표현 방식
  (현재 `error`는 `code`/`message` 단일 객체만 정의됨)
- 영속성 스택(JPA·MySQL) 도입 시점 — 현재 `build.gradle`에는 아직 없음

## 계약 변경

1. 확정 명세인지 확인한다.
2. 프론트 화면, 백엔드 endpoint, 테스트 영향을 찾는다.
3. 하위 호환성과 배포 순서를 정한다.
4. 서버·클라이언트·문서를 함께 수정한다.
5. 통합 흐름을 검증한다.
