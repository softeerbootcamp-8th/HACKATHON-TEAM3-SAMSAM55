import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { useFindSchedule } from '@/api/generated/schedule-controller/schedule-controller'
import { Button } from '@/components/ui/button'
import { DayTab } from '@/components/trip/day-tab'
import { ItemCard } from '@/components/trip/item-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { formatDateRange } from '@/lib/date'
import { toItemStatus } from '@/lib/itinerary-item-status'

export const Route = createFileRoute('/parent/')({
  component: ParentHomePage,
})

function ParentHomePage() {
  const navigate = useNavigate()
  const { tripId } = Route.useRouteContext()
  const [selectedDayId, setSelectedDayId] = useState<number | null>(null)

  const scheduleQuery = useFindSchedule(tripId ?? 0, {
    query: { enabled: tripId !== undefined, retry: false },
  })
  const schedule = scheduleQuery.data?.success
    ? scheduleQuery.data.data
    : undefined
  const days = schedule?.days ?? []
  const day = days.find((d) => d.id === selectedDayId) ?? days[0]
  const items = day?.items ?? []
  const votingCount = schedule?.votingCount ?? 0
  const firstVotingItem = days
    .flatMap((scheduleDay) => scheduleDay.items ?? [])
    .find((item) => item.status === 'VOTING')

  if (scheduleQuery.isLoading) {
    return (
      <MobileScreen>
        <p className="px-5 pt-4 text-[14px] text-muted-foreground">
          불러오는 중...
        </p>
      </MobileScreen>
    )
  }

  if (scheduleQuery.isError || !schedule) {
    return (
      <MobileScreen>
        <p className="px-5 pt-4 text-[14px] text-destructive">
          {getApiError(scheduleQuery.error)?.message ??
            scheduleQuery.data?.error?.message ??
            '일정을 불러오지 못했습니다.'}
        </p>
      </MobileScreen>
    )
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="border-t border-border px-5 pt-3 pb-7">
          <Button
            size="cta"
            disabled={votingCount === 0 || firstVotingItem?.id === undefined}
            onClick={() => {
              if (!firstVotingItem?.id) return
              navigate({
                to: '/parent/items/$itemId/vote',
                params: { itemId: String(firstVotingItem.id) },
              })
            }}
          >
            {votingCount > 0
              ? `투표 시작하기 (남은 ${votingCount}개)`
              : '투표할 일정 없음'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col px-5 pt-4 pb-6">
        <div className="flex flex-col gap-1.5">
          <p className="text-display text-foreground">{schedule.title}</p>
          {schedule.startDate && schedule.endDate && (
            <p className="text-[13px] leading-[1.55] text-muted-foreground">
              {formatDateRange(schedule.startDate, schedule.endDate)}
            </p>
          )}
        </div>

        <div className="mt-5 flex min-w-0 flex-nowrap gap-2 overflow-x-auto">
          {days.map((d) =>
            d.id === undefined ? null : (
              <DayTab
                key={d.id}
                label={`${d.dayNumber}일차`}
                pending={d.items?.some((item) => item.status === 'VOTING')}
                selected={d.id === day?.id}
                onClick={() => setSelectedDayId(d.id ?? null)}
              />
            ),
          )}
        </div>

        <p className="mt-6 text-[14px] font-medium text-muted-foreground">
          일정 {items.length}개
        </p>

        <div className="mt-2.5 flex flex-col gap-3">
          {items.map((item) => {
            const status = toItemStatus(item.status)
            const meta =
              item.status === 'VOTING'
                ? `${item.votedCount ?? 0}/${item.totalParticipants ?? 0}표 완료`
                : undefined

            return (
              <ItemCard
                key={item.id}
                title={item.name ?? ''}
                category={item.category ?? ''}
                meta={meta}
                status={status ?? 'draft'}
                onClick={() =>
                  navigate({
                    to: '/parent/items/$itemId',
                    params: { itemId: String(item.id) },
                  })
                }
              />
            )
          })}
        </div>
      </div>
    </MobileScreen>
  )
}
