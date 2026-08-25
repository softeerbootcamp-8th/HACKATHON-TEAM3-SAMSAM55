import * as React from 'react'

import { cn } from '@/lib/utils'

type TextInputProps = React.ComponentProps<'input'> & {
  label?: string
  error?: string
}

function TextInput({ label, error, className, id, ...props }: TextInputProps) {
  const inputId = React.useId()
  const resolvedId = id ?? inputId

  return (
    <div className="flex w-full flex-col gap-1.5">
      {label && (
        <label
          htmlFor={resolvedId}
          className="text-caption text-muted-foreground"
        >
          {label}
        </label>
      )}
      <input
        id={resolvedId}
        data-slot="text-input"
        aria-invalid={!!error}
        className={cn(
          'h-13 w-full rounded-card border border-border bg-muted px-4 text-body text-foreground placeholder:text-muted-foreground outline-none',
          'focus-visible:border-primary-deep',
          error &&
            'border-2 border-destructive focus-visible:border-destructive',
          className,
        )}
        {...props}
      />
      {error && <p className="text-caption-sm text-destructive">{error}</p>}
    </div>
  )
}

export { TextInput }
