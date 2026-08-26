import { Camera } from 'lucide-react'
import * as React from 'react'

import { Button } from '@/components/ui/button'
import { BottomSheet } from '@/components/ui/bottom-sheet'
import { TextInput } from '@/components/ui/text-input'
import { defaultOptionImageSquare } from '@/lib/default-option-image'

type EditOptionSheetProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  initialName: string
  initialDescription: string
  initialDescriptionSource?: string
  initialImageSrc?: string
  onSave?: (name: string, description: string, image: File | null) => void
}

function EditOptionSheet({
  open,
  onOpenChange,
  initialName,
  initialDescription,
  initialDescriptionSource,
  initialImageSrc,
  onSave,
}: EditOptionSheetProps) {
  const isAiGenerated = initialDescriptionSource === 'AI'
  const [name, setName] = React.useState(initialName)
  const [description, setDescription] = React.useState(initialDescription)
  const [image, setImage] = React.useState<File | null>(null)
  const [newPreviewUrl, setNewPreviewUrl] = React.useState<string | null>(null)
  const fileInputRef = React.useRef<HTMLInputElement>(null)

  React.useEffect(() => {
    if (!image) {
      setNewPreviewUrl(null)
      return
    }
    const url = URL.createObjectURL(image)
    setNewPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [image])

  const previewUrl = newPreviewUrl ?? initialImageSrc

  return (
    <BottomSheet open={open} onOpenChange={onOpenChange} title="선택지 수정">
      <div className="flex items-end gap-3">
        <button
          type="button"
          aria-label="사진 변경"
          onClick={() => fileInputRef.current?.click()}
          className="relative flex size-[74px] shrink-0 items-center justify-center overflow-hidden rounded-card bg-muted"
        >
          <img
            src={previewUrl ?? defaultOptionImageSquare}
            alt=""
            className="absolute inset-0 size-full object-cover"
          />
          <span className="absolute -right-1.5 -bottom-1.5 flex size-[30px] items-center justify-center rounded-full bg-background shadow-[0px_0px_3.5px_rgba(0,0,0,0.1)]">
            <Camera className="size-4 text-muted-foreground" />
          </span>
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
          value={name}
          onChange={(event) => setName(event.target.value)}
          className="flex-1"
        />
      </div>

      <div className="flex flex-col gap-2">
        <p className="text-caption text-muted-foreground">설명</p>
        <textarea
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          rows={3}
          className="h-21 w-full resize-none rounded-card border-2 border-primary-deep bg-muted px-4 py-3.5 text-[14px] text-foreground outline-none"
        />
        <p className="text-caption-sm text-muted-foreground">
          {isAiGenerated
            ? 'AI가 쓴 내용이라 사실과 다를 수 있어요. 확인해주세요'
            : '직접 쓴 설명이에요. 자유롭게 수정할 수 있어요'}
        </p>
      </div>

      <Button
        size="cta"
        onClick={() => {
          onSave?.(name, description, image)
          onOpenChange(false)
        }}
      >
        저장
      </Button>
    </BottomSheet>
  )
}

export { EditOptionSheet }
