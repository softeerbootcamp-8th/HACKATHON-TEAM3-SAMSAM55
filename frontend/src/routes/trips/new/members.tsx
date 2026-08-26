import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useState } from 'react'
import { Plus } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'

import {
  getFindTripsQueryKey,
  useCreateTrip,
} from '@/api/generated/trip-controller/trip-controller'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { MobileScreen } from '@/components/layout/mobile-screen'
import { StepIndicator } from '@/components/ui/step-indicator'
import { TextInput } from '@/components/ui/text-input'
import { getApiError } from '@/features/auth/auth'
import { getMemberEmoji } from '@/lib/member-emoji'
import { cn } from '@/lib/utils'

export const Route = createFileRoute('/trips/new/members')({
  validateSearch: (search: Record<string, unknown>) => ({
    title: typeof search.title === 'string' ? search.title : '',
    startDate: typeof search.startDate === 'string' ? search.startDate : '',
    endDate: typeof search.endDate === 'string' ? search.endDate : '',
  }),
  component: NewTripMembersPage,
})

const FAMILY_GROUPS = [
  { label: '부모', members: ['엄마', '아빠'] },
  {
    label: '조부모',
    members: ['친할머니', '친할아버지', '외할머니', '외할아버지'],
  },
  { label: '형제자매', members: ['첫째', '둘째', '셋째'] },
]

function NewTripMembersPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { title, startDate, endDate } = Route.useSearch()
  const [selected, setSelected] = useState<string[]>([])
  const [customMember, setCustomMember] = useState('')
  const [customMembers, setCustomMembers] = useState<string[]>([])
  const [errorMessage, setErrorMessage] = useState<string>()
  const createTrip = useCreateTrip()
  const hasCompanion = selected.length > 0 || customMembers.length > 0

  function toggle(member: string) {
    setSelected((prev) =>
      prev.includes(member)
        ? prev.filter((m) => m !== member)
        : [...prev, member],
    )
  }

  const addCustomMember = () => {
    const member = customMember.trim()
    if (
      !member ||
      selected.includes(member) ||
      customMembers.includes(member)
    ) {
      return
    }

    setCustomMembers((prev) => [...prev, member])
    setCustomMember('')
  }

  const handleCreateTrip = async () => {
    setErrorMessage(undefined)

    if (!title || !startDate || !endDate) {
      setErrorMessage('여행 이름과 기간을 다시 확인해주세요.')
      return
    }

    try {
      const response = await createTrip.mutateAsync({
        data: {
          title,
          startDate,
          endDate,
          companions: [...new Set([...selected, ...customMembers])],
        },
      })

      if (!response.success || response.data?.id === undefined) {
        setErrorMessage(response.error?.message ?? '여행을 만들지 못했습니다.')
        return
      }

      await queryClient.invalidateQueries({ queryKey: getFindTripsQueryKey() })
      await navigate({
        to: '/trips/$tripId',
        params: { tripId: String(response.data.id) },
        replace: true,
      })
    } catch (error) {
      const apiError = getApiError(error)
      setErrorMessage(
        apiError?.code === 'INVALID_TRIP_PERIOD'
          ? (apiError.message ?? '여행 기간이 올바르지 않습니다.')
          : (apiError?.message ?? '여행을 만들지 못했습니다.'),
      )
    }
  }

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            disabled={createTrip.isPending || !hasCompanion}
            onClick={handleCreateTrip}
          >
            {createTrip.isPending ? '여행 만드는 중...' : '여행 만들기'}
          </Button>
        </div>
      }
    >
      <AppBar
        type="back"
        title="여행 만들기"
        onBack={() =>
          navigate({
            to: '/trips/new/period',
            search: { title, startDate, endDate },
          })
        }
      />
      <div className="flex flex-col gap-6 px-5 pt-[38px]">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2.5">
            <div className="flex-1">
              <StepIndicator step={3} totalSteps={3} />
            </div>
            <p className="text-caption text-muted-foreground whitespace-nowrap">
              3 / 3
            </p>
          </div>
          <div className="flex flex-col gap-1.5">
            <p className="text-title-1 text-foreground">누구와 함께 가세요?</p>
            <p className="text-body text-muted-foreground">
              함께 갈 가족을 골라주세요.
            </p>
          </div>
        </div>

        <div className="flex flex-col gap-4.5">
          {FAMILY_GROUPS.map((group) => (
            <div key={group.label} className="flex flex-col gap-2">
              <p className="text-caption-sm font-semibold text-muted-foreground">
                {group.label}
              </p>
              <div className="flex flex-wrap gap-2">
                {group.members.map((member) => {
                  const isSelected = selected.includes(member)
                  return (
                    <button
                      key={member}
                      type="button"
                      onClick={() => toggle(member)}
                      className={cn(
                        'rounded-chip px-3 py-1.5 text-label',
                        isSelected
                          ? 'bg-primary-tint text-primary-deep'
                          : 'bg-muted text-foreground',
                      )}
                    >
                      {getMemberEmoji(member)} {member}
                    </button>
                  )
                })}
              </div>
            </div>
          ))}
          <div className="flex flex-col gap-2">
            <p className="text-caption-sm font-semibold text-muted-foreground">
              기타
            </p>
            {customMembers.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {customMembers.map((member) => (
                  <button
                    key={member}
                    type="button"
                    onClick={() =>
                      setCustomMembers((prev) =>
                        prev.filter((item) => item !== member),
                      )
                    }
                    className="rounded-chip bg-primary-tint px-3 py-1.5 text-label text-primary-deep"
                  >
                    {member} ×
                  </button>
                ))}
              </div>
            )}
            <div className="flex items-center gap-2">
              <div className="flex-1">
                <TextInput
                  placeholder="예: 이모"
                  value={customMember}
                  maxLength={50}
                  onChange={(event) => setCustomMember(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      addCustomMember()
                    }
                  }}
                />
              </div>
              <Button
                type="button"
                variant="secondary"
                size="default"
                className="w-fit gap-1"
                disabled={!customMember.trim()}
                onClick={addCustomMember}
              >
                <Plus className="size-4" />
                추가
              </Button>
            </div>
          </div>
          {errorMessage && (
            <p className="text-caption-sm text-destructive" role="alert">
              {errorMessage}
            </p>
          )}
        </div>
      </div>
    </MobileScreen>
  )
}
