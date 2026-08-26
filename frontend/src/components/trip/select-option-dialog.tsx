import { Dialog } from 'radix-ui'

import type { VoteOptionSummaryDto } from '@/api/generated/model'
import { OptionCard } from '@/components/trip/option-card'
import { Button } from '@/components/ui/button'

type SelectOptionDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  options: VoteOptionSummaryDto[]
  selectedOptionId: number | null
  onSelect: (optionId: number) => void
  onConfirm: () => void
  title?: string
  description?: string
  confirmLabel?: string
}

function SelectOptionDialog({
  open,
  onOpenChange,
  options,
  selectedOptionId,
  onSelect,
  onConfirm,
  title = '선택지가 여러 개에요.',
  description = '어떤 곳으로 선택할까요?',
  confirmLabel = '선택하기',
}: SelectOptionDialogProps) {
  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/40" />
        <Dialog.Content className="fixed top-1/2 left-1/2 z-50 flex max-h-[80svh] w-[320px] -translate-x-1/2 -translate-y-1/2 flex-col gap-5 rounded-[20px] bg-background px-5 pt-7 pb-6">
          <div className="flex flex-col gap-[13px] text-center">
            <Dialog.Title className="text-title-2 text-foreground">
              {title}
            </Dialog.Title>
            <Dialog.Description className="text-body text-muted-foreground">
              {description}
            </Dialog.Description>
          </div>
          <div className="flex flex-col gap-2.5 overflow-y-auto">
            {options.map((option) => (
              <OptionCard
                key={option.id}
                title={option.name ?? ''}
                imageSrc={option.imageUrl}
                leading={option.id === selectedOptionId}
                onClick={() => option.id !== undefined && onSelect(option.id)}
              />
            ))}
          </div>
          <div className="flex gap-2.5">
            <Dialog.Close asChild>
              <Button variant="secondary" size="dialog" className="flex-1">
                취소
              </Button>
            </Dialog.Close>
            <Button
              size="dialog"
              className="flex-1"
              disabled={selectedOptionId === null}
              onClick={onConfirm}
            >
              {confirmLabel}
            </Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}

export { SelectOptionDialog }
