# SAMSAM55 Repository

SAMSAM55는 부모님과 자녀가 함께 여행 일정을 정하는 서비스다.
여행 결정이 자녀 한 명에게 몰리지 않도록, 자녀가 후보를 준비하고
부모가 밸런스 게임처럼 골라 여행 일정을 "함께의 결정"으로 만든다.

이 문서는 프론트엔드와 백엔드를 함께 관리하는 저장소의 공통 규칙이다.
영역별 상세 규칙은 하위 `CLAUDE.md`(`backend/CLAUDE.md`, `frontend/CLAUDE.md`)와
`.claude/skills/trip-development/`를 필요할 때 참고한다.

## 공통 필수 규칙

- 작업 전에 관련 코드·테스트·문서를 읽고 최소 범위만 수정한다.
- API Method, Path, 요청·응답 형식, 에러 코드는 공용 계약이다. 변경 시 프론트·백엔드·문서를 함께 갱신한다.
- 커밋 메시지는 `[BE/FE/ALL] type: 요약` 형식을 쓴다 (콜론 앞 공백 없음).
- 환경 변수의 실제 값은 `.env`로 관리하고 절대 커밋하지 않는다. key 목록과 값 공유는 Notion에 남긴다.
- 로컬 MySQL 등 인프라는 팀 공용 docker compose 파일 하나로 함께 띄운다.
- CI/CD는 GitHub Actions를 사용한다.
- 기능을 구현하면 그 기능에 대한 테스트 코드를 작성한다.
- 기존 빌드 도구, lockfile, formatter, lint 설정을 유지한다.
- 새 의존성이나 주요 구조 변경은 근거와 영향 범위를 제시하고 팀 확인을 받는다.
- 비밀값·토큰·개인정보를 코드, 로그, 클라이언트에 노출하지 않는다.

## 영역별 책임

- Frontend: 화면 조합, 사용자 상태, API 소비, 접근성
- Backend: 비즈니스 규칙, 검증, 트랜잭션, 영속성
- Infra: 환경 설정, Docker Compose, CI/CD, 배포
- 공용 계약(API, 응답 형식, 에러 코드) 변경 시 프론트·백엔드·문서를 함께 갱신한다.

## 금지

- 팀 확인 없는 API·DB·응답 형식 계약 변경
- 새 패키지 관리자나 lockfile 혼용
- `e.printStackTrace()`, `System.out.println()` 사용
- 필드 주입(`@Autowired` on field) — 생성자 주입만 사용
- 테스트 삭제 또는 검증 완화로 CI 통과
- 관련 없는 대규모 리팩터링이나 전체 포맷 변경
- `git push --force`, `git reset --hard`, `git clean`
- `docker compose down -v`

위 명령 중 일부는 `.claude/hooks/block-forbidden-commands.sh`가 자동 차단한다.
다만 이는 **1차 방어선이며 보증이 아니다.** 훅은 Bash 명령 문자열만 보므로
파일 안의 SQL, Gradle 태스크로 감싼 실행, 스크립트 경유 실행은 잡지 못한다.
최종 방어선은 DB 권한 분리, 백업, 브랜치 보호 규칙이다.

## 검증과 완료 보고

- 변경한 영역의 테스트, lint, 타입 검사, 빌드를 실행한다.
- 공용 계약 변경은 프론트-백엔드 통합 흐름까지 검증한다.
- 완료 시 변경 영역, 계약 영향, 검증 결과, 미검증 사항을 보고한다.

## 상세 규칙 라우팅

`.claude/skills/trip-development/`에서 작업에 필요한 문서만 읽는다.

- API·응답·에러 코드 계약: `.claude/skills/trip-development/references/contracts.md`
- Spring 백엔드 구현 규칙: `.claude/skills/trip-development/references/backend.md`
- 협업·Git·환경·안전: `.claude/skills/trip-development/references/workflow.md`
