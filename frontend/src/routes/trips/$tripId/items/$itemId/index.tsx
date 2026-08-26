import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { User, Users } from 'lucide-react'
import * as React from 'react'

import {
  getGetItineraryItemQueryKey,
  useCreateVoteOption,
  useGetItineraryItem,
  useGetVoteStatus,
} from '@/api/generated/itinerary-item-controller/itinerary-item-controller'
import type {
  CommonResponseItineraryItemDetailResponseDto,
  VoteOptionSummaryDto,
} from '@/api/generated/model'
import {
  useConfirm,
  useStartVote,
  useUnconfirm,
} from '@/api/generated/vote-controller/vote-controller'
import {
  useDeleteVoteOption,
  useUpdateVoteOption,
} from '@/api/generated/vote-option-controller/vote-option-controller'
import { AddOptionSheet } from '@/components/trip/add-option-sheet'
import { EditOptionSheet } from '@/components/trip/edit-option-sheet'
import { OptionCard } from '@/components/trip/option-card'
import { VoteStatusRow } from '@/components/trip/vote-status-row'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/trips/$tripId/items/$itemId/')({
  component: ItemDetailPage,
})

const STATUS_BADGE: Record<
  string,
  { label: string; bg: string; text: string }
> = {
  PENDING: { label: '준비 중', bg: 'bg-[#eeeff1]', text: 'text-[#6b7075]' },
  VOTING: {
    label: '투표 중',
    bg: 'bg-primary-tint',
    text: 'text-primary-deep',
  },
  VOTED: { label: '투표 중', bg: 'bg-primary-tint', text: 'text-primary-deep' },
  CONFIRMED: { label: '확정', bg: 'bg-[#e6f6e9]', text: 'text-[#37b24d]' },
}

// 서버(POST vote/start)가 요구하는 것과 같은 규칙 — 선택지가 2개 미만이면 투표를 시작할 수 없다.
const MIN_VOTE_OPTION_COUNT = 2

// 투표 중일 때만 폴링한다 — 확정되면 더 이상 집계가 바뀌지 않으므로 멈춘다.
const VOTE_STATUS_POLLING_INTERVAL_MS = 3000

