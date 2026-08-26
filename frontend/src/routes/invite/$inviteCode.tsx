import { createFileRoute, Outlet } from '@tanstack/react-router'

import { redirectIfParticipant } from '@/features/auth/auth'

export const Route = createFileRoute('/invite/$inviteCode')({
  beforeLoad: ({ context }) => redirectIfParticipant(context.queryClient),
  component: () => <Outlet />,
})
