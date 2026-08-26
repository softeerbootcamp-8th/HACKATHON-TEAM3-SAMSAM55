import type { QueryClient } from '@tanstack/react-query'
import { redirect } from '@tanstack/react-router'
import axios from 'axios'

import { getMeQueryOptions } from '@/api/generated/auth-controller/auth-controller'
import type { CommonResponseVoid, ErrorResponse } from '@/api/generated/model'

type ActorType = 'HOST' | 'PARTICIPANT'

export function getApiError(error: unknown): ErrorResponse | undefined {
  if (!axios.isAxiosError<CommonResponseVoid>(error)) {
    return undefined
  }

  return error.response?.data.error
}

export async function requireActor(
  queryClient: QueryClient,
  expectedActorType: ActorType,
) {
  let response

  try {
    response = await queryClient.fetchQuery(
      getMeQueryOptions({ query: { retry: false, staleTime: 0 } }),
    )
  } catch (error) {
    if (getApiError(error)?.code === 'UNAUTHENTICATED') {
      throw redirect({ to: '/login', replace: true })
    }

    throw error
  }

  if (!response.success || !response.data) {
    if (response.error?.code === 'UNAUTHENTICATED') {
      throw redirect({ to: '/login', replace: true })
    }

    throw new Error(
      response.error?.message ?? '인증 정보를 확인할 수 없습니다.',
    )
  }

  if (
    response.data.actorType !== 'HOST' &&
    response.data.actorType !== 'PARTICIPANT'
  ) {
    throw new Error('알 수 없는 인증 주체입니다.')
  }

  if (response.data.actorType !== expectedActorType) {
    throw redirect({
      to: response.data.actorType === 'PARTICIPANT' ? '/parent' : '/trips',
      replace: true,
    })
  }

  return response.data
}

export async function redirectIfParticipant(queryClient: QueryClient) {
  let response

  try {
    response = await queryClient.fetchQuery(
      getMeQueryOptions({ query: { retry: false, staleTime: 0 } }),
    )
  } catch (error) {
    if (getApiError(error)?.code === 'UNAUTHENTICATED') {
      return
    }

    throw error
  }

  if (!response.success || !response.data) {
    if (response.error?.code === 'UNAUTHENTICATED') {
      return
    }

    throw new Error(
      response.error?.message ?? '인증 정보를 확인할 수 없습니다.',
    )
  }

  if (response.data.actorType === 'PARTICIPANT') {
    throw redirect({ to: '/parent', replace: true })
  }
}
