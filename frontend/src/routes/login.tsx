import { createFileRoute, Link } from '@tanstack/react-router'

import { SamsamLogo } from '@/components/auth/samsam-logo'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { TextInput } from '@/components/ui/text-input'

export const Route = createFileRoute('/login')({
  component: LoginPage,
})

function LoginPage() {
  return (
    <MobileScreen>
      <AppBar type="plain" title="로그인" />
      <div className="flex flex-col gap-4 px-5 pt-6">
        <SamsamLogo />
        <p className="text-title-1 whitespace-pre-line text-foreground">
          {'가족이 함께\n정하는 여행'}
        </p>
        <form className="flex flex-col gap-4">
          <TextInput label="아이디" placeholder="example" />
          <TextInput
            label="비밀번호"
            type="password"
            placeholder="비밀번호를 입력하세요"
          />
          <Button size="cta">로그인</Button>
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
