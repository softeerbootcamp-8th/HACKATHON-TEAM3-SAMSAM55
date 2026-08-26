const MEMBER_EMOJI: Record<string, string> = {
  엄마: '👩🏻',
  아빠: '👨🏻',
  친할머니: '👵🏻',
  친할아버지: '👴🏻',
  외할머니: '👵🏻',
  외할아버지: '👴🏻',
  첫째: '🧑🏻',
  둘째: '🧒🏻',
  셋째: '👶🏻',
}

const DEFAULT_MEMBER_EMOJI = '🙂'

export function getMemberEmoji(roleName: string | undefined): string {
  if (!roleName) {
    return DEFAULT_MEMBER_EMOJI
  }
  return MEMBER_EMOJI[roleName] ?? DEFAULT_MEMBER_EMOJI
}

export { MEMBER_EMOJI }
