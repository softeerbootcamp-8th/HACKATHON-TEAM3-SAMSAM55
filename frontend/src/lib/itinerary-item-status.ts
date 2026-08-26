export type BackendItemStatus = 'PENDING' | 'VOTING' | 'VOTED' | 'CONFIRMED'

export type ItemStatus = 'draft' | 'voting' | 'voteDone' | 'confirmed'

const BACKEND_TO_ITEM_STATUS: Record<BackendItemStatus, ItemStatus> = {
  PENDING: 'draft',
  VOTING: 'voting',
  VOTED: 'voteDone',
  CONFIRMED: 'confirmed',
}

export function toItemStatus(
  status: string | undefined,
): ItemStatus | undefined {
  if (!status) {
    return undefined
  }
  return BACKEND_TO_ITEM_STATUS[status as BackendItemStatus]
}
