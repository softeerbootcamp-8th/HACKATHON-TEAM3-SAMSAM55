import { useState, type FormEvent } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'

import {
  getMeQueryKey,
  useLogin,
} from '@/api/generated/auth-controller/auth-controller'
import { SamsamLogo } from '@/components/auth/samsam-logo'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { TextInput } from '@/components/ui/text-input'
import { getApiError } from '@/features/auth/auth'

const PASSWORD_PATTERN = /^[\x21-\x7e]+$/

export const Route = createFileRoute('/login')({
  component: LoginPage,
})

function LoginPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const login = useLogin()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [loginIdError, setLoginIdError] = useState<string>()
  const [passwordError, setPasswordError] = useState<string>()

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    const nextLoginIdError = loginId.trim()
      ? undefined
      : '아이디를 입력해주세요'
    const nextPasswordError = !password.trim()
      ? '비밀번호를 입력해주세요'
      : !PASSWORD_PATTERN.test(password)
        ? '비밀번호는 영문, 숫자, 특수문자만 사용할 수 있습니다.'
        : password.length > 72
          ? '비밀번호는 72자 이하여야 합니다.'
          : undefined

    setLoginIdError(nextLoginIdError)
    setPasswordError(nextPasswordError)

    if (nextLoginIdError || nextPasswordError) {
      return
    }

    try {
      const response = await login.mutateAsync({ data: { loginId, password } })

      if (!response.success || !response.data) {
        setPasswordError(
          response.error?.message ?? '로그인 중 오류가 발생했습니다.',
        )
        return
      }

      queryClient.removeQueries({ queryKey: getMeQueryKey() })
      await navigate({ to: '/trips', replace: true })
    } catch (error) {
      const apiError = getApiError(error)
      setPasswordError(
        apiError?.code === 'INVALID_CREDENTIALS'
          ? (apiError.message ?? '아이디 또는 비밀번호가 올바르지 않습니다.')
          : (apiError?.message ?? '로그인 중 오류가 발생했습니다.'),
      )
    }
  }

  return (
    <MobileScreen>
      <AppBar type="plain" title="로그인" />
      <div className="flex flex-col gap-4 px-5 pt-6">
        <SamsamLogo />
        <p className="text-title-1 whitespace-pre-line text-foreground">
          {'가족이 함께\n정하는 여행'}
        </p>
        <form
          className="flex flex-col gap-4"
          noValidate
          onSubmit={handleSubmit}
        >
          <TextInput
            label="아이디"
            name="loginId"
            placeholder="아이디를 입력하세요"
            autoComplete="username"
            value={loginId}
            onChange={(event) => {
              setLoginId(event.target.value)
              setLoginIdError(undefined)
            }}
            error={loginIdError}
            required
          />
          <TextInput
            label="비밀번호"
            name="password"
            type="password"
            autoComplete="current-password"
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChange={(event) => {
              setPassword(event.target.value)
              setPasswordError(undefined)
            }}
            error={passwordError}
            maxLength={72}
            required
          />
          <Button size="cta" type="submit" disabled={login.isPending}>
            {login.isPending ? '로그인 중...' : '로그인'}
          </Button>
        </form>
        <p className="flex justify-center gap-1 text-caption text-muted-foreground">
          계정이 없으신가요?
          <Link to="/signup" className="font-medium text-primary-deep">
            회원가입
          </Link>
        </p>
      </div>
    </MobileScreen>
  )
}
