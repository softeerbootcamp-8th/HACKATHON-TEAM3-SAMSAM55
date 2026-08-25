import { BottomSheet } from '@/components/ui/bottom-sheet'

type TripMoreSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  tripTitle: string
  tripPeriod: string
  onEditTrip?: () => void
  onDeleteTrip: () => void
}

function TripMoreSheet({
  open,
  onOpenChange,
  tripTitle,
  tripPeriod,
  onEditTrip,
  onDeleteTrip,
}: TripMoreSheetProps) {
  return (
    <BottomSheet open={open} onOpenChange={onOpenChange}>
      <div className="flex flex-col gap-2">
        <p className="text-[17px] font-bold text-foreground">{tripTitle}</p>
        <p className="text-caption text-muted-foreground">{tripPeriod}</p>
      </div>
      <div className="flex flex-col">
        <button
          type="button"
          onClick={onEditTrip}
          className="flex h-14 w-full items-center text-body-strong text-foreground"
        >
          여행 정보 수정
        </button>
        <div className="h-px w-full bg-border" />
        <button
          type="button"
          onClick={onDeleteTrip}
          className="flex h-14 w-full items-center text-body-strong text-destructive"
        >
          여행 삭제
        </button>
      </div>
    </BottomSheet>
  )
}

export { TripMoreSheet }
