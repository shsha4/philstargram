import { useState, type FormEvent } from 'react'
import { api, Member } from '../api'
import { toMessage } from '../util'
import Avatar from './Avatar'

interface Props {
  currentUserId: number | null
  onToast: (m: string) => void
}

export default function ExploreView({ currentUserId, onToast }: Props) {
  const [query, setQuery] = useState('')
  const [profile, setProfile] = useState<Member | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function lookup(e: FormEvent) {
    e.preventDefault()
    const id = Number(query)
    if (query.trim() === '' || isNaN(id)) {
      setError('사용자 ID를 입력하세요.')
      return
    }
    setLoading(true)
    setError(null)
    setProfile(null)
    try {
      setProfile(await api.getMember(id))
    } catch (err) {
      setError(toMessage(err))
    } finally {
      setLoading(false)
    }
  }

  async function doFollow(kind: 'follow' | 'unfollow') {
    if (currentUserId === null || profile === null) return
    setError(null)
    try {
      if (kind === 'follow') {
        await api.follow(currentUserId, profile.id)
        onToast(`@${profile.nickname} 님을 팔로우했습니다.`)
      } else {
        await api.unfollow(currentUserId, profile.id)
        onToast(`@${profile.nickname} 님을 언팔로우했습니다.`)
      }
    } catch (err) {
      setError(toMessage(err))
    }
  }

  const isSelf = profile !== null && profile.id === currentUserId

  return (
    <div className="explore">
      <form className="search" onSubmit={lookup}>
        <input
          type="number"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="사용자 ID로 프로필 찾기"
        />
        <button type="submit" disabled={loading}>
          {loading ? '…' : '찾기'}
        </button>
      </form>

      {error && <p className="err">{error}</p>}

      {profile && (
        <div className="profile-card">
          <Avatar name={profile.nickname} size={72} />
          <div className="profile-info">
            <div className="profile-name">@{profile.nickname}</div>
            <div className="muted">
              id {profile.id} · {profile.email}
            </div>
            {profile.bio && <div className="profile-bio">{profile.bio}</div>}
            {currentUserId === null ? (
              <div className="muted small">현재 사용자를 선택하면 팔로우할 수 있어요.</div>
            ) : isSelf ? (
              <div className="muted small">나 자신입니다.</div>
            ) : (
              <div className="row">
                <button onClick={() => doFollow('follow')}>팔로우</button>
                <button className="ghost" onClick={() => doFollow('unfollow')}>
                  언팔로우
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
