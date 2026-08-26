import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useState } from 'react'

import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { StepIndicator } from '@/components/ui/step-indicator'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/new/period')({
  validateSearch: (search: Record<string, unknown>) => ({
    title: typeof search.title === 'string' ? search.title : '',
    startDate:
      typeof search.startDate === 'string' ? search.startDate : undefined,
    endDate: typeof search.endDate === 'string' ? search.endDate : undefined,
  }),
  component: NewTripPeriodPage,
})

const WEEKDAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa']

type CalendarDay = {
  date: Date
  currentMonth: boolean
}

function toDateString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatSelectedDate(value?: string): string {
  if (!value) {
    return '선택해주세요'
  }

  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'short',
  }).format(date)
}

function getCalendarDays(month: Date): CalendarDay[] {
  const firstDay = new Date(month.getFullYear(), month.getMonth(), 1)
  const calendarStart = new Date(firstDay)
  calendarStart.setDate(firstDay.getDate() - firstDay.getDay())

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(calendarStart)
    date.setDate(calendarStart.getDate() + index)
    return { date, currentMonth: date.getMonth() === month.getMonth() }
  })
}

function NewTripPeriodPage() {
  const navigate = useNavigate()
  const {
    title,
    startDate: initialStartDate,
    endDate: initialEndDate,
  } = Route.useSearch()
  const today = new Date()
  const todayValue = toDateString(today)
  const normalizedInitialStartDate =
    initialStartDate && initialStartDate >= todayValue
      ? initialStartDate
      : undefined
  const normalizedInitialEndDate =
    normalizedInitialStartDate &&
    initialEndDate &&
    initialEndDate >= todayValue &&
    initialEndDate >= normalizedInitialStartDate
      ? initialEndDate
      : undefined
  const [month, setMonth] = useState(
    () => new Date(today.getFullYear(), today.getMonth(), 1),
  )
  const [startDate, setStartDate] = useState<string | undefined>(
    normalizedInitialStartDate,
  )
  const [endDate, setEndDate] = useState<string | undefined>(
    normalizedInitialEndDate,
  )
  const calendarDays = getCalendarDays(month)
  const isValidSelectedPeriod =
    !!startDate &&
    !!endDate &&
    startDate >= todayValue &&
    endDate >= todayValue &&
    startDate <= endDate

  const handleDateClick = (date: Date) => {
    const value = toDateString(date)
    if (value < todayValue) {
      return
    }

    if (!startDate || endDate) {
      setStartDate(value)
      setEndDate(undefined)
      return
    }

    if (value < startDate) {
      setStartDate(value)
      setEndDate(undefined)
      return
    }

    setEndDate(value)
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            disabled={!title || !isValidSelectedPeriod}
            onClick={() =>
              navigate({
                to: '/trips/new/members',
                search: { title, startDate: startDate!, endDate: endDate! },
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
        onBack={() => navigate({ to: '/trips/new/name', search: { title } })}
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
              <p className="text-body-strong text-foreground">
                {formatSelectedDate(startDate)}
              </p>
            </div>
            <div className="flex flex-1 flex-col gap-1 rounded-tab border border-primary-deep px-3.5 pt-2.5 pb-3">
              <p className="text-caption text-muted-foreground">종료일</p>
              <p className="text-body-strong text-foreground">
                {formatSelectedDate(endDate)}
              </p>
            </div>
          </div>

          <div className="flex flex-col gap-4 rounded-[20px] p-4 shadow-[0px_0px_8px_rgba(0,0,0,0.08)]">
            <div className="flex items-center gap-4">
              <button
                type="button"
                aria-label="이전 달"
                className="flex size-8 items-center justify-center"
                onClick={() =>
                  setMonth(
                    (current) =>
                      new Date(
                        current.getFullYear(),
                        current.getMonth() - 1,
                        1,
                      ),
                  )
                }
              >
                <ChevronLeft className="size-4 text-muted-foreground" />
              </button>
              <p className="flex-1 text-center text-body-strong text-foreground">
                {month.getFullYear()}년 {month.getMonth() + 1}월
              </p>
              <button
                type="button"
                aria-label="다음 달"
                className="flex size-8 items-center justify-center"
                onClick={() =>
                  setMonth(
                    (current) =>
                      new Date(
                        current.getFullYear(),
                        current.getMonth() + 1,
                        1,
                      ),
                  )
                }
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
              {calendarDays.map((day) => {
                const value = toDateString(day.date)
                const isPast = value < todayValue
                const isStart = value === startDate
                const isEnd = value === endDate
                const isInRange =
                  !!startDate &&
                  !!endDate &&
                  value > (startDate < endDate ? startDate : endDate) &&
                  value < (startDate < endDate ? endDate : startDate)

                return (
                  <div
                    key={value}
                    className="flex items-center justify-center py-0.5"
                  >
                    <button
                      type="button"
                      onClick={() => handleDateClick(day.date)}
                      disabled={isPast}
                      aria-label={`${value} 선택`}
                      className={cn(
                        'flex size-9 items-center justify-center rounded-full text-body',
                        !day.currentMonth && 'text-text-disabled',
                        day.currentMonth && 'text-foreground',
                        isPast && 'text-text-disabled',
                        isInRange && 'bg-primary-tint text-primary-deep',
                        (isStart || isEnd) &&
                          'bg-primary-deep font-medium text-white',
                      )}
                    >
                      {day.date.getDate()}
                    </button>
                  </div>
                )
              })}
            </div>
          </div>
        </div>
      </div>
    </MobileScreen>
  )
}
