import { cn } from '@/lib/utils'

type AddItemRowProps = {
  onClick?: () => void
  className?: string
}

function AddItemRow({ onClick, className }: AddItemRowProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'flex h-14 w-full items-center justify-center rounded-card border-[1.5px] border-dashed border-primary-deep text-card-title text-primary-deep',
        className,
      )}
    >
      + 일정 추가
    </button>
  )
}

export { AddItemRow }
