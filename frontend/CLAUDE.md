# Frontend (SAMSAM55)

이 문서는 프론트엔드 전용 규칙이다. 저장소 공통 규칙은 [../CLAUDE.md](../CLAUDE.md)를 따른다.
공용 계약(API, 응답 형식, 에러 코드)은 `.claude/skills/trip-development/references/contracts.md`가 기준이다.

## 스택

- Vite + React 19 + TypeScript, 패키지 매니저는 pnpm 고정
- 라우팅: TanStack Router (file-based, `src/routes/`)
- 서버 상태: TanStack Query
- API 클라이언트: Orval로 OpenAPI 스펙에서 자동 생성 (`src/api/generated`)
- UI: shadcn/ui + Tailwind CSS v4
- Lint: oxlint (`.oxlintrc.json`) / Format: Prettier (`.prettierrc.json`)

## 폴더 구조

```
src/
  api/
    generated/     # Orval 자동 생성 — 절대 직접 수정하지 않는다
    mutator/        # axios 커스텀 인스턴스 (직접 관리)
  components/
    ui/             # shadcn/ui가 생성한 컴포넌트 — 생성 후에는 우리 코드로 취급하고 자유롭게 수정
  lib/
    utils.ts        # cn() 등 공용 유틸
  routes/
    __root.tsx      # 루트 레이아웃
    index.tsx       # "/" 페이지
  styles/
    globals.css     # Tailwind 토큰(CSS 변수) 정의
  main.tsx
  routeTree.gen.ts  # TanStack Router 자동 생성 파일 — 직접 수정 금지, git에 커밋하지 않는다 (gitignore)
orval.config.ts
components.json      # shadcn/ui 설정
```

## Route-based 구조 규칙

- 화면 단위는 `src/routes/` 아래 파일로만 추가한다 (`createFileRoute` 사용).
- 레이아웃 공유가 필요하면 `src/routes/{prefix}.tsx` + `src/routes/{prefix}/...` 패턴을 쓴다 (TanStack Router 공식 컨벤션).
- `src/routeTree.gen.ts`는 dev/build 시 플러그인이 자동 생성한다. 손으로 편집하지 않고, git에도 커밋하지 않는다.
  대신 `package.json`의 `build` 스크립트를 `vite build && tsc -b` 순서로 고정해뒀다 — `tsc -b`가
  먼저 돌면 이 파일이 없어서(새 클론 직후) 타입 체크가 실패한다. 순서를 바꾸지 않는다.
- 라우트 컴포넌트는 화면 조합만 담당한다. 데이터 패칭은 `src/api/generated`의 훅을 그대로 쓰거나, 여러 훅을 묶는 경우에만 `src/features/{도메인}/` 같은 하위 폴더를 새로 만들어 분리한다 (임의로 큰 구조를 미리 만들지 않는다).

## API 연동 (Orval)

- `orval.config.ts`의 `input.target`은 백엔드가 로컬(`:8080`)에서 노출하는 `http://localhost:8080/v3/api-docs`다. 즉 **`pnpm api:generate`를 돌리려면 백엔드가 로컬에서 떠 있어야 한다** (`./gradlew bootRun`, MySQL은 저장소 루트 `docker-compose.yml`로 띄운다).
- 계약 변경 시 순서: 1) 백엔드 컨트롤러/DTO 수정(springdoc이 스펙에 자동 반영) → 2) 백엔드를 로컬에서 기동한 채 `pnpm api:generate` 재실행 → 3) 생성된 타입/훅을 쓰는 화면 코드 갱신.
- `src/api/generated`는 손으로 고치지 않는다. 스펙이 틀렸으면 백엔드 컨트롤러/DTO를 고치고 재생성한다.
- 인증은 Session Cookie 방식이 확정 사항이므로 (`contracts.md`), `src/api/mutator/custom-instance.ts`의 axios 인스턴스는 `withCredentials: true`를 유지한다.
- 응답은 항상 `{ success, data, error }` 3필드 봉투(envelope)를 따른다 (`contracts.md`, 백엔드의 `CommonResponse<T>`). 화면 코드에서 `success` 분기 없이 `data`만 믿고 쓰지 않는다.
- 개발 서버는 `/api` 요청을 `http://localhost:8080`(백엔드)으로 프록시한다 (`vite.config.ts`의 `server.proxy`). 그래서 axios `baseURL`을 비워두면 dev에서도 프로덕션과 같은 동일 출처 구성이 되어 Session Cookie가 CORS 없이 그대로 전달된다. dev 중 API가 안 붙으면 먼저 백엔드가 `:8080`에 떠 있는지 확인한다.
- API prefix는 실제 컨트롤러 기준 `/api/...`로 확인됐다 (`AuthController`의 `@RequestMapping("/api/auth")`). `contracts.md`가 여전히 이를 "미확정"으로 적어두고 있으니 팀과 확인해서 문서를 맞추는 걸 추천한다.

## 스타일 (Tailwind v4 + shadcn/ui)

- 디자인 토큰(색상, radius 등)은 `src/styles/globals.css`의 CSS 변수 하나로 관리한다. 컴포넌트 안에 색상 값을 하드코딩하지 않는다.
- Tailwind v4는 별도 `tailwind.config.js`가 없다 (`@tailwindcss/vite` + `@theme inline` CSS 설정 방식). 토큰 추가는 `globals.css`에서 한다.
- 새 shadcn 컴포넌트는 `pnpm dlx shadcn@latest add <component>`로 추가한다. 추가된 뒤에는 `src/components/ui/` 아래 우리 코드이므로 자유롭게 커스터마이징한다 (Orval 생성 코드와 다르게 취급).
- 클래스 병합은 `cn()` (`src/lib/utils.ts`)을 쓴다.

## 환경 변수

- 실제 값은 `.env`로 관리하고 커밋하지 않는다 (`.env.example`에 키 목록만 남긴다).
- `VITE_API_BASE_URL`: axios 인스턴스와 백엔드 API 서버 주소를 연결한다. 값 공유는 Notion에 남긴다.

## 아직 정하지 않은 것 (임의로 결정하지 않는다)

- 테스트 도구 (Vitest, Testing Library 등) — 아직 설치되지 않았다. 저장소 공통 규칙상 기능 구현 시 테스트 코드가 필요하므로, 첫 기능 작업 전에 팀과 도구를 정하고 추가한다.
- 인증 이후의 세부 에러 코드 목록 — `contracts.md`의 "미확정 사항"을 따른다.
- 전역 상태 관리 라이브러리 (필요해지면 그때 논의한다. TanStack Query가 다루는 서버 상태와 혼동하지 않는다).

## 명령어

```bash
pnpm dev            # 개발 서버 (라우트 트리 자동 생성, /api 프록시)
pnpm build          # vite build(라우트 트리 생성 포함) + 타입 체크
pnpm lint           # oxlint
pnpm format         # Prettier 자동 정렬
pnpm format:check   # Prettier 검사만 (CI가 이걸 돌린다)
pnpm preview        # 빌드 결과 미리보기
pnpm api:generate   # openapi/spec.yaml(또는 실제 백엔드 스펙) 기준으로 src/api/generated 재생성
```

### push 전에

`.github/workflows/frontend-build.yml`이 PR에서 돌리는 것과 같은 순서다.

```bash
pnpm format:check   # 실패하면 pnpm format 으로 정렬하고 다시
pnpm lint
pnpm build
```

패키지 매니저 버전은 `package.json`의 `packageManager` 필드로 고정돼 있다 (CI가 같은 버전을 쓰도록).
