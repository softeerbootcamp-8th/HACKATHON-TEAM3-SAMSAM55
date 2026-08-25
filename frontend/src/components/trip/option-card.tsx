import { Trash2 } from 'lucide-react'

import { cn } from '@/lib/utils'

type OptionCardProps = {
  title: string
  description?: string
  aiGenerated?: boolean
  // voteCount가 있으면 투표중/확정 결과 카드 스타일(회색 사진 박스, rounded-btn)로,
  // 없으면 준비중/선택 카드 스타일(점선 업로드 박스, rounded-card)로 그려진다.
  voteCount?: number
  voters?: string[]
  leading?: boolean
  editable?: boolean
  onClick?: () => void
  onDelete?: () => void
  className?: string
}

function OptionCard({
  title,
  description,
  aiGenerated,
  voteCount,
  voters,
  leading = false,
  editable = false,
  onClick,
  onDelete,
  className,
}: OptionCardProps) {
  const isResultCard = typeof voteCount === 'number'

  return (
    <div className={cn('flex items-center gap-2.5', className)}>
      <button
        type="button"
        onClick={onClick}
        disabled={!onClick}
        className={cn(
          'flex flex-1 flex-col items-start gap-2.5 text-left',
          isResultCard ? 'rounded-btn p-3.5' : 'rounded-card p-3',
          leading
            ? 'border-2 border-primary-deep bg-primary-tint'
            : 'border border-border bg-background',
        )}
      >
        <div className="flex w-full items-center gap-3">
          {isResultCard ? (
            <div className="size-11 shrink-0 rounded-thumb bg-muted" />
          ) : (
            <div className="flex size-11 shrink-0 items-center justify-center rounded-card border-[1.5px] border-dashed border-border bg-muted" />
          )}
          <p className="flex-1 text-card-title text-foreground">{title}</p>
          {isResultCard && (
            <p
              className={cn(
                'text-[16px] font-bold',
                leading ? 'text-primary-deep' : 'text-muted-foreground',
              )}
            >
              {voteCount}표
            </p>
          )}
        </div>
        {description && (
          <div className="flex w-full items-center gap-1.5">
            <p className="flex-1 text-[12.5px] text-muted-foreground">
              {description}
            </p>
            {aiGenerated && (
              <span className="shrink-0 rounded-chip bg-primary-tint px-2 py-[3px] text-[11px] leading-none font-medium text-primary-deep">
                ✨ AI 작성
              </span>
            )}
          </div>
        )}
        {voters && voters.length > 0 && (
          <div className="flex items-center -space-x-1.5">
            {voters.map((initial, index) => (
              <span
                key={index}
                className="flex size-[22px] items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-medium text-primary-foreground"
              >
                {initial}
              </span>
            ))}
          </div>
        )}
      </button>
      {editable && (
        <button
          type="button"
          onClick={onDelete}
          aria-label="선택지 삭제"
          className="flex size-7 shrink-0 items-center justify-center rounded-card bg-muted"
        >
          <Trash2 className="size-3.5 text-muted-foreground" />
        </button>
      )}
    </div>
  )
}

export { OptionCard }
