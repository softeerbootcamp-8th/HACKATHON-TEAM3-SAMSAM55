---
description: 팀 컨벤션에 맞춰 GitHub 이슈를 생성한다
argument-hint: <작업 내용 설명> [assignee 지정 시 함께 언급]
allowed-tools: Bash(gh issue:*), Bash(gh label:*), Bash(gh api:*), Bash(gh auth:*), Bash(git branch:*), Bash(git switch:*), Bash(git log:*), Bash(git status:*), Read, Write, Grep, Glob
---

# 이슈 생성

사용자 요청: **$ARGUMENTS**

`.github/ISSUE_TEMPLATE/이슈-템플릿.md` 형식과 팀 컨벤션에 맞춰 이슈를 생성한다.

## 사전 확인

`gh auth status`로 인증을 확인한다. 실패하면 아래를 안내하고 중단한다.

    brew install gh && gh auth login

## 1. 제목 결정

형식: `[영역] type: 이슈 이름`  (콜론 앞 공백 없음, 템플릿 형식 그대로)

- 영역: `BE` / `FE` / `ALL`(양쪽 공통) / `Infra`(CI·배포·개발도구)
- type: `feat` / `fix` / `bug` / `refactor` / `chore` / `test` / `docs`

요청 내용만으로 영역·type이 애매하면 추측하지 말고 사용자에게 묻는다.
이름은 무엇을 하는지 드러나게 쓴다. "API 작업" 대신 "여행방 생성 API 구현".

예: `[BE] feat: 여행방 생성 API 구현`

## 2. 본문 작성

이 저장소의 이슈 템플릿은 해커톤 특성상 가볍게 두 섹션만 쓴다
(먼저 이슈를 만들고 해결 방법은 진행하며 정하기로 함). 인용문(`>`) 안내
문구는 지우고 실제 내용을 넣는다.

```markdown
## 🌿 Branch Name

`{브랜치명}`

---

## 📄 작업 내용

- 작업 내용 1
- 작업 내용 2
```

**작업 내용**은 구현 대상과 작업 범위를 항목으로 쓴다. 요청이 짧으면
관련 코드·문서(`.claude/skills/trip-development/references/`)를 읽고
필요한 작업을 구체화한다. 추측으로 범위를 부풀리지 않는다.

**브랜치명** 형식: `{type}/#{이슈번호}-{제목}`

- type은 이슈 제목의 type과 동일 (`feat`/`fix`/`bug`/`refactor`/`chore`/`test`/`docs`)
- 이슈번호 앞에 `#`를 붙인다
- 제목은 영문 kebab-case
- 예: `feat/#7-trip-room-create`

이슈 번호는 생성 전에는 모르므로, 먼저 브랜치명 자리를 `TBD`로 두고
이슈를 만든 뒤 실제 번호로 채워 넣는다.

**글쓰기 톤** — 쉬운 말로 짧게 쓴다. 시맨틱·트레이드오프·원자성처럼
불필요하게 어려운 용어나 AI가 쓴 티 나는 표현은 피하고, 팀원이 한 번
읽고 바로 핵심을 파악할 수 있게 쓴다. 이미 리뷰로 해결된 내용을
장황하게 설명하지 않는다.

## 3. assignee와 label

**assignee** — 사용자가 지정하지 않으면 **본인**으로 설정한다.

    --assignee @me

다른 사람을 지정했으면 그 GitHub 아이디를 쓴다.

**label** — 이 저장소에는 아직 영역/type 전용 라벨이 없고, GitHub
기본 라벨만 있다 (`gh label list --limit 100 --json name,description`로
실행 시점에 다시 확인한다). 아래는 뜻이 겹치는 기본 라벨만 최소로 매칭한
것이고, 나머지 type에는 대응 라벨이 없다.

| type | 사용 가능한 라벨 |
|---|---|
| feat | `enhancement` |
| fix / bug | `bug` |
| docs | `documentation` |
| refactor / chore / test | 없음 (라벨 생략) |

영역(`BE`/`FE`/`ALL`) 라벨은 저장소에 아예 없다. **label을 새로 만들지
않는다** — 저장소 전체에 영향을 주는 변경이라 팀 확인이 필요하다.
필요하면 라벨 신설을 제안만 하고, 마땅한 라벨이 없으면 라벨 없이 진행한다.

## 4. 생성

본문은 임시 파일에 쓰고 `--body-file`로 넘긴다. 인라인 `--body`는
따옴표 처리가 깨지기 쉽고, 금지 명령어 차단 hook의 오탐도 유발한다.

    gh issue create --title "{제목}" --body-file {임시파일} --assignee @me

라벨을 붙일 경우에만 `--label "{label}"`을 추가한다.

생성된 번호 `N`을 받아 본문의 `TBD`를 실제 브랜치명으로 교체한다.

    gh issue edit N --body-file {수정한 임시파일}

## 5. 보고

생성된 이슈 번호·URL·확정된 브랜치명, 그리고 **설정된 assignee와 label**을
알린다. label을 못 붙였으면 이유(마땅한 label 없음)를 함께 알린다.

그리고 브랜치를 지금 만들지 물어본다. 만든다고 하면:

    git switch -c {브랜치명}

현재 브랜치가 `dev`가 아니면 어디서 분기할지 먼저 확인한다.
컨벤션상 기능 브랜치는 `dev`에서 딴다 (`main ← dev ← 기능 브랜치`).
