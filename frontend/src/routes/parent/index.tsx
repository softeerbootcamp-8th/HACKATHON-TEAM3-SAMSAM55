import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { ItemCard } from '@/components/trip/item-card'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/parent/')({
  component: ParentHomePage,
})

type DecisionMethod = '부모님과 투표' | '내가 결정'
type ItemStatus = 'draft' | 'voting' | 'confirmed' | 'voteDone'

type MockItem = {
  id: string
  title: string
  category: string
  decisionMethod: DecisionMethod
  status: ItemStatus
  meta: string
}

const DAYS = [
  { label: '1일차', pending: false },
  { label: '2일차', pending: false },
  { label: '3일차', pending: true },
  { label: '4일차', pending: false },
]

const ITEMS_BY_DAY: Record<number, MockItem[]> = {
  0: [
    {
      id: 'item-1',
      title: '점심 식사',
      category: '식사',
      decisionMethod: '부모님과 투표',
      status: 'voting',
      meta: '2/3표 완료',
    },
    {
      id: 'item-2',
      title: '관광지 이동',
      category: '이동',
      decisionMethod: '부모님과 투표',
      status: 'voteDone',
      meta: '',
    },
    {
      id: 'item-3',
      title: '1일차 관광지',
      category: '관광',
      decisionMethod: '내가 결정',
      status: 'confirmed',
      meta: '',
    },
  ],
}

function ParentHomePage() {
  const navigate = useNavigate()
  const [dayIndex, setDayIndex] = useState(0)
  const items = ITEMS_BY_DAY[dayIndex] ?? []
  const pendingCount = items.filter((item) => item.status === 'voting').length

  return (
    <MobileScreen
      bottomBar={
        <div className="border-t border-border px-5 pt-3 pb-7">
          <Button
            size="cta"
            disabled={pendingCount === 0}
            onClick={() =>
              navigate({
                to: '/parent/items/$itemId/vote',
                params: {
                  itemId: items.find((i) => i.status === 'voting')!.id,
                },
              })
            }
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
          <p className="text-display text-foreground">도쿄 가족여행</p>
          <p className="text-[13px] leading-[1.55] text-muted-foreground">
            8월 24일 - 8월 27일
          </p>
        </div>

        <div className="flex gap-2">
          {DAYS.map((day, index) => (
            <button
              key={day.label}
              type="button"
              onClick={() => setDayIndex(index)}
              className={cn(
                'relative rounded-tab px-4 py-2 text-label',
                index === dayIndex
                  ? 'bg-primary text-primary-foreground'
                  : 'border border-border bg-background text-muted-foreground',
              )}
            >
              {day.label}
              {day.pending && (
                <span className="absolute top-1 right-2 size-1.5 rounded-full bg-status-voting" />
              )}
            </button>
          ))}
        </div>

        <p className="text-[14px] font-medium text-muted-foreground">
          일정 {items.length}개
        </p>

        <div className="flex flex-col gap-3">
          {items.map((item) => (
            <ItemCard
              key={item.id}
              title={item.title}
              category={item.category}
              meta={item.meta || undefined}
              status={item.status}
              onClick={() =>
                navigate({
                  to: '/parent/items/$itemId',
                  params: { itemId: item.id },
                })
              }
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
