import { AlertDialog } from 'radix-ui'

import { Button } from '@/components/ui/button'

type ConfirmDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: string
  cancelLabel?: string
  confirmLabel: string
  danger?: boolean
  onConfirm: () => void
}

function ConfirmDialog({
  open,
  onOpenChange,
  title,
  description,
  cancelLabel = '취소',
  confirmLabel,
  danger = false,
  onConfirm,
}: ConfirmDialogProps) {
  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-50 bg-black/40" />
        <AlertDialog.Content className="fixed top-1/2 left-1/2 z-50 flex w-[320px] -translate-x-1/2 -translate-y-1/2 flex-col gap-5 rounded-[20px] bg-background px-6 pt-7 pb-6">
          <div className="flex flex-col gap-[13px] text-center">
            <AlertDialog.Title className="text-title-2 text-foreground">
              {title}
            </AlertDialog.Title>
            {description && (
              <AlertDialog.Description className="text-body text-muted-foreground">
                {description}
              </AlertDialog.Description>
            )}
          </div>
          <div className="flex gap-2.5">
            <AlertDialog.Cancel asChild>
              <Button variant="secondary" size="dialog" className="flex-1">
                {cancelLabel}
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action asChild>
              <Button
                variant={danger ? 'danger' : 'primary'}
                size="dialog"
                className="flex-1"
                onClick={onConfirm}
              >
                {confirmLabel}
              </Button>
            </AlertDialog.Action>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  )
}

export { ConfirmDialog }
