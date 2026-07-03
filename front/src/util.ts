export function formatTime(iso: string): string {
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  return d.toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
}

export function toMessage(err: unknown): string {
  return err instanceof Error ? err.message : String(err)
}
