import { Plus } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import { BottomSheet } from '@/components/ui/bottom-sheet'
import { TextInput } from '@/components/ui/text-input'

type AddOptionSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  onAdd?: (name: string, image: File | null) => void
}

function AddOptionSheet({ open, onOpenChange, onAdd }: AddOptionSheetProps) {
  const [name, setName] = React.useState('')
  const [image, setImage] = React.useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = React.useState<string | null>(null)
  const fileInputRef = React.useRef<HTMLInputElement>(null)

  React.useEffect(() => {
    if (!image) {
      setPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(image)
    setPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [image])

  const reset = () => {
    setName('')
    setImage(null)
  }

  return (
    <BottomSheet open={open} onOpenChange={onOpenChange} title="선택지 추가">
      <div className="flex items-end gap-3">
        <button
          type="button"
          aria-label="사진 추가"
          onClick={() => fileInputRef.current?.click()}
          className="flex size-[74px] shrink-0 items-center justify-center overflow-hidden rounded-card border-[1.5px] border-dashed border-border bg-muted"
        >
          {previewUrl ? (
            <img
              src={previewUrl}
              alt="선택지 이미지 미리보기"
              className="size-full object-cover"
            />
          ) : (
            <Plus className="size-4 text-muted-foreground" />
          )}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(event) => setImage(event.target.files?.[0] ?? null)}
        />
        <TextInput
          label="이름"
          placeholder="예: 신주쿠 라멘"
          value={name}
          onChange={(event) => setName(event.target.value)}
          className="flex-1"
        />
      </div>
      <Button
        size="cta"
        disabled={name.trim().length === 0}
        onClick={() => {
          onAdd?.(name, image)
          reset()
          onOpenChange(false)
        }}
      >
        선택지 추가하기
      </Button>
    </BottomSheet>
  )
}

export { AddOptionSheet }
