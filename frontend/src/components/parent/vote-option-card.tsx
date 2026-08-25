import { Camera, Check } from 'lucide-react'

import { cn } from '@/lib/utils'

type VoteOptionCardProps = {
  title: string
  description: string
  selected?: boolean
  voteCount?: number
  onClick?: () => void
  className?: string
}

function VoteOptionCard({
  title,
  description,
  selected = false,
  voteCount,
  onClick,
  className,
}: VoteOptionCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'relative flex w-full flex-col items-start overflow-hidden rounded-[18px] border bg-background text-left',
        selected ? 'border-[3px] border-primary-deep' : 'border-border',
        className,
      )}
    >
      <div className="flex h-[180px] w-full items-center justify-center bg-muted">
        <Camera className="size-8 text-text-disabled" />
      </div>
      {selected && (
        <span className="absolute top-2.5 right-2.5 flex size-8 items-center justify-center rounded-full bg-primary">
          <Check className="size-4 text-foreground" />
        </span>
      )}
      <div className="flex w-full flex-col gap-2.5 p-4">
        <div className="flex w-full items-center justify-between gap-2">
          <p className="text-[18px] font-bold text-foreground">{title}</p>
          {voteCount !== undefined && (
            <span
              className={cn(
                'text-body-strong shrink-0',
                voteCount > 0 ? 'text-primary-deep' : 'text-muted-foreground',
              )}
            >
              {voteCount}표
            </span>
          )}
        </div>
        <p className="text-body text-muted-foreground">{description}</p>
      </div>
    </button>
  )
}

export { VoteOptionCard }
