import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQueryClient } from '@tanstack/react-query'
import * as React from 'react'

import {
  getGetItineraryItemQueryKey,
  useCreateVoteOption,
  useGetItineraryItem,
  useUpdateItineraryItem,
} from '@/api/generated/itinerary-item-controller/itinerary-item-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { TextInput } from '@/components/ui/text-input'
import { cn } from '@/lib/utils'
import { getApiErrorMessage } from '@/lib/api-error'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/trips/$tripId/items/$itemId/edit')({
  component: ItemEditPage,
})

const CATEGORIES = ['숙소', '식사', '관광', '이동', '기타'] as const

function ItemEditPage() {
  const { tripId, itemId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const itemIdNumber = Number(itemId)

  const {
    data: response,
    isLoading,
    isError,
    error,
    refetch,
  } = useGetItineraryItem(itemIdNumber)
  const detail = response?.data

  const [title, setTitle] = React.useState('')
  const [category, setCategory] =
    React.useState<(typeof CATEGORIES)[number]>('식사')
  const [decisionMethod, setDecisionMethod] = React.useState<
    '부모님과 투표' | '내가 결정'
  >('부모님과 투표')
  const [decidedPlace, setDecidedPlace] = React.useState('')

  // 조회가 끝나면 폼 값을 실제 데이터로 한 번 채운다. 이후엔 사용자가 입력한 값을 유지한다.
  React.useEffect(() => {
    if (!detail) return
    setTitle(detail.name ?? '')
    setCategory(
      CATEGORIES.includes(detail.category as (typeof CATEGORIES)[number])
        ? (detail.category as (typeof CATEGORIES)[number])
        : '식사',
    )
    setDecisionMethod(
      detail.decisionType === 'HOST_PICK' ? '내가 결정' : '부모님과 투표',
    )
    setDecidedPlace(detail.voteOptions?.[0]?.name ?? '')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detail?.id])

  const updateItineraryItemMutation = useUpdateItineraryItem()
  const createVoteOptionMutation = useCreateVoteOption()

  const goBack = () =>
    navigate({
      to: '/trips/$tripId/items/$itemId',
      params: { tripId, itemId },
    })

  const isSubmitting =
    updateItineraryItemMutation.isPending || createVoteOptionMutation.isPending

  const handleSave = () => {
    const decisionType =
      decisionMethod === '부모님과 투표' ? 'VOTE' : 'HOST_PICK'

    updateItineraryItemMutation.mutate(
      { itemId: itemIdNumber, data: { name: title, category, decisionType } },
      {
        onSuccess: () => {
          void queryClient.invalidateQueries({
            queryKey: getGetItineraryItemQueryKey(itemIdNumber),
          })

          // HOST_PICK인데 아직 선택지가 없으면(= PENDING 상태에서만 수정 가능하므로
          // 있을 수 있는 유일한 경우), 입력한 장소를 선택지로 추가한다 — 서버가
          // HOST_PICK 선택지 추가 시 즉시 확정한다.
          const hasNoOption = (detail?.voteOptions?.length ?? 0) === 0
          if (
            decisionType === 'HOST_PICK' &&
            hasNoOption &&
            decidedPlace.trim().length > 0
          ) {
            createVoteOptionMutation.mutate(
              { itemId: itemIdNumber, params: { name: decidedPlace.trim() } },
              { onSuccess: goBack, onError: goBack },
            )
            return
          }

          goBack()
        },
      },
    )
  }

  if (isLoading) {
    return <MobileScreen>{null}</MobileScreen>
  }

  if (isError || !detail) {
    return (
      <MobileScreen>
        <AppBar type="close" title="일정 수정" onClose={goBack} />
        <div className="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
          <p className="text-[14px] text-destructive">
            {getApiErrorMessage(error)}
          </p>
          <Button size="cta" onClick={() => refetch()}>
            다시 시도
          </Button>
        </div>
      </MobileScreen>
    )
  }

  if (detail.status !== 'PENDING') {
    return (
      <MobileScreen>
        <AppBar type="close" title="일정 수정" onClose={goBack} />
        <div className="flex flex-1 flex-col items-center justify-center gap-2 px-6 text-center">
          <p className="text-[16px] font-bold text-foreground">
            수정할 수 없는 일정이에요
          </p>
          <p className="text-[14px] text-muted-foreground">
            투표가 시작된 일정은 수정할 수 없어요
          </p>
        </div>
      </MobileScreen>
    )
  }

  return (
    <MobileScreen>
      <AppBar type="close" title="일정 수정" onClose={goBack} />

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
          <TextInput
            label="정한 곳"
            placeholder="예: 스시 오마카세 긴자점"
            value={decidedPlace}
            onChange={(event) => setDecidedPlace(event.target.value)}
          />
        )}
      </div>

      <div className="flex flex-col gap-2 px-5 pt-3 pb-7">
        <Button
          size="cta"
          disabled={!title || isSubmitting}
          onClick={handleSave}
        >
          {isSubmitting ? '저장하는 중...' : '저장하기'}
        </Button>
        {updateItineraryItemMutation.isError && (
          <p className="text-center text-caption-sm text-destructive">
            {getApiErrorMessage(updateItineraryItemMutation.error)}
          </p>
        )}
      </div>
    </MobileScreen>
  )
}
