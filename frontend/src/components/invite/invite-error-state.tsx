import { useNavigate } from '@tanstack/react-router'
import axios from 'axios'
import { TriangleAlert } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { getApiError } from '@/features/auth/auth'

type InviteErrorStateProps = {
  error: unknown
  errorCode?: string
  variant?: 'invite' | 'generic'
}

const INVITE_CODE_NOT_FOUND = 'INVITE_CODE_NOT_FOUND'
const TRIP_NOT_FOUND = 'TRIP_NOT_FOUND'

// 초대 코드 조회(GET /api/invites/{inviteCode})가 실패했을 때 오류 유형에 맞는
// 안내를 보여준다. 존재하지 않는 초대 코드는 복구할 수 없으므로 재시도 버튼을
// 노출하지 않고, 일시적인 오류는 홈으로 이동할 수 있게 한다.
function InviteErrorState({
  error,
  errorCode,
  variant = 'generic',
}: InviteErrorStateProps) {
  const navigate = useNavigate()
  const resolvedErrorCode = errorCode ?? getApiError(error)?.code
  const isInvalidInvite =
    variant === 'invite' &&
    (resolvedErrorCode === INVITE_CODE_NOT_FOUND ||
      resolvedErrorCode === TRIP_NOT_FOUND ||
      (axios.isAxiosError(error) && error.response?.status === 404))

  return (
    <div className="flex flex-1 flex-col">
      <div className="flex flex-1 flex-col items-center justify-center gap-6 px-6 text-center">
        <TriangleAlert
          aria-hidden="true"
          className="size-9 stroke-[2.5] text-destructive"
        />
        <div className="flex flex-col gap-3">
          <p className="text-title-1 text-foreground">
            {isInvalidInvite ? '링크를 열 수 없어요' : '문제가 발생했어요'}
          </p>
          <p className="text-body text-muted-foreground">
            {isInvalidInvite ? (
              <>
                만료되었거나 사라진 여행일 수 있어요.
                <br />
                초대해 준 가족에게 새 링크를 받아 주세요.
              </>
            ) : (
              '잠시 후 다시 시도해주세요.'
            )}
          </p>
        </div>
      </div>
      {!isInvalidInvite && (
        <div className="px-5 pb-6">
          <Button
            size="cta"
            onClick={() => void navigate({ to: '/', replace: true })}
          >
            홈 화면으로
          </Button>
        </div>
      )}
    </div>
  )
}

export { InviteErrorState }
