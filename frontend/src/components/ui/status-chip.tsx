import { cva, type VariantProps } from 'class-variance-authority'

import { cn } from '@/lib/utils'

// 디자인 시스템 스펙: h26 r20, Medium 13 (칩 텍스트가 아니라 실제 컴포넌트 값 기준).
// line-height를 폰트에 맡기면 Noto Sans KR 특성상 26px보다 커져서, 높이를 직접 고정한다.
const statusChipVariants = cva(
  'inline-flex h-[26px] w-fit items-center rounded-chip px-3 text-[13px] leading-none font-medium whitespace-nowrap',
  {
    variants: {
      status: {
        draft: 'bg-status-draft-tint text-status-draft',
        voting: 'bg-status-voting-tint text-status-voting',
        confirmed: 'bg-status-confirmed-tint text-status-confirmed',
        voteDone: 'bg-status-attention-tint text-status-attention',
      },
    },
  },
)

const STATUS_LABEL = {
  draft: '준비 중',
  voting: '투표 중',
  confirmed: '확정',
  voteDone: '투표 완료',
} as const

type StatusChipProps = React.ComponentProps<'span'> &
  VariantProps<typeof statusChipVariants> & {
    status: keyof typeof STATUS_LABEL
  }

function StatusChip({ status, className, ...props }: StatusChipProps) {
  return (
    <span
      data-slot="status-chip"
      className={cn(statusChipVariants({ status }), className)}
      {...props}
    >
      {STATUS_LABEL[status]}
    </span>
  )
}

export { StatusChip }
