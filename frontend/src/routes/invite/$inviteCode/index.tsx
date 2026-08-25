import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { useVerify } from '@/api/generated/invite-controller/invite-controller'
import { InviteErrorState } from '@/components/invite/invite-error-state'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/invite/$inviteCode/')({
  component: InviteEntryPage,
})

function InviteEntryPage() {
  const navigate = useNavigate()
  const { inviteCode } = useParams({ from: '/invite/$inviteCode/' })

  // InviteVerifyResponseDto엔 여행 제목만 있고 방장 이름은 없어서,
  // "OOO님의 여행에 초대되었어요" 대신 제목만으로 문구를 구성한다.
  const {
    data: response,
    isLoading,
    isError,
    error,
    refetch,
  } = useVerify(inviteCode)
  const trip = response?.data

  if (isLoading) {
    return <MobileScreen>{null}</MobileScreen>
  }

  if (isError || !trip) {
    return (
      <MobileScreen>
        <InviteErrorState error={error} onRetry={() => refetch()} />
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
              navigate({
                to: '/invite/$inviteCode/role',
                params: { inviteCode },
              })
            }
          >
            다음
          </Button>
        </div>
      }
    >
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
        <p className="text-[22px] leading-[1.45] font-bold text-foreground">
          {trip.title} 여행에
          <br />
          초대되었어요
        </p>
        <p className="text-[14px] leading-[1.55] text-muted-foreground">
          가족들과 함께 일정을 정해보세요.
          <br />
          앱을 설치하지 않아도 돼요
        </p>
      </div>
    </MobileScreen>
  )
}
