import { useEffect, useState } from 'react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { useFindSchedule } from '@/api/generated/schedule-controller/schedule-controller'
import { useCastVotes } from '@/api/generated/vote-controller/vote-controller'
import { useFindVoteResult } from '@/api/generated/vote-result-controller/vote-result-controller'
import { Button } from '@/components/ui/button'
import { VoteOptionCard } from '@/components/parent/vote-option-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import { getApiErrorMessage } from '@/lib/api-error'

export const Route = createFileRoute('/parent/items/$itemId/vote/')({
  component: ParentVotePage,
})

const SKIPPABLE_ERROR_CODES = new Set([
  'ITINERARY_ITEM_NOT_FOUND',
  'ITINERARY_ITEM_NOT_VOTABLE',
  'VOTE_ALREADY_CAST',
])

function ParentVotePage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/vote/' })
  const { tripId } = Route.useRouteContext()
  const itemIdNumber = Number(itemId)
  const isValidItemId = Number.isInteger(itemIdNumber) && itemIdNumber > 0
  const [selection, setSelection] = useState<{
    itemId: number
    optionId: number
  } | null>(null)
  const selectedOptionId =
    selection?.itemId === itemIdNumber ? selection.optionId : null

  const voteResultQuery = useFindVoteResult(itemIdNumber, {
    query: { enabled: isValidItemId, retry: false },
  })
  const scheduleQuery = useFindSchedule(tripId ?? 0, {
    query: { enabled: tripId !== undefined, retry: false },
  })
  const errorCode =
    voteResultQuery.data?.error?.code ??
    getApiError(voteResultQuery.error)?.code
  const result = voteResultQuery.data?.success
    ? voteResultQuery.data.data
    : undefined
  const schedule = scheduleQuery.data?.success
    ? scheduleQuery.data.data
    : undefined
  const votingItems = (schedule?.days ?? [])
    .flatMap((day) => day.items ?? [])
    .filter((item) => item.status === 'VOTING')
  const votingCount = schedule?.votingCount ?? 0
  const itemIndex = votingItems.findIndex((item) => item.id === itemIdNumber)
  const currentItemNumber = Math.min(
    itemIndex >= 0 ? itemIndex + 1 : 1,
    Math.max(votingCount, 1),
  )
  const progress =
    votingCount > 0 ? Math.min((currentItemNumber / votingCount) * 100, 100) : 0

  // 투표 도중 이 일정이 삭제되거나(자녀가 지움) 확정되면(자녀가 먼저 확정) 이
  // 일정은 더 이상 투표 대상이 아니다 — 대상에서 빼고 다음 미투표 일정으로
  // 넘어간다. 갈 곳이 없으면 완료 화면으로 보낸다.
  const goToNextItem = async () => {
    const refetched = await scheduleQuery.refetch()
    const freshSchedule = refetched.data?.success
      ? refetched.data.data
      : undefined
    const nextItem = (freshSchedule?.days ?? [])
      .flatMap((day) => day.items ?? [])
      .find((item) => item.needsVote)

    if (nextItem?.id !== undefined) {
      void navigate({
        to: '/parent/items/$itemId/vote',
        params: { itemId: String(nextItem.id) },
        replace: true,
      })
      return
    }
    void navigate({
      to: '/parent/items/$itemId/vote/done',
      params: { itemId },
      replace: true,
    })
  }

  useEffect(() => {
    if (!isValidItemId) {
      void navigate({ to: '/parent', replace: true })
      return
    }
    if (errorCode === 'ITINERARY_ITEM_NOT_FOUND') {
      void goToNextItem()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [errorCode, isValidItemId])

  // 투표 화면에 머무는 동안 자녀가 먼저 확정해버리면 result는 정상 응답하지만
  // status가 더 이상 VOTING이 아니다 — 이때도 다음 일정으로 넘긴다.
  useEffect(() => {
    if (result && result.status !== 'VOTING') {
      void goToNextItem()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [result?.status])

  // PUT /api/itinerary-items/my-votes — 한 번에 하나씩 제출한다(밸런스 게임 UX).
  // 응답의 nextItemId로 다음에 투표할 일정 항목을 안내받아 그대로 이어서 이동한다.
  const castVotesMutation = useCastVotes({
    mutation: {
      onSuccess: (mutationResponse) => {
        const nextItemId = mutationResponse.data?.nextItemId
        if (nextItemId) {
          navigate({
            to: '/parent/items/$itemId/vote',
            params: { itemId: String(nextItemId) },
          })
          return
        }
        navigate({ to: '/parent/items/$itemId/vote/done', params: { itemId } })
      },
      onError: (error) => {
        // 제출하는 사이 일정이 삭제·확정되거나(위 두 효과가 못 잡을 만큼 빠른
        // 경합), 다른 경로로 이미 투표한 상태였다면 에러로 멈추지 않고 넘어간다.
        const code = getApiError(error)?.code
        if (code && SKIPPABLE_ERROR_CODES.has(code)) {
          void goToNextItem()
        }
      },
    },
  })

  const handleNext = () => {
    if (selectedOptionId === null) {
      return
    }
    castVotesMutation.mutate({
      data: {
        votes: [{ itemId: itemIdNumber, voteOptionId: selectedOptionId }],
      },
    })
  }

  if (
    voteResultQuery.isLoading ||
    !isValidItemId ||
    errorCode === 'ITINERARY_ITEM_NOT_FOUND' ||
    (result && result.status !== 'VOTING')
  ) {
    return (
      <MobileScreen>
        <p className="px-6 pt-5 text-[14px] text-muted-foreground">
          불러오는 중...
        </p>
      </MobileScreen>
    )
  }

  if (voteResultQuery.isError || !result) {
    return (
      <MobileScreen>
        <p className="px-6 pt-5 text-[14px] text-destructive">
          {getApiError(voteResultQuery.error)?.message ??
            voteResultQuery.data?.error?.message ??
            '투표 정보를 불러오지 못했습니다.'}
        </p>
      </MobileScreen>
    )
  }

  return (
    <MobileScreen
      floatingBottomBar
      bottomBar={
        <div className="flex flex-col gap-2 px-6 pb-6">
          <Button
            size="cta"
            disabled={selectedOptionId === null || castVotesMutation.isPending}
            onClick={handleNext}
          >
            {castVotesMutation.isPending ? '제출하는 중...' : '다음'}
          </Button>
          {castVotesMutation.isError && (
            <p className="text-center text-caption-sm text-destructive">
              {getApiErrorMessage(castVotesMutation.error)}
            </p>
          )}
        </div>
      }
    >
      {votingCount > 0 && (
        <>
          <p className="px-6 pt-4 pb-3 text-[13px] leading-[1.55] text-muted-foreground">
            {currentItemNumber} / {votingCount}
          </p>
          <div className="px-6 pb-1">
            <div className="h-1 w-full overflow-hidden rounded-2xs bg-muted">
              <div
                className="h-full rounded-2xs bg-primary transition-all"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        </>
      )}
      <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 pt-5 pb-4">
        <p className="text-[22px] leading-[1.45] font-bold text-foreground">
          {result.name}
        </p>
        <div className="flex flex-col gap-3">
          {result.options?.map((option) => (
            <VoteOptionCard
              key={option.optionId}
              title={option.name ?? ''}
              description={option.description ?? ''}
              selected={selectedOptionId === option.optionId}
              imageSrc={option.imageUrl}
              onClick={() =>
                option.optionId &&
                setSelection({
                  itemId: itemIdNumber,
                  optionId: option.optionId,
                })
              }
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
