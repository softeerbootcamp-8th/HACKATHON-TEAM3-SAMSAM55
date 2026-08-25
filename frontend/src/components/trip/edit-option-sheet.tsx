import { Camera } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import { BottomSheet } from '@/components/ui/bottom-sheet'
import { TextInput } from '@/components/ui/text-input'

type EditOptionSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialName: string
  initialDescription: string
  onSave?: (name: string, description: string) => void
}

function EditOptionSheet({
  open,
  onOpenChange,
  initialName,
  initialDescription,
  onSave,
}: EditOptionSheetProps) {
  const [name, setName] = React.useState(initialName)
  const [description, setDescription] = React.useState(initialDescription)

  return (
    <BottomSheet open={open} onOpenChange={onOpenChange} title="선택지 수정">
      <div className="flex items-end gap-3">
        <div className="relative flex size-[74px] shrink-0 items-center justify-center rounded-card bg-muted">
          <span className="absolute -right-1.5 -bottom-1.5 flex size-[30px] items-center justify-center rounded-full bg-background shadow-[0px_0px_3.5px_rgba(0,0,0,0.1)]">
            <Camera className="size-4 text-muted-foreground" />
          </span>
        </div>
        <TextInput
          label="이름"
          value={name}
          onChange={(event) => setName(event.target.value)}
          className="flex-1"
        />
      </div>

      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between">
          <p className="text-caption text-muted-foreground">설명</p>
          <span className="rounded-chip bg-primary-tint px-2 py-[3px] text-[11px] leading-none font-medium text-primary-deep">
            ✨ AI 작성
          </span>
        </div>
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          rows={3}
          className="h-21 w-full resize-none rounded-card border-2 border-primary-deep bg-muted px-4 py-3.5 text-[14px] text-foreground outline-none"
        />
        <p className="text-caption-sm text-muted-foreground">
          AI가 쓴 내용이라 사실과 다를 수 있어요. 확인해주세요
        </p>
      </div>

      <Button
        size="cta"
        onClick={() => {
          onSave?.(name, description)
          onOpenChange(false)
        }}
      >
        저장
      </Button>
    </BottomSheet>
  )
}

export { EditOptionSheet }
