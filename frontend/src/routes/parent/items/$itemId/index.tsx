import { Users } from 'lucide-react'
import { createFileRoute, useNavigate, useParams } from '@tanstack/react-router'

import { OptionCard } from '@/components/trip/option-card'
import { VoteStatusRow } from '@/components/trip/vote-status-row'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/parent/items/$itemId/')({
  component: ParentItemDetailPage,
})

type MockOption = {
  id: string
  title: string
  voteCount: number
  voters: string[]
}

// TODO(일정 항목 상세 조회 담당자): 이 화면 전체(상태·선택지·득표수)가
// 일정 항목 상세 조회 API에 의존한다. GET 엔드포인트가 생기면 mock을 걷어내고
// 실제 데이터로 교체한다. "투표하러 가기" 버튼 자체는 단순 이동이라 API 연동이
// 필요 없다 (실제 제출은 /vote 화면의 PUT /api/itinerary-items/my-votes가 담당).
const MOCK_ITEM_STATUS: Record<string, 'voting' | 'confirmed'> = {
  '102': 'confirmed',
}

const MOCK_OPTIONS: MockOption[] = [
  {
    id: 'opt-1',
    title: '스시 오마카세 긴자점',
    voteCount: 2,
    voters: ['엄', '첫'],
  },
  { id: 'opt-2', title: '라멘 이치란 신주쿠점', voteCount: 0, voters: [] },
]

const VOTERS = [
  { initial: '엄', voted: true },
  { initial: '아', voted: false },
  { initial: '첫', voted: true },
]

function ParentItemDetailPage() {
  const navigate = useNavigate()
  const { itemId } = useParams({ from: '/parent/items/$itemId/' })
  const status = MOCK_ITEM_STATUS[itemId] ?? 'voting'
  const winner = [...MOCK_OPTIONS].sort((a, b) => b.voteCount - a.voteCount)[0]

  return (
    <MobileScreen>
      <AppBar
        type="back"
        title="일정 상세"
        onBack={() => navigate({ to: '/parent' })}
      />

      <div className="flex flex-col gap-2 px-5 pt-4">
        <p className="text-[24px] font-bold text-foreground">점심 식사</p>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Users className="size-4 text-primary-deep" />
            <span className="text-card-title text-primary-deep">투표</span>
          </div>
          {status === 'voting' ? (
            <span className="rounded-chip bg-primary-tint px-2.5 py-1 text-[12px] leading-none font-medium text-primary-deep">
              투표 중
            </span>
          ) : (
            <span className="rounded-chip bg-[#e6f6e9] px-2.5 py-1 text-[12px] leading-none font-medium text-[#37b24d]">
              확정
            </span>
          )}
        </div>
        <p className="text-caption text-muted-foreground">1일차 · 식사</p>
      </div>

      {status === 'voting' ? (
        <div className="flex flex-col gap-5 px-5 pt-4">
          <VoteStatusRow
            votedCount={VOTERS.filter((v) => v.voted).length}
            totalCount={VOTERS.length}
            voters={VOTERS}
          />

          <p className="text-subtitle text-foreground">
            선택지 {MOCK_OPTIONS.length}개
          </p>
          <div className="flex flex-col gap-3">
            {MOCK_OPTIONS.map((option) => (
              <OptionCard
                key={option.id}
                title={option.title}
                voteCount={option.voteCount}
                voters={option.voters}
                leading={option.voteCount > 0}
              />
            ))}
          </div>

          <Button
            size="cta"
            onClick={() =>
              navigate({ to: '/parent/items/$itemId/vote', params: { itemId } })
            }
          >
            투표하러 가기
          </Button>
        </div>
      ) : (
        <div className="flex flex-col gap-4 px-5 pt-4">
          <div className="flex flex-col overflow-hidden rounded-[18px] border border-border">
            <div className="h-[233px] w-full bg-muted" />
            <div className="flex flex-col gap-2.5 p-4">
              <p className="text-title-2 text-foreground">{winner.title}</p>
              <p className="text-caption text-muted-foreground">
                신선한 제철 재료로 만든 프리미엄 스시 코스
              </p>
              <div className="flex items-center gap-2">
                <div className="flex items-center -space-x-1.5">
                  {winner.voters.map((initial, index) => (
                    <span
                      key={index}
                      className="flex size-6 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-medium text-foreground"
                    >
                      {initial}
                    </span>
                  ))}
                </div>
                <p className="text-[14px] font-medium text-primary-deep">
                  엄마, 첫째가 골랐어요
                </p>
              </div>
            </div>
          </div>

          <p className="text-subtitle text-foreground">최종 투표 결과</p>
          <div className="flex flex-col gap-2">
            {MOCK_OPTIONS.map((option) => (
              <div
                key={option.id}
                className={
                  option.id === winner.id
                    ? 'flex h-[50px] items-center justify-between rounded-thumb border-2 border-primary-deep bg-primary-tint px-3.5'
                    : 'flex h-[50px] items-center justify-between rounded-thumb bg-muted px-3.5'
                }
              >
                <p
                  className={
                    option.id === winner.id
                      ? 'text-[14px] font-medium text-foreground'
                      : 'text-[14px] text-muted-foreground'
                  }
                >
                  {option.title}
                </p>
                <p
                  className={
                    option.id === winner.id
                      ? 'text-[14px] font-bold text-primary-deep'
                      : 'text-[14px] font-bold text-muted-foreground'
                  }
                >
                  {option.voteCount}표
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
    </MobileScreen>
  )
}
