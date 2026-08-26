import { cn } from '@/lib/utils'

type MobileScreenProps = {
  children: React.ReactNode
  className?: string
  bottomBar?: React.ReactNode
  // true면 bottomBar가 문서 흐름을 따라가지 않고 뷰포트 하단에 고정된다.
  // 콘텐츠가 길어져 스크롤이 생겨도 확인 버튼이 항상 보이게 하려는 용도라,
  // 콘텐츠 영역 하단에 겹치지 않을 만큼 여백을 같이 준다.
  floatingBottomBar?: boolean
}

function MobileScreen({
  children,
  className,
  bottomBar,
  floatingBottomBar = false,
}: MobileScreenProps) {
  return (
    <div className="mx-auto flex min-h-svh w-full flex-col bg-background sm:max-w-[402px] sm:border-x sm:border-border">
      <div
        className={cn(
          'flex flex-1 flex-col',
          floatingBottomBar && bottomBar && 'pb-28',
          className,
        )}
      >
        {children}
      </div>
      {bottomBar &&
        (floatingBottomBar ? (
          <div className="fixed inset-x-0 bottom-0 z-40 mx-auto w-full bg-background sm:max-w-[402px] sm:border-x sm:border-border">
            {bottomBar}
          </div>
        ) : (
          bottomBar
        ))}
    </div>
  )
}

export { MobileScreen }
