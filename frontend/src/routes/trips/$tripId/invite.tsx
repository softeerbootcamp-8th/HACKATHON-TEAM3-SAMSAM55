import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { useFindTrip } from '@/api/generated/trip-controller/trip-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/$tripId/invite')({
  component: TripInvitePage,
})

function TripInvitePage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()
  const tripIdNumber = Number(tripId)
  const isValidTripId = Number.isInteger(tripIdNumber) && tripIdNumber > 0
  const tripQuery = useFindTrip(tripIdNumber, {
    query: { enabled: isValidTripId, retry: false },
  })
  const detail = tripQuery.data?.success ? tripQuery.data.data : undefined
  const inviteLink = detail?.inviteCode
    ? `${window.location.origin}/invite/${detail.inviteCode}`
    : ''

  const handleCopy = () => {
    if (!inviteLink) {
      return
    }

    void navigator.clipboard.writeText(inviteLink)
  }

  if (tripQuery.isLoading) {
    return (
      <MobileScreen>
        <div className="flex flex-1 items-center justify-center text-body text-muted-foreground">
          여행을 불러오는 중...
        </div>
      </MobileScreen>
    )
  }

  if (tripQuery.isError || !detail) {
    return (
      <MobileScreen>
        <AppBar
          type="back"
          title="초대하기"
          onBack={() =>
            void navigate({
              to: '/trips/$tripId',
              params: { tripId },
            })
          }
        />
        <div className="flex flex-1 items-center justify-center px-5 text-center text-body text-destructive">
          {getApiError(tripQuery.error)?.message ??
            tripQuery.data?.error?.message ??
            '여행 정보를 불러오지 못했습니다.'}
        </div>
      </MobileScreen>
    )
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            onClick={() =>
              void navigate({
                to: '/trips/$tripId',
                params: { tripId },
              })
            }
          >
            여행 홈으로
          </Button>
        </div>
      }
    >
      <AppBar type="plain" title="초대하기" />
      <div className="flex flex-1 flex-col px-5 pt-24">
        <div className="flex flex-col gap-2">
          <p className="text-display text-foreground">가족을 초대해보세요</p>
          <p className="text-body text-muted-foreground">
            앱 설치나 회원가입 없이 링크로 바로 들어오실 수 있어요
          </p>
        </div>
        <div className="mt-12 flex h-[104px] items-center justify-between gap-4 rounded-btn bg-muted px-8">
          <span
            className={cn(
              'min-w-0 truncate text-body',
              inviteLink ? 'text-foreground' : 'text-muted-foreground',
            )}
          >
            {inviteLink}
          </span>
          <button
            type="button"
            onClick={handleCopy}
            className="shrink-0 text-body-strong text-primary-deep"
          >
            복사
          </button>
        </div>
      </div>
    </MobileScreen>
  )
}
