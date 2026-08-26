import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import {
  getMeQueryKey,
  useLogout,
} from '@/api/generated/auth-controller/auth-controller'
import { useFindSchedule } from '@/api/generated/schedule-controller/schedule-controller'
import { InviteErrorState } from '@/components/invite/invite-error-state'
import { Button } from '@/components/ui/button'
import { DayTab } from '@/components/trip/day-tab'
import { ItemCard } from '@/components/trip/item-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { formatDateRange } from '@/lib/date'
import { toItemStatus } from '@/lib/itinerary-item-status'
import { useHorizontalDragScroll } from '@/hooks/use-horizontal-drag-scroll'

type ParentSearch = {
  day?: number
}

export const Route = createFileRoute('/parent/')({
  validateSearch: (search: Record<string, unknown>): ParentSearch => {
    const day = Number(search.day)

    return {
      day: Number.isInteger(day) && day > 0 ? day : undefined,
    }
  },
  component: ParentHomePage,
})

function ParentHomePage() {
  const navigate = useNavigate({ from: '/parent/' })
  const queryClient = useQueryClient()
  const { day: selectedDayParam } = Route.useSearch()
  const { tripId } = Route.useRouteContext()
  const { mutate: logoutParticipant } = useLogout()
  const logoutStartedRef = useRef(false)

  const scheduleQuery = useFindSchedule(tripId ?? 0, {
    query: { enabled: tripId !== undefined, retry: false },
  })
  const scheduleErrorCode =
    scheduleQuery.data?.error?.code ?? getApiError(scheduleQuery.error)?.code
  const shouldClearParticipantSession =
    scheduleQuery.isError && scheduleErrorCode === 'TRIP_NOT_FOUND'
  const schedule = scheduleQuery.data?.success
    ? scheduleQuery.data.data
    : undefined
  const days = schedule?.days ?? []
  const selectedDayId =
    days.find((d) => d.id === selectedDayParam)?.id ?? days[0]?.id
  const day = days.find((d) => d.id === selectedDayId)
  const items = day?.items ?? []
  const votingCount = schedule?.votingCount ?? 0
  const firstVotingItem = days
    .flatMap((scheduleDay) => scheduleDay.items ?? [])
    .find((item) => item.status === 'VOTING')
  const dayScrollHandlers = useHorizontalDragScroll()

  useEffect(() => {
    if (!shouldClearParticipantSession || logoutStartedRef.current) {
      return
    }

    // 삭제된 여행의 참여자 세션과 복구 쿠키가 남아 있으면 초대 링크 접근 시
    // 삭제된 여행의 /parent로 다시 이동하므로, 만료된 인증을 함께 정리한다.
    logoutStartedRef.current = true
    logoutParticipant(undefined, {
      onSettled: () => {
        queryClient.removeQueries({ queryKey: getMeQueryKey() })
      },
    })
  }, [logoutParticipant, queryClient, shouldClearParticipantSession])

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
        <InviteErrorState
          error={scheduleQuery.error}
          errorCode={scheduleQuery.data?.error?.code}
          variant={
            scheduleErrorCode === 'TRIP_NOT_FOUND' ? 'invite' : 'generic'
          }
        />
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

        <div
          {...dayScrollHandlers}
          className="scrollbar-none mt-5 flex w-full min-w-0 flex-nowrap gap-2 overflow-x-auto overflow-y-hidden touch-pan-x"
        >
          {days.map((d) =>
            d.id === undefined ? null : (
              <DayTab
                key={d.id}
                label={`${d.dayNumber}일차`}
                pending={d.items?.some((item) => item.status === 'VOTING')}
                selected={d.id === day?.id}
                onClick={() => void navigate({ search: { day: d.id } })}
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
