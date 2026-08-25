import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { useStartVote } from '@/api/generated/vote-controller/vote-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { AddItemRow } from '@/components/trip/add-item-row'
import { DayTab } from '@/components/trip/day-tab'
import { EditRow } from '@/components/trip/edit-row'
import { ItemCard } from '@/components/trip/item-card'
import { TripMoreSheet } from '@/components/trip/trip-more-sheet'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiErrorMessage } from '@/lib/api-error'
import { toItemStatus } from '@/lib/itinerary-item-status'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/$tripId/')({
  component: TripHomePage,
})

// TODO: 일정 목록 조회 API가 붙으면 이 mock을 걷어내고 실제 데이터로 교체한다 (별도 이슈).
type MockItem = {
  id: number
  title: string
  category: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  voteMeta?: string
}

type MockDay = {
  id: number
  label: string
  pending?: boolean
  items: MockItem[]
}

const INITIAL_DAYS: MockDay[] = [
  {
    id: 1,
    label: '1일차',
    items: [
      {
        id: 101,
        title: '점심 식사',
        category: '식사',
        status: 'voting',
        voteMeta: '2/3표 완료',
      },
      {
        id: 102,
        title: '숙소 체크인',
        category: '숙소',
        status: 'draft',
      },
      {
        id: 103,
        title: '관광지 이동',
        category: '이동',
        status: 'voteDone',
      },
      {
        id: 104,
        title: '1일차 관광지',
        category: '관광',
        status: 'confirmed',
      },
    ],
  },
  { id: 2, label: '2일차', items: [] },
  { id: 3, label: '3일차', pending: true, items: [] },
  { id: 4, label: '4일차', items: [] },
]

function TripHomePage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()

  const [days, setDays] = useState(INITIAL_DAYS)
  const [selectedDay, setSelectedDay] = useState(INITIAL_DAYS[0].id)
  const [isEditing, setIsEditing] = useState(false)
  const [isMoreSheetOpen, setIsMoreSheetOpen] = useState(false)
  const [deleteItemId, setDeleteItemId] = useState<number | null>(null)
  const [isDeleteTripOpen, setIsDeleteTripOpen] = useState(false)

  const startVoteMutation = useStartVote({
    mutation: {
      onSuccess: (response) => {
        const updatedItems = response.data?.items ?? []
        setDays((prev) =>
          prev.map((d) => ({
            ...d,
            items: d.items.map((item) => {
              const updated = updatedItems.find((u) => u.itemId === item.id)
              const nextStatus = toItemStatus(updated?.status)
              return nextStatus ? { ...item, status: nextStatus } : item
            }),
          })),
        )
      },
    },
  })

  const day = days.find((d) => d.id === selectedDay) ?? days[0]
  const draftItems = days
    .flatMap((d) => d.items)
    .filter((item) => item.status === 'draft')
  const draftCount = draftItems.length
  const hasItems = day.items.length > 0

  const handleStartVote = () => {
    if (draftItems.length === 0) {
      return
    }
    startVoteMutation.mutate({
      data: { itemIds: draftItems.map((item) => item.id) },
    })
  }

  const draftSummary = days
    .filter((d) => d.items.some((item) => item.status === 'draft'))
    .map(
      (d) =>
        `${d.label} ${d.items.filter((item) => item.status === 'draft').length}개`,
    )
    .join(' · ')

  return (
    <MobileScreen
      bottomBar={
        !isEditing && (
          <div className="flex flex-col gap-2 border-t border-border px-5 pt-3 pb-7">
            <Button
              size="cta"
              disabled={draftCount === 0 || startVoteMutation.isPending}
              onClick={handleStartVote}
            >
              {startVoteMutation.isPending
                ? '투표 올리는 중...'
                : draftCount > 0
                  ? `준비 중인 일정 투표 올리기 (${draftCount}개)`
                  : '투표 올리기'}
            </Button>
            <p className="text-center text-caption-sm text-muted-foreground">
              {startVoteMutation.isError
                ? getApiErrorMessage(startVoteMutation.error)
                : draftCount > 0
                  ? draftSummary
                  : '일정을 추가하면 부모님께 보낼 수 있어요'}
            </p>
          </div>
        )
      }
    >
      <AppBar
        type="backWithMore"
        onBack={() => navigate({ to: '/trips' })}
        onMore={() => setIsMoreSheetOpen(true)}
      />
      <div className="flex flex-col px-5 pt-4 pb-6">
        <p className="text-display text-foreground">도쿄 가족여행</p>

        <div className="mt-5 flex gap-2 overflow-x-auto">
          {days.map((d) => (
            <DayTab
              key={d.id}
              label={d.label}
              pending={d.pending}
              selected={d.id === selectedDay}
              onClick={() => setSelectedDay(d.id)}
            />
          ))}
        </div>

        {hasItems && (
          <div className="mt-6 flex items-center justify-between">
            <p className="text-[14px] text-muted-foreground">
              일정 {day.items.length}개
            </p>
            <button
              type="button"
              onClick={() => setIsEditing((prev) => !prev)}
              className="text-card-title text-primary-deep"
            >
              {isEditing ? '완료' : '편집'}
            </button>
          </div>
        )}

        <div
          className={cn('flex flex-col gap-3', hasItems ? 'mt-2.5' : 'mt-6')}
        >
          {!hasItems && (
            <div className="flex flex-col items-center gap-2.5 pt-12 pb-7 text-center">
              <p className="text-[17px] font-bold text-foreground">
                {day.label}는 아직 비어있어요
              </p>
              <p className="text-[14px] text-muted-foreground">
                정하고 싶은 일정을 추가해보세요
              </p>
            </div>
          )}

          {hasItems &&
            day.items.map((item) =>
              isEditing ? (
                <EditRow
                  key={item.id}
                  title={item.title}
                  category={item.category}
                  meta={item.voteMeta}
                  status={item.status}
                  onDelete={() => setDeleteItemId(item.id)}
                />
              ) : (
                <ItemCard
                  key={item.id}
                  title={item.title}
                  category={item.category}
                  meta={item.voteMeta}
                  status={item.status}
                  onClick={() =>
                    navigate({
                      to: '/trips/$tripId/items/$itemId',
                      params: { tripId, itemId: String(item.id) },
                    })
                  }
                />
              ),
            )}

          {!isEditing && (
            <AddItemRow
              onClick={() =>
                navigate({ to: '/trips/$tripId/items/new', params: { tripId } })
              }
            />
          )}
        </div>
      </div>

      <TripMoreSheet
        open={isMoreSheetOpen}
        onOpenChange={setIsMoreSheetOpen}
        tripTitle="도쿄 가족여행"
        tripPeriod="8월 26일 - 8월 29일 · 4명"
        onDeleteTrip={() => {
          setIsMoreSheetOpen(false)
          setIsDeleteTripOpen(true)
        }}
      />

      <ConfirmDialog
        open={deleteItemId !== null}
        onOpenChange={(open) => !open && setDeleteItemId(null)}
        title="일정을 삭제할까요?"
        description="일정판에서 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={() => setDeleteItemId(null)}
      />

      <ConfirmDialog
        open={isDeleteTripOpen}
        onOpenChange={setIsDeleteTripOpen}
        title="여행을 삭제할까요?"
        description="등록한 일정이 모두 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={() => setIsDeleteTripOpen(false)}
      />
    </MobileScreen>
  )
}
