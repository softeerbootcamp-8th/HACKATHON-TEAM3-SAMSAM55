import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { useFindSchedule } from '@/api/generated/schedule-controller/schedule-controller'
import { Button } from '@/components/ui/button'
import { ItemCard } from '@/components/trip/item-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { formatDateRange } from '@/lib/date'
import { toItemStatus } from '@/lib/itinerary-item-status'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/parent/')({
  component: ParentHomePage,
})

function ParentHomePage() {
  const navigate = useNavigate()
  const { tripId } = Route.useRouteContext()
  const [selectedDayId, setSelectedDayId] = useState<number | null>(null)

  const { data: response, isLoading } = useFindSchedule(tripId ?? 0, {
    query: { enabled: tripId !== undefined },
  })
  const schedule = response?.data
  const days = schedule?.days ?? []
  const day = days.find((d) => d.id === selectedDayId) ?? days[0]
  const items = day?.items ?? []
  const pendingCount = items.filter((item) => item.status === 'VOTING').length

  if (isLoading || !schedule) {
    return (
      <MobileScreen>
        <p className="px-5 pt-4 text-[14px] text-muted-foreground">
          불러오는 중...
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
            disabled={pendingCount === 0}
            onClick={() => {
              const firstVotingItem = items.find((i) => i.status === 'VOTING')
              if (!firstVotingItem?.id) return
              navigate({
                to: '/parent/items/$itemId/vote',
                params: { itemId: String(firstVotingItem.id) },
              })
            }}
          >
            {pendingCount > 0
              ? `투표 시작하기 (남은 ${pendingCount}개)`
              : '투표할 일정 없음'}
          </Button>
        </div>
      }
    >
      <div className="flex flex-col gap-4 px-5 pt-4">
        <div className="flex flex-col gap-1.5">
          <p className="text-display text-foreground">{schedule.title}</p>
          {schedule.startDate && schedule.endDate && (
            <p className="text-[13px] leading-[1.55] text-muted-foreground">
              {formatDateRange(schedule.startDate, schedule.endDate)}
            </p>
          )}
        </div>

        <div className="flex gap-2">
          {days.map((d) => (
            <button
              key={d.id}
              type="button"
              onClick={() => setSelectedDayId(d.id ?? null)}
              className={cn(
                'relative rounded-tab px-4 py-2 text-label',
                d.id === (day?.id ?? days[0]?.id)
                  ? 'bg-primary text-primary-foreground'
                  : 'border border-border bg-background text-muted-foreground',
              )}
            >
              {d.dayNumber}일차
              {d.items?.some((item) => item.status === 'VOTING') && (
                <span className="absolute top-1 right-2 size-1.5 rounded-full bg-status-voting" />
              )}
            </button>
          ))}
        </div>

        <p className="text-[14px] font-medium text-muted-foreground">
          일정 {items.length}개
        </p>

        <div className="flex flex-col gap-3">
          {items.map((item) => {
            const status = toItemStatus(item.status)
            const meta =
              item.status === 'VOTING' || item.status === 'VOTED'
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
