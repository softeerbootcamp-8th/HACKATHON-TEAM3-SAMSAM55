import axios from 'axios'

const DEFAULT_ERROR_MESSAGE =
  '요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.'

type ApiErrorBody = {
  error?: {
    code?: string
    message?: string
  }
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined
    if (body?.error?.message) {
      return body.error.message
    }
  }
  return DEFAULT_ERROR_MESSAGE
}
