import { useState, type FormEvent } from 'react'
import { api, Member } from '../api'
import { toMessage } from '../util'

interface Props {
  onSignedUp: (m: Member) => void
}

export default function SignupView({ onSignedUp }: Props) {
  const [email, setEmail] = useState('')
  const [nickname, setNickname] = useState('')
  const [bio, setBio] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const m = await api.signUp({ email, nickname, bio: bio || undefined })
      onSignedUp(m)
    } catch (err) {
      setError(toMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="signup-wrap">
      <div className="signup-card">
        <div className="signup-logo">philstargram</div>
        <p className="muted center">가입하고 생각을 공유하세요.</p>
        <form onSubmit={submit}>
          <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="이메일" />
          <input value={nickname} onChange={(e) => setNickname(e.target.value)} placeholder="닉네임 (2~50자)" />
          <input value={bio} onChange={(e) => setBio(e.target.value)} placeholder="소개 (선택)" />
          <button type="submit" disabled={loading}>
            {loading ? '가입 중…' : '가입'}
          </button>
        </form>
        {error && <p className="err center">{error}</p>}
      </div>
      <div className="signup-note muted small">가입하면 자동으로 현재 사용자로 설정됩니다.</div>
    </div>
  )
}
