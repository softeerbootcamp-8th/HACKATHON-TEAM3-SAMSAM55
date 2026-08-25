import { cn } from '@/lib/utils'

type StepIndicatorProps = {
  step: number
  totalSteps: number
  className?: string
}

function StepIndicator({ step, totalSteps, className }: StepIndicatorProps) {
  return (
    <div
      data-slot="step-indicator"
      className={cn('flex h-1 w-full items-start gap-1.5', className)}
    >
      {Array.from({ length: totalSteps }, (_, index) => (
        <div
          key={index}
          className={cn(
            'h-full min-w-px flex-1 rounded-2xs',
            index < step ? 'bg-primary-deep' : 'bg-border',
          )}
        />
      ))}
    </div>
  )
}

export { StepIndicator }
