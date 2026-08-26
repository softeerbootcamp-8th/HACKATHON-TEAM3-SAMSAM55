import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

type EmptyStateProps = {
  emoji?: string
  message: string
  actionLabel?: string
  onAction?: () => void
  className?: string
}

function EmptyState({
  emoji,
  message,
  actionLabel,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      data-slot="empty-state"
      className={cn(
        'flex w-full flex-col items-center justify-center gap-4 py-10',
        className,
      )}
    >
      {emoji && <span className="text-[40px] leading-none">{emoji}</span>}
      <p className="text-title-3 text-foreground">{message}</p>
      {actionLabel && (
        <Button size="dialog" onClick={onAction} className="w-[180px]">
          {actionLabel}
        </Button>
      )}
    </div>
  )
}

export { EmptyState }
