// 백엔드 REST API 클라이언트. 응답은 공통 봉투 { success, data, error } 를 벗겨서 data 만 반환한다.
// 경로는 상대(/api/...)로 호출한다 → 개발은 Vite 프록시, 운영은 nginx 프록시가 백엔드로 넘겨
// 준다. 프론트와 같은 오리진이 되므로 CORS 가 필요 없다.
const API_BASE = ''

export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: { code: string; message: string } | null
}

export interface Member {
  id: number
  email: string
  nickname: string
  bio: string | null
  createdAt: string
}

export interface Post {
  id: number
  authorId: number
  content: string
  createdAt: string
}

export interface FeedItem {
  postId: number
  authorId: number
  authorNickname: string
  contentPreview: string
  createdAt: string
}

export type NotificationType = 'NEW_POST' | 'NEW_FOLLOWER'

// DOM 전역 Notification 과 이름이 겹치지 않도록 AppNotification 으로 둔다.
export interface AppNotification {
  id: number
  type: NotificationType
  message: string
  createdAt: string
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })

  const text = await res.text()
  const body = text ? JSON.parse(text) : null

  if (!res.ok) {
    const message = body?.error?.message ?? `요청 실패 (HTTP ${res.status})`
    throw new Error(message)
  }

  // 본문 없는 성공 응답(팔로우 201 / 언팔로우 204)
  if (body === null) {
    return undefined as T
  }

  // 공통 봉투면 data 만 꺼낸다
  if (typeof body === 'object' && 'success' in body) {
    const envelope = body as ApiResponse<T>
    if (!envelope.success) {
      throw new Error(envelope.error?.message ?? '요청 실패')
    }
    return envelope.data as T
  }

  return body as T
}

export const api = {
  signUp: (b: { email: string; nickname: string; bio?: string }) =>
    request<Member>('/api/members', { method: 'POST', body: JSON.stringify(b) }),

  getMember: (id: number) => request<Member>(`/api/members/${id}`),

  createPost: (b: { authorId: number; content: string }) =>
    request<Post>('/api/posts', { method: 'POST', body: JSON.stringify(b) }),

  getPost: (id: number) => request<Post>(`/api/posts/${id}`),

  follow: (followerId: number, followeeId: number) =>
    request<void>(`/api/members/${followerId}/follow/${followeeId}`, { method: 'POST' }),

  unfollow: (followerId: number, followeeId: number) =>
    request<void>(`/api/members/${followerId}/follow/${followeeId}`, { method: 'DELETE' }),

  getFeed: (memberId: number) => request<FeedItem[]>(`/api/members/${memberId}/feed`),

  getNotifications: (memberId: number) =>
    request<AppNotification[]>(`/api/members/${memberId}/notifications`),
}
