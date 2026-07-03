import { useEffect, useState, type FormEvent } from 'react'
import { Member } from '../api'
import type { View } from '../App'
import Icon from './Icon'
import Avatar from './Avatar'

interface Props {
  currentUserId: number | null
  currentMember: Member | null
  memberError: string | null
  onSelectUser: (id: number | null) => void
  view: View
  onNav: (v: View) => void
  onCompose: () => void
}

export default function TopBar({
  currentUserId,
  currentMember,
  memberError,
  onSelectUser,
  view,
  onNav,
  onCompose,
}: Props) {
  const [draft, setDraft] = useState(currentUserId ? String(currentUserId) : '')

  // 회원가입 성공 등 외부에서 현재 사용자가 바뀌면 입력값 동기화
  useEffect(() => {
    setDraft(currentUserId ? String(currentUserId) : '')
  }, [currentUserId])

  function apply(e: FormEvent) {
    e.preventDefault()
    const n = Number(draft)
    onSelectUser(draft.trim() === '' || isNaN(n) ? null : n)
  }

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <button className="brand" onClick={() => onNav('feed')}>
          philstargram
        </button>

        <nav className="nav">
          <button className={`navbtn ${view === 'feed' ? 'active' : ''}`} title="홈" onClick={() => onNav('feed')}>
            <Icon name="home" filled={view === 'feed'} />
          </button>
          <button
            className={`navbtn ${view === 'explore' ? 'active' : ''}`}
            title="탐색"
            onClick={() => onNav('explore')}
          >
            <Icon name="compass" filled={view === 'explore'} />
          </button>
          <button className="navbtn" title="글쓰기" onClick={onCompose}>
            <Icon name="plus" />
          </button>
          <button
            className={`navbtn ${view === 'notifications' ? 'active' : ''}`}
            title="알림"
            onClick={() => onNav('notifications')}
          >
            <Icon name="heart" filled={view === 'notifications'} />
          </button>
          <button
            className={`navbtn ${view === 'signup' ? 'active' : ''}`}
            title="회원가입"
            onClick={() => onNav('signup')}
          >
            <Icon name="user" filled={view === 'signup'} />
          </button>
        </nav>

        <form className="userswitch" onSubmit={apply}>
          {currentMember && <Avatar name={currentMember.nickname} size={28} />}
          <input
            type="number"
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            placeholder="ID"
            title="현재 사용자 ID"
          />
          <button type="submit" className="switch-btn">
            전환
          </button>
          <span className="who">
            {currentUserId === null
              ? '미선택'
              : currentMember
                ? `@${currentMember.nickname}`
                : memberError
                  ? '조회 실패'
                  : '…'}
          </span>
        </form>
      </div>
    </header>
  )
}
