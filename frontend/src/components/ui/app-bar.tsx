import { ArrowLeft, MoreHorizontal, X } from 'lucide-react'

import { cn } from '@/lib/utils'

type AppBarProps = {
  type?: 'back' | 'close' | 'backWithMore' | 'plain'
  title?: string
  onBack?: () => void
  onClose?: () => void
  onMore?: () => void
  className?: string
}

function AppBar({
  type = 'back',
  title,
  onBack,
  onClose,
  onMore,
  className,
}: AppBarProps) {
  return (
    <header
      data-slot="app-bar"
      className={cn(
        'relative flex h-11 w-full items-center justify-between bg-background pr-5 pl-3',
        className,
      )}
    >
      {type === 'close' ? (
        <button
          type="button"
          onClick={onClose}
          aria-label="닫기"
          className="flex size-10 items-center justify-center"
        >
          <X className="size-[18px] text-foreground" />
        </button>
      ) : type === 'plain' ? (
        <span className="size-10" />
      ) : (
        <button
          type="button"
          onClick={onBack}
          aria-label="뒤로가기"
          className="flex size-10 items-center justify-center"
        >
          <ArrowLeft className="size-6 text-foreground" />
        </button>
      )}

      {title && (
        <p className="absolute left-1/2 -translate-x-1/2 text-[17px] font-medium whitespace-nowrap text-foreground">
          {title}
        </p>
      )}

      {type === 'backWithMore' ? (
        <button
          type="button"
          onClick={onMore}
          aria-label="더보기"
          className="flex size-10 items-center justify-center"
        >
          <MoreHorizontal className="size-5 text-foreground" />
        </button>
      ) : (
        <span className="size-10" />
      )}
    </header>
  )
}

export { AppBar }
