# Frontend (SAMSAM55)

이 문서는 프론트엔드 전용 규칙이다. 저장소 공통 규칙은 [../CLAUDE.md](../CLAUDE.md)를 따른다.
공용 계약(API, 응답 형식, 에러 코드)은 `.claude/skills/trip-development/references/contracts.md`가 기준이다.

## 스택

- Vite + React 19 + TypeScript, 패키지 매니저는 pnpm 고정
- 라우팅: TanStack Router (file-based, `src/routes/`)
- 서버 상태: TanStack Query
- API 클라이언트: Orval로 OpenAPI 스펙에서 자동 생성 (`src/api/generated`)
- UI: shadcn/ui + Tailwind CSS v4
- Lint: oxlint (`.oxlintrc.json`)

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
  routeTree.gen.ts  # TanStack Router 자동 생성 파일 — 직접 수정 금지, git에 커밋한다
openapi/
  spec.yaml         # Orval 입력 스펙 (임시 로컬 파일, 아래 "API 계약 동기화" 참고)
orval.config.ts
components.json      # shadcn/ui 설정
```

## Route-based 구조 규칙

- 화면 단위는 `src/routes/` 아래 파일로만 추가한다 (`createFileRoute` 사용).
- 레이아웃 공유가 필요하면 `src/routes/{prefix}.tsx` + `src/routes/{prefix}/...` 패턴을 쓴다 (TanStack Router 공식 컨벤션).
- `src/routeTree.gen.ts`는 dev/build 시 플러그인이 자동 생성한다. 손으로 편집하지 않는다.
- 라우트 컴포넌트는 화면 조합만 담당한다. 데이터 패칭은 `src/api/generated`의 훅을 그대로 쓰거나, 여러 훅을 묶는 경우에만 `src/features/{도메인}/` 같은 하위 폴더를 새로 만들어 분리한다 (임의로 큰 구조를 미리 만들지 않는다).

## API 연동 (Orval)

- 계약 변경 시 순서: 1) 백엔드 OpenAPI 스펙 갱신 → 2) `pnpm api:generate` 재실행 → 3) 생성된 타입/훅을 쓰는 화면 코드 갱신.
- `src/api/generated`는 손으로 고치지 않는다. 스펙이 틀렸으면 스펙(또는 백엔드)을 고치고 재생성한다.
- 인증은 Session Cookie 방식이 확정 사항이므로 (`contracts.md`), `src/api/mutator/custom-instance.ts`의 axios 인스턴스는 `withCredentials: true`를 유지한다.
- 응답은 항상 `{ success, data, error }` 3필드 봉투(envelope)를 따른다 (`contracts.md`). 화면 코드에서 `success` 분기 없이 `data`만 믿고 쓰지 않는다.

### API 계약 동기화 (현재 미확정 상태)

- `openapi/spec.yaml`은 백엔드에 아직 springdoc-openapi 같은 스펙 자동 노출이 없어서 만든 **임시 로컬 스펙**이다 (echo 엔드포인트만 정의).
- 백엔드가 실제 OpenAPI 스펙을 노출하면 `orval.config.ts`의 `input.target`을 그 URL 또는 export된 파일로 교체하고, 이 임시 파일은 제거한다. 이 교체는 공용 계약 변경에 준하므로 팀 확인 후 진행한다.
- API prefix(`/api/v1` 등)는 아직 팀 미확정 사항이다 (`contracts.md`). 확정되면 `openapi/spec.yaml`의 `servers.url`과 axios `baseURL`을 함께 맞춘다.

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
- API prefix, 인증 이후의 세부 에러 코드 목록 — `contracts.md`의 "미확정 사항"을 따른다.
- 전역 상태 관리 라이브러리 (필요해지면 그때 논의한다. TanStack Query가 다루는 서버 상태와 혼동하지 않는다).

## 명령어

```bash
pnpm dev            # 개발 서버
pnpm build          # 타입 체크 + 프로덕션 빌드
pnpm lint           # oxlint
pnpm preview        # 빌드 결과 미리보기
pnpm api:generate   # openapi/spec.yaml(또는 실제 백엔드 스펙) 기준으로 src/api/generated 재생성
```
