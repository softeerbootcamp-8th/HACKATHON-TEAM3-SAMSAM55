import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { Camera, X } from 'lucide-react'

import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/$tripId/items/new')({
  component: CreateItemPage,
})

const CATEGORIES = ['숙소', '식사', '관광', '이동', '기타'] as const

function CreateItemPage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()

  const [title, setTitle] = useState('')
  const [category, setCategory] = useState<(typeof CATEGORIES)[number]>('식사')
  const [decisionMethod, setDecisionMethod] = useState<'투표' | '내가 결정'>(
    '투표',
  )
  const [options, setOptions] = useState<string[]>(['스시 오마카세 긴자점', ''])

  const goBack = () => navigate({ to: '/trips/$tripId', params: { tripId } })

  return (
    <MobileScreen>
      <AppBar type="close" title="일정 만들기" onClose={goBack} />

      <div className="flex flex-1 flex-col gap-7 overflow-y-auto px-5 py-6">
        <TextInput
          label="일정 이름"
          placeholder="예: 점심 식사"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
        />

        <div className="flex flex-col gap-2">
          <p className="text-caption text-muted-foreground">카테고리</p>
          <div className="flex flex-wrap gap-2">
            {CATEGORIES.map((c) => (
              <button
                key={c}
                type="button"
                onClick={() => setCategory(c)}
                className={cn(
                  'rounded-chip px-4 py-2 text-label',
                  c === category
                    ? 'bg-primary-tint text-primary-deep'
                    : 'border border-border bg-background text-muted-foreground',
                )}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-2.5">
          <p className="text-caption text-muted-foreground">누가 정할까요?</p>
          <div className="flex gap-2.5">
            {(['투표', '내가 결정'] as const).map((method) => (
              <button
                key={method}
                type="button"
                onClick={() => setDecisionMethod(method)}
                className={cn(
                  'h-13 flex-1 rounded-card text-card-title',
                  method === decisionMethod
                    ? 'border-2 border-primary-deep bg-primary-tint text-primary-deep'
                    : 'border border-border bg-background text-foreground',
                )}
              >
                {method}
              </button>
            ))}
          </div>
        </div>

        {decisionMethod === '투표' ? (
          <div className="flex flex-col gap-2.5">
            <div className="flex items-center justify-between">
              <p className="text-caption text-muted-foreground">선택지</p>
              <p className="text-caption-sm text-muted-foreground">2개 이상</p>
            </div>
            <p className="text-caption-sm text-muted-foreground">
              사진은 선택이에요 · 넣으면 부모님이 고르기 쉬워요
            </p>

            {options.map((option, index) => (
              <div
                key={index}
                className="flex items-center gap-3 rounded-card border border-border px-3 py-2.5"
              >
                <div className="flex size-11 shrink-0 items-center justify-center rounded-card border-[1.5px] border-dashed border-border bg-muted">
                  <Camera className="size-4 text-muted-foreground" />
                </div>
                <input
                  value={option}
                  onChange={(event) =>
                    setOptions((prev) =>
                      prev.map((o, i) =>
                        i === index ? event.target.value : o,
                      ),
                    )
                  }
                  placeholder="여행지를 입력해주세요"
                  className="flex-1 text-card-title text-foreground placeholder:text-[#bcbcbc] outline-none"
                />
                <button
                  type="button"
                  aria-label="선택지 삭제"
                  onClick={() =>
                    setOptions((prev) => prev.filter((_, i) => i !== index))
                  }
                >
                  <X className="size-3.5 text-muted-foreground" />
                </button>
              </div>
            ))}

            <button
              type="button"
              onClick={() => setOptions((prev) => [...prev, ''])}
              className="flex h-13 w-full items-center justify-center rounded-card border-[1.5px] border-dashed border-primary-deep text-card-title text-primary-deep"
            >
              + 선택지 추가
            </button>
          </div>
        ) : (
          <>
            <TextInput label="정한 곳" placeholder="예: 스시 오마카세 긴자점" />
            <div className="flex flex-col gap-2">
              <p className="text-caption text-muted-foreground">사진</p>
              <div className="flex size-[75px] items-center justify-center rounded-card border-[1.5px] border-dashed border-border bg-muted">
                <Camera className="size-4 text-muted-foreground" />
              </div>
              <p className="text-caption-sm text-muted-foreground">
                사진을 넣으면 부모님이 확정 일정표에서 보기 편해요
              </p>
            </div>
          </>
        )}
      </div>

      <div className="flex flex-col gap-2 px-5 pb-7">
        <Button
          size="cta"
          disabled={!title}
          onClick={() =>
            navigate({
              to: '/trips/$tripId/items/$itemId',
              params: { tripId, itemId: 'item-1' },
            })
          }
        >
          만들기
        </Button>
        <p className="text-center text-caption-sm text-muted-foreground">
          만들면 목록에 담겨요. 부모님께는 아직 안 보내요
        </p>
      </div>
    </MobileScreen>
  )
}
