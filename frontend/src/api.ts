export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export type Session = {
  accessToken: string
  refreshToken: string
}

export type AuthResponse = Session & {
  accessTokenTtlMinutes: number
  refreshTokenTtlDays: number
}

export type CurrentUser = {
  id: number
  email: string
  firstName: string
  lastName: string
  about?: string | null
  birthDate?: string | null
  city?: string | null
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | null
}

export type UserProfile = {
  id: number
  first_name: string
  last_name: string
  is_closed: boolean
  about?: string | null
  bdate?: string | null
  city?: string | null
  gender?: 'MALE' | 'FEMALE' | 'OTHER' | null
}

export type PrivacyMode = 'NOBODY' | 'ONLY_SELECTED' | 'ALL_EXCEPT_SELECTED'

export type PrivacyRule = {
  mode: PrivacyMode
  userIds: number[]
}

export type PrivacySettings = {
  canMessageMe: PrivacyRule
  canSeeInfo: PrivacyRule
  canInviteMe: PrivacyRule
}

export type Attachment = {
  id: number
  type: string
  attachment_key: string
  preview_url?: string | null
  file_name: string
  content_type: string
  size_bytes: number
  width?: number | null
  height?: number | null
}

export type Message = {
  id: number
  date: string
  peer_id: number
  from_id: number
  message: string | null
  attachments: Attachment[]
}

export type ChatSummary = {
  id: number
  peer_id: number
  peer_name: string
  last_message?: string | null
}

export type GroupSummary = {
  id: number
  title: string
}

export type GroupPrivacy = {
  membersVisible: boolean
  canUsersInvite: boolean
}

export type SearchResult = {
  id: number
  type: string
  title: string
  subtitle: string
}

export type PagedResponse<T> = {
  items: T[]
  limit: number
  offset: number
  page: number
  total: number
  hasNext: boolean
  hasPrevious: boolean
}

type RequestOptions = RequestInit & {
  session: Session | null
  onSessionChange: (session: Session | null) => void
}

async function refreshSession(session: Session, onSessionChange: (session: Session | null) => void) {
  const response = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: session.refreshToken }),
  })

  if (!response.ok) {
    onSessionChange(null)
    throw new Error('Session expired')
  }

  const data = (await response.json()) as AuthResponse
  const nextSession = { accessToken: data.accessToken, refreshToken: data.refreshToken }
  onSessionChange(nextSession)
  return nextSession
}

export async function apiFetch<T>(path: string, options: RequestOptions): Promise<T> {
  const { session, onSessionChange, headers, ...rest } = options
  const requestHeaders = new Headers(headers ?? {})
  if (!(rest.body instanceof FormData) && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json')
  }
  if (session?.accessToken) {
    requestHeaders.set('Authorization', `Bearer ${session.accessToken}`)
  }

  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: requestHeaders,
    })
  } catch {
    throw new Error(`Cannot reach API at ${API_BASE_URL}. Check backend and CORS settings.`)
  }

  if (response.status === 401 && session?.refreshToken) {
    const nextSession = await refreshSession(session, onSessionChange)
    requestHeaders.set('Authorization', `Bearer ${nextSession.accessToken}`)
    try {
      response = await fetch(`${API_BASE_URL}${path}`, {
        ...rest,
        headers: requestHeaders,
      })
    } catch {
      throw new Error(`Cannot reach API at ${API_BASE_URL}. Check backend and CORS settings.`)
    }
  }

  if (!response.ok) {
    const contentType = response.headers.get('Content-Type') ?? ''
    if (contentType.includes('application/json')) {
      const payload = (await response.json()) as { message?: string; error?: string; status?: number }
      const rawMessage = (payload.message ?? payload.error ?? '').trim()
      let normalized = rawMessage
      if (rawMessage === 'Current user cannot message this user') {
        normalized = 'This user does not allow messages from you.'
      }
      throw new Error(normalized || `Request failed with status ${response.status}`)
    }
    const text = await response.text()
    throw new Error(text || `Request failed with status ${response.status}`)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes('application/json')) {
    return undefined as T
  }
  return (await response.json()) as T
}

export async function fetchAuthorizedBlob(path: string, session: Session | null, onSessionChange: (session: Session | null) => void) {
  const requestHeaders = new Headers()
  if (session?.accessToken) {
    requestHeaders.set('Authorization', `Bearer ${session.accessToken}`)
  }

  let response = await fetch(`${API_BASE_URL}${path}`, {
    headers: requestHeaders,
  })

  if (response.status === 401 && session?.refreshToken) {
    const nextSession = await refreshSession(session, onSessionChange)
    requestHeaders.set('Authorization', `Bearer ${nextSession.accessToken}`)
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: requestHeaders,
    })
  }

  if (!response.ok) {
    throw new Error(`Failed to load asset: ${response.status}`)
  }

  return response.blob()
}
