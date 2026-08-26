import { BottomSheet } from '@/components/ui/bottom-sheet'

type ItemMoreSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  itemName: string
  itemMeta: string
  onEditItem: () => void
  onDeleteItem: () => void
}

function ItemMoreSheet({
  open,
  onOpenChange,
  itemName,
  itemMeta,
  onEditItem,
  onDeleteItem,
}: ItemMoreSheetProps) {
  return (
    <BottomSheet open={open} onOpenChange={onOpenChange}>
      <div className="flex flex-col gap-2">
        <p className="text-[17px] font-bold text-foreground">{itemName}</p>
        <p className="text-caption text-muted-foreground">{itemMeta}</p>
      </div>
      <div className="flex flex-col">
        <button
          type="button"
          onClick={onEditItem}
          className="flex h-14 w-full items-center text-body-strong text-foreground"
        >
          일정 수정
        </button>
        <div className="h-px w-full bg-border" />
        <button
          type="button"
          onClick={onDeleteItem}
          className="flex h-14 w-full items-center text-body-strong text-destructive"
        >
          일정 삭제
        </button>
      </div>
    </BottomSheet>
  )
}

export { ItemMoreSheet }
