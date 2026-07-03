import { useState, type FormEvent } from 'react'
import { api } from '../api'
import { toMessage } from '../util'

interface Props {
  currentUserId: number
  onClose: () => void
  onPosted: () => void
}

export default function ComposeModal({ currentUserId, onClose, onPosted }: Props) {
  const [content, setContent] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await api.createPost({ authorId: currentUserId, content })
      onPosted()
    } catch (err) {
      setError(toMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <span>새 게시글</span>
          <button className="link-btn" onClick={onClose}>
            닫기
          </button>
        </div>
        <form onSubmit={submit}>
          <textarea
            autoFocus
            rows={5}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="무슨 생각을 하고 있나요?"
          />
          {error && <p className="err">{error}</p>}
          <button type="submit" disabled={loading}>
            {loading ? '공유 중…' : '공유'}
          </button>
        </form>
      </div>
    </div>
  )
}
