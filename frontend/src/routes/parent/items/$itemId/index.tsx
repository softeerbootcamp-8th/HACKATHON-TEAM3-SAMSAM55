import { useEffect } from 'react'
import { User, Users } from 'lucide-react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { useFindVoteResult } from '@/api/generated/vote-result-controller/vote-result-controller'
import { OptionCard } from '@/components/trip/option-card'
import { VoteStatusRow } from '@/components/trip/vote-status-row'
import { AppBar } from '@/components/ui/app-bar'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'

export const Route = createFileRoute('/parent/items/$itemId/')({
  component: ParentItemDetailPage,
})

function ParentItemDetailPage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/' })
  const itemIdNumber = Number(itemId)
  const isValidItemId = Number.isInteger(itemIdNumber) && itemIdNumber > 0

  const voteResultQuery = useFindVoteResult(itemIdNumber, {
    query: { enabled: isValidItemId, retry: false },
  })
  const errorCode =
    voteResultQuery.data?.error?.code ??
    getApiError(voteResultQuery.error)?.code
  const result = voteResultQuery.data?.success
    ? voteResultQuery.data.data
    : undefined

  useEffect(() => {
    if (!isValidItemId || errorCode === 'ITINERARY_ITEM_NOT_FOUND') {
      void navigate({ to: '/parent', replace: true })
    }
  }, [errorCode, isValidItemId, navigate])

  if (
    voteResultQuery.isLoading ||
    !isValidItemId ||
    errorCode === 'ITINERARY_ITEM_NOT_FOUND'
  ) {
    return (
      <MobileScreen>
        <AppBar
          type="back"
          title="일정 상세"
          onBack={() => navigate({ to: '/parent' })}
        />
      </MobileScreen>
    )
  }

  if (voteResultQuery.isError || !result) {
    return (
      <MobileScreen>
        <AppBar
          type="back"
          title="일정 상세"
          onBack={() => navigate({ to: '/parent' })}
        />
        <p className="px-5 pt-4 text-[14px] text-destructive">
          {getApiError(voteResultQuery.error)?.message ??
            voteResultQuery.data?.error?.message ??
            '일정 상세를 불러오지 못했습니다.'}
        </p>
      </MobileScreen>
    )
  }

  const isConfirmed = result.status === 'CONFIRMED'
  const isVote = result.decisionType === 'VOTE'
  const confirmedOption = result.options?.find(
    (option) => option.optionId === result.confirmedOptionId,
  )
  const maxVoteCount = Math.max(
    0,
    ...(result.options ?? []).map((option) => option.voteCount ?? 0),
  )
  const confirmedVoterRoleNames = (confirmedOption?.voters ?? []).flatMap(
    (voter) => (voter.roleName ? [voter.roleName] : []),
  )

  const pendingParticipantIds = new Set(
    result.pendingParticipants?.map((participant) => participant.participantId),
  )
  const voters = (result.participants ?? []).map((participant) => ({
    initial: participant.roleName?.charAt(0) ?? '?',
    voted: !pendingParticipantIds.has(participant.participantId),
  }))

  return (
    <MobileScreen>
      <AppBar
        type="back"
        title="일정 상세"
        onBack={() => navigate({ to: '/parent' })}
      />

      <div className="flex flex-col gap-2 px-5 pt-4">
        <p className="text-[24px] font-bold text-foreground">{result.name}</p>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            {isVote ? (
              <Users className="size-4 text-primary-deep" />
            ) : (
              <User className="size-4 text-primary-deep" />
            )}
            <span className="text-card-title text-primary-deep">
              {isVote ? '투표' : '자녀가 결정'}
            </span>
          </div>
          {isConfirmed ? (
            <span className="rounded-chip bg-[#e6f6e9] px-2.5 py-1 text-[12px] leading-none font-medium text-[#37b24d]">
              확정
            </span>
          ) : (
            <span className="rounded-chip bg-primary-tint px-2.5 py-1 text-[12px] leading-none font-medium text-primary-deep">
              투표 중
            </span>
          )}
        </div>
        <p className="text-caption text-muted-foreground">
          {result.dayNumber}일차 · {result.category}
        </p>
      </div>

      {!isConfirmed ? (
        <div className="flex flex-col gap-5 px-5 pt-4">
          <VoteStatusRow
            votedCount={result.votedCount ?? 0}
            totalCount={result.totalParticipants ?? 0}
            voters={voters}
          />

          <p className="text-subtitle text-foreground">
            선택지 {result.optionCount ?? 0}개
          </p>
          <div className="flex flex-col gap-3">
            {result.options?.map((option) => (
              <OptionCard
                key={option.optionId}
                title={option.name ?? ''}
                voteCount={option.voteCount}
                voters={option.voters?.map((v) => v.roleName?.charAt(0) ?? '?')}
                leading={maxVoteCount > 0 && option.voteCount === maxVoteCount}
                imageSrc={option.imageUrl}
              />
            ))}
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-4 px-5 pt-4">
          <div className="flex flex-col overflow-hidden rounded-[18px] border border-border">
            {confirmedOption?.imageUrl ? (
              <img
                src={confirmedOption.imageUrl}
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
              <p className="text-caption text-muted-foreground">
                {confirmedOption?.description}
              </p>
              {confirmedVoterRoleNames.length > 0 && (
                <div className="flex items-center gap-2">
                  <div className="flex items-center -space-x-1.5">
                    {(confirmedOption?.voters ?? []).map((voter) => (
                      <span
                        key={voter.participantId}
                        className="flex size-6 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-medium text-foreground"
                      >
                        {voter.roleName?.charAt(0) ?? '?'}
                      </span>
                    ))}
                  </div>
                  <p className="text-[14px] font-medium text-primary-deep">
                    {confirmedVoterRoleNames.join(', ')}가 골랐어요
                  </p>
                </div>
              )}
            </div>
          </div>
          {isVote && (
            <>
              <p className="text-subtitle text-foreground">최종 투표 결과</p>
              <div className="flex flex-col gap-2">
                {result.options?.map((option) => (
                  <div
                    key={option.optionId}
                    className={
                      option.optionId === result.confirmedOptionId
                        ? 'flex h-[50px] items-center justify-between rounded-thumb border-2 border-primary-deep bg-primary-tint px-3.5'
                        : 'flex h-[50px] items-center justify-between rounded-thumb bg-muted px-3.5'
                    }
                  >
                    <p
                      className={
                        option.optionId === result.confirmedOptionId
                          ? 'text-[14px] font-medium text-foreground'
                          : 'text-[14px] text-muted-foreground'
                      }
                    >
                      {option.name}
                    </p>
                    <p
                      className={
                        'text-[14px] font-bold ' +
                        (option.optionId === result.confirmedOptionId
                          ? 'text-primary-deep'
                          : 'text-muted-foreground')
                      }
                    >
                      {option.voteCount}표
                    </p>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}
    </MobileScreen>
  )
}
