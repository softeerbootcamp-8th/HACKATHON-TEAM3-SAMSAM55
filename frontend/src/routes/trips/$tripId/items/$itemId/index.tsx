import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { User, Users } from 'lucide-react'
import * as React from 'react'

import { AddOptionSheet } from '@/components/trip/add-option-sheet'
import { EditOptionSheet } from '@/components/trip/edit-option-sheet'
import {
  ITEM_FIXTURES,
  type ItemFixture,
  type Option,
} from '@/components/trip/item-fixtures'
import { OptionCard } from '@/components/trip/option-card'
import { VoteStatusRow } from '@/components/trip/vote-status-row'
import { AppBar } from '@/components/ui/app-bar'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/ui/confirm-dialog'
import { MobileScreen } from '@/components/layout/mobile-screen'

export const Route = createFileRoute('/trips/$tripId/items/$itemId/')({
  component: ItemDetailPage,
})

const STATUS_BADGE: Record<
  ItemFixture['status'],
  { label: string; bg: string; text: string }
> = {
  draft: { label: '준비 중', bg: 'bg-[#eeeff1]', text: 'text-[#6b7075]' },
  voting: {
    label: '투표 중',
    bg: 'bg-primary-tint',
    text: 'text-primary-deep',
  },
  confirmed: {
    label: '확정',
    bg: 'bg-[#e6f6e9]',
    text: 'text-[#37b24d]',
  },
}

