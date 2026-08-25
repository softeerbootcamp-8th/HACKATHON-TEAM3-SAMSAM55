import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'

import { SamsamLogo } from '@/components/auth/samsam-logo'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { TextInput } from '@/components/ui/text-input'

export const Route = createFileRoute('/signup')({
  component: SignupPage,
})

function SignupPage() {
  const navigate = useNavigate()

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
          {'아이디과 비밀번호로\n시작해보세요'}
        </p>
        <form className="flex flex-col gap-4">
          <TextInput label="아이디" placeholder="example" />
          <TextInput
            label="비밀번호"
            type="password"
            placeholder="8자 이상 입력해주세요"
          />
          <Button size="cta">가입하기</Button>
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
