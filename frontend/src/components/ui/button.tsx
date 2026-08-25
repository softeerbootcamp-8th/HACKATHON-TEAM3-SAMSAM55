import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { Slot } from 'radix-ui'

import { cn } from '@/lib/utils'

const buttonVariants = cva(
  "inline-flex shrink-0 items-center justify-center gap-2 rounded-btn text-body-strong whitespace-nowrap transition-colors outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 disabled:pointer-events-none [&_svg]:pointer-events-none [&_svg]:shrink-0 [&_svg:not([class*='size-'])]:size-4",
  {
    variants: {
      variant: {
        primary:
          'bg-primary text-primary-foreground hover:bg-primary-deep disabled:bg-disabled-fill disabled:text-white',
        secondary:
          'bg-secondary text-foreground border-[1.5px] border-border hover:bg-muted disabled:border-border disabled:text-text-disabled',
        text: 'bg-transparent text-primary-deep hover:underline disabled:text-text-disabled',
        danger:
          'bg-destructive text-destructive-foreground hover:bg-destructive/90 disabled:bg-disabled-fill disabled:text-white',
        // 확정 해제하기 같은 "위험하지만 되돌릴 수 있는" 액션 전용 — 빨간 테두리 + 빨간 글자, 채우지 않음.
        dangerOutline:
          'border-[1.5px] border-destructive bg-background text-destructive hover:bg-destructive/5 disabled:border-border disabled:text-text-disabled',
      },
      size: {
        cta: 'h-[52px] w-full px-5',
        dialog: 'h-12 px-5',
        default: 'h-11 px-5',
        icon: 'size-10 rounded-full p-0',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'default',
    },
  },
)

function Button({
  className,
  variant,
  size,
  asChild = false,
  ...props
}: React.ComponentProps<'button'> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean
  }) {
  const Comp = asChild ? Slot.Root : 'button'

  return (
    <Comp
      data-slot="button"
      data-variant={variant}
      data-size={size}
      className={cn(buttonVariants({ variant, size, className }))}
      {...props}
    />
  )
}

export { Button, buttonVariants }
