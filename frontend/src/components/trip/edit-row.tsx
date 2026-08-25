import { Menu, X } from 'lucide-react'

import { StatusChip } from '@/components/ui/status-chip'

type EditRowProps = {
  title: string
  category: string
  meta?: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  onDelete?: () => void
}

function EditRow({ title, category, meta, status, onDelete }: EditRowProps) {
  return (
    <div className="flex w-full items-center gap-2.5">
      <div className="flex size-6 shrink-0 items-center justify-center">
        <Menu className="size-4 text-muted-foreground" />
      </div>
      <div className="flex flex-1 items-center justify-between rounded-btn border border-border bg-background p-4">
        <div className="flex flex-col gap-1.5">
          <p className="text-body-strong text-foreground">{title}</p>
          <div className="flex gap-1.5 text-caption-sm">
            <span className="text-muted-foreground">{category}</span>
            {meta && (
              <>
                <span className="text-muted-foreground">·</span>
                <span className="text-primary-deep">{meta}</span>
              </>
            )}
          </div>
        </div>
        <StatusChip status={status} className="shrink-0" />
      </div>
      <button
        type="button"
        onClick={onDelete}
        aria-label="일정 삭제"
        className="flex size-7 shrink-0 items-center justify-center rounded-card bg-[#ffefef]"
      >
        <X className="size-3 text-destructive" />
      </button>
    </div>
  )
}

export { EditRow }
