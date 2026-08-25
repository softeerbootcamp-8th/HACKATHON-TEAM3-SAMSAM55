export function formatMonthDay(isoDate: string): string {
  const date = new Date(isoDate)
  return `${date.getMonth() + 1}월 ${date.getDate()}일`
}

export function formatDateRange(
  startIsoDate: string,
  endIsoDate: string,
): string {
  return `${formatMonthDay(startIsoDate)} - ${formatMonthDay(endIsoDate)}`
}
