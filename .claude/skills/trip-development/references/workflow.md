# Workflow and Safety

## Git

- 흐름: `main ← dev ← {type}/#{issue}-{feature}`
- 커밋: `[BE/FE/ALL] {type}: {요약}` (콜론 앞 공백 없음)
- PR: 관련 이슈, 요약, 변경 내용 (템플릿 3섹션, `.github/PULL_REQUEST_TEMPLATE` 기준)
- 이슈: 브랜치명, 작업 내용 (템플릿 2섹션, `.github/ISSUE_TEMPLATE/이슈-템플릿.md` 기준)

## 환경

- 실제 값은 `.env`로 관리하고, key와 값 공유는 Notion에 남긴다 (`.env`는 커밋하지 않는다)
- 로컬 MySQL 등은 팀 공용 docker compose 파일 하나로 함께 띄운다
- 기존 package manager와 lockfile을 유지한다
- 환경 변수 추가 시 `.env.example`(있다면), README, 배포 설정을 함께 확인한다

## CI/CD

- GitHub Actions를 사용한다

## 사람 확인이 필요한 변경

- API 요청·응답 형식과 에러 코드 계약
- DB schema와 영속성 스택(JPA 등) 도입
- 환경 변수·배포
- 새 의존성·주요 구조 변경

## 금지

- secret, token, 개인정보 출력·커밋
- `DROP`, `TRUNCATE`
- `docker compose down -v`
- `git push --force`, `git reset --hard`, `git clean`
- 테스트 삭제·약화
- 관련 없는 리팩터링·전체 포맷 변경

## 완료 보고

- 변경 기능과 파일
- 영향 영역
- API·응답 형식 계약 영향
- 테스트·lint·타입 검사·빌드 결과
- 미검증 사항과 남은 위험
