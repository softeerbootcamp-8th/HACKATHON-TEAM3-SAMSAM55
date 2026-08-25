import { cn } from '@/lib/utils'

type MobileScreenProps = {
  children: React.ReactNode
  className?: string
  bottomBar?: React.ReactNode
}

function MobileScreen({ children, className, bottomBar }: MobileScreenProps) {
  return (
    <div className="mx-auto flex min-h-svh w-full flex-col bg-background sm:max-w-[402px] sm:border-x sm:border-border">
      <div className={cn('flex flex-1 flex-col', className)}>{children}</div>
      {bottomBar}
    </div>
  )
}

export { MobileScreen }
