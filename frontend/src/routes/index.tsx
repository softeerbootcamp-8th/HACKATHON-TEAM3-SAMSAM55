import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/')({
  component: HomePage,
})

function HomePage() {
  return (
    <main className="mx-auto flex min-h-svh max-w-xl flex-col items-center justify-center gap-2 p-6 text-center">
      <h1 className="text-3xl font-semibold text-foreground">SAMSAM55</h1>
      <p className="text-muted-foreground">
        부모님과 자녀가 함께 여행 일정을 정하는 서비스
      </p>
    </main>
  )
}
