import { useNavigate } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { getApiErrorMessage } from '@/lib/api-error'

type InviteErrorStateProps = {
  error: unknown
  onRetry: () => void
}

// 초대 코드 조회(GET /api/invites/{inviteCode})가 실패했을 때(잘못된 코드, 네트워크
// 오류 등) 화면이 영구히 비어 보이지 않도록, 로딩과 구분되는 실패 상태를 보여준다.
function InviteErrorState({ error, onRetry }: InviteErrorStateProps) {
  const navigate = useNavigate()

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
      <p className="text-[18px] font-bold text-foreground">
        초대 정보를 불러오지 못했어요
      </p>
      <p className="text-[14px] leading-[1.55] text-muted-foreground">
        {getApiErrorMessage(error)}
      </p>
      <div className="flex w-full flex-col gap-2">
        <Button size="cta" onClick={onRetry}>
          다시 시도
        </Button>
        <Button
          size="cta"
          variant="text"
          onClick={() => navigate({ to: '/login' })}
        >
          로그인 화면으로
        </Button>
      </div>
    </div>
  )
}

export { InviteErrorState }
