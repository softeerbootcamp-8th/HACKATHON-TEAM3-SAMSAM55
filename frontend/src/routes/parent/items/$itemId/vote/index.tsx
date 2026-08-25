import { useState } from 'react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { useCastVotes } from '@/api/generated/vote-controller/vote-controller'
import { useFindVoteResult } from '@/api/generated/vote-result-controller/vote-result-controller'
import { Button } from '@/components/ui/button'
import { VoteOptionCard } from '@/components/parent/vote-option-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiErrorMessage } from '@/lib/api-error'

export const Route = createFileRoute('/parent/items/$itemId/vote/')({
  component: ParentVotePage,
})

function ParentVotePage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/vote/' })
  const itemIdNumber = Number(itemId)
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null)

  const { data: response, isLoading } = useFindVoteResult(itemIdNumber)
  const result = response?.data

  // 기존 mock은 "N / 전체 개수" 진행 바를 로컬 배열 인덱스로 그렸는데, 다음
  // 일정 항목은 서버가 nextItemId로 알려주는 값이라 전체 개수를 프론트가 알 수 없다.
  // "다음 미투표 항목 조회" 목록형 API(팀 확인 필요, 별도 스코프)가 생기면
  // 그걸로 전체 개수를 받아와 진행 바를 다시 넣을 수 있다.

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

  if (isLoading || !result) {
    return (
      <MobileScreen>
        <p className="px-6 pt-5 text-[14px] text-muted-foreground">
          불러오는 중...
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
              imageSrc={
                option.hasImage
                  ? `/api/vote-options/${option.optionId}/image`
                  : undefined
              }
              onClick={() =>
                option.optionId && setSelectedOptionId(option.optionId)
              }
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
