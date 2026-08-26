import { useEffect, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  createFileRoute,
  Outlet,
  useNavigate,
  useParams,
} from '@tanstack/react-router'
import axios from 'axios'

import {
  getMeQueryKey,
  useLogout,
  useMe,
} from '@/api/generated/auth-controller/auth-controller'
import { useVerify } from '@/api/generated/invite-controller/invite-controller'
import { getApiError } from '@/features/auth/auth'

export const Route = createFileRoute('/invite/$inviteCode')({
  component: InviteLayout,
})

function InviteLayout() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { inviteCode } = useParams({ from: '/invite/$inviteCode' })
  const verifyQuery = useVerify(inviteCode, {
    query: { retry: false, staleTime: 0, refetchOnMount: 'always' },
  })
  const meQuery = useMe({ query: { retry: false } })
  const { mutate: logoutParticipant } = useLogout()
  const logoutStartedRef = useRef(false)
  const verifyErrorCode =
    verifyQuery.data?.error?.code ?? getApiError(verifyQuery.error)?.code
  const verifyRequestSettled =
    verifyQuery.isFetchedAfterMount && !verifyQuery.isFetching
  const isInvalidInvite =
    verifyRequestSettled &&
    (verifyErrorCode === 'INVITE_CODE_NOT_FOUND' ||
      verifyErrorCode === 'TRIP_NOT_FOUND' ||
      (axios.isAxiosError(verifyQuery.error) &&
        verifyQuery.error.response?.status === 404))
  const hasValidInvite = Boolean(
    verifyRequestSettled &&
    !verifyQuery.isError &&
    verifyQuery.data?.success &&
    verifyQuery.data.data,
  )
  const meRequestSettled = meQuery.isFetchedAfterMount && !meQuery.isFetching
  const isParticipant =
    meRequestSettled &&
    !meQuery.isError &&
    meQuery.data?.success &&
    meQuery.data.data?.actorType === 'PARTICIPANT'

  useEffect(() => {
    if (hasValidInvite && isParticipant) {
      void navigate({ to: '/parent', replace: true })
    }
  }, [hasValidInvite, isParticipant, navigate])

  useEffect(() => {
    if (!isInvalidInvite || !isParticipant || logoutStartedRef.current) {
      return
    }

    // 삭제된 여행의 참여자 세션/복구 쿠키가 남아 있으면 다음 초대 링크도
    // 이전 여행의 /parent로 이동하므로, 무효 초대에서는 참여자 인증을 정리한다.
    logoutStartedRef.current = true
    logoutParticipant(undefined, {
      onSettled: () => {
        queryClient.removeQueries({ queryKey: getMeQueryKey() })
      },
    })
  }, [isInvalidInvite, isParticipant, logoutParticipant, queryClient])

  return <Outlet />
}
