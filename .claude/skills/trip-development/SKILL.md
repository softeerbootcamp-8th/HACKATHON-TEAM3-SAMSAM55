---
name: trip-development
description: SAMSAM55(부모-자녀 여행 일정 공동 결정 서비스)의 Spring 백엔드 기능 구현과 리뷰를 위한 팀 전용 개발 지침. API, 응답 형식, 에러 코드, DTO, 예외, 테스트, 공용 계약을 다루는 작업에 사용한다.
---

# Trip Development

작업 범위에 필요한 reference만 읽는다.

- API, 응답 형식, 에러 코드를 다루면 [contracts.md](references/contracts.md)를 읽는다.
- Spring 코드를 구현하거나 리뷰하면 [backend.md](references/backend.md)를 읽는다.
- Git, 환경 변수, PR 또는 위험 작업을 다루면 [workflow.md](references/workflow.md)를 읽는다.

## 작업 절차

1. 루트 및 작업 영역의 `CLAUDE.md`를 확인한다.
2. 관련 reference와 기존 코드·테스트를 읽는다.
3. 공용 계약(API 응답 형식, 에러 코드) 영향을 먼저 식별한다.
4. 최소 범위로 구현한다.
5. 변경 영역과 통합 경계를 검증한다.
6. 변경 내용, 계약 영향, 검증 결과와 남은 위험을 보고한다.

불명확한 계약을 한쪽 영역의 편의로 결정하지 않는다.
API 명세가 초안 또는 검토 중이면 확정 여부를 확인한다.
