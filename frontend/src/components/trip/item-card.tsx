import { StatusChip } from '@/components/ui/status-chip'
import { cn } from '@/lib/utils'

type ItemCardProps = {
  title: string
  category: string
  // 실제 화면(249:6492)은 투표중일 때만 득표 현황("2/3표 완료")을 보여주고,
  // 그 외 상태는 카테고리만 보여준다 — decisionMethod 캡션은 상세 화면 등
  // 다른 맥락에서만 쓰고, meta가 있으면 그걸 우선한다.
  decisionMethod?: '부모님과 투표' | '내가 결정'
  meta?: string
  status: 'draft' | 'voting' | 'confirmed' | 'voteDone'
  onClick?: () => void
  className?: string
}

function ItemCard({
  title,
  category,
  decisionMethod,
  meta,
  status,
  onClick,
  className,
}: ItemCardProps) {
  const secondSegment = meta ?? decisionMethod

  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        'flex w-full items-center justify-between rounded-btn border border-border bg-background p-4 text-left',
        className,
      )}
    >
      <div className="flex flex-col gap-1.5 overflow-hidden whitespace-nowrap">
        <p className="text-body-strong text-foreground">{title}</p>
        <div className="flex gap-1.5 text-caption-sm">
          <span className="text-muted-foreground">{category}</span>
          {secondSegment && (
            <>
              <span className="text-muted-foreground">·</span>
              <span className="text-primary-deep">{secondSegment}</span>
            </>
          )}
        </div>
      </div>
      <StatusChip status={status} className="shrink-0" />
    </button>
  )
}

export { ItemCard }
