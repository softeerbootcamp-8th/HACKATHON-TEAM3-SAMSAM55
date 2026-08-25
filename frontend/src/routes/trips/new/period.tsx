import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { ChevronLeft, ChevronRight } from 'lucide-react'

import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { StepIndicator } from '@/components/ui/step-indicator'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/new/period')({
  component: NewTripPeriodPage,
})

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

type CalendarDay = {
  label: number
  state: 'disabled' | 'default' | 'start' | 'range' | 'end'
}

// 2026년 8월 캘린더 목업 — 시작일 26일 · 종료일 29일 고정 표시
const CALENDAR_WEEKS: CalendarDay[][] = [
  [
    { label: 27, state: 'disabled' },
    { label: 28, state: 'disabled' },
    { label: 29, state: 'disabled' },
    { label: 30, state: 'disabled' },
    { label: 31, state: 'disabled' },
    { label: 1, state: 'disabled' },
    { label: 2, state: 'disabled' },
  ],
  Array.from({ length: 7 }, (_, i) => ({
    label: 3 + i,
    state: 'disabled' as const,
  })),
  Array.from({ length: 7 }, (_, i) => ({
    label: 10 + i,
    state: 'disabled' as const,
  })),
  Array.from({ length: 7 }, (_, i) => ({
    label: 17 + i,
    state: 'disabled' as const,
  })),
  [
    { label: 24, state: 'disabled' },
    { label: 25, state: 'default' },
    { label: 26, state: 'start' },
    { label: 27, state: 'range' },
    { label: 28, state: 'range' },
    { label: 29, state: 'end' },
    { label: 30, state: 'default' },
  ],
  [
    { label: 31, state: 'default' },
    { label: 1, state: 'disabled' },
    { label: 2, state: 'disabled' },
    { label: 3, state: 'disabled' },
    { label: 4, state: 'disabled' },
    { label: 5, state: 'disabled' },
    { label: 6, state: 'disabled' },
  ],
]

function NewTripPeriodPage() {
  const navigate = useNavigate()

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            onClick={() => navigate({ to: '/trips/new/members' })}
          >
            다음
          </Button>
        </div>
      }
    >
      <AppBar
        type="back"
        title="여행 만들기"
        onBack={() => navigate({ to: '/trips/new/name' })}
      />
      <div className="flex flex-col gap-5 px-5 pt-[38px]">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2.5">
            <div className="flex-1">
              <StepIndicator step={2} totalSteps={3} />
            </div>
            <p className="text-caption text-muted-foreground whitespace-nowrap">
              2 / 3
            </p>
          </div>
          <div className="flex flex-col gap-1.5">
            <p className="text-title-1 text-foreground">언제 떠나세요?</p>
            <p className="text-body text-muted-foreground">
              달력에서 시작일과 종료일을 고르세요.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-[5px]">
          <div className="flex gap-2.5">
            <div className="flex flex-1 flex-col gap-1 rounded-tab border border-primary-deep px-3.5 pt-2.5 pb-3">
              <p className="text-caption text-muted-foreground">시작일</p>
              <p className="text-body-strong text-foreground">8월 26일 (화)</p>
            </div>
            <div className="flex flex-1 flex-col gap-1 rounded-tab border border-primary-deep px-3.5 pt-2.5 pb-3">
              <p className="text-caption text-muted-foreground">종료일</p>
              <p className="text-body-strong text-foreground">8월 29일 (금)</p>
            </div>
          </div>

          <div className="flex flex-col gap-4 rounded-[20px] p-4 shadow-[0px_0px_8px_rgba(0,0,0,0.08)]">
            <div className="flex items-center gap-4">
              <button
                type="button"
                aria-label="이전 달"
                className="flex size-8 items-center justify-center"
              >
                <ChevronLeft className="size-4 text-muted-foreground" />
              </button>
              <p className="flex-1 text-center text-body-strong text-foreground">
                2026년 8월
              </p>
              <button
                type="button"
                aria-label="다음 달"
                className="flex size-8 items-center justify-center"
              >
                <ChevronRight className="size-4 text-muted-foreground" />
              </button>
            </div>
            <div className="grid grid-cols-7 gap-y-1">
              {WEEKDAYS.map((day) => (
                <div
                  key={day}
                  className="pb-2 text-center text-caption-sm text-muted-foreground"
                >
                  {day}
                </div>
              ))}
              {CALENDAR_WEEKS.flat().map((day, index) => (
                <div
                  key={index}
                  className="flex items-center justify-center py-0.5"
                >
                  <span
                    className={cn(
                      'flex size-9 items-center justify-center rounded-full text-body',
                      day.state === 'disabled' && 'text-text-disabled',
                      day.state === 'default' && 'text-foreground',
                      day.state === 'range' &&
                        'bg-primary-tint text-primary-deep',
                      (day.state === 'start' || day.state === 'end') &&
                        'bg-primary-deep font-medium text-white',
                    )}
                  >
                    {day.label}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </MobileScreen>
  )
}
