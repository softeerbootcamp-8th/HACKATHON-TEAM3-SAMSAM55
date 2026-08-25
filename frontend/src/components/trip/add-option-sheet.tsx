import { Plus } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import { BottomSheet } from '@/components/ui/bottom-sheet'
import { TextInput } from '@/components/ui/text-input'

type AddOptionSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onAdd?: (name: string) => void
}

function AddOptionSheet({ open, onOpenChange, onAdd }: AddOptionSheetProps) {
  const [name, setName] = React.useState('')

  return (
    <BottomSheet open={open} onOpenChange={onOpenChange} title="선택지 추가">
      <div className="flex items-end gap-3">
        <button
          type="button"
          aria-label="사진 추가"
          className="flex size-[74px] shrink-0 items-center justify-center rounded-card border-[1.5px] border-dashed border-border bg-muted"
        >
          <Plus className="size-4 text-muted-foreground" />
        </button>
        <TextInput
          label="이름"
          placeholder="예: 신주쿠 라멘"
          value={name}
          onChange={(event) => setName(event.target.value)}
          className="flex-1"
        />
      </div>
      <div className="flex items-center gap-2 rounded-thumb bg-primary-tint px-3.5 py-3">
        <span className="shrink-0 text-[14px]">✨</span>
        <p className="text-caption text-foreground">
          여행지 이름만 넣으면 AI가 설명을 써줘요
        </p>
      </div>
      <Button
        size="cta"
        onClick={() => {
          onAdd?.(name)
          setName('')
          onOpenChange(false)
        }}
      >
        선택지 추가하기
      </Button>
    </BottomSheet>
  )
}

export { AddOptionSheet }
