import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { Camera, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'

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
import { uploadImage } from '@/lib/upload-image'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/$tripId/items/new')({
  component: CreateItemPage,
})

const CATEGORIES = ['숙소', '식사', '관광', '이동', '기타'] as const
const MAX_VOTE_OPTION_COUNT = 4

type OptionDraft = {
  name: string
  image: File | null
}

type OptionRowProps = {
  option: OptionDraft
  onNameChange: (name: string) => void
  onImageChange: (image: File | null) => void
  onDelete: () => void
}

function OptionRow({
  option,
  onNameChange,
  onImageChange,
  onDelete,
}: OptionRowProps) {
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!option.image) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(option.image)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [option.image])

  return (
    <div className="flex items-center gap-3 rounded-card border border-border px-3 py-2.5">
      <button
        type="button"
        aria-label="사진 추가"
        onClick={() => fileInputRef.current?.click()}
        className="flex size-11 shrink-0 items-center justify-center overflow-hidden rounded-card border-[1.5px] border-dashed border-border bg-muted"
      >
        {previewUrl ? (
          <img src={previewUrl} alt="" className="size-full object-cover" />
        ) : (
          <Camera className="size-4 text-muted-foreground" />
        )}
      </button>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(event) => onImageChange(event.target.files?.[0] ?? null)}
      />
      <input
        value={option.name}
        onChange={(event) => onNameChange(event.target.value)}
        placeholder="여행지를 입력해주세요"
        className="flex-1 text-card-title text-foreground placeholder:text-[#bcbcbc] outline-none"
      />
      <button type="button" aria-label="선택지 삭제" onClick={onDelete}>
        <X className="size-3.5 text-muted-foreground" />
      </button>
    </div>
  )
}

function CreateItemPage() {
  const { tripId } = Route.useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const tripIdNumber = Number(tripId)

  const tripQuery = useFindTrip(tripIdNumber)
  const days = tripQuery.data?.data?.days ?? []

  const [selectedDayId, setSelectedDayId] = useState<number | null>(null)
  const [title, setTitle] = useState('')
  const [category, setCategory] = useState<(typeof CATEGORIES)[number] | null>(
    null,
  )
  const [decisionMethod, setDecisionMethod] = useState<'투표' | '내가 결정'>(
    '투표',
  )
  const [options, setOptions] = useState<OptionDraft[]>([
    { name: '', image: null },
    { name: '', image: null },
  ])
  const [decidedPlace, setDecidedPlace] = useState('')
  const [decidedPlaceImage, setDecidedPlaceImage] = useState<File | null>(null)
  const [decidedPlacePreviewUrl, setDecidedPlacePreviewUrl] = useState<
    string | null
  >(null)
  const decidedPlaceFileInputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (!decidedPlaceImage) {
      setDecidedPlacePreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(decidedPlaceImage)
    setDecidedPlacePreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [decidedPlaceImage])

  const currentDayId = selectedDayId ?? days[0]?.id ?? null

  const createItineraryItemMutation = useCreateItineraryItem()
  const createVoteOptionMutation = useCreateVoteOption()
  const [isUploading, setIsUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)

  const goBack = () => navigate({ to: '/trips/$tripId', params: { tripId } })

  const handleCreate = async () => {
    if (currentDayId === undefined || currentDayId === null) return
    if (category === null) return

    const decisionType = decisionMethod === '투표' ? 'VOTE' : 'HOST_PICK'
    const filledOptions = options.filter(
      (option) => option.name.trim().length > 0,
    )

    setUploadError(null)
    setIsUploading(true)
    let optionPayloads: { name: string; imageKey?: string }[] | undefined
    let decidedPlaceImageKey: string | undefined
    try {
      if (decisionType === 'VOTE') {
        optionPayloads = await Promise.all(
          filledOptions.map(async (option) => ({
            name: option.name.trim(),
            imageKey: option.image
              ? await uploadImage(option.image)
              : undefined,
          })),
        )
      } else if (decidedPlaceImage) {
        decidedPlaceImageKey = await uploadImage(decidedPlaceImage)
      }
    } catch {
      setUploadError('사진 업로드에 실패했어요. 다시 시도해주세요.')
      setIsUploading(false)
      return
    }
    setIsUploading(false)

    createItineraryItemMutation.mutate(
      {
        dayId: currentDayId,
        data: {
          name: title,
          category,
          decisionType,
          options: optionPayloads,
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
              {
                itemId: created.id,
                data: {
                  name: decidedPlace.trim(),
                  imageKey: decidedPlaceImageKey,
                },
              },
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
    isUploading ||
    createItineraryItemMutation.isPending ||
    createVoteOptionMutation.isPending

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
              <p className="text-caption-sm text-muted-foreground">2~4개</p>
            </div>
            <p className="text-caption-sm text-muted-foreground">
              사진은 선택이에요 · 넣으면 부모님이 고르기 쉬워요
            </p>

            {options.map((option, index) => (
              <OptionRow
                key={index}
                option={option}
                onNameChange={(name) =>
                  setOptions((prev) =>
                    prev.map((o, i) => (i === index ? { ...o, name } : o)),
                  )
                }
                onImageChange={(image) =>
                  setOptions((prev) =>
                    prev.map((o, i) => (i === index ? { ...o, image } : o)),
                  )
                }
                onDelete={() =>
                  setOptions((prev) => prev.filter((_, i) => i !== index))
                }
              />
            ))}

            <button
              type="button"
              disabled={options.length >= MAX_VOTE_OPTION_COUNT}
              onClick={() =>
                setOptions((prev) => [...prev, { name: '', image: null }])
              }
              className={cn(
                'flex h-13 w-full items-center justify-center rounded-card border-[1.5px] border-dashed text-card-title',
                options.length >= MAX_VOTE_OPTION_COUNT
                  ? 'border-border text-muted-foreground'
                  : 'border-primary-deep text-primary-deep',
              )}
            >
              {options.length >= MAX_VOTE_OPTION_COUNT
                ? '선택지는 최대 4개까지예요'
                : '+ 선택지 추가'}
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
              <button
                type="button"
                aria-label="사진 추가"
                onClick={() => decidedPlaceFileInputRef.current?.click()}
                className="flex size-[75px] items-center justify-center overflow-hidden rounded-card border-[1.5px] border-dashed border-border bg-muted"
              >
                {decidedPlacePreviewUrl ? (
                  <img
                    src={decidedPlacePreviewUrl}
                    alt=""
                    className="size-full object-cover"
                  />
                ) : (
                  <Camera className="size-4 text-muted-foreground" />
                )}
              </button>
              <input
                ref={decidedPlaceFileInputRef}
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(event) =>
                  setDecidedPlaceImage(event.target.files?.[0] ?? null)
                }
              />
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
          disabled={
            !title || !category || currentDayId === null || isSubmitting
          }
          onClick={handleCreate}
        >
          {isSubmitting ? '만드는 중...' : '만들기'}
        </Button>
        <p className="text-center text-caption-sm text-muted-foreground">
          {uploadError
            ? uploadError
            : createItineraryItemMutation.isError
              ? getApiErrorMessage(createItineraryItemMutation.error)
              : '만들면 목록에 담겨요. 부모님께는 아직 안 보내요'}
        </p>
      </div>
    </MobileScreen>
  )
}
