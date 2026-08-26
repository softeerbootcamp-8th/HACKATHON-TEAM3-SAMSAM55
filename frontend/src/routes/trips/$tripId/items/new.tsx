import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { Camera, X } from 'lucide-react'
import { useState } from 'react'

import {
  useCreateItineraryItem,
  useCreateVoteOption,
} from '@/api/generated/itinerary-item-controller/itinerary-item-controller'
import {
  getFindTripQueryKey,
  useFindTrip,
} from '@/api/generated/trip-controller/trip-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { DayTab } from '@/components/trip/day-tab'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiErrorMessage } from '@/lib/api-error'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/$tripId/items/new')({
  component: CreateItemPage,
})

const CATEGORIES = ['숙소', '식사', '관광', '이동', '기타'] as const

function CreateItemPage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const tripIdNumber = Number(tripId)

  const tripQuery = useFindTrip(tripIdNumber)
  const days = tripQuery.data?.data?.days ?? []

  const [selectedDayId, setSelectedDayId] = useState<number | null>(null)
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState<(typeof CATEGORIES)[number]>('식사')
  const [decisionMethod, setDecisionMethod] = useState<'투표' | '내가 결정'>(
    '투표',
  )
  const [options, setOptions] = useState<string[]>(['스시 오마카세 긴자점', ''])
  const [decidedPlace, setDecidedPlace] = useState('')

  const currentDayId = selectedDayId ?? days[0]?.id ?? null

  const createItineraryItemMutation = useCreateItineraryItem()
  const createVoteOptionMutation = useCreateVoteOption()

  const goBack = () => navigate({ to: '/trips/$tripId', params: { tripId } })

  const handleCreate = () => {
    if (currentDayId === undefined || currentDayId === null) return

    const decisionType = decisionMethod === '투표' ? 'VOTE' : 'HOST_PICK'
    const optionNames = options
      .map((option) => option.trim())
      .filter((option) => option.length > 0)

    createItineraryItemMutation.mutate(
      {
        dayId: currentDayId,
        data: {
          // request는 멀티파트 폼 필드(문자열)로 보내야 해서 JSON으로 직접 직렬화한다.
          request: JSON.stringify({
            name: title,
            category,
            decisionType,
            options: decisionType === 'VOTE' ? optionNames : undefined,
          }),
        },
      },
      {
        onSuccess: (response) => {
          const created = response.data
          if (created?.id === undefined) return

          void queryClient.invalidateQueries({
            queryKey: getFindTripQueryKey(tripIdNumber),
          })

          const goToItem = () =>
            navigate({
              to: '/trips/$tripId/items/$itemId',
              params: { tripId, itemId: String(created.id) },
            })

          // HOST_PICK은 생성 시점엔 선택지가 없으므로, 입력한 장소를 바로
          // 선택지로 추가한다 — 서버가 HOST_PICK 선택지 추가 시 즉시 확정한다.
          if (decisionType === 'HOST_PICK' && decidedPlace.trim().length > 0) {
            createVoteOptionMutation.mutate(
              { itemId: created.id, params: { name: decidedPlace.trim() } },
              { onSuccess: goToItem, onError: goToItem },
            )
            return
          }

          goToItem()
        },
      },
    )
  }

  const isSubmitting =
    createItineraryItemMutation.isPending || createVoteOptionMutation.isPending

  return (
    <MobileScreen>
      <AppBar type="close" title="일정 만들기" onClose={goBack} />

      <div className="flex flex-1 flex-col gap-7 overflow-y-auto px-5 py-6">
        {days.length > 0 && (
          <div className="flex flex-col gap-2">
            <p className="text-caption text-muted-foreground">며칠차</p>
            <div className="flex gap-2 overflow-x-auto">
              {days.map((day) =>
                day.id === undefined ? null : (
                  <DayTab
                    key={day.id}
                    label={`${day.dayNumber}일차`}
                    selected={day.id === currentDayId}
                    onClick={() => setSelectedDayId(day.id ?? null)}
                  />
                ),
              )}
            </div>
          </div>
        )}

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
            <TextInput
              label="정한 곳"
              placeholder="예: 스시 오마카세 긴자점"
              value={decidedPlace}
              onChange={(event) => setDecidedPlace(event.target.value)}
            />
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
          disabled={!title || currentDayId === null || isSubmitting}
          onClick={handleCreate}
        >
          {isSubmitting ? '만드는 중...' : '만들기'}
        </Button>
        <p className="text-center text-caption-sm text-muted-foreground">
          {createItineraryItemMutation.isError
            ? getApiErrorMessage(createItineraryItemMutation.error)
            : '만들면 목록에 담겨요. 부모님께는 아직 안 보내요'}
        </p>
      </div>
    </MobileScreen>
  )
}
