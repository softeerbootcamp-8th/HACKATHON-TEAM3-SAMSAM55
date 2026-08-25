export type Option = {
  id: string
  title: string
  description: string
  aiGenerated?: boolean
  voteCount?: number
  voters?: string[]
}

export type ItemFixture = {
  title: string
  category: string
  dayLabel: string
  decisionMethod: '부모님과 투표' | '내가 결정'
  status: 'draft' | 'voting' | 'confirmed'
  options: Option[]
  votedCount?: number
  voterStatus?: { initial: string; voted: boolean }[]
  decidedBy?: string[]
}

// 일정 상세(index.tsx)와 일정 수정(edit.tsx)이 같은 mock 데이터를 봐야
// 수정 화면이 실제로 그 일정 값으로 열린다 — 화면별로 따로 두면 어긋난다.
export const ITEM_FIXTURES: Record<string, ItemFixture> = {
  'item-1': {
    title: '점심 식사',
    category: '식사',
    dayLabel: '1일차 · 식사',
    decisionMethod: '부모님과 투표',
    status: 'draft',
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
        aiGenerated: true,
      },
      {
        id: 'opt-2',
        title: '라멘 이치란 신주쿠점',
        description: '진한 돈코츠 육수로 유명한 라멘 전문점',
        aiGenerated: true,
      },
    ],
  },
  'item-2': {
    title: '점심 식사',
    category: '식사',
    dayLabel: '1일차 · 식사',
    decisionMethod: '부모님과 투표',
    status: 'voting',
    votedCount: 2,
    voterStatus: [
      { initial: '엄', voted: true },
      { initial: '아', voted: false },
      { initial: '첫', voted: true },
    ],
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
        voteCount: 2,
        voters: ['엄', '첫'],
      },
      {
        id: 'opt-2',
        title: '라멘 이치란 신주쿠점',
        description: '진한 돈코츠 육수로 유명한 라멘 전문점',
        voteCount: 0,
        voters: [],
      },
    ],
  },
  'item-3': {
    title: '점심 식사',
    category: '식사',
    dayLabel: '1일차 · 식사',
    decisionMethod: '부모님과 투표',
    status: 'confirmed',
    decidedBy: ['엄', '첫'],
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
        voteCount: 2,
      },
      {
        id: 'opt-2',
        title: '라멘 이치란 신주쿠점',
        description: '진한 돈코츠 육수로 유명한 라멘 전문점',
        voteCount: 1,
      },
    ],
  },
  'item-4': {
    title: '점심 식사',
    category: '식사',
    dayLabel: '1일차 · 식사',
    decisionMethod: '내가 결정',
    status: 'draft',
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '20년 경력 전문 셰프가 제공하는 고급 스시 오마카세',
        aiGenerated: true,
      },
    ],
  },
  'item-5': {
    title: '저녁 식사',
    category: '식사',
    dayLabel: '1일차 · 식사',
    decisionMethod: '내가 결정',
    status: 'confirmed',
    options: [
      {
        id: 'opt-1',
        title: '스시 오마카세 긴자점',
        description: '신선한 제철 재료로 만든 프리미엄 스시 코스',
      },
    ],
  },
}
