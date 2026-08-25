import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { Plus } from 'lucide-react'

import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { StepIndicator } from '@/components/ui/step-indicator'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/new/members')({
  component: NewTripMembersPage,
})

const FAMILY_GROUPS = [
  { label: '부모', members: ['엄마', '아빠'] },
  {
    label: '조부모',
    members: ['친할머니', '친할아버지', '외할머니', '외할아버지'],
  },
  { label: '형제자매', members: ['첫째', '둘째', '셋째'] },
]

const MEMBER_EMOJI: Record<string, string> = {
  엄마: '👩🏻',
  아빠: '👨🏻',
  친할머니: '👵🏻',
  친할아버지: '👴🏻',
  외할머니: '👵🏻',
  외할아버지: '👴🏻',
  첫째: '🧑🏻',
  둘째: '🧒🏻',
  셋째: '👶🏻',
}

function NewTripMembersPage() {
  const navigate = useNavigate()
  const [selected, setSelected] = useState<string[]>([
    '엄마',
    '아빠',
    '외할머니',
    '첫째',
  ])

  function toggle(member: string) {
    setSelected((prev) =>
      prev.includes(member)
        ? prev.filter((m) => m !== member)
        : [...prev, member],
    )
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            onClick={() =>
              navigate({ to: '/trips/$tripId', params: { tripId: 'trip-1' } })
            }
          >
            여행 만들기
          </Button>
        </div>
      }
    >
      <AppBar
        type="back"
        title="여행 만들기"
        onBack={() => navigate({ to: '/trips/new/period' })}
      />
      <div className="flex flex-col gap-6 px-5 pt-[38px]">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2.5">
            <div className="flex-1">
              <StepIndicator step={3} totalSteps={3} />
            </div>
            <p className="text-caption text-muted-foreground whitespace-nowrap">
              3 / 3
            </p>
          </div>
          <div className="flex flex-col gap-1.5">
            <p className="text-title-1 text-foreground">누구와 함께 가세요?</p>
            <p className="text-body text-muted-foreground">
              함께 갈 가족을 골라주세요.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-4.5">
          {FAMILY_GROUPS.map((group) => (
            <div key={group.label} className="flex flex-col gap-2">
              <p className="text-caption-sm font-semibold text-muted-foreground">
                {group.label}
              </p>
              <div className="flex flex-wrap gap-2">
                {group.members.map((member) => {
                  const isSelected = selected.includes(member)
                  return (
                    <button
                      key={member}
                      type="button"
                      onClick={() => toggle(member)}
                      className={cn(
                        'rounded-chip px-3 py-1.5 text-label',
                        isSelected
                          ? 'bg-primary-tint text-primary-deep'
                          : 'bg-muted text-foreground',
                      )}
                    >
                      {MEMBER_EMOJI[member]} {member}
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
          <div className="flex flex-col gap-2">
            <p className="text-caption-sm font-semibold text-muted-foreground">
              기타
            </p>
            <Button variant="secondary" size="default" className="w-fit gap-1">
              <Plus className="size-4" />
              직접 입력
            </Button>
          </div>
        </div>
      </div>
    </MobileScreen>
  )
}
