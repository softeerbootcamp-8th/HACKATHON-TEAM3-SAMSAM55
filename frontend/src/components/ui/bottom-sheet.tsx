import { Dialog } from 'radix-ui'

import { cn } from '@/lib/utils'

type BottomSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  title?: string
  children: React.ReactNode
  className?: string
  fullScreen?: boolean
}

function BottomSheet({
  open,
  onOpenChange,
  title,
  children,
  className,
  fullScreen = false,
}: BottomSheetProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40" />
        <Dialog.Content
          className={cn(
            'fixed inset-x-0 bottom-0 z-50 mx-auto flex w-full flex-col gap-5 rounded-t-[20px] bg-background px-5 pt-3 pb-6 sm:max-w-[402px]',
            fullScreen && 'top-0 h-svh rounded-t-none pt-0',
            className,
          )}
        >
          {!fullScreen && (
            <div className="mx-auto h-0.5 w-9 shrink-0 rounded-2xs bg-border" />
          )}
          {title && (
            <Dialog.Title className="text-title-2 text-foreground">
              {title}
            </Dialog.Title>
          )}
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

export { BottomSheet }
