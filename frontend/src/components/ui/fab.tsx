import { Plus } from 'lucide-react'

import { cn } from '@/lib/utils'

function Fab({ className, ...props }: React.ComponentProps<'button'>) {
  return (
    <button
      type="button"
      data-slot="fab"
      className={cn(
        'flex size-14 items-center justify-center rounded-fab bg-primary text-foreground shadow-[0px_4px_12px_0px_rgba(0,0,0,0.18)]',
        className,
      )}
      {...props}
    >
      <Plus className="size-6" />
    </button>
  )
}

export { Fab }
