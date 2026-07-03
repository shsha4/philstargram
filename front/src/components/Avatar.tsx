function colorFor(seed: string): string {
  let h = 0
  for (let i = 0; i < seed.length; i++) {
    h = (h * 31 + seed.charCodeAt(i)) % 360
  }
  return `hsl(${h}, 60%, 52%)`
}

interface Props {
  name: string
  size?: number
}

export default function Avatar({ name, size = 32 }: Props) {
  const initial = (name.trim()[0] ?? '?').toUpperCase()
  return (
    <span
      className="avatar"
      style={{ width: size, height: size, background: colorFor(name || '?'), fontSize: size * 0.42 }}
    >
      {initial}
    </span>
  )
}
