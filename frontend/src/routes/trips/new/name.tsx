import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'

import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { StepIndicator } from '@/components/ui/step-indicator'
import { TextInput } from '@/components/ui/text-input'

export const Route = createFileRoute('/trips/new/name')({
  validateSearch: (search: Record<string, unknown>) => ({
    title: typeof search.title === 'string' ? search.title : undefined,
  }),
  component: NewTripNamePage,
})

function NewTripNamePage() {
  const navigate = useNavigate()
  const { title } = Route.useSearch()
  const [name, setName] = useState(title ?? '')

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            disabled={!name.trim()}
            onClick={() =>
              navigate({
                to: '/trips/new/period',
                search: {
                  title: name.trim(),
                  startDate: undefined,
                  endDate: undefined,
                },
              })
            }
          >
            다음
          </Button>
        </div>
      }
    >
      <AppBar
        type="back"
        title="여행 만들기"
        onBack={() => navigate({ to: '/trips' })}
      />
      <div className="flex flex-col gap-6 px-5 pt-[38px]">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2.5">
            <div className="flex-1">
              <StepIndicator step={1} totalSteps={3} />
            </div>
            <p className="text-caption text-muted-foreground whitespace-nowrap">
              1 / 3
            </p>
          </div>
          <div className="flex flex-col gap-1.5">
            <p className="text-title-1 text-foreground">어디로 떠나세요?</p>
            <p className="text-body text-muted-foreground">
              우리 여행의 이름을 입력해주세요.
            </p>
          </div>
        </div>
        <div className="flex h-28 items-center justify-center">
          <div className="flex size-24 items-center justify-center rounded-full bg-primary-tint text-[40px]">
            ✈️
          </div>
        </div>
        <div className="flex flex-col gap-3">
          <TextInput
            placeholder="예: 도쿄 가족여행"
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={100}
          />
          <p className="text-caption text-muted-foreground">
            이름은 100자 이하로 입력해주세요
          </p>
        </div>
      </div>
    </MobileScreen>
  )
}
