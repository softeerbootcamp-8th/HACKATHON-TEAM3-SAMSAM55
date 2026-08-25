import { useState } from 'react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { VoteOptionCard } from '@/components/parent/vote-option-card'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/parent/items/$itemId/vote/')({
  component: ParentVotePage,
})

const MOCK_QUESTIONS = [
  {
    title: '저녁 식사는 어디서 먹을까요?',
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
      },
      {
        id: 'opt-2',
        title: '라멘 이치란 신주쿠점',
        description: '진한 돈코츠 육수로 유명한 라멘 전문점',
      },
    ],
  },
  {
    title: '야경은 어디서 볼까요?',
    options: [
      {
        id: 'opt-3',
        title: '도쿄 타워',
        description: '도쿄 야경을 대표하는 333m 전망 타워',
      },
      {
        id: 'opt-4',
        title: '시부야 스카이',
        description: '탁 트인 옥상에서 도심을 내려다보는 전망대',
      },
      {
        id: 'opt-5',
        title: '오다이바',
        description: '레인보우 브릿지 야경이 한눈에 들어오는 해변',
      },
    ],
  },
]

function ParentVotePage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/vote/' })
  const [questionIndex, setQuestionIndex] = useState(0)
  const [selections, setSelections] = useState<Record<number, string | null>>(
    {},
  )

  const question = MOCK_QUESTIONS[questionIndex]
  const selectedOptionId = selections[questionIndex] ?? null
  const total = MOCK_QUESTIONS.length
  const progress = ((questionIndex + 1) / total) * 100

  const handleNext = () => {
    if (questionIndex < total - 1) {
      setQuestionIndex(questionIndex + 1)
      return
    }
    navigate({ to: '/parent/items/$itemId/vote/done', params: { itemId } })
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="px-6 pb-6">
          <Button size="cta" disabled={!selectedOptionId} onClick={handleNext}>
            다음
          </Button>
        </div>
      }
    >
      <p className="px-6 pt-4 pb-3 text-[13px] leading-[1.55] text-muted-foreground">
        {questionIndex + 1} / {total}
      </p>
      <div className="px-6 pb-1">
        <div className="h-1 w-full overflow-hidden rounded-2xs bg-muted">
          <div
            className="h-full rounded-2xs bg-primary transition-all"
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      <div className="flex flex-1 flex-col gap-5 overflow-y-auto px-6 pt-5 pb-4">
        <p className="text-[22px] leading-[1.45] font-bold text-foreground">
          {question.title}
        </p>
        <div className="flex flex-col gap-3">
          {question.options.map((option) => (
            <VoteOptionCard
              key={option.id}
              title={option.title}
              description={option.description}
              selected={selectedOptionId === option.id}
              onClick={() =>
                setSelections((prev) => ({
                  ...prev,
                  [questionIndex]: option.id,
                }))
              }
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
