import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { usePostEcho } from '@/api/generated/echo/echo'

export const Route = createFileRoute('/')({
  component: HomePage,
})

function HomePage() {
  const [message, setMessage] = useState('')
  const { mutate, data, error, isPending } = usePostEcho<Error>()

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    mutate({ data: { message } })
  }

  return (
    <main className="mx-auto flex min-h-svh max-w-xl flex-col justify-center gap-6 p-6">
      <div className="space-y-2 text-center">
        <h1 className="text-3xl font-semibold text-foreground">배포 테스트</h1>
        <p className="text-muted-foreground">
          입력한 값을 백엔드로 보내고, 그대로 돌아오는지 확인합니다.
        </p>
      </div>
      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="아무 값이나 입력"
          required
          className="flex-1 rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
        />
        <Button type="submit" disabled={isPending}>
          {isPending ? '전송 중...' : '전송'}
        </Button>
      </form>
      {data && (
        <p className="text-sm text-foreground">
          서버 응답: {data.success ? data.data?.message : data.error?.message}
        </p>
      )}
      {error && (
        <p className="text-sm text-destructive">에러: {error.message}</p>
      )}
    </main>
  )
}
