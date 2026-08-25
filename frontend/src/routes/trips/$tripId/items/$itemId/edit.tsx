import { createFileRoute, useNavigate } from '@tanstack/react-router'
import * as React from 'react'

import { ITEM_FIXTURES } from '@/components/trip/item-fixtures'
import { OptionCard } from '@/components/trip/option-card'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { cn } from '@/lib/utils'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/trips/$tripId/items/$itemId/edit')({
  component: ItemEditPage,
})

const CATEGORIES = ['숙소', '식사', '관광', '이동', '기타'] as const

function ItemEditPage() {
  const { tripId, itemId } = Route.useParams()
  const navigate = useNavigate()
  const item = ITEM_FIXTURES[itemId] ?? ITEM_FIXTURES['item-1']

  // 실제 결정 선택지가 없는 "투표" 일정을 열었을 때도 목록이 비지 않도록 안전값을 둔다.
  const decidedOptions =
    item.options.length > 0
      ? item.options
      : [{ id: 'opt-1', title: item.title, description: '' }]

  const [title, setTitle] = React.useState(item.title)
  const [category, setCategory] = React.useState<(typeof CATEGORIES)[number]>(
    CATEGORIES.includes(item.category as (typeof CATEGORIES)[number])
      ? (item.category as (typeof CATEGORIES)[number])
      : '식사',
  )
  const [decisionMethod, setDecisionMethod] = React.useState<
    '부모님과 투표' | '내가 결정'
  >(item.decisionMethod)
  const [decidedOptionId, setDecidedOptionId] = React.useState(
    decidedOptions[0].id,
  )

  return (
    <MobileScreen>
      <AppBar
        type="close"
        title="일정 수정"
        onClose={() => window.history.back()}
      />

      <div className="flex flex-1 flex-col gap-7 px-5 pt-5">
        <TextInput
          label="일정 이름"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
        />

        <div className="flex flex-col gap-2">
          <p className="text-caption text-muted-foreground">카테고리</p>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setCategory(value)}
                className={cn(
                  'rounded-chip border px-4 py-2 text-label',
                  value === category
                    ? 'border-transparent bg-primary-tint text-primary-deep'
                    : 'border-border bg-background text-muted-foreground',
                )}
              >
                {value}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2.5">
          <p className="text-caption text-muted-foreground">누가 정할까요?</p>
          <div className="flex gap-2.5">
            {(['부모님과 투표', '내가 결정'] as const).map((method) => (
              <button
                key={method}
                type="button"
                onClick={() => setDecisionMethod(method)}
                className={cn(
                  'h-13 flex-1 rounded-card border text-card-title',
                  method === decisionMethod
                    ? 'border-2 border-primary-deep bg-primary-tint text-primary-deep'
                    : 'border-border bg-background text-foreground',
                )}
              >
                {method}
              </button>
            ))}
          </div>
        </div>

        {decisionMethod === '내가 결정' && (
          <div className="flex flex-col gap-2.5">
            <p className="text-caption text-muted-foreground">
              어떤 곳으로 결정할까요?
            </p>
            {decidedOptions.map((option) => (
              <OptionCard
                key={option.id}
                title={option.title}
                leading={option.id === decidedOptionId}
                onClick={() => setDecidedOptionId(option.id)}
              />
            ))}
          </div>
        )}
      </div>

      <div className="flex flex-col gap-2 px-5 pt-3 pb-7">
        <Button
          size="cta"
          onClick={() =>
            navigate({
              to: '/trips/$tripId/items/$itemId',
              params: { tripId, itemId },
            })
          }
        >
          저장하기
        </Button>
        <p className="text-center text-caption-sm text-muted-foreground">
          만들면 목록에 담겨요. 부모님께는 아직 안 보내요
        </p>
      </div>
    </MobileScreen>
  )
}
