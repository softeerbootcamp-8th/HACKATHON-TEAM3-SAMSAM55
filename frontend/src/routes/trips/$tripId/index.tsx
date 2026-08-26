import { useEffect, useRef, useState } from 'react'
import {
  DndContext,
  type DragEndEvent,
  PointerSensor,
  closestCenter,
  useSensor,
  useSensors,
} from '@dnd-kit/core'
import {
  SortableContext,
  arrayMove,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable'
import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import {
  useDeleteItineraryItem,
  useReorderItineraryItems,
} from '@/api/generated/itinerary-item-controller/itinerary-item-controller'
import type { CommonResponseTripDetailResponseDto } from '@/api/generated/model'
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
import { InviteLinkDialog } from '@/components/trip/invite-link-dialog'
import { ItemCard } from '@/components/trip/item-card'
import { TripMoreSheet } from '@/components/trip/trip-more-sheet'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { formatTripPeriod } from '@/features/trip/trip-format'
import { getApiErrorMessage } from '@/lib/api-error'
import { toItemStatus } from '@/lib/itinerary-item-status'
import { cn } from '@/lib/utils'
import { useHorizontalDragScroll } from '@/hooks/use-horizontal-drag-scroll'

type TripSearch = {
  day?: number
  invite?: boolean
}

// 서버(startVote)가 요구하는 것과 같은 규칙 — 선택지가 2개 미만이면 투표를 시작할 수 없다.
const MIN_VOTE_OPTION_COUNT = 2

export const Route = createFileRoute('/trips/$tripId/')({
  validateSearch: (search: Record<string, unknown>): TripSearch => {
    const day = Number(search.day)

    return {
      day: Number.isInteger(day) && day > 0 ? day : undefined,
      invite: search.invite === true ? true : undefined,
    }
  },
  component: TripHomePage,
})

type TripItem = {
  id: number
  title: string
  category: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  decisionType?: string
  optionCount?: number
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
  const { day: selectedDayParam, invite: shouldOpenInvite } = Route.useSearch()
  const navigate = useNavigate({ from: '/trips/$tripId/' })
  const queryClient = useQueryClient()
  const tripIdNumber = Number(tripId)
  const isValidTripId = Number.isInteger(tripIdNumber) && tripIdNumber > 0
  const tripQuery = useFindTrip(tripIdNumber, {
    query: { enabled: isValidTripId, retry: false },
  })
  const deleteTrip = useDeleteTrip()
  const deleteItineraryItemMutation = useDeleteItineraryItem()
  const reorderItineraryItemsMutation = useReorderItineraryItems()
  const dragSensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
  )
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

          const name = item.name ?? '이름 없는 일정'

          return [
            {
              id: item.id,
              title:
                status === 'confirmed' && item.confirmedOptionName
                  ? `${name} - ${item.confirmedOptionName}`
                  : name,
              category: item.category ?? '기타',
              status,
              decisionType: item.decisionType,
              optionCount: item.optionCount,
            },
          ]
        }),
      },
    ]
  })

  const [isEditing, setIsEditing] = useState(false)
  const [isMoreSheetOpen, setIsMoreSheetOpen] = useState(false)
  const [isCreateItemOpen, setIsCreateItemOpen] = useState(false)
  const [deleteItemId, setDeleteItemId] = useState<number | null>(null)
  const [isDeleteTripOpen, setIsDeleteTripOpen] = useState(false)
  const [deleteError, setDeleteError] = useState<string>()
  const [isInviteDialogOpen, setIsInviteDialogOpen] = useState(false)
  const [isCopyToastVisible, setIsCopyToastVisible] = useState(false)
  const copyToastTimeoutRef = useRef<number | null>(null)
  const inviteLink = detail?.inviteCode
    ? `${window.location.origin}/invite/${detail.inviteCode}`
    : ''

  useEffect(() => {
    const errorCode =
      tripQuery.data?.error?.code ?? getApiError(tripQuery.error)?.code
    if (!isValidTripId || errorCode === 'TRIP_NOT_FOUND') {
      void navigate({ to: '/trips', replace: true })
    }
  }, [isValidTripId, navigate, tripQuery.data?.error?.code, tripQuery.error])

  useEffect(() => {
    if (!shouldOpenInvite) {
      return
    }
    setIsInviteDialogOpen(true)
    void navigate({
      search: (prev) => ({ ...prev, invite: undefined }),
      replace: true,
    })
  }, [shouldOpenInvite, navigate])

  useEffect(() => {
    return () => {
      if (copyToastTimeoutRef.current !== null) {
        window.clearTimeout(copyToastTimeoutRef.current)
      }
    }
  }, [])

  const handleCopyInviteLink = async () => {
    if (!inviteLink) {
      return
    }

    try {
      await navigator.clipboard.writeText(inviteLink)
    } catch {
      return
    }

    setIsCopyToastVisible(true)
    if (copyToastTimeoutRef.current !== null) {
      window.clearTimeout(copyToastTimeoutRef.current)
    }
    copyToastTimeoutRef.current = window.setTimeout(() => {
      setIsCopyToastVisible(false)
      copyToastTimeoutRef.current = null
    }, 2000)
  }

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

  const selectedDay =
    days.find((d) => d.id === selectedDayParam)?.id ?? days[0]?.id
  const day = days.find((d) => d.id === selectedDay)
  const dayScrollHandlers = useHorizontalDragScroll()
  // HOST_PICK(내가 결정) 항목은 방장이 직접 확정하는 방식이라 투표에 올릴 수 없고,
  // 선택지가 2개 미만인 VOTE 항목도 투표를 시작할 수 없다 — 서버(startVote)가 이 조건을
  // 하나라도 못 채우는 항목이 섞여 있으면 배치 전체를 거부한다.
  const isDraftItem = (item: TripItem) =>
    item.status === 'draft' &&
    item.decisionType === 'VOTE' &&
    (item.optionCount ?? 0) >= MIN_VOTE_OPTION_COUNT
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

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event
    if (!day || !over || active.id === over.id) return

    const oldIndex = day.items.findIndex((item) => item.id === active.id)
    const newIndex = day.items.findIndex((item) => item.id === over.id)
    if (oldIndex === -1 || newIndex === -1) return

    const tripDayId = detail?.days?.find((d) => d.dayNumber === day.id)?.id
    if (tripDayId === undefined) return

    const reorderedIds = arrayMove(day.items, oldIndex, newIndex).map(
      (item) => item.id,
    )

    // 서버 응답을 기다리지 않고 캐시를 먼저 새 순서로 바꿔서 바로 반영되게 하고,
    // 요청이 실패하면 서버 상태로 다시 불러와 되돌린다.
    queryClient.setQueryData<CommonResponseTripDetailResponseDto>(
      getFindTripQueryKey(tripIdNumber),
      (old) => {
        if (!old?.data?.days) return old
        return {
          ...old,
          data: {
            ...old.data,
            days: old.data.days.map((d) =>
              d.dayNumber === day.id
                ? {
                    ...d,
                    items: reorderedIds
                      .map((id) => d.items?.find((item) => item.id === id))
                      .filter((item) => item !== undefined),
                  }
                : d,
            ),
          },
        }
      },
    )

    reorderItineraryItemsMutation.mutate(
      { dayId: tripDayId, data: { itemIds: reorderedIds } },
      {
        onError: () => {
          void queryClient.invalidateQueries({
            queryKey: getFindTripQueryKey(tripIdNumber),
          })
        },
      },
    )
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
              onClick={() => void navigate({ search: { day: d.id } })}
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

          {hasItems && isEditing && (
            <DndContext
              sensors={dragSensors}
              collisionDetection={closestCenter}
              onDragEnd={handleDragEnd}
            >
              <SortableContext
                items={day?.items.map((item) => item.id) ?? []}
                strategy={verticalListSortingStrategy}
              >
                {day?.items.map((item) => (
                  <EditRow
                    key={item.id}
                    id={item.id}
                    title={item.title}
                    category={item.category}
                    meta={item.voteMeta}
                    status={item.status}
                    onDelete={() => setDeleteItemId(item.id)}
                  />
                ))}
              </SortableContext>
            </DndContext>
          )}

          {hasItems &&
            !isEditing &&
            day?.items.map((item) => (
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
            ))}

          {/* 편집 중에 마지막 일정을 지우면 hasItems가 false가 되면서 편집/완료
              토글 버튼 자체가 사라져 isEditing을 끌 방법이 없어진다 — 그 상태에서도
              일정 추가는 항상 보여야 한다. */}
          {(!isEditing || !hasItems) && (
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
          setIsInviteDialogOpen(true)
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

      <InviteLinkDialog
        open={isInviteDialogOpen}
        onOpenChange={setIsInviteDialogOpen}
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
        onCopyLink={handleCopyInviteLink}
      />

      {isCopyToastVisible && (
        <div
          className="pointer-events-none fixed bottom-24 left-1/2 z-50 -translate-x-1/2 rounded-chip bg-foreground px-4 py-2 text-caption text-background"
          role="status"
          aria-live="polite"
        >
          복사되었습니다
        </div>
      )}
    </MobileScreen>
  )
}
