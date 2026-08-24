---
description: 지정한 커밋부터 현재까지의 변경사항으로 팀 컨벤션에 맞는 PR을 생성한다
argument-hint: <시작 커밋 SHA> [assignee 지정 시 함께 언급]
allowed-tools: Bash(gh pr:*), Bash(gh label:*), Bash(gh auth:*), Bash(git log:*), Bash(git diff:*), Bash(git show:*), Bash(git status:*), Bash(git branch:*), Bash(git rev-parse:*), Bash(git rev-list:*), Bash(git push:*), Read, Write, Grep, Glob
---

# PR 생성

시작 커밋: **$1**

`$1` 커밋을 **포함해서** 현재 HEAD까지의 변경사항으로 PR을 만든다.
`.github/PULL_REQUEST_TEMPLATE` 형식을 따른다.

## 1. 사전 확인

`gh auth status`로 인증을 확인한다. 실패하면 `gh auth login`을 안내하고 중단한다.

인자가 없으면 최근 커밋 목록(`git log --oneline -15`)을 보여주고
어느 커밋부터 묶을지 물어본다. 임의로 정하지 않는다.

`git rev-parse --verify $1^{commit}` 으로 SHA가 유효한지 확인한다.

## 2. 범위 계산

`$1`을 포함해야 하므로 범위는 `$1^..HEAD` 다.

    git log --oneline $1^..HEAD

`$1`이 최초 커밋이면 `$1^`가 없어 실패한다. 이때는 `--root`를 쓴다.

    git log --oneline --root HEAD

범위에 잡힌 커밋 목록을 사용자에게 먼저 보여주고, 의도한 범위가 맞는지
확인받은 뒤 진행한다. 범위를 잘못 잡으면 남의 커밋까지 PR에 들어간다.

## 3. 변경사항 파악

    git diff --stat $1^..HEAD
    git diff $1^..HEAD

커밋 메시지만 요약하지 말고 실제 diff를 읽는다.

## 4. 관련 이슈 번호 찾기

순서대로 시도한다.

1. 현재 브랜치명에서 추출 — `feat/#7-xxx` → `7`
2. 범위 내 커밋 메시지의 `#N` 참조
3. 못 찾으면 사용자에게 묻는다. 이슈 없이 진행하면 `Closes #` 줄은 비워 둔다

## 5. 제목

커밋 메시지와 같은 형식: `[영역] type: 요약`

- 영역: `BE` / `FE` / `ALL` / `Infra`
- 범위 내 커밋이 여러 영역에 걸치면 `ALL`
- 예: `[BE] feat: 여행방 생성 API 구현`

## 6. 본문

이 저장소의 PR 템플릿은 3섹션이다 (팀 컨벤션상 관련이슈·요약·변경내용만
쓰기로 함 — 리뷰 요청이나 고민 과정 섹션은 없다). 인용문(`>`) 안내
문구는 지운다.

```markdown
## 📌 관련 이슈

- Closes #{번호}

---

## ✨ 요약

- {이번 PR에서 어떤 작업을 진행했는지 간략하게}

## ✅ 변경 내용

- {주된 변경 내용}
- {그렇게 구현한 이유}
```

**요약**은 커밋 제목 나열이 아니라 변경의 의미를 쓴다.

**변경 내용**에는 diff에서 판단이 개입된 지점(왜 그렇게 했는지)을
같이 적는다. 단순 작업이면 억지로 이유를 만들지 않는다.

**글쓰기 톤** — 쉬운 말로 짧게 쓴다. 시맨틱·트레이드오프·원자성처럼
불필요하게 어려운 용어나 AI가 쓴 티 나는 표현은 피하고, 팀원이 한 번
읽고 바로 핵심을 파악할 수 있게 쓴다. 이미 리뷰로 해결된 내용을
장황하게 설명하지 않는다.

## 7. assignee · 리뷰어 · label

**assignee** — 사용자가 지정하지 않으면 본인으로 한다. `--assignee @me`

**리뷰어** — **자동으로 지정하지 않는다.** 이 저장소는 부트캠프 조직에
속해 있어 collaborator 목록에 팀원이 아닌 사람까지 포함될 수 있다.
잘못 지정하면 관계없는 사람에게 리뷰 요청이 간다. 사용자가 명시한
경우에만 지정한다.

**label** — 이 저장소에는 영역/type 전용 라벨이 없고 GitHub 기본
라벨만 있다. `gh label list --limit 100 --json name,description`으로
실행 시점에 확인하고, 뜻이 겹치는 라벨(`bug`, `enhancement`,
`documentation`)만 최소로 매칭한다. 마땅한 게 없으면 라벨 없이
진행하고 알린다. **label을 새로 만들지 않는다.**

## 8. 푸시와 생성

브랜치가 원격에 없으면 먼저 푸시한다.

    git push -u origin HEAD

본문은 임시 파일에 쓰고 `--body-file`로 넘긴다. 인라인 `--body`는
따옴표 처리가 깨지기 쉽고, 금지 명령어 차단 hook의 오탐도 유발한다.

    gh pr create --base dev --title "{제목}" --body-file {임시파일} --assignee @me

라벨을 붙일 경우에만 `--label "{label}"`을 추가한다.

**base는 `dev`** 다 (`main ← dev ← 기능 브랜치`). `main`으로 열지 않는다.
현재 브랜치가 `dev`나 `main`이면 PR을 만들 수 없으므로 중단하고 알린다.

label 지정이 실패해도 **PR 본체는 살린다.** 실패하면 PR을 먼저 만든 뒤
`gh pr edit`으로 재시도하고, 그래도 안 되면 사실대로 알린다.

## 9. 보고

PR URL과 함께 지정된 assignee·label을 알린다.
label을 못 붙였으면 이유를 알린다.

**리뷰어는 지정하지 않았음을 알리고, 직접 지정하도록 안내한다.**

    gh pr edit {번호} --add-reviewer {아이디},{아이디}
