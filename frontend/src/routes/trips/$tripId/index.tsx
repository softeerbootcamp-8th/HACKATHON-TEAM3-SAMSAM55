import { useEffect, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { useDeleteItineraryItem } from '@/api/generated/itinerary-item-controller/itinerary-item-controller'
import {
  getFindTripQueryKey,
  getFindTripsQueryKey,
  useDeleteTrip,
  useFindTrip,
} from '@/api/generated/trip-controller/trip-controller'
import { useStartVote } from '@/api/generated/vote-controller/vote-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { AddItemRow } from '@/components/trip/add-item-row'
import { CreateItemSheet } from '@/components/trip/create-item-sheet'
import { DayTab } from '@/components/trip/day-tab'
import { EditRow } from '@/components/trip/edit-row'
import { ItemCard } from '@/components/trip/item-card'
import { TripMoreSheet } from '@/components/trip/trip-more-sheet'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { formatTripPeriod } from '@/features/trip/trip-format'
import { getApiErrorMessage } from '@/lib/api-error'
import { toItemStatus } from '@/lib/itinerary-item-status'
import { cn } from '@/lib/utils'
import { useHorizontalDragScroll } from '@/hooks/use-horizontal-drag-scroll'

export const Route = createFileRoute('/trips/$tripId/')({
  component: TripHomePage,
})

type TripItem = {
  id: number
  title: string
  category: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  decisionType?: string
  voteMeta?: string
}

type TripDay = {
  id: number
  label: string
  pending?: boolean
  items: TripItem[]
}

function TripHomePage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const tripIdNumber = Number(tripId)
  const isValidTripId = Number.isInteger(tripIdNumber) && tripIdNumber > 0
  const tripQuery = useFindTrip(tripIdNumber, {
    query: { enabled: isValidTripId, retry: false },
  })
  const deleteTrip = useDeleteTrip()
  const deleteItineraryItemMutation = useDeleteItineraryItem()
  const detail = tripQuery.data?.success ? tripQuery.data.data : undefined
  const days: TripDay[] = (detail?.days ?? []).flatMap((tripDay) => {
    if (tripDay.dayNumber === undefined) {
      return []
    }

    return [
      {
        id: tripDay.dayNumber,
        label: `${tripDay.dayNumber}일차`,
        pending: tripDay.items?.some((item) => item.status === 'PENDING'),
        items: (tripDay.items ?? []).flatMap((item) => {
          const status = toItemStatus(item.status)
          if (item.id === undefined || !status) {
            return []
          }

          return [
            {
              id: item.id,
              title: item.name ?? '이름 없는 일정',
              category: item.category ?? '기타',
              status,
              decisionType: item.decisionType,
            },
          ]
        }),
      },
    ]
  })

  const [selectedDay, setSelectedDay] = useState(1)
  const [isEditing, setIsEditing] = useState(false)
  const [isMoreSheetOpen, setIsMoreSheetOpen] = useState(false)
  const [isCreateItemOpen, setIsCreateItemOpen] = useState(false)
  const [deleteItemId, setDeleteItemId] = useState<number | null>(null)
  const [isDeleteTripOpen, setIsDeleteTripOpen] = useState(false)
  const [deleteError, setDeleteError] = useState<string>()

  useEffect(() => {
    const errorCode =
      tripQuery.data?.error?.code ?? getApiError(tripQuery.error)?.code
    if (!isValidTripId || errorCode === 'TRIP_NOT_FOUND') {
      void navigate({ to: '/trips', replace: true })
    }
  }, [isValidTripId, navigate, tripQuery.data?.error?.code, tripQuery.error])

  const startVoteMutation = useStartVote({
    mutation: {
      onSuccess: (response) => {
        if (response.success) {
          void queryClient.invalidateQueries({
            queryKey: getFindTripQueryKey(tripIdNumber),
          })
        }
      },
    },
  })

  const day = days.find((d) => d.id === selectedDay) ?? days[0]
  const dayScrollHandlers = useHorizontalDragScroll()
  // HOST_PICK(내가 결정) 항목은 방장이 직접 확정하는 방식이라 투표에 올릴 수 없다 —
  // 서버(startVote)가 VOTE가 아닌 항목이 하나라도 섞여 있으면 배치 전체를 거부한다.
  const isDraftItem = (item: TripItem) =>
    item.status === 'draft' && item.decisionType === 'VOTE'
  const draftItems = days.flatMap((d) => d.items).filter(isDraftItem)
  const draftCount = draftItems.length
  const hasItems = (day?.items.length ?? 0) > 0

  const handleStartVote = () => {
    if (draftItems.length === 0) {
      return
    }
    startVoteMutation.mutate({
      data: { itemIds: draftItems.map((item) => item.id) },
    })
  }

  const draftSummary = days
    .filter((d) => d.items.some(isDraftItem))
    .map((d) => `${d.label} ${d.items.filter(isDraftItem).length}개`)
    .join(' · ')

  const handleDeleteItem = async () => {
    if (deleteItemId === null) return
    setDeleteError(undefined)

    try {
      const response = await deleteItineraryItemMutation.mutateAsync({
        itemId: deleteItemId,
      })
      if (!response.success) {
        setDeleteError(response.error?.message ?? '일정을 삭제하지 못했습니다.')
        return
      }

      await queryClient.invalidateQueries({
        queryKey: getFindTripQueryKey(tripIdNumber),
      })
      setDeleteItemId(null)
    } catch (error) {
      setDeleteError(
        getApiError(error)?.message ?? '일정을 삭제하지 못했습니다.',
      )
    }
  }

  const handleDeleteTrip = async () => {
    setDeleteError(undefined)

    try {
      const response = await deleteTrip.mutateAsync({ tripId: tripIdNumber })
      if (!response.success) {
        setDeleteError(response.error?.message ?? '여행을 삭제하지 못했습니다.')
        return
      }

      queryClient.removeQueries({ queryKey: getFindTripQueryKey(tripIdNumber) })
      await queryClient.invalidateQueries({ queryKey: getFindTripsQueryKey() })
      await navigate({ to: '/trips', replace: true })
    } catch (error) {
      const apiError = getApiError(error)
      if (apiError?.code === 'TRIP_NOT_FOUND') {
        await navigate({ to: '/trips', replace: true })
        return
      }
      setDeleteError(apiError?.message ?? '여행을 삭제하지 못했습니다.')
    }
  }

  if (tripQuery.isLoading) {
    return (
      <MobileScreen>
        <div className="flex flex-1 items-center justify-center text-body text-muted-foreground">
          여행을 불러오는 중...
        </div>
      </MobileScreen>
    )
  }

  if (tripQuery.isError || !detail) {
    return (
      <MobileScreen>
        <AppBar type="back" onBack={() => navigate({ to: '/trips' })} />
        <div className="flex flex-1 items-center justify-center px-5 text-center text-body text-destructive">
          {getApiError(tripQuery.error)?.message ??
            tripQuery.data?.error?.message ??
            '여행 정보를 불러오지 못했습니다.'}
        </div>
      </MobileScreen>
    )
  }

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
        <p className="text-display text-foreground">{detail.title}</p>

        {deleteError && (
          <p className="mt-2 text-caption-sm text-destructive" role="alert">
            {deleteError}
          </p>
        )}

        <div
          {...dayScrollHandlers}
          className="scrollbar-none mt-5 flex w-full min-w-0 flex-nowrap gap-2 overflow-x-auto overflow-y-hidden touch-pan-x"
        >
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
              일정 {day?.items.length ?? 0}개
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
                {day?.label ?? '선택한 날짜'}는 아직 비어있어요
              </p>
              <p className="text-[14px] text-muted-foreground">
                정하고 싶은 일정을 추가해보세요
              </p>
            </div>
          )}

          {hasItems &&
            day?.items.map((item) =>
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
            <AddItemRow onClick={() => setIsCreateItemOpen(true)} />
          )}
        </div>
      </div>

      <TripMoreSheet
        open={isMoreSheetOpen}
        onOpenChange={setIsMoreSheetOpen}
        tripTitle={detail.title ?? ''}
        tripPeriod={
          detail.startDate && detail.endDate
            ? formatTripPeriod(
                detail.startDate,
                detail.endDate,
                detail.companionCount ?? 0,
              )
            : ''
        }
        onInviteLink={() => {
          setIsMoreSheetOpen(false)
          void navigate({
            to: '/trips/$tripId/invite',
            params: { tripId },
          })
        }}
        onDeleteTrip={() => {
          setIsMoreSheetOpen(false)
          setIsDeleteTripOpen(true)
        }}
      />

      <CreateItemSheet
        tripId={tripId}
        open={isCreateItemOpen}
        onOpenChange={setIsCreateItemOpen}
        days={detail.days ?? []}
        initialDayNumber={selectedDay}
        onCreated={(itemId) => {
          setIsCreateItemOpen(false)
          void navigate({
            to: '/trips/$tripId/items/$itemId',
            params: { tripId, itemId: String(itemId) },
          })
        }}
      />

      <ConfirmDialog
        open={deleteItemId !== null}
        onOpenChange={(open) => !open && setDeleteItemId(null)}
        title="일정을 삭제할까요?"
        description="일정판에서 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={handleDeleteItem}
      />

      <ConfirmDialog
        open={isDeleteTripOpen}
        onOpenChange={setIsDeleteTripOpen}
        title="여행을 삭제할까요?"
        description="등록한 일정이 모두 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={handleDeleteTrip}
      />
    </MobileScreen>
  )
}
