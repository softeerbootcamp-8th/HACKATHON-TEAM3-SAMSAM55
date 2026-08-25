import { createFileRoute, Outlet } from '@tanstack/react-router'

import { requireActor } from '@/features/auth/auth'

export const Route = createFileRoute('/trips')({
  beforeLoad: ({ context }) => requireActor(context.queryClient, 'HOST'),
  component: TripsLayout,
})

function TripsLayout() {
  return <Outlet />
}
