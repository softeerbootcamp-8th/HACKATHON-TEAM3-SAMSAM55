import { useState } from 'react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { useCastVotes } from '@/api/generated/vote-controller/vote-controller'
import { Button } from '@/components/ui/button'
import { VoteOptionCard } from '@/components/parent/vote-option-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiErrorMessage } from '@/lib/api-error'

export const Route = createFileRoute('/parent/items/$itemId/vote/')({
  component: ParentVotePage,
})

// TODO(일정 항목 상세 조회 담당자): 일정 항목 상세 조회 API가 아직 없어서 선택지를
// 여기서 가져올 방법이 없다. GET 엔드포인트가 생기면 이 mock을 걷어내고
// itemId 기준으로 실제 선택지(title/description/voteOptionId)를 받아온다.
// id는 실제 백엔드 voteOptionId(number)와 형태를 맞추기 위해 숫자로 둔다.
const MOCK_ITEM_OPTIONS: Record<
  string,
  {
    title: string
    options: { id: number; title: string; description: string }[]
  }
> = {
  '101': {
    title: '저녁 식사는 어디서 먹을까요?',
    options: [
      {
        id: 1001,
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
      },
      {
        id: 1002,
        title: '라멘 이치란 신주쿠점',
        description: '진한 돈코츠 육수로 유명한 라멘 전문점',
      },
    ],
  },
  '102': {
    title: '야경은 어디서 볼까요?',
    options: [
      {
        id: 1003,
        title: '도쿄 타워',
        description: '도쿄 야경을 대표하는 333m 전망 타워',
      },
      {
        id: 1004,
        title: '시부야 스카이',
        description: '탁 트인 옥상에서 도심을 내려다보는 전망대',
      },
      {
        id: 1005,
        title: '오다이바',
        description: '레인보우 브릿지 야경이 한눈에 들어오는 해변',
      },
    ],
  },
}

function ParentVotePage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/vote/' })
  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null)

  const item = MOCK_ITEM_OPTIONS[itemId] ?? MOCK_ITEM_OPTIONS['101']

  // 기존 mock은 "N / 전체 개수" 진행 바를 로컬 배열 인덱스로 그렸는데, 이제 다음
  // 일정 항목은 서버가 nextItemId로 알려주는 값이라 전체 개수를 프론트가 알 수 없다.
  // "다음 미투표 항목 조회" 목록형 API(팀 확인 필요, 별도 스코프)가 생기면
  // 그걸로 전체 개수를 받아와 진행 바를 다시 넣을 수 있다.

  // PUT /api/itinerary-items/my-votes — 한 번에 하나씩 제출한다(밸런스 게임 UX).
  // 응답의 nextItemId로 다음에 투표할 일정 항목을 안내받아 그대로 이어서 이동한다.
  const castVotesMutation = useCastVotes({
    mutation: {
      onSuccess: (response) => {
        const nextItemId = response.data?.nextItemId
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
        votes: [{ itemId: Number(itemId), voteOptionId: selectedOptionId }],
      },
    })
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
          {item.title}
        </p>
        <div className="flex flex-col gap-3">
          {item.options.map((option) => (
            <VoteOptionCard
              key={option.id}
              title={option.title}
              description={option.description}
              selected={selectedOptionId === option.id}
              onClick={() => setSelectedOptionId(option.id)}
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
