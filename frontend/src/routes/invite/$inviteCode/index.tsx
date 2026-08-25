import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/invite/$inviteCode/')({
  component: InviteEntryPage,
})

function InviteEntryPage() {
  const navigate = useNavigate()
  const { inviteCode } = useParams({ from: '/invite/$inviteCode/' })

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
          정하은 님의 도쿄 가족 여행에
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
