import { createFileRoute, Outlet } from '@tanstack/react-router'

import { requireActor } from '@/features/auth/auth'

export const Route = createFileRoute('/parent')({
  beforeLoad: ({ context }) => requireActor(context.queryClient, 'PARTICIPANT'),
  component: ParentLayout,
})

function ParentLayout() {
  return <Outlet />
}
