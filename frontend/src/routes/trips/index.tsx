import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { ChevronRight } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { Fab } from '@/components/ui/fab'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/trips/')({
  component: TripListPage,
})

// 화면 요소 확인용 mock 토글 — API 연동 시 실제 목록 유무로 대체
const hasTrips = true

const heroTrip = {
  id: 'trip-1',
  title: '도쿄 가족 여행',
  dDay: 'D-18',
  period: '8월 24일 - 8월 27일 · 4명',
  progressLabel: '일정 8개 중 5개 확정',
  progressPercent: 62,
}

const otherTrips = [
  {
    id: 'trip-2',
    title: '부산 2박 3일',
    period: '9월 12일 - 9월 14일 · 3명',
    dDay: 'D-37',
  },
  {
    id: 'trip-3',
    title: '부산 2박 3일',
    period: '9월 23일 - 9월 27일 · 4명',
    dDay: 'D-53',
  },
]

function TripListPage() {
  const navigate = useNavigate()

  return (
    <MobileScreen
      bottomBar={
        hasTrips ? (
          <div className="pointer-events-none fixed inset-x-0 bottom-0 mx-auto flex w-full justify-end pr-6 pb-7 sm:max-w-[402px]">
            <Link to="/trips/new/name" className="pointer-events-auto">
              <Fab />
            </Link>
          </div>
        ) : undefined
      }
    >
      <div className="flex flex-1 flex-col gap-6 px-6 py-4">
        {hasTrips ? (
          <>
            <div className="flex items-center gap-3">
              <div className="flex flex-1 flex-col gap-1">
                <p className="text-[22px] leading-[1.45] font-bold text-foreground">
                  정하은님의 여행
                </p>
                <p className="text-[13px] leading-[1.5] text-muted-foreground">
                  준비 중인 여행 {1 + otherTrips.length}개
                </p>
              </div>
              <div className="flex size-11 items-center justify-center rounded-full border border-primary-deep bg-primary-tint">
                <span className="text-[16px] leading-[1.5] font-medium text-primary-deep">
                  정
                </span>
              </div>
            </div>

            <div className="flex flex-col gap-3">
              <p className="text-[14px] leading-[1.5] font-medium text-muted-foreground">
                다가오는 여행
              </p>
              <div className="flex flex-col gap-4 rounded-btn border-2 border-primary-deep p-5">
                <div className="flex w-fit items-start rounded-card bg-primary px-3 py-[5px]">
                  <span className="text-[13px] leading-[1.5] font-medium text-foreground">
                    {heroTrip.dDay}
                  </span>
                </div>
                <div className="flex flex-col gap-1">
                  <p className="text-[20px] leading-[1.45] font-bold text-foreground">
                    {heroTrip.title}
                  </p>
                  <p className="text-[13px] leading-[1.5] text-muted-foreground">
                    {heroTrip.period}
                  </p>
                </div>
                <div className="flex flex-col gap-2">
                  <div className="flex items-center gap-2">
                    <p className="flex-1 text-[13px] leading-[1.5] text-muted-foreground">
                      {heroTrip.progressLabel}
                    </p>
                    <p className="text-[13px] leading-[1.5] font-medium text-primary-deep">
                      {heroTrip.progressPercent}%
                    </p>
                  </div>
                  <div className="h-2.5 w-full overflow-hidden rounded-[3px] bg-[#e6e9e9]">
                    <div
                      className="h-full rounded-[3px] bg-primary"
                      style={{ width: `${heroTrip.progressPercent}%` }}
                    />
                  </div>
                </div>
                <Link to="/trips/$tripId" params={{ tripId: heroTrip.id }}>
                  <Button size="cta">일정 보러가기</Button>
                </Link>
              </div>
            </div>

            <div className="flex flex-col">
              <p className="pb-3 text-[14px] leading-[1.5] font-medium text-muted-foreground">
                다른 여행
              </p>
              {otherTrips.map((trip) => (
                <Link
                  key={trip.id}
                  to="/trips/$tripId"
                  params={{ tripId: trip.id }}
                  className="flex items-center gap-3 py-3.5"
                >
                  <div className="flex flex-1 flex-col gap-0.5">
                    <p className="text-body-strong text-foreground">
                      {trip.title}
                    </p>
                    <p className="text-caption-sm text-muted-foreground">
                      {trip.period}
                    </p>
                  </div>
                  <span className="text-caption text-primary-deep">
                    {trip.dDay}
                  </span>
                  <ChevronRight className="size-[18px] text-[#afb4b4]" />
                </Link>
              ))}
            </div>
          </>
        ) : (
          <>
            <div className="flex flex-col gap-1.5">
              <p className="text-title-1 text-foreground">내 여행</p>
              <p className="text-[14px] text-muted-foreground">
                만든 여행을 선택해 이어서 준비해요
              </p>
            </div>
            <div className="flex flex-1 items-center justify-center">
              <EmptyState
                message="아직 만든 여행이 없어요"
                actionLabel="여행 만들기"
                onAction={() => navigate({ to: '/trips/new/name' })}
              />
            </div>
          </>
        )}
      </div>
    </MobileScreen>
  )
}
