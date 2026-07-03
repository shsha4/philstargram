import { useCallback, useEffect, useState } from 'react'
import { api, FeedItem } from '../api'
import { formatTime, toMessage } from '../util'
import Avatar from './Avatar'

interface Props {
  currentUserId: number | null
  onGoSignup: () => void
}

export default function FeedView({ currentUserId, onGoSignup }: Props) {
  const [items, setItems] = useState<FeedItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    if (currentUserId === null) {
      setItems([])
      return
    }
    setLoading(true)
    setError(null)
    try {
      setItems(await api.getFeed(currentUserId))
    } catch (err) {
      setError(toMessage(err))
    } finally {
      setLoading(false)
    }
  }, [currentUserId])

  useEffect(() => {
    load()
  }, [load])

  if (currentUserId === null) {
    return (
      <div className="feed">
        <div className="empty-hero">
          <h2>philstargram 데모</h2>
          <p className="muted">로그인이 없는 데모예요. 상단에서 사용자 ID를 전환하거나 먼저 가입하세요.</p>
          <button onClick={onGoSignup}>회원 가입하기</button>
        </div>
      </div>
    )
  }

  return (
    <div className="feed">
      <div className="feed-top">
        <span className="feed-title">홈</span>
        <button className="link-btn" onClick={load} disabled={loading}>
          {loading ? '불러오는 중…' : '새로고침'}
        </button>
      </div>

      {error && <p className="err">{error}</p>}

      {!error && items.length === 0 && !loading && (
        <div className="empty-card">
          피드가 비어 있어요. <strong>탐색</strong>에서 다른 사용자를 팔로우하고, 그 사람이 새 글을 올리면 여기에
          나타납니다.
          <div className="muted small">쓰기 시점 팬아웃이라 과거 글은 소급되지 않아요.</div>
        </div>
      )}

      {items.map((it) => (
        <article key={it.postId} className="post">
          <header className="post-head">
            <Avatar name={it.authorNickname} size={34} />
            <span className="post-author">@{it.authorNickname}</span>
            <span className="post-time">{formatTime(it.createdAt)}</span>
          </header>
          <div className="post-body">{it.contentPreview}</div>
        </article>
      ))}
    </div>
  )
}
