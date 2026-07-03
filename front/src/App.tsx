import { useEffect, useState } from 'react'
import { api, Member } from './api'
import { toMessage } from './util'
import TopBar from './components/TopBar'
import FeedView from './components/FeedView'
import ExploreView from './components/ExploreView'
import NotificationsView from './components/NotificationsView'
import SignupView from './components/SignupView'
import ComposeModal from './components/ComposeModal'

const STORAGE_KEY = 'philstargram.currentUserId'

export type View = 'feed' | 'explore' | 'notifications' | 'signup'

export default function App() {
  const [currentUserId, setCurrentUserId] = useState<number | null>(() => {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? Number(raw) : null
  })
  const [currentMember, setCurrentMember] = useState<Member | null>(null)
  const [memberError, setMemberError] = useState<string | null>(null)
  const [view, setView] = useState<View>('feed')
  const [composeOpen, setComposeOpen] = useState(false)
  const [toast, setToast] = useState<string | null>(null)

  function selectUser(id: number | null) {
    setCurrentUserId(id)
    if (id === null) localStorage.removeItem(STORAGE_KEY)
    else localStorage.setItem(STORAGE_KEY, String(id))
  }

  // 토스트 자동 사라짐
  useEffect(() => {
    if (toast === null) return
    const t = setTimeout(() => setToast(null), 3200)
    return () => clearTimeout(t)
  }, [toast])

  // 현재 사용자 프로필 조회
  useEffect(() => {
    if (currentUserId === null) {
      setCurrentMember(null)
      setMemberError(null)
      return
    }
    let cancelled = false
    api
      .getMember(currentUserId)
      .then((m) => {
        if (!cancelled) {
          setCurrentMember(m)
          setMemberError(null)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setCurrentMember(null)
          setMemberError(toMessage(err))
        }
      })
    return () => {
      cancelled = true
    }
  }, [currentUserId])

  return (
    <div className="app">
      <TopBar
        currentUserId={currentUserId}
        currentMember={currentMember}
        memberError={memberError}
        onSelectUser={selectUser}
        view={view}
        onNav={setView}
        onCompose={() => {
          if (currentUserId === null) {
            setView('signup')
            setToast('먼저 사용자를 선택하거나 가입하세요.')
            return
          }
          setComposeOpen(true)
        }}
      />

      <main className="content">
        {view === 'feed' && <FeedView currentUserId={currentUserId} onGoSignup={() => setView('signup')} />}
        {view === 'explore' && <ExploreView currentUserId={currentUserId} onToast={setToast} />}
        {view === 'notifications' && <NotificationsView currentUserId={currentUserId} />}
        {view === 'signup' && (
          <SignupView
            onSignedUp={(m) => {
              selectUser(m.id)
              setView('feed')
              setToast(`@${m.nickname} 로 가입 완료! 현재 사용자로 설정했어요.`)
            }}
          />
        )}
      </main>

      {composeOpen && currentUserId !== null && (
        <ComposeModal
          currentUserId={currentUserId}
          onClose={() => setComposeOpen(false)}
          onPosted={() => {
            setComposeOpen(false)
            setToast('게시글을 공유했습니다. 팔로워의 피드/알림에 곧 반영됩니다.')
          }}
        />
      )}

      {toast && <div className="toast">{toast}</div>}
    </div>
  )
}
