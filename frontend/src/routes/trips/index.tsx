import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { ChevronRight } from 'lucide-react'

import { useLogout } from '@/api/generated/auth-controller/auth-controller'
import type { TripSummaryResponseDto } from '@/api/generated/model'
import { useFindTrips } from '@/api/generated/trip-controller/trip-controller'
import { Button } from '@/components/ui/button'
import { EmptyState } from '@/components/ui/empty-state'
import { Fab } from '@/components/ui/fab'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { getApiError } from '@/features/auth/auth'
import {
  formatDDay,
  formatTripPeriod,
  isActiveTrip,
} from '@/features/trip/trip-format'

export const Route = createFileRoute('/trips/')({
  component: TripListPage,
})

type TripSummary = TripSummaryResponseDto & {
  id: number
  title: string
  startDate: string
  endDate: string
}

function TripListPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const logout = useLogout()
  const tripsQuery = useFindTrips()
  const [logoutError, setLogoutError] = useState<string>()
  const trips = (
    tripsQuery.data?.success ? (tripsQuery.data.data?.items ?? []) : []
  ).filter(
    (trip): trip is TripSummary =>
      trip.id !== undefined &&
      trip.title !== undefined &&
      trip.startDate !== undefined &&
      trip.endDate !== undefined,
  )
  const today = new Date()
  const activeTrips = trips.filter((trip) => isActiveTrip(trip.endDate, today))
  const pastTrips = trips.filter((trip) => !isActiveTrip(trip.endDate, today))
  const [heroTrip, ...otherActiveTrips] = activeTrips
  const tripSections = [
    { label: '다른 여행', items: otherActiveTrips },
    { label: '지난 여행', items: pastTrips },
  ].filter((section) => section.items.length > 0)
  const hasTrips = trips.length > 0

  const handleLogout = async () => {
    setLogoutError(undefined)

    try {
      const response = await logout.mutateAsync()

      if (!response.success) {
        setLogoutError(
          response.error?.message ?? '로그아웃 중 오류가 발생했습니다.',
        )
        return
      }

      queryClient.clear()
      await navigate({ to: '/login', replace: true })
    } catch (error) {
      setLogoutError(
        getApiError(error)?.message ?? '로그아웃 중 오류가 발생했습니다.',
      )
    }
  }

  return (
    <MobileScreen
      bottomBar={
        hasTrips ? (
          <div className="pointer-events-none fixed inset-x-0 bottom-0 mx-auto flex w-full justify-end pr-6 pb-7 sm:max-w-[402px]">
            <Link
              to="/trips/new/name"
              search={{ title: undefined }}
              className="pointer-events-auto"
            >
              <Fab />
            </Link>
          </div>
        ) : undefined
      }
    >
      <div className="flex flex-1 flex-col gap-6 px-6 py-4">
        {tripsQuery.isLoading ? (
          <div className="flex flex-1 items-center justify-center text-body text-muted-foreground">
            여행을 불러오는 중...
          </div>
        ) : tripsQuery.isError || !tripsQuery.data?.success ? (
          <div className="flex flex-1 items-center justify-center text-center text-body text-destructive">
            {tripsQuery.data?.error?.message ??
              getApiError(tripsQuery.error)?.message ??
              '여행 목록을 불러오지 못했습니다.'}
          </div>
        ) : hasTrips ? (
          <>
            <div className="flex items-center gap-2">
              <div className="flex flex-1 flex-col gap-1">
                <p className="text-[22px] leading-[1.45] font-bold text-foreground">
                  내 여행
                </p>
                <p className="text-[13px] leading-[1.5] text-muted-foreground">
                  준비 중인 여행 {activeTrips.length}개
                </p>
              </div>
              <Button
                type="button"
                variant="text"
                className="px-2"
                disabled={logout.isPending}
                onClick={handleLogout}
              >
                {logout.isPending ? '로그아웃 중...' : '로그아웃'}
              </Button>
            </div>
            {logoutError && (
              <p className="text-caption-sm text-destructive" role="alert">
                {logoutError}
              </p>
            )}

            {heroTrip && (
              <div className="flex flex-col gap-3">
                <p className="text-[14px] leading-[1.5] font-medium text-muted-foreground">
                  다가오는 여행
                </p>
                <div className="flex flex-col gap-4 rounded-btn border-2 border-primary-deep p-5">
                  <div className="flex w-fit items-start rounded-card bg-primary px-3 py-[5px]">
                    <span className="text-[13px] leading-[1.5] font-medium text-foreground">
                      {formatDDay(heroTrip.startDate)}
                    </span>
                  </div>
                  <div className="flex flex-col gap-1">
                    <p className="text-[20px] leading-[1.45] font-bold text-foreground">
                      {heroTrip.title}
                    </p>
                    <p className="text-[13px] leading-[1.5] text-muted-foreground">
                      {formatTripPeriod(
                        heroTrip.startDate,
                        heroTrip.endDate,
                        heroTrip.companionCount ?? 0,
                      )}
                    </p>
                  </div>
                  <div className="flex flex-col gap-2">
                    <div className="flex items-center justify-between">
                      <span className="text-[14px] leading-[1.5] text-muted-foreground">
                        일정 {heroTrip.totalItems ?? 0}개 중{' '}
                        {heroTrip.confirmedItems ?? 0}개 확정
                      </span>
                      <span className="text-[16px] leading-[1.5] font-medium text-primary-deep">
                        {heroTrip.progressPercent ?? 0}%
                      </span>
                    </div>
                    <div
                      className="h-2.5 w-full overflow-hidden rounded-full bg-border"
                      role="progressbar"
                      aria-label="일정 확정 진척률"
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={heroTrip.progressPercent ?? 0}
                    >
                      <div
                        className="h-full rounded-full bg-primary transition-[width]"
                        style={{
                          width: `${heroTrip.progressPercent ?? 0}%`,
                        }}
                      />
                    </div>
                  </div>
                  <Link
                    to="/trips/$tripId"
                    params={{ tripId: String(heroTrip.id) }}
                  >
                    <Button size="cta">일정 보러가기</Button>
                  </Link>
                </div>
              </div>
            )}

            {tripSections.map((section) => (
              <div key={section.label} className="flex flex-col">
                <p className="pb-3 text-[14px] leading-[1.5] font-medium text-muted-foreground">
                  {section.label}
                </p>
                {section.items.map((trip) => (
                  <Link
                    key={trip.id}
                    to="/trips/$tripId"
                    params={{ tripId: String(trip.id) }}
                    className="flex items-center gap-3 py-3.5"
                  >
                    <div className="flex flex-1 flex-col gap-0.5">
                      <p className="text-body-strong text-foreground">
                        {trip.title}
                      </p>
                      <p className="text-caption-sm text-muted-foreground">
                        {formatTripPeriod(
                          trip.startDate,
                          trip.endDate,
                          trip.companionCount ?? 0,
                        )}
                      </p>
                    </div>
                    <span className="text-caption text-primary-deep">
                      {formatDDay(trip.startDate)}
                    </span>
                    <ChevronRight className="size-[18px] text-[#afb4b4]" />
                  </Link>
                ))}
              </div>
            ))}
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
                onAction={() =>
                  navigate({
                    to: '/trips/new/name',
                    search: { title: undefined },
                  })
                }
              />
            </div>
          </>
        )}
      </div>
    </MobileScreen>
  )
}
