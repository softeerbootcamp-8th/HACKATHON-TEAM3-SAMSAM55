import { Check } from 'lucide-react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/parent/items/$itemId/vote/done')({
  component: ParentVoteDonePage,
})

function ParentVoteDonePage() {
  const navigate = useNavigate()

  return (
    <MobileScreen
      floatingBottomBar
      bottomBar={
        <div className="px-5 pb-6">
          <Button size="cta" onClick={() => navigate({ to: '/parent' })}>
            홈으로
          </Button>
        </div>
      }
    >
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
        <span className="flex size-18 items-center justify-center rounded-full bg-primary">
          <Check className="size-8 text-foreground" />
        </span>
        <p className="text-[22px] leading-[1.45] font-bold text-foreground">
          다 골랐어요!
        </p>
        <p className="text-[14px] leading-[1.55] text-muted-foreground">
          자녀가 확인하고
          <br />곧 일정을 확정할 거예요
        </p>
      </div>
    </MobileScreen>
  )
}
