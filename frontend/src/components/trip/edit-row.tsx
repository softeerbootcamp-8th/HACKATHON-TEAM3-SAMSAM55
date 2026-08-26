import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Menu, X } from 'lucide-react'

import { StatusChip } from '@/components/ui/status-chip'
import { cn } from '@/lib/utils'

type EditRowProps = {
  id: number
  title: string
  category: string
  meta?: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  onDelete?: () => void
}

function EditRow({
  id,
  title,
  category,
  meta,
  status,
  onDelete,
}: EditRowProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id })

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        'flex w-full items-center gap-2.5',
        isDragging && 'z-10 opacity-50',
      )}
    >
      <button
        type="button"
        aria-label="드래그해서 순서 변경"
        className="flex size-6 shrink-0 touch-none items-center justify-center"
        {...attributes}
        {...listeners}
      >
        <Menu className="size-4 text-muted-foreground" />
      </button>
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
