export const DEFAULT_ROLEPLAY_USER_ID = 1

export function toServerMode(label: '文本' | '语音'): 'text' | 'voice' {
  return label === '语音' ? 'voice' : 'text'
}