function ItemDetailPage() {
  const { tripId, itemId } = Route.useParams()
  const navigate = useNavigate()
  const itemIdNumber = Number(itemId)
  const queryClient = useQueryClient()
  const queryKey = getGetItineraryItemQueryKey(itemIdNumber)

  const { data: response, isLoading } = useGetItineraryItem(itemIdNumber)
  const detail = response?.data
  const options = detail?.voteOptions ?? []
  const status = detail?.status
  const isVoting = status === 'VOTING' || status === 'VOTED'
  const isConfirmed = status === 'CONFIRMED'

  const { data: voteStatusResponse } = useGetVoteStatus(itemIdNumber, {
    query: {
      // 확정된 뒤에도 최종 득표수를 보여줘야 해서 조회는 계속하지만, 더는 안 바뀌니 폴링은 멈춘다.
      enabled: isVoting || isConfirmed,
      refetchInterval: isVoting ? VOTE_STATUS_POLLING_INTERVAL_MS : false,
    },
  })
  const voteStatus = voteStatusResponse?.data
  const voteStatusByOptionId = new Map(
    (voteStatus?.options ?? []).map((option) => [option.optionId, option]),
  )

  const [isEditingOptions, setIsEditingOptions] = React.useState(false)
  const [addOpen, setAddOpen] = React.useState(false)
  const [editingOption, setEditingOption] =
    React.useState<VoteOptionSummaryDto | null>(null)
  const [deletingOption, setDeletingOption] =
    React.useState<VoteOptionSummaryDto | null>(null)
  const [deleteItemOpen, setDeleteItemOpen] = React.useState(false)

  const deleteVoteOptionMutation = useDeleteVoteOption()
  const updateVoteOptionMutation = useUpdateVoteOption()
  const startVoteMutation = useStartVote()
  const createVoteOptionMutation = useCreateVoteOption()
  const confirmMutation = useConfirm()
  const unconfirmMutation = useUnconfirm()

  if (isLoading || !detail) {
    return (
      <MobileScreen>
        <AppBar
          type="back"
          title="일정 상세"
          onBack={() => window.history.back()}
        />
      </MobileScreen>
    )
  }

  const isVote = detail.decisionType === 'VOTE'
  const badge = STATUS_BADGE[status ?? 'PENDING']
  const canStartVote = options.length >= MIN_VOTE_OPTION_COUNT
  const leadingOption = options.reduce<VoteOptionSummaryDto | undefined>(
    (leading, option) => {
      const votes = voteStatusByOptionId.get(option.id)?.voteCount ?? 0
      const leadingVotes = leading
        ? (voteStatusByOptionId.get(leading.id)?.voteCount ?? 0)
        : -1
      return votes > leadingVotes ? option : leading
    },
    undefined,
  )
  const confirmedOption = options.find(
    (option) => option.id === detail.confirmedOptionId,
  )
  const confirmedOptionVoters =
    voteStatusByOptionId.get(confirmedOption?.id ?? -1)?.voters ?? []

  const handleDeleteOption = () => {
    const optionId = deletingOption?.id
    if (optionId === undefined) return

    deleteVoteOptionMutation.mutate(
      { voteOptionId: optionId },
      {
        onSuccess: () => {
          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? {
                    ...old,
                    data: {
                      ...old.data,
                      voteOptions: old.data.voteOptions?.filter(
                        (option) => option.id !== optionId,
                      ),
                    },
                  }
                : old,
          )
          setDeletingOption(null)
        },
      },
    )
  }

  const handleAddOption = (name: string, image: File | null) => {
    createVoteOptionMutation.mutate(
      {
        itemId: itemIdNumber,
        params: { name },
        data: { image: image ?? undefined },
      },
      {
        onSuccess: (created) => {
          const createdOption = created.data
          if (!createdOption) return

          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? {
                    ...old,
                    data: {
                      ...old.data,
                      voteOptions: [
                        ...(old.data.voteOptions ?? []),
                        createdOption,
                      ],
                    },
                  }
                : old,
          )
        },
      },
    )
  }

  const handleEditOption = (
    name: string,
    description: string,
    image: File | null,
  ) => {
    const optionId = editingOption?.id
    if (optionId === undefined) return

    updateVoteOptionMutation.mutate(
      {
        voteOptionId: optionId,
        params: { name, description },
        data: { image: image ?? undefined },
      },
      {
        onSuccess: (updated) => {
          const updatedOption = updated.data
          if (!updatedOption) return

          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? {
                    ...old,
                    data: {
                      ...old.data,
                      voteOptions: old.data.voteOptions?.map((option) =>
                        option.id === optionId ? updatedOption : option,
                      ),
                    },
                  }
                : old,
          )
          setEditingOption(null)
        },
      },
    )
  }

  const handleStartVote = () => {
    startVoteMutation.mutate(
      { data: { itemIds: [itemIdNumber] } },
      {
        onSuccess: () => {
          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? { ...old, data: { ...old.data, status: 'VOTING' } }
                : old,
          )
        },
      },
    )
  }

  const handleConfirm = () => {
    if (leadingOption?.id === undefined) return

    confirmMutation.mutate(
      { itemId: itemIdNumber, data: { voteOptionId: leadingOption.id } },
      {
        onSuccess: (response) => {
          const confirmed = response.data
          if (!confirmed) return

          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? {
                    ...old,
                    data: {
                      ...old.data,
                      status: confirmed.status ?? old.data.status,
                      confirmedOptionId: confirmed.confirmedOptionId,
                    },
                  }
                : old,
          )
        },
      },
    )
  }

  const handleUnconfirm = () => {
    unconfirmMutation.mutate(
      { itemId: itemIdNumber },
      {
        onSuccess: (response) => {
          const result = response.data
          if (!result) return

          queryClient.setQueryData<CommonResponseItineraryItemDetailResponseDto>(
            queryKey,
            (old) =>
              old?.data
                ? {
                    ...old,
                    data: {
                      ...old.data,
                      status: result.status ?? old.data.status,
                    },
                  }
                : old,
          )
        },
      },
    )
  }

  return (
    <MobileScreen>
      <AppBar
        type="backWithMore"
        title="일정 상세"
        onBack={() => window.history.back()}
        onMore={() => setDeleteItemOpen(true)}
      />

      <div className="flex flex-col gap-2 px-5 pt-4">
        <p className="text-[24px] font-bold text-foreground">{detail.name}</p>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            {isVote ? (
              <Users className="size-4 text-primary-deep" />
            ) : (
              <User className="size-4 text-muted-foreground" />
            )}
            <p
              className={
                'text-card-title ' +
                (isVote ? 'text-primary-deep' : 'text-muted-foreground')
              }
            >
              {isVote ? '부모님과 투표' : '내가 결정'}
            </p>
          </div>
          <span
            className={`rounded-chip px-2.5 py-1 text-[12px] leading-none font-medium ${badge.bg} ${badge.text}`}
          >
            {badge.label}
          </span>
        </div>
        <p className="text-caption text-muted-foreground">
          {detail.dayNumber}일차 · {detail.category}
        </p>
      </div>

      <div className="flex flex-1 flex-col gap-4 px-5 pt-4">
        {isVote && status === 'PENDING' && (
          <>
            <div className="flex items-center justify-between">
              <p className="text-[14px] font-medium text-muted-foreground">
                선택지 {options.length}개
              </p>
              <button
                type="button"
                className="text-card-title text-primary-deep"
                onClick={() => setIsEditingOptions((value) => !value)}
              >
                {isEditingOptions ? '완료' : '편집'}
              </button>
            </div>
            <div className="flex flex-col gap-2.5">
              {options.map((option) => (
                <OptionCard
                  key={option.id}
                  title={option.name ?? ''}
                  description={option.description}
                  descriptionSource={option.descriptionSource}
                  editable={isEditingOptions}
                  imageSrc={
                    option.hasImage
                      ? `/api/vote-options/${option.id}/image`
                      : undefined
                  }
                  onClick={() => setEditingOption(option)}
                  onDelete={() => setDeletingOption(option)}
                />
              ))}
            </div>
            <button
              type="button"
              onClick={() => setAddOpen(true)}
              className="flex h-13 items-center justify-center rounded-card border-[1.5px] border-dashed border-primary-deep text-card-title text-primary-deep"
            >
              + 선택지 추가
            </button>
            <p className="text-caption-sm text-muted-foreground">
              카드를 탭하면 이름, 설명, 사진을 수정할 수 있어요
            </p>
          </>
        )}

        {isVote && isVoting && (
          <>
            <VoteStatusRow
              votedCount={voteStatus?.votedCount ?? 0}
              totalCount={voteStatus?.totalParticipants ?? 0}
              voters={(voteStatus?.participants ?? []).map((participant) => ({
                initial: participant.roleName?.charAt(0) ?? '?',
                voted: participant.voted ?? false,
              }))}
            />
            <p className="text-subtitle text-foreground">
              선택지 {options.length}개
            </p>
            <div className="flex flex-col gap-3">
              {options.map((option) => {
                const optionVotes = voteStatusByOptionId.get(option.id)
                return (
                  <OptionCard
                    key={option.id}
                    title={option.name ?? ''}
                    voteCount={optionVotes?.voteCount ?? 0}
                    voters={(optionVotes?.voters ?? []).map(
                      (voter) => voter.roleName?.charAt(0) ?? '?',
                    )}
                    leading={(optionVotes?.voteCount ?? 0) > 0}
                    imageSrc={
                      option.hasImage
                        ? `/api/vote-options/${option.id}/image`
                        : undefined
                    }
                  />
                )
              })}
            </div>
          </>
        )}

        {isVote && isConfirmed && (
          <>
            <div className="flex flex-col gap-2 overflow-hidden rounded-[18px] border border-border">
              {confirmedOption?.hasImage ? (
                <img
                  src={`/api/vote-options/${confirmedOption.id}/image`}
                  alt=""
                  className="h-[233px] w-full object-cover"
                />
              ) : (
                <div className="h-[233px] w-full bg-muted" />
              )}
              <div className="flex flex-col gap-2.5 p-4">
                <p className="text-title-2 text-foreground">
                  {confirmedOption?.name}
                </p>
                {confirmedOption?.description && (
                  <p className="text-caption text-muted-foreground">
                    {confirmedOption.description}
                  </p>
                )}
                {confirmedOptionVoters.length > 0 && (
                  <div className="flex items-center gap-2">
                    <div className="flex items-center -space-x-1.5">
                      {confirmedOptionVoters.map((voter, index) => (
                        <span
                          key={voter.participantId ?? index}
                          className="flex size-6 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-medium text-foreground"
                        >
                          {voter.roleName?.charAt(0) ?? '?'}
                        </span>
                      ))}
                    </div>
                    <p className="text-[14px] font-medium text-primary-deep">
                      {confirmedOptionVoters
                        .map((voter) => voter.roleName)
                        .filter(Boolean)
                        .join(', ')}
                      가 골랐어요
                    </p>
                  </div>
                )}
              </div>
            </div>
            <p className="text-subtitle text-foreground">최종 투표 결과</p>
            <div className="flex flex-col gap-2">
              {options.map((option) => {
                const isOptionConfirmed = option.id === detail.confirmedOptionId
                const voteCount =
                  voteStatusByOptionId.get(option.id)?.voteCount ?? 0
                return (
                  <div
                    key={option.id}
                    className={
                      'flex h-[50px] items-center justify-between rounded-thumb px-3.5 ' +
                      (isOptionConfirmed
                        ? 'border-2 border-primary-deep bg-primary-tint'
                        : 'bg-muted')
                    }
                  >
                    <p
                      className={
                        isOptionConfirmed
                          ? 'text-[14px] font-medium text-foreground'
                          : 'text-[14px] text-muted-foreground'
                      }
                    >
                      {option.name}
                    </p>
                    <p
                      className={
                        'text-[14px] font-bold ' +
                        (isOptionConfirmed
                          ? 'text-primary-deep'
                          : 'text-muted-foreground')
                      }
                    >
                      {voteCount}표
                    </p>
                  </div>
                )
              })}
            </div>
          </>
        )}

        {!isVote && (
          <div className="flex flex-col gap-2.5">
            <div className="flex flex-col gap-2.5 rounded-card border border-border p-3">
              <div className="flex w-full items-center gap-3">
                {options[0]?.hasImage ? (
                  <img
                    src={`/api/vote-options/${options[0].id}/image`}
                    alt=""
                    className="size-11 shrink-0 rounded-card object-cover"
                  />
                ) : (
                  <div className="size-11 shrink-0 rounded-card border-[1.5px] border-dashed border-border bg-muted" />
                )}
                <p className="flex-1 text-card-title text-foreground">
                  {options[0]?.name ?? '아직 선택지가 없어요'}
                </p>
              </div>
              {options[0]?.description && options[0]?.descriptionSource && (
                <div className="flex items-center gap-1.5">
                  <p className="flex-1 text-[12.5px] text-muted-foreground">
                    {options[0].description}
                  </p>
                  <span className="shrink-0 rounded-chip bg-primary-tint px-2 py-[3px] text-[11px] leading-none font-medium text-primary-deep">
                    {options[0].descriptionSource === 'AI'
                      ? '✨ AI 작성'
                      : '✏️ 직접 작성'}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-2 border-t border-border px-5 pt-3 pb-7">
        {status === 'CONFIRMED' ? (
          <Button
            variant="dangerOutline"
            size="cta"
            disabled={unconfirmMutation.isPending}
            onClick={handleUnconfirm}
          >
            확정 해제하기
          </Button>
        ) : isVote && isVoting ? (
          <Button
            size="cta"
            disabled={!leadingOption || confirmMutation.isPending}
            onClick={handleConfirm}
          >
            {leadingOption?.name}로 확정하기
          </Button>
        ) : isVote ? (
          <Button size="cta" disabled={!canStartVote} onClick={handleStartVote}>
            이 일정만 투표 올리기
          </Button>
        ) : (
          <Button
            size="cta"
            onClick={() =>
              navigate({
                to: '/trips/$tripId/items/$itemId/edit',
                params: { tripId, itemId },
              })
            }
          >
            {options[0]?.name ? `${options[0].name}로 확정하기` : '확정하기'}
          </Button>
        )}
        {isVote && (
          <p className="text-center text-caption-sm text-muted-foreground">
            {status === 'CONFIRMED'
              ? '해제하면 다시 투표 상태로 돌아가요'
              : isVoting
                ? '확정해도 나중에 되돌릴 수 있어요'
                : canStartVote
                  ? '투표를 올리면 가족들이 투표할 수 있고, 더는 수정할 수 없어요'
                  : '선택지는 2개 이상이어야 해요'}
          </p>
        )}
      </div>

      <AddOptionSheet
        open={addOpen}
        onOpenChange={setAddOpen}
        onAdd={handleAddOption}
      />
      <EditOptionSheet
        key={editingOption?.id}
        open={editingOption !== null}
        onOpenChange={(open) => !open && setEditingOption(null)}
        initialName={editingOption?.name ?? ''}
        initialDescription={editingOption?.description ?? ''}
        initialDescriptionSource={editingOption?.descriptionSource}
        initialImageSrc={
          editingOption?.hasImage
            ? `/api/vote-options/${editingOption.id}/image`
            : undefined
        }
        onSave={handleEditOption}
      />
      <ConfirmDialog
        open={deletingOption !== null}
        onOpenChange={(open) => !open && setDeletingOption(null)}
        title="선택지를 삭제할까요?"
        description="해당 선택지가 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={handleDeleteOption}
      />
      <ConfirmDialog
        open={deleteItemOpen}
        onOpenChange={setDeleteItemOpen}
        title="일정을 삭제할까요?"
        description="삭제하면 되돌릴 수 없어요."
        confirmLabel="삭제하기"
        danger
        onConfirm={() => {
          setDeleteItemOpen(false)
          navigate({ to: '/trips/$tripId', params: { tripId } })
        }}
      />
    </MobileScreen>
  )
}
