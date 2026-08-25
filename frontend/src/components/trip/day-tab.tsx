import { cn } from '@/lib/utils'

type DayTabProps = {
  label: string
  selected: boolean
  pending?: boolean
  onClick: () => void
}

function DayTab({ label, selected, pending, onClick }: DayTabProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'relative rounded-tab px-4 py-2 text-label whitespace-nowrap',
        selected
          ? 'bg-primary text-primary-foreground'
          : 'border border-border bg-background text-muted-foreground',
      )}
    >
      {label}
      {pending && (
        <span className="absolute top-[5px] right-[9px] size-1.5 rounded-full bg-primary-deep" />
      )}
    </button>
  )
}

export { DayTab }
