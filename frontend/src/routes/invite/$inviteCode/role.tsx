import { useState } from 'react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import {
  useJoin,
  useVerify,
} from '@/api/generated/invite-controller/invite-controller'
import { InviteErrorState } from '@/components/invite/invite-error-state'
import { Button } from '@/components/ui/button'
import { RoleChip } from '@/components/ui/role-chip'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiErrorMessage } from '@/lib/api-error'
import { getMemberEmoji } from '@/lib/member-emoji'

export const Route = createFileRoute('/invite/$inviteCode/role')({
  component: InviteRolePage,
})

function InviteRolePage() {
  const navigate = useNavigate()
  const { inviteCode } = useParams({ from: '/invite/$inviteCode/role' })
  const [selectedParticipantId, setSelectedParticipantId] = useState<
    number | null
  >(null)

  const {
    data: response,
    isLoading,
    isFetching,
    isError,
    error,
  } = useVerify(inviteCode, {
    query: { retry: false, staleTime: 0, refetchOnMount: 'always' },
  })
  const trip = response?.data
  const participants = trip?.participants ?? []

  const joinMutation = useJoin({
    mutation: {
      onSuccess: () => navigate({ to: '/parent' }),
    },
  })

  const handleJoin = () => {
    if (selectedParticipantId === null) {
      return
    }
    joinMutation.mutate({
      inviteCode,
      data: { participantId: selectedParticipantId },
    })
  }

  if (isLoading || isFetching) {
    return <MobileScreen>{null}</MobileScreen>
  }

  if (isError || !trip) {
    return (
      <MobileScreen>
        <InviteErrorState
          error={error}
          errorCode={response?.error?.code}
          variant="invite"
        />
      </MobileScreen>
    )
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="flex flex-col gap-2 px-5 pb-6">
          <Button
            size="cta"
            disabled={selectedParticipantId === null || joinMutation.isPending}
            onClick={handleJoin}
          >
            {joinMutation.isPending ? '참여하는 중...' : '참여하기'}
          </Button>
          {joinMutation.isError && (
            <p className="text-center text-caption-sm text-destructive">
              {getApiErrorMessage(joinMutation.error)}
            </p>
          )}
        </div>
      }
    >
      <div className="flex flex-col gap-6 px-6 pt-7 pb-4">
        <div className="flex flex-col gap-2">
          <p className="text-[22px] leading-[1.45] font-bold text-foreground">
            나의 역할을 골라주세요
          </p>
          <p className="text-[14px] leading-[1.55] text-muted-foreground">
            가족 안에서의 역할을 선택해주세요
          </p>
        </div>
        <div className="grid grid-cols-2 gap-3">
          {participants.map((participant) => (
            <RoleChip
              key={participant.participantId}
              emoji={getMemberEmoji(participant.roleName)}
              label={participant.roleName ?? ''}
              state={
                participant.joined
                  ? 'taken'
                  : selectedParticipantId === participant.participantId
                    ? 'selected'
                    : 'default'
              }
              onClick={() =>
                participant.participantId &&
                setSelectedParticipantId(participant.participantId)
              }
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
