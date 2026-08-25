const DATE_FORMATTER = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
})

function parseDate(value: string): Date {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function formatTripPeriod(
  startDate: string,
  endDate: string,
  companionCount: number,
): string {
  return `${DATE_FORMATTER.format(parseDate(startDate))} - ${DATE_FORMATTER.format(parseDate(endDate))} · ${companionCount}명`
}

export function formatDDay(startDate: string): string {
  const today = new Date()
  const target = parseDate(startDate)

  const difference = Math.round(
    (Date.UTC(target.getFullYear(), target.getMonth(), target.getDate()) -
      Date.UTC(today.getFullYear(), today.getMonth(), today.getDate())) /
      86_400_000,
  )

  if (difference === 0) {
    return 'D-Day'
  }

  return difference > 0 ? `D-${difference}` : `D+${Math.abs(difference)}`
}
