import { AlertDialog } from 'radix-ui'

import { Button } from '@/components/ui/button'

type InviteLinkDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  tripTitle: string
  tripPeriod: string
  onCopyLink: () => void | Promise<void>
}

function InviteLinkDialog({
  open,
  onOpenChange,
  tripTitle,
  tripPeriod,
  onCopyLink,
}: InviteLinkDialogProps) {
  const handleCopy = async () => {
    await onCopyLink()
    onOpenChange(false)
  }

  return (
    <AlertDialog.Root open={open} onOpenChange={onOpenChange}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="fixed inset-0 z-50 bg-black/40" />
        <AlertDialog.Content className="fixed top-1/2 left-1/2 z-50 flex w-[320px] -translate-x-1/2 -translate-y-1/2 flex-col gap-[22px] rounded-[20px] bg-background px-4 pt-6 pb-4">
          <div className="flex flex-col gap-2 px-1">
            <AlertDialog.Title className="text-[17px] font-bold text-foreground">
              가족들을 초대해보세요
            </AlertDialog.Title>
            <AlertDialog.Description className="text-[14px] text-muted-foreground">
              앱 설치나 회원가입 없이 링크로 바로 들어오실 수 있어요
            </AlertDialog.Description>
          </div>
          <div className="flex flex-col gap-1.5 rounded-card bg-primary-tint px-[17px] py-[14px]">
            <p className="text-[16px] font-bold text-foreground">{tripTitle}</p>
            <p className="text-caption text-muted-foreground">{tripPeriod}</p>
          </div>
          <div className="flex flex-col gap-2.5">
            <Button size="dialog" onClick={handleCopy}>
              링크 복사하기
            </Button>
            <AlertDialog.Cancel asChild>
              <Button
                variant="secondary"
                size="dialog"
                className="border-primary"
              >
                취소
              </Button>
            </AlertDialog.Cancel>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  )
}

export { InviteLinkDialog }
