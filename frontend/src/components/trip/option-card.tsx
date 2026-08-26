import { Trash2 } from 'lucide-react'

import { cn } from '@/lib/utils'

type OptionCardProps = {
  title: string
  description?: string
  // 서버가 내려주는 설명 출처 — "AI"면 AI가 쓴 것이고, 그 외(스텁/방장이 직접 수정)는
  // 방장이 직접 쓴 것으로 표시한다.
  descriptionSource?: string
  // voteCount가 있으면 투표중/확정 결과 카드 스타일(회색 사진 박스, rounded-btn)로,
  // 없으면 준비중/선택 카드 스타일(점선 업로드 박스, rounded-card)로 그려진다.
  voteCount?: number
  voters?: string[]
  leading?: boolean
  editable?: boolean
  // 있으면 회색 박스 대신 이 주소의 이미지를 채운다 (세션 쿠키로 인증되는 GET 엔드포인트).
  // DB에 저장된 값이 아니라 <img> 렌더링 시점에 필요한 src일 뿐이라 imageUrl이 아닌 imageSrc로 부른다.
  imageSrc?: string
  onClick?: () => void
  onDelete?: () => void
  className?: string
}

function OptionCard({
  title,
  description,
  descriptionSource,
  voteCount,
  voters,
  leading = false,
  editable = false,
  imageSrc,
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
          {imageSrc ? (
            <img
              src={imageSrc}
              alt=""
              className={cn(
                'size-11 shrink-0 object-cover',
                isResultCard ? 'rounded-thumb' : 'rounded-card',
              )}
            />
          ) : isResultCard ? (
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
        {description && descriptionSource && (
          <div className="flex w-full items-center gap-1.5">
            <p className="flex-1 text-[12.5px] text-muted-foreground">
              {description}
            </p>
            <span className="shrink-0 rounded-chip bg-primary-tint px-2 py-[3px] text-[11px] leading-none font-medium text-primary-deep">
              {descriptionSource === 'AI' ? '✨ AI 작성' : '✏️ 직접 작성'}
            </span>
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
