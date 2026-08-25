import { useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'

import { Button } from '@/components/ui/button'
import { RoleChip } from '@/components/ui/role-chip'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/invite/$inviteCode/role')({
  component: InviteRolePage,
})

const MOCK_ROLES = [
  { id: 'mom', emoji: '👩', label: '엄마', taken: false },
  { id: 'dad', emoji: '👨', label: '아빠', taken: false },
  { id: 'grandma', emoji: '👵', label: '외할머니', taken: false },
  { id: 'first', emoji: '🧑', label: '첫째', taken: true },
]

function InviteRolePage() {
  const navigate = useNavigate()
  const [selectedRole, setSelectedRole] = useState<string | null>(null)

  return (
    <MobileScreen
      bottomBar={
        <div className="px-5 pb-6">
          <Button
            size="cta"
            disabled={!selectedRole}
            onClick={() => navigate({ to: '/parent' })}
          >
            참여하기
          </Button>
        </div>
      }
    >
      <div className="flex flex-col gap-6 px-6 pt-7 pb-4">
        <div className="flex flex-col gap-2">
          <p className="text-[22px] leading-[1.45] font-bold text-foreground">
            나의 역할을 골라주세요
          </p>
          <p className="text-[14px] leading-[1.55] text-muted-foreground">
            가족 안에서의 역할을 선택해주세요
          </p>
        </div>
        <div className="grid grid-cols-2 gap-3">
          {MOCK_ROLES.map((role) => (
            <RoleChip
              key={role.id}
              emoji={role.emoji}
              label={role.label}
              state={
                role.taken
                  ? 'taken'
                  : selectedRole === role.id
                    ? 'selected'
                    : 'default'
              }
              onClick={() => setSelectedRole(role.id)}
            />
          ))}
        </div>
      </div>
    </MobileScreen>
  )
}
