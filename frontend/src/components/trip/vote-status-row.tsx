type VoteStatusRowProps = {
  votedCount: number
  totalCount: number
  voters: { initial: string; voted: boolean }[]
}

function VoteStatusRow({ votedCount, totalCount, voters }: VoteStatusRowProps) {
  return (
    <div className="flex w-full flex-col items-start rounded-btn bg-muted p-4">
      <div className="flex w-full items-center justify-between">
        <p className="text-subtitle text-foreground">
          {votedCount} / {totalCount}명 투표 완료
        </p>
        <div className="flex items-start gap-1.5">
          {voters.map((voter, index) => (
            <span
              key={index}
              className={
                'flex size-7 items-center justify-center rounded-card text-chip ' +
                (voter.voted
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-border text-muted-foreground')
              }
            >
              {voter.initial}
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}

export { VoteStatusRow }
