import { cn } from '@/lib/utils'

type RoleChipProps = React.ComponentProps<'button'> & {
  emoji: string
  label: string
  state?: 'default' | 'selected' | 'taken'
}

function RoleChip({
  emoji,
  label,
  state = 'default',
  className,
  disabled,
  ...props
}: RoleChipProps) {
  return (
    <button
      type="button"
      data-slot="role-chip"
      data-state={state}
      disabled={disabled ?? state === 'taken'}
      className={cn(
        'flex h-[100px] w-full flex-col items-center justify-center gap-[15px] rounded-thumb border text-center transition-colors',
        state === 'default' && 'border-border bg-background text-foreground',
        state === 'selected' &&
          'border-2 border-primary-deep bg-primary-tint text-primary-deep',
        state === 'taken' &&
          'border-border bg-muted text-muted-foreground cursor-not-allowed',
        className,
      )}
      {...props}
    >
      <span className="text-[26px] leading-none">{emoji}</span>
      <span className="text-card-title">{label}</span>
    </button>
  )
}

export { RoleChip }
