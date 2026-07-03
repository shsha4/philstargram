import { useCallback, useEffect, useState } from 'react'
import { api, AppNotification, NotificationType } from '../api'
import { formatTime, toMessage } from '../util'

const LABEL: Record<NotificationType, string> = {
  NEW_POST: '새 게시글',
  NEW_FOLLOWER: '새 팔로워',
}

interface Props {
  currentUserId: number | null
}

export default function NotificationsView({ currentUserId }: Props) {
  const [items, setItems] = useState<AppNotification[]>([])
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
      setItems(await api.getNotifications(currentUserId))
    } catch (err) {
      setError(toMessage(err))
    } finally {
      setLoading(false)
    }
  }, [currentUserId])

  useEffect(() => {
    load()
  }, [load])

  return (
    <div className="notis">
      <div className="feed-top">
        <span className="feed-title">알림</span>
        <button className="link-btn" onClick={load} disabled={loading || currentUserId === null}>
          {loading ? '…' : '새로고침'}
        </button>
      </div>

      {currentUserId === null && <p className="muted">현재 사용자를 선택하세요.</p>}
      {error && <p className="err">{error}</p>}
      {currentUserId !== null && !error && items.length === 0 && !loading && (
        <p className="muted">알림이 없습니다.</p>
      )}

      <ul className="noti-list">
        {items.map((n) => (
          <li key={n.id} className="noti">
            <span className={`badge ${n.type}`}>{LABEL[n.type] ?? n.type}</span>
            <span className="noti-msg">{n.message}</span>
            <span className="noti-time">{formatTime(n.createdAt)}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
