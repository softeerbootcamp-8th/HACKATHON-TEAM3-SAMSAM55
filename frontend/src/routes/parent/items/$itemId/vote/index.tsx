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

  useEffect(() => {
    if (!isValidItemId || errorCode === 'ITINERARY_ITEM_NOT_FOUND') {
      void navigate({ to: '/parent', replace: true })
    }
  }, [errorCode, isValidItemId, navigate])

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
    errorCode === 'ITINERARY_ITEM_NOT_FOUND'
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
