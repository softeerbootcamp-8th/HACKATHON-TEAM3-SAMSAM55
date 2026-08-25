import { useState, type FormEvent } from 'react'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'

import { useSignup } from '@/api/generated/auth-controller/auth-controller'
import { SamsamLogo } from '@/components/auth/samsam-logo'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { TextInput } from '@/components/ui/text-input'
import { getApiError } from '@/features/auth/auth'

export const Route = createFileRoute('/signup')({
  component: SignupPage,
})

function SignupPage() {
  const navigate = useNavigate()
  const signup = useSignup()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState<string>()

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorMessage(undefined)

    try {
      const response = await signup.mutateAsync({ data: { loginId, password } })

      if (!response.success || !response.data) {
        setErrorMessage(
          response.error?.message ?? '회원가입 중 오류가 발생했습니다.',
        )
        return
      }

      await navigate({ to: '/login', replace: true })
    } catch (error) {
      const apiError = getApiError(error)
      setErrorMessage(
        apiError?.code === 'DUPLICATE_LOGIN_ID'
          ? (apiError.message ?? '이미 사용 중인 아이디입니다.')
          : (apiError?.message ?? '회원가입 중 오류가 발생했습니다.'),
      )
    }
  }

  return (
    <MobileScreen>
      <AppBar
        type="back"
        title="회원가입"
        onBack={() => navigate({ to: '/login' })}
      />
      <div className="flex flex-col gap-4 px-5 pt-6">
        <SamsamLogo />
        <p className="text-title-1 whitespace-pre-line text-foreground">
          {'아이디와 비밀번호로\n시작해보세요'}
        </p>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <TextInput
            label="아이디"
            name="loginId"
            autoComplete="username"
            placeholder="example"
            value={loginId}
            onChange={(event) => setLoginId(event.target.value)}
            error={errorMessage}
            maxLength={100}
            required
          />
          <TextInput
            label="비밀번호"
            name="password"
            type="password"
            autoComplete="new-password"
            placeholder="8자 이상 입력해주세요"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            minLength={8}
            maxLength={72}
            required
          />
          <Button size="cta" type="submit" disabled={signup.isPending}>
            {signup.isPending ? '가입 중...' : '가입하기'}
          </Button>
        </form>
        <p className="flex justify-center gap-1 text-caption text-muted-foreground">
          이미 계정이 있으신가요?
          <Link to="/login" className="font-medium text-primary-deep">
            로그인
          </Link>
        </p>
      </div>
    </MobileScreen>
  )
}
