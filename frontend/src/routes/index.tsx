import { createFileRoute } from '@tanstack/react-router'

import { SamsamLogo } from '@/components/auth/samsam-logo'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/')({
  component: SplashPage,
})

function SplashPage() {
  return (
    <MobileScreen className="items-center bg-primary">
      <div className="flex flex-1 flex-col items-center justify-center gap-3 px-6">
        <SamsamLogo variant="onBrand" size={88} />
        {/* Figma 실제 스플래시 화면(198:2389) 기준 40px — 타이포 스케일의 Display(25px)는
            "스플래시 서비스명" 용도라고 적혀 있지만 실제 화면 값과는 다르다. 이 화면만 예외. */}
        <p className="text-center text-[40px] leading-[1.3] font-bold text-foreground">
          삼삼오오
        </p>
        <p className="text-center text-[15px] leading-[1.55] text-foreground/85">
          가족이 함께 정하는 여행
        </p>
      </div>
      <div className="flex flex-col items-center justify-center gap-3.5 px-6 pb-16">
        <svg
          width="38"
          height="8"
          viewBox="0 0 38 8"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <circle cx="4" cy="4" r="4" fill="white" />
          <circle cx="19" cy="4" r="4" fill="white" fillOpacity="0.55" />
          <circle cx="34" cy="4" r="4" fill="white" fillOpacity="0.3" />
        </svg>
        <p className="text-center text-[13px] text-foreground/80">
          잠시만 기다려주세요
        </p>
      </div>
    </MobileScreen>
  )
}