function ItemDetailPage() {
  const { tripId, itemId } = Route.useParams()
  const navigate = useNavigate()
  const item = ITEM_FIXTURES[itemId] ?? ITEM_FIXTURES['item-1']

  const [isEditingOptions, setIsEditingOptions] = React.useState(false)
  const [addOpen, setAddOpen] = React.useState(false)
  const [editingOption, setEditingOption] = React.useState<Option | null>(null)
  const [deletingOption, setDeletingOption] = React.useState<Option | null>(
    null,
  )
  const [deleteItemOpen, setDeleteItemOpen] = React.useState(false)

  const isVote = item.decisionMethod === '부모님과 투표'
  const badge = STATUS_BADGE[item.status]

  return (
    <MobileScreen>
      <AppBar
        type="backWithMore"
        title="일정 상세"
        onBack={() => window.history.back()}
        onMore={() => setDeleteItemOpen(true)}
      />

      <div className="flex flex-col gap-2 px-5 pt-4">
        <p className="text-[24px] font-bold text-foreground">{item.title}</p>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            {isVote ? (
              <Users className="size-4 text-primary-deep" />
            ) : (
              <User className="size-4 text-muted-foreground" />
            )}
            <p
              className={
                'text-card-title ' +
                (isVote ? 'text-primary-deep' : 'text-muted-foreground')
              }
            >
              {item.decisionMethod}
            </p>
          </div>
          <span
            className={`rounded-chip px-2.5 py-1 text-[12px] leading-none font-medium ${badge.bg} ${badge.text}`}
          >
            {badge.label}
          </span>
        </div>
        <p className="text-caption text-muted-foreground">{item.dayLabel}</p>
      </div>

      <div className="flex flex-1 flex-col gap-4 px-5 pt-4">
        {isVote && item.status === 'draft' && (
          <>
            <div className="flex items-center justify-between">
              <p className="text-[14px] font-medium text-muted-foreground">
                선택지 {item.options.length}개
              </p>
              <button
                type="button"
                className="text-card-title text-primary-deep"
                onClick={() => setIsEditingOptions((value) => !value)}
              >
                {isEditingOptions ? '완료' : '편집'}
              </button>
            </div>
            <div className="flex flex-col gap-2.5">
              {item.options.map((option) => (
                <OptionCard
                  key={option.id}
                  title={option.title}
                  description={option.description}
                  aiGenerated={option.aiGenerated}
                  editable={isEditingOptions}
                  onClick={() => setEditingOption(option)}
                  onDelete={() => setDeletingOption(option)}
                />
              ))}
            </div>
            <button
              type="button"
              onClick={() => setAddOpen(true)}
              className="flex h-13 items-center justify-center rounded-card border-[1.5px] border-dashed border-primary-deep text-card-title text-primary-deep"
            >
              + 선택지 추가
            </button>
            <p className="text-caption-sm text-muted-foreground">
              카드를 탭하면 이름, 설명, 사진을 수정할 수 있어요
            </p>
          </>
        )}

        {isVote && item.status === 'voting' && item.voterStatus && (
          <>
            <VoteStatusRow
              votedCount={item.votedCount ?? 0}
              totalCount={item.voterStatus.length}
              voters={item.voterStatus}
            />
            <p className="text-subtitle text-foreground">
              선택지 {item.options.length}개
            </p>
            <div className="flex flex-col gap-3">
              {item.options.map((option) => (
                <OptionCard
                  key={option.id}
                  title={option.title}
                  voteCount={option.voteCount}
                  voters={option.voters}
                  leading={(option.voteCount ?? 0) > 0}
                />
              ))}
            </div>
          </>
        )}

        {isVote && item.status === 'confirmed' && (
          <>
            <div className="flex flex-col gap-2 overflow-hidden rounded-[18px] border border-border">
              <div className="h-[233px] w-full bg-muted" />
              <div className="flex flex-col gap-2.5 p-4">
                <p className="text-title-2 text-foreground">
                  {item.options[0].title}
                </p>
                <p className="text-caption text-muted-foreground">
                  {item.options[0].description}
                </p>
                {item.decidedBy && (
                  <div className="flex items-center gap-2">
                    <div className="flex items-center -space-x-1.5">
                      {item.decidedBy.map((initial) => (
                        <span
                          key={initial}
                          className="flex size-6 items-center justify-center rounded-full border-2 border-background bg-primary text-[10px] font-medium text-foreground"
                        >
                          {initial}
                        </span>
                      ))}
                    </div>
                    <p className="text-[14px] font-medium text-primary-deep">
                      {item.decidedBy.length === 2
                        ? '엄마, 첫째가 골랐어요'
                        : '골랐어요'}
                    </p>
                  </div>
                )}
              </div>
            </div>
            <p className="text-subtitle text-foreground">최종 투표 결과</p>
            <div className="flex flex-col gap-2">
              {item.options.map((option, index) => (
                <div
                  key={option.id}
                  className={
                    'flex h-[50px] items-center justify-between rounded-thumb px-3.5 ' +
                    (index === 0
                      ? 'border-2 border-primary-deep bg-primary-tint'
                      : 'bg-muted')
                  }
                >
                  <p
                    className={
                      index === 0
                        ? 'text-[14px] font-medium text-foreground'
                        : 'text-[14px] text-muted-foreground'
                    }
                  >
                    {option.title}
                  </p>
                  <p
                    className={
                      'text-[14px] font-bold ' +
                      (index === 0
                        ? 'text-primary-deep'
                        : 'text-muted-foreground')
                    }
                  >
                    {option.voteCount}표
                  </p>
                </div>
              ))}
            </div>
          </>
        )}

        {!isVote && (
          <div className="flex flex-col gap-2.5">
            {item.status === 'confirmed' && (
              <div className="h-[233px] w-full overflow-hidden rounded-t-[18px] bg-muted" />
            )}
            <div
              className={
                item.status === 'confirmed'
                  ? 'flex flex-col gap-2.5 rounded-b-[18px] border border-t-0 border-border p-4'
                  : 'flex flex-col gap-2.5 rounded-card border border-border p-3'
              }
            >
              <div className="flex w-full items-center gap-3">
                {item.status === 'draft' && (
                  <div className="size-11 shrink-0 rounded-card border-[1.5px] border-dashed border-border bg-muted" />
                )}
                <p
                  className={
                    item.status === 'confirmed'
                      ? 'flex-1 text-title-2 text-foreground'
                      : 'flex-1 text-card-title text-foreground'
                  }
                >
                  {item.options[0].title}
                </p>
              </div>
              <div className="flex items-center gap-1.5">
                <p className="flex-1 text-[12.5px] text-muted-foreground">
                  {item.options[0].description}
                </p>
                {item.options[0].aiGenerated && (
                  <span className="shrink-0 rounded-chip bg-primary-tint px-2 py-[3px] text-[11px] leading-none font-medium text-primary-deep">
                    ✨ AI 작성
                  </span>
                )}
              </div>
              {item.status === 'confirmed' && (
                <div className="flex items-center gap-1.5">
                  <User className="size-4 text-muted-foreground" />
                  <p className="text-[14px] font-medium text-muted-foreground">
                    내가 정했어요
                  </p>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-2 border-t border-border px-5 pt-3 pb-7">
        {item.status === 'confirmed' ? (
          <Button variant="dangerOutline" size="cta">
            확정 해제하기
          </Button>
        ) : (
          <Button
            size="cta"
            onClick={() =>
              navigate({
                to: '/trips/$tripId/items/$itemId/edit',
                params: { tripId, itemId },
              })
            }
          >
            {isVote
              ? '이 일정만 투표 올리기'
              : `${item.options[0].title}로 확정하기`}
          </Button>
        )}
        {isVote && (
          <p className="text-center text-caption-sm text-muted-foreground">
            {item.status === 'confirmed'
              ? '해제하면 다시 투표 상태로 돌아가요'
              : '투표를 올리면 가족들이 투표할 수 있고, 더는 수정할 수 없어요'}
          </p>
        )}
      </div>

      <AddOptionSheet open={addOpen} onOpenChange={setAddOpen} />
      <EditOptionSheet
        key={editingOption?.id}
        open={editingOption !== null}
        onOpenChange={(open) => !open && setEditingOption(null)}
        initialName={editingOption?.title ?? ''}
        initialDescription={editingOption?.description ?? ''}
      />
      <ConfirmDialog
        open={deletingOption !== null}
        onOpenChange={(open) => !open && setDeletingOption(null)}
        title="선택지를 삭제할까요?"
        description="해당 선택지가 삭제됩니다."
        confirmLabel="삭제하기"
        danger
        onConfirm={() => setDeletingOption(null)}
      />
      <ConfirmDialog
        open={deleteItemOpen}
        onOpenChange={setDeleteItemOpen}
        title="일정을 삭제할까요?"
        description="삭제하면 되돌릴 수 없어요."
        confirmLabel="삭제하기"
        danger
        onConfirm={() => {
          setDeleteItemOpen(false)
          navigate({ to: '/trips/$tripId', params: { tripId } })
        }}
      />
    </MobileScreen>
  )
}
