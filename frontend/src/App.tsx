import {
  startTransition,
  useDeferredValue,
  useEffect,
  useRef,
  useState,
} from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import './App.css'
import { apiFetch, fetchAuthorizedBlob } from './api'
import type {
  Attachment,
  AuthResponse,
  ChatSummary,
  CurrentUser,
  GroupPrivacy,
  GroupSummary,
  Message,
  PagedResponse,
  PrivacyMode,
  PrivacySettings,
  SearchResult,
  Session,
  UserProfile,
} from './api'

type AuthMode = 'login' | 'register'
type ConversationKind = 'chat' | 'group'
type AppSection = 'profile' | 'chats' | 'global-search'

type ConversationState = {
  kind: ConversationKind
  id: number
  title: string
  peerId?: number
}

type ComposerState = {
  message: string
  attachmentKeys: string[]
}

type MessageEditState = {
  messageId: number
  text: string
}

type ChatListItem = {
  kind: ConversationKind
  id: number
  title: string
  subtitle: string
  peerId?: number
}

type PrivacyDraft = {
  mode: PrivacyMode
  userIds: number[]
}

type GlobalSearchTypeFilter = 'users' | 'groups'
type UserGenderFilter = 'ANY' | 'MALE' | 'FEMALE'

const SESSION_STORAGE_KEY = 'social-network-ui-session'

type HistoryStatePayload = {
  section: AppSection
  conversationKind?: ConversationKind
  conversationId?: number
}

function App() {
  const [session, setSession] = useState<Session | null>(() => {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  })
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [busy, setBusy] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)
  const [activeSection, setActiveSection] = useState<AppSection>('profile')

  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [registerForm, setRegisterForm] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  })

  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)
  const [privacySettings, setPrivacySettings] = useState<PrivacySettings | null>(null)
  const [privacyDrafts, setPrivacyDrafts] = useState<Record<string, PrivacyDraft>>({})

  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [composer, setComposer] = useState<ComposerState>({ message: '', attachmentKeys: [] })
  const [isAttachmentPopupOpen, setAttachmentPopupOpen] = useState(false)
  const [messageEditState, setMessageEditState] = useState<MessageEditState | null>(null)
  const [messageDeleteCandidate, setMessageDeleteCandidate] = useState<Message | null>(null)

  const [chats, setChats] = useState<ChatSummary[]>([])
  const [groups, setGroups] = useState<GroupSummary[]>([])
  const [ownedGroups, setOwnedGroups] = useState<GroupSummary[]>([])
  const [selectedConversation, setSelectedConversation] = useState<ConversationState | null>(null)
  const [messages, setMessages] = useState<Message[]>([])
  const [userNamesById, setUserNamesById] = useState<Record<number, string>>({})
  const [groupMembers, setGroupMembers] = useState<number[]>([])
  const [canOpenMembers, setCanOpenMembers] = useState(false)
  const [historyPage, setHistoryPage] = useState(1)
  const [historyHasNext, setHistoryHasNext] = useState(false)
  const historyRef = useRef<HTMLDivElement | null>(null)
  const [groupInvites, setGroupInvites] = useState<number[]>([])
  const [groupPrivacy, setGroupPrivacy] = useState<GroupPrivacy | null>(null)
  const [isSelectedGroupAdmin, setSelectedGroupAdmin] = useState(false)

  const [globalQuery, setGlobalQuery] = useState('')
  const [globalTypeFilter, setGlobalTypeFilter] = useState<GlobalSearchTypeFilter>('users')
  const [globalAgeFrom, setGlobalAgeFrom] = useState('')
  const [globalAgeTo, setGlobalAgeTo] = useState('')
  const [globalBirthDate, setGlobalBirthDate] = useState('')
  const [globalGender, setGlobalGender] = useState<UserGenderFilter>('ANY')
  const [globalCity, setGlobalCity] = useState('')
  const [chatQuery, setChatQuery] = useState('')
  const deferredChatQuery = useDeferredValue(chatQuery)
  const [globalUsers, setGlobalUsers] = useState<SearchResult[]>([])
  const [globalGroups, setGlobalGroups] = useState<SearchResult[]>([])
  const [localChats, setLocalChats] = useState<ChatSummary[]>([])

  const [groupTitle, setGroupTitle] = useState('')
  const [isCreateGroupPopupOpen, setCreateGroupPopupOpen] = useState(false)
  const [groupInviteQuery, setGroupInviteQuery] = useState('')
  const [groupInviteSearchResults, setGroupInviteSearchResults] = useState<SearchResult[]>([])
  const [groupInviteAgeFrom, setGroupInviteAgeFrom] = useState('')
  const [groupInviteAgeTo, setGroupInviteAgeTo] = useState('')
  const [groupInviteBirthDate, setGroupInviteBirthDate] = useState('')
  const [groupInviteGender, setGroupInviteGender] = useState<UserGenderFilter>('ANY')
  const [groupInviteCity, setGroupInviteCity] = useState('')
  const [isGroupInvitePopupOpen, setGroupInvitePopupOpen] = useState(false)
  const [isGroupInvitesPopupOpen, setGroupInvitesPopupOpen] = useState(false)
  const [isPrivacyUsersPopupOpen, setPrivacyUsersPopupOpen] = useState(false)
  const [privacyUsersPopupKey, setPrivacyUsersPopupKey] = useState<keyof PrivacySettings | null>(null)
  const [privacyUsersQuery, setPrivacyUsersQuery] = useState('')
  const [privacyUsersSearchResults, setPrivacyUsersSearchResults] = useState<SearchResult[]>([])
  const [privacyUsersAgeFrom, setPrivacyUsersAgeFrom] = useState('')
  const [privacyUsersAgeTo, setPrivacyUsersAgeTo] = useState('')
  const [privacyUsersBirthDate, setPrivacyUsersBirthDate] = useState('')
  const [privacyUsersGender, setPrivacyUsersGender] = useState<UserGenderFilter>('ANY')
  const [privacyUsersCity, setPrivacyUsersCity] = useState('')
  const [isGroupSettingsPopupOpen, setGroupSettingsPopupOpen] = useState(false)
  const [isGroupMembersPopupOpen, setGroupMembersPopupOpen] = useState(false)
  const [isGroupLeaveChoicePopupOpen, setGroupLeaveChoicePopupOpen] = useState(false)
  const [isGroupLeavePopupOpen, setGroupLeavePopupOpen] = useState(false)
  const [leaveDeleteMessages, setLeaveDeleteMessages] = useState(false)
  const [isChatClearPopupOpen, setChatClearPopupOpen] = useState(false)
  const [globalUserPreview, setGlobalUserPreview] = useState<UserProfile | null>(null)
  const [profileDraft, setProfileDraft] = useState({
    about: '',
    birthDate: '',
    city: '',
    gender: 'MALE' as 'MALE' | 'FEMALE',
  })
  const [isProfileDraftDirty, setProfileDraftDirty] = useState(false)
  const [privacyDirtyByKey, setPrivacyDirtyByKey] = useState<Record<keyof PrivacySettings, boolean>>({
    canMessageMe: false,
    canSeeInfo: false,
    canInviteMe: false,
  })
  const isProfileDraftDirtyRef = useRef(false)
  const privacyDirtyByKeyRef = useRef<Record<keyof PrivacySettings, boolean>>({
    canMessageMe: false,
    canSeeInfo: false,
    canInviteMe: false,
  })
  const hasAppliedInitialHistoryRef = useRef(false)

  const visibleChats = deferredChatQuery.trim() ? localChats : chats
  const normalizedChatQuery = deferredChatQuery.trim().toLowerCase()
  const chatListItems: ChatListItem[] = [
    ...visibleChats.map((chat) => ({
      kind: 'chat' as const,
      id: chat.id,
      title: chat.peer_name,
      subtitle: 'Chat',
      peerId: chat.peer_id,
    })),
    ...groups
      .filter((group) => !normalizedChatQuery
        || group.title.toLowerCase().includes(normalizedChatQuery)
        || String(group.id).includes(normalizedChatQuery))
      .map((group) => ({
        kind: 'group' as const,
        id: group.id,
        title: group.title,
        subtitle: 'Group',
      })),
  ]

  function clearConversation() {
    setSelectedConversation(null)
    setMessages([])
    setHistoryPage(1)
    setHistoryHasNext(false)
    setGroupInvites([])
    setGroupPrivacy(null)
    setGroupMembers([])
    setCanOpenMembers(false)
    setSelectedGroupAdmin(false)
    setComposer({ message: '', attachmentKeys: [] })
  }

  function buildHistoryUrl(state: HistoryStatePayload) {
    const params = new URLSearchParams()
    params.set('section', state.section)
    if (state.conversationKind) {
      params.set('kind', state.conversationKind)
    }
    if (state.conversationId) {
      params.set('id', String(state.conversationId))
    }
    return `${window.location.pathname}?${params.toString()}`
  }

  function writeHistoryState(state: HistoryStatePayload, replace = false) {
    const method = replace ? 'replaceState' : 'pushState'
    window.history[method](state, '', buildHistoryUrl(state))
  }

  function findConversation(kind: ConversationKind, id: number) {
    if (kind === 'chat') {
      const chat = chats.find((item) => item.id === id)
      return chat
        ? {
            kind: 'chat' as const,
            id: chat.id,
            title: chat.peer_name,
            peerId: chat.peer_id,
          }
        : null
    }

    const group = groups.find((item) => item.id === id)
    return group
      ? {
          kind: 'group' as const,
          id: group.id,
          title: group.title,
        }
      : null
  }

  async function applyHistoryState(state: HistoryStatePayload | null, replace = false) {
    const normalizedSection = window.location.search.includes('section=groups') ? 'chats' : state?.section
    const nextState = { ...(state ?? { section: 'profile' as const }), section: normalizedSection ?? 'profile' }
    setActiveSection(nextState.section)

    if (!nextState.conversationKind || !nextState.conversationId) {
      clearConversation()
      writeHistoryState(nextState, replace)
      return
    }

    const conversation = findConversation(nextState.conversationKind, nextState.conversationId)
    if (!conversation) {
      clearConversation()
      writeHistoryState({ section: nextState.section }, replace)
      return
    }

    await selectConversation(conversation, replace)
  }

  function openSection(section: AppSection, replace = false) {
    setActiveSection(section)
    if (section === 'chats' || section === 'profile' || section === 'global-search') {
      clearConversation()
    }
    writeHistoryState({ section }, replace)
  }

  async function bootstrap(showBusy = true) {
    try {
      if (showBusy) {
        setBusy(true)
        setErrorMessage(null)
      }
      const [me, privacy, userAttachments, myChats, myGroups, myOwnedGroups] = await Promise.all([
        request<CurrentUser>('/api/auth/me'),
        request<PrivacySettings>('/api/account/privacy'),
        request<Attachment[]>('/api/attachments'),
        request<ChatSummary[]>('/api/chats'),
        request<GroupSummary[]>('/api/groups'),
        request<GroupSummary[]>('/api/groups/owned'),
      ])

      setCurrentUser(me)
      if (!isProfileDraftDirtyRef.current) {
        setProfileDraft({
          about: me.about ?? '',
          birthDate: me.birthDate ?? '',
          city: me.city ?? '',
          gender: me.gender === 'FEMALE' ? 'FEMALE' : 'MALE',
        })
      }
      setPrivacySettings(privacy)
      setPrivacyDrafts((current) => ({
        canMessageMe: privacyDirtyByKeyRef.current.canMessageMe ? (current.canMessageMe ?? toDraft(privacy.canMessageMe)) : toDraft(privacy.canMessageMe),
        canSeeInfo: privacyDirtyByKeyRef.current.canSeeInfo ? (current.canSeeInfo ?? toDraft(privacy.canSeeInfo)) : toDraft(privacy.canSeeInfo),
        canInviteMe: privacyDirtyByKeyRef.current.canInviteMe ? (current.canInviteMe ?? toDraft(privacy.canInviteMe)) : toDraft(privacy.canInviteMe),
      }))
      setAttachments(userAttachments)
      setChats(myChats)
      setGroups(myGroups)
      setOwnedGroups(myOwnedGroups)
      setLocalChats(myChats)
    } catch (error) {
      handleError(error)
    } finally {
      if (showBusy) {
        setBusy(false)
      }
    }
  }

  async function request<T>(path: string, init?: RequestInit) {
    return apiFetch<T>(path, {
      ...init,
      session,
      onSessionChange: setSession,
    })
  }

  function formatUserName(profile: UserProfile) {
    const fullName = `${profile.first_name ?? ''} ${profile.last_name ?? ''}`.trim()
    return fullName || 'Unknown user'
  }

  function clearFeedback() {
    setErrorMessage(null)
    setSuccessMessage(null)
  }

  function closeAllPopups() {
    setAttachmentPopupOpen(false)
    setMessageDeleteCandidate(null)
    setGroupInvitePopupOpen(false)
    setGroupInvitesPopupOpen(false)
    setPrivacyUsersPopupOpen(false)
    setPrivacyUsersPopupKey(null)
    setGroupSettingsPopupOpen(false)
    setGroupMembersPopupOpen(false)
    setGroupLeaveChoicePopupOpen(false)
    setGroupLeavePopupOpen(false)
    setChatClearPopupOpen(false)
    setGlobalUserPreview(null)
    setCreateGroupPopupOpen(false)
    setErrorMessage(null)
    setSuccessMessage(null)
  }

  function resetWorkspace() {
    setCurrentUser(null)
    setPrivacySettings(null)
    setChats([])
    setGroups([])
    setOwnedGroups([])
    setLocalChats([])
    setMessages([])
    setAttachments([])
    clearConversation()
  }

  function handleError(error: unknown) {
    const message = error instanceof Error ? error.message : 'Unexpected error'
    setErrorMessage(message)
    setSuccessMessage(null)
  }

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    clearFeedback()
    setBusy(true)
    try {
      const payload =
        authMode === 'login'
          ? {
              email: loginEmail,
              password: loginPassword,
            }
          : {
              email: registerForm.email,
              password: registerForm.password,
              firstName: registerForm.firstName,
              lastName: registerForm.lastName,
            }
      const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/register'
      const auth = await apiFetch<AuthResponse>(endpoint, {
        method: 'POST',
        body: JSON.stringify(payload),
        session: null,
        onSessionChange: setSession,
      })
      setSession({
        accessToken: auth.accessToken,
        refreshToken: auth.refreshToken,
      })
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function logout() {
    if (!session) {
      return
    }
    try {
      await request('/api/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ refreshToken: session.refreshToken }),
      })
    } catch {
      // ignore remote logout failure and clear local session anyway
    }
    resetWorkspace()
    setSession(null)
  }

  async function savePrivacy(key: keyof PrivacySettings) {
    if (!privacySettings) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const draft = privacyDrafts[key]
      const payload = {
        [key]: {
          mode: draft.mode,
          userIds: draft.userIds,
        },
      }
      const next = await request<PrivacySettings>('/api/account/privacy', {
        method: 'PATCH',
        body: JSON.stringify(payload),
      })
      setPrivacySettings(next)
      setPrivacyDirtyByKey((current) => ({ ...current, [key]: false }))
      setPrivacyDrafts((current) => ({
        ...current,
        [key]: toDraft(next[key]),
      }))
      setSuccessMessage(`${privacyLabel(key)} saved`)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  function openPrivacyUsersPopup(key: keyof PrivacySettings) {
    setPrivacyUsersPopupKey(key)
    setPrivacyUsersPopupOpen(true)
    setPrivacyUsersSearchResults([])
    setPrivacyUsersQuery('')
    setPrivacyUsersAgeFrom('')
    setPrivacyUsersAgeTo('')
    setPrivacyUsersBirthDate('')
    setPrivacyUsersGender('ANY')
    setPrivacyUsersCity('')
  }

  function closePrivacyUsersPopup() {
    setPrivacyUsersPopupOpen(false)
    setPrivacyUsersPopupKey(null)
    setPrivacyUsersSearchResults([])
  }

  async function searchPrivacyUsers() {
    const trimmed = privacyUsersQuery.trim()
    if (!trimmed) {
      setPrivacyUsersSearchResults([])
      return
    }
    clearFeedback()
    try {
      const filters = `&ageFrom=${encodeURIComponent(privacyUsersAgeFrom.trim())}&ageTo=${encodeURIComponent(privacyUsersAgeTo.trim())}&birthDate=${encodeURIComponent(privacyUsersBirthDate.trim())}&city=${encodeURIComponent(privacyUsersCity.trim())}&gender=${encodeURIComponent(privacyUsersGender === 'ANY' ? '' : privacyUsersGender)}`
      const result = await request<PagedResponse<SearchResult>>(
        `/api/search/users?query=${encodeURIComponent(trimmed)}&limit=8&page=1${filters}`,
      )
      setPrivacyUsersSearchResults(
        result.items.filter((item) => item.id !== (currentUser?.id ?? -1)),
      )
    } catch (error) {
      handleError(error)
    }
  }

  function togglePrivacyUserId(key: keyof PrivacySettings, userId: number) {
    if (userId === currentUser?.id) {
      return
    }
    setPrivacyDirtyByKey((current) => ({ ...current, [key]: true }))
    setPrivacyDrafts((current) => {
      const existing = current[key] ?? { mode: 'ALL_EXCEPT_SELECTED', userIds: [] }
      const alreadySelected = existing.userIds.includes(userId)
      return {
        ...current,
        [key]: {
          ...existing,
          userIds: alreadySelected
            ? existing.userIds.filter((item) => item !== userId)
            : [...existing.userIds, userId],
        },
      }
    })
    const matchedUser = privacyUsersSearchResults.find((item) => item.id === userId)
    if (matchedUser) {
      setUserNamesById((current) => ({
        ...current,
        [userId]: matchedUser.title,
      }))
    }
  }

  async function uploadAttachments(event: ChangeEvent<HTMLInputElement>) {
    const files = event.target.files
    if (!files?.length) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const formData = new FormData()
      Array.from(files).forEach((file) => formData.append('files', file))
      const uploaded = await request<Attachment[]>('/api/attachments?type=photo', {
        method: 'POST',
        body: formData,
      })
      setAttachments((current) => [...uploaded, ...current])
      setComposer((current) => ({
        ...current,
        attachmentKeys: [...current.attachmentKeys, ...uploaded.map((item) => item.attachment_key)],
      }))
    } catch (error) {
      handleError(error)
    } finally {
      event.target.value = ''
      setBusy(false)
    }
  }

  async function deleteAttachment(attachmentKey: string) {
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/attachments', {
        method: 'DELETE',
        body: JSON.stringify({ attachmentKey }),
      })
      setAttachments((current) => current.filter((item) => item.attachment_key !== attachmentKey))
      setComposer((current) => ({
        ...current,
        attachmentKeys: current.attachmentKeys.filter((item) => item !== attachmentKey),
      }))
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  function validateGlobalFilters(typeFilter: GlobalSearchTypeFilter) {
    if (typeFilter === 'users') {
      const fromRaw = globalAgeFrom.trim()
      const toRaw = globalAgeTo.trim()
      if ((fromRaw !== '' && !/^\d+$/.test(fromRaw)) || (toRaw !== '' && !/^\d+$/.test(toRaw))) {
        setErrorMessage('Age filters must contain only digits')
        return false
      }
      const from = fromRaw === '' ? null : Number(fromRaw)
      const to = toRaw === '' ? null : Number(toRaw)
      if ((from !== null && (!Number.isFinite(from) || from < 0))
        || (to !== null && (!Number.isFinite(to) || to < 0))) {
        setErrorMessage('Age filters must be non-negative numbers')
        return false
      }
      if (from !== null && to !== null && from > to) {
        setErrorMessage('Age from must be less than or equal to age to')
        return false
      }
      if (globalBirthDate.trim()) {
        const birth = new Date(globalBirthDate.trim())
        if (!Number.isNaN(birth.getTime())) {
          const now = new Date()
          let age = now.getFullYear() - birth.getFullYear()
          const monthDiff = now.getMonth() - birth.getMonth()
          if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < birth.getDate())) {
            age -= 1
          }
          if ((from !== null && age < from) || (to !== null && age > to)) {
            setErrorMessage('Birth date conflicts with age range')
            return false
          }
        }
      }
    }
    return true
  }

  async function searchGlobal(query: string, typeFilter: GlobalSearchTypeFilter) {
    if (!validateGlobalFilters(typeFilter)) {
      return
    }
    const trimmed = query.trim()
    if (!trimmed) {
      startTransition(() => {
        setGlobalUsers([])
        setGlobalGroups([])
      })
      return
    }
    try {
      const baseParams = `query=${encodeURIComponent(trimmed)}&limit=8&page=1`
      const userFilters = typeFilter === 'users'
        ? `&ageFrom=${encodeURIComponent(globalAgeFrom.trim())}&ageTo=${encodeURIComponent(globalAgeTo.trim())}&birthDate=${encodeURIComponent(globalBirthDate.trim())}&city=${encodeURIComponent(globalCity.trim())}&gender=${encodeURIComponent(globalGender === 'ANY' ? '' : globalGender)}`
        : ''
      const emptyPagedResult: PagedResponse<SearchResult> = {
        items: [],
        limit: 8,
        offset: 0,
        page: 1,
        total: 0,
        hasNext: false,
        hasPrevious: false,
      }
      const usersPromise = typeFilter === 'groups'
        ? Promise.resolve(emptyPagedResult)
        : request<PagedResponse<SearchResult>>(`/api/search/users?${baseParams}${userFilters}`)
      const groupsPromise = typeFilter === 'users'
        ? Promise.resolve(emptyPagedResult)
        : request<PagedResponse<SearchResult>>(`/api/search/groups?${baseParams}`)
      const [users, groupsResult] = await Promise.all([usersPromise, groupsPromise])
      startTransition(() => {
        setGlobalUsers(users.items)
        setGlobalGroups(groupsResult.items)
      })
    } catch (error) {
      handleError(error)
    }
  }

  function runGlobalSearch() {
    clearFeedback()
    if (!validateGlobalFilters(globalTypeFilter)) {
      return
    }
    void searchGlobal(globalQuery, globalTypeFilter)
  }

  async function searchChats(query: string) {
    const trimmed = query.trim()
    if (!trimmed) {
      setLocalChats(chats)
      return
    }
    try {
      const results = await request<ChatSummary[]>(`/api/chats/search?query=${encodeURIComponent(trimmed)}`)
      startTransition(() => setLocalChats(results))
    } catch (error) {
      handleError(error)
    }
  }

  async function openChat(peerId: number, title: string) {
    clearFeedback()
    setBusy(true)
    try {
      const chat = await request<ChatSummary>('/api/chats/open', {
        method: 'POST',
        body: JSON.stringify({ peer_id: peerId }),
      })
      await refreshChats()
      openSection('chats')
      await selectConversation({
        kind: 'chat',
        id: chat.id,
        title,
        peerId,
      }, false)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function previewGlobalUser(userId: number) {
    clearFeedback()
    try {
      const profile = await request<UserProfile>(`/api/users/${userId}`)
      setGlobalUserPreview(profile)
    } catch (error) {
      handleError(error)
    }
  }

  async function refreshChats() {
    const freshChats = await request<ChatSummary[]>('/api/chats')
    setChats(freshChats)
    setLocalChats(freshChats)
  }

  async function refreshGroups() {
    const [freshGroups, freshOwnedGroups] = await Promise.all([
      request<GroupSummary[]>('/api/groups'),
      request<GroupSummary[]>('/api/groups/owned'),
    ])
    setGroups(freshGroups)
    setOwnedGroups(freshOwnedGroups)
  }

  async function selectConversation(next: ConversationState, replace = false) {
    setSelectedConversation(next)
    setMessages([])
    setHistoryPage(1)
    await loadHistory(next, 1, true)
    if (next.kind === 'group') {
      setActiveSection('chats')
      await loadGroupDetails(next.id)
    } else {
      setActiveSection('chats')
      setGroupMembers([])
      setCanOpenMembers(false)
      setGroupInvites([])
      setGroupPrivacy(null)
    }
    writeHistoryState({
      section: 'chats',
      conversationKind: next.kind,
      conversationId: next.id,
    }, replace)
  }

  async function loadHistory(conversation: ConversationState, page: number, replace: boolean) {
    try {
      const params =
        conversation.kind === 'chat'
          ? `chat_id=${conversation.id}`
          : `group_id=${conversation.id}`
      const response = await request<PagedResponse<Message>>(
        `/api/messages/history?${params}&limit=20&page=${page}`,
      )
      const ordered = [...response.items].reverse()
      setHistoryHasNext(response.hasNext)
      setHistoryPage(page)
      setMessages((current) => (replace ? ordered : [...ordered, ...current]))
    } catch (error) {
      handleError(error)
    }
  }

  async function loadGroupDetails(groupId: number) {
    try {
      const members = await request<PagedResponse<number>>(`/api/groups/members?group_id=${groupId}&limit=20&page=1`)
      setGroupMembers(members.items)
      setCanOpenMembers(true)
    } catch {
      setGroupMembers([])
      setCanOpenMembers(false)
    }

    let admin = false
    try {
      const invites = await request<number[]>(`/api/groups/invites?group_id=${groupId}`)
      setGroupInvites(invites)
      admin = true
    } catch {
      setGroupInvites([])
    }

    try {
      const privacy = await request<GroupPrivacy>(`/api/groups/privacy?group_id=${groupId}`)
      setGroupPrivacy(privacy)
      admin = true
    } catch {
      setGroupPrivacy(null)
    }
    setSelectedGroupAdmin(admin)
  }

  async function handleHistoryScroll() {
    const container = historyRef.current
    if (!container || !selectedConversation || !historyHasNext) {
      return
    }
    if (container.scrollTop > 40) {
      return
    }
    const nextPage = historyPage + 1
    const previousHeight = container.scrollHeight
    await loadHistory(selectedConversation, nextPage, false)
    requestAnimationFrame(() => {
      if (historyRef.current) {
        const newHeight = historyRef.current.scrollHeight
        historyRef.current.scrollTop = newHeight - previousHeight
      }
    })
  }

  async function sendMessage() {
    if (!selectedConversation) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const payload =
        selectedConversation.kind === 'chat'
          ? {
              chat_id: selectedConversation.id,
              message: composer.message,
              attachments: composer.attachmentKeys,
            }
          : {
              group_id: selectedConversation.id,
              message: composer.message,
              attachments: composer.attachmentKeys,
            }
      await request<Message>('/api/messages/send', {
        method: 'POST',
        body: JSON.stringify(payload),
      })
      setComposer({ message: '', attachmentKeys: [] })
      await loadHistory(selectedConversation, 1, true)
      if (selectedConversation.kind === 'chat') {
        await refreshChats()
      }
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  function startMessageEdit(message: Message) {
    setMessageEditState({
      messageId: message.id,
      text: message.message ?? '',
    })
  }

  async function commitMessageEdit(message: Message) {
    if (!selectedConversation) {
      return
    }
    if (!messageEditState || messageEditState.messageId !== message.id) {
      return
    }
    const nextText = messageEditState.text
    clearFeedback()
    setBusy(true)
    try {
      const payload =
        selectedConversation.kind === 'chat'
          ? {
              chat_id: selectedConversation.id,
              message_id: message.id,
              new_message: nextText,
              new_attachments: message.attachments.map((item) => item.attachment_key),
            }
          : {
              group_id: selectedConversation.id,
              message_id: message.id,
              new_message: nextText,
              new_attachments: message.attachments.map((item) => item.attachment_key),
            }
      await request<Message>('/api/messages/edit', {
        method: 'PATCH',
        body: JSON.stringify(payload),
      })
      await loadHistory(selectedConversation, 1, true)
      setMessageEditState(null)
      if (selectedConversation.kind === 'chat') {
        await refreshChats()
      }
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function deleteMessage(message: Message) {
    if (!selectedConversation) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const payload =
        selectedConversation.kind === 'chat'
          ? {
              chat_id: selectedConversation.id,
              message_id: message.id,
            }
          : {
              group_id: selectedConversation.id,
              message_id: message.id,
            }
      await request('/api/messages/delete', {
        method: 'DELETE',
        body: JSON.stringify(payload),
      })
      await loadHistory(selectedConversation, 1, true)
      if (selectedConversation.kind === 'chat') {
        await refreshChats()
      }
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function confirmDeleteMessage() {
    if (!messageDeleteCandidate) {
      return
    }
    await deleteMessage(messageDeleteCandidate)
    setMessageDeleteCandidate(null)
  }

  function toggleComposerAttachment(attachmentKey: string) {
    setComposer((current) => {
      const exists = current.attachmentKeys.includes(attachmentKey)
      return {
        ...current,
        attachmentKeys: exists
          ? current.attachmentKeys.filter((item) => item !== attachmentKey)
          : [...current.attachmentKeys, attachmentKey],
      }
    })
  }

  async function createGroup() {
    if (!groupTitle.trim()) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const group = await request<GroupSummary>('/api/groups/create', {
        method: 'POST',
        body: JSON.stringify({ title: groupTitle.trim() }),
      })
      setGroupTitle('')
      await refreshGroups()
      await selectConversation({
        kind: 'group',
        id: group.id,
        title: group.title,
      }, false)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function searchGroupUsersForInvite() {
    const trimmed = groupInviteQuery.trim()
    if (!trimmed) {
      setGroupInviteSearchResults([])
      return
    }
    clearFeedback()
    try {
      const filters = `&ageFrom=${encodeURIComponent(groupInviteAgeFrom.trim())}&ageTo=${encodeURIComponent(groupInviteAgeTo.trim())}&birthDate=${encodeURIComponent(groupInviteBirthDate.trim())}&city=${encodeURIComponent(groupInviteCity.trim())}&gender=${encodeURIComponent(groupInviteGender === 'ANY' ? '' : groupInviteGender)}`
      const result = await request<PagedResponse<SearchResult>>(
        `/api/search/users?query=${encodeURIComponent(trimmed)}&limit=8&page=1${filters}`,
      )
      setGroupInviteSearchResults(result.items)
    } catch (error) {
      handleError(error)
    }
  }

  async function inviteApplicantToSelectedGroup(userId: number) {
    if (!selectedConversation || selectedConversation.kind !== 'group') {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      await request<number[]>('/api/groups/invite', {
        method: 'POST',
        body: JSON.stringify({
          group_id: selectedConversation.id,
          user_ids: [userId],
        }),
      })
      setGroupInviteSearchResults((current) => current.filter((item) => item.id !== userId))
      await loadGroupDetails(selectedConversation.id)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function kickMemberFromSelectedGroup(userId: number) {
    if (!selectedConversation || selectedConversation.kind !== 'group') {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/groups/kick', {
        method: 'POST',
        body: JSON.stringify({
          group_id: selectedConversation.id,
          user_id: userId,
          delete_messages: false,
        }),
      })
      await loadGroupDetails(selectedConversation.id)
      await loadHistory(selectedConversation, 1, true)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function applyToGroupById(groupId: number) {
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/groups/apply', {
        method: 'POST',
        body: JSON.stringify({ group_id: groupId }),
      })
      setSuccessMessage('Request sent')
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function saveGroupPrivacy() {
    if (!selectedConversation || selectedConversation.kind !== 'group' || !groupPrivacy) {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      const next = await request<GroupPrivacy>('/api/groups/privacy', {
        method: 'PATCH',
        body: JSON.stringify({
          group_id: selectedConversation.id,
          membersVisible: groupPrivacy.membersVisible,
          canUsersInvite: groupPrivacy.canUsersInvite,
        }),
      })
      setGroupPrivacy(next)
      setGroupSettingsPopupOpen(false)
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function leaveSelectedGroup() {
    if (!selectedConversation || selectedConversation.kind !== 'group') {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/groups/leave', {
        method: 'POST',
        body: JSON.stringify({
          group_id: selectedConversation.id,
          delete_messages: leaveDeleteMessages,
        }),
      })
      setGroupLeavePopupOpen(false)
      setLeaveDeleteMessages(false)
      clearConversation()
      await refreshGroups()
      writeHistoryState({ section: 'chats' }, false)
      setActiveSection('chats')
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function deleteSelectedGroup() {
    if (!selectedConversation || selectedConversation.kind !== 'group') {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/groups/delete', {
        method: 'POST',
        body: JSON.stringify({ group_id: selectedConversation.id }),
      })
      setGroupLeaveChoicePopupOpen(false)
      clearConversation()
      await refreshGroups()
      writeHistoryState({ section: 'chats' }, false)
      setActiveSection('chats')
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  async function clearSelectedChat(mode: 'mine' | 'all') {
    if (!selectedConversation || selectedConversation.kind !== 'chat') {
      return
    }
    clearFeedback()
    setBusy(true)
    try {
      await request('/api/chats', {
        method: 'DELETE',
        body: JSON.stringify({
          chat_id: selectedConversation.id,
          mine: true,
          yours: mode === 'all',
        }),
      })
      setChatClearPopupOpen(false)
      clearConversation()
      await refreshChats()
      writeHistoryState({ section: 'chats' }, false)
      setActiveSection('chats')
    } catch (error) {
      handleError(error)
    } finally {
      setBusy(false)
    }
  }

  function renderConversationCard(hideHeader = false) {
    const currentUserId = currentUser?.id ?? -1
    const selectedAttachments = attachments.filter((attachment) =>
      composer.attachmentKeys.includes(attachment.attachment_key),
    )
    const visibleSelectedAttachments = selectedAttachments.slice(-3)
    return (
      <section className="conversation-card">
        {!hideHeader ? (
          <div className="card-head">
            <div>
              <h2>{selectedConversation?.title ?? 'Pick a chat or group'}</h2>
            </div>
          </div>
        ) : null}
        <div className="history" onScroll={() => void handleHistoryScroll()} ref={historyRef}>
          {messages.map((message) => (
            <article
              className={`message-card ${message.from_id === currentUserId ? 'mine' : ''}`}
              key={message.id}
            >
              <header>
                <strong>{message.from_id === currentUserId ? 'You' : (userNamesById[message.from_id] ?? 'Unknown user')}</strong>
                <time>{new Date(message.date).toLocaleString()}</time>
              </header>
              {messageEditState?.messageId === message.id ? (
                <div className="message-edit">
                  <textarea
                    rows={3}
                    value={messageEditState.text}
                    onChange={(event) =>
                      setMessageEditState((current) =>
                        current ? { ...current, text: event.target.value } : current,
                      )
                    }
                  />
                  <div className="message-actions">
                    <button className="primary-button message-action-button" onClick={() => void commitMessageEdit(message)} type="button">
                      Save
                    </button>
                    <button className="ghost-button message-action-button" onClick={() => setMessageEditState(null)} type="button">
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                message.message ? <p>{message.message}</p> : null
              )}
              {message.attachments.length ? (
                <div className="message-attachments">
                  {message.attachments.map((attachment) => (
                    <AttachmentPreview
                      attachment={attachment}
                      className="message-image"
                      onDelete={undefined}
                      onSessionChange={setSession}
                      session={session}
                      key={attachment.attachment_key}
                    />
                  ))}
                </div>
              ) : null}
              {message.from_id === currentUserId && messageEditState?.messageId !== message.id ? (
                <div className="message-actions">
                  <button className="ghost-button message-action-button" onClick={() => startMessageEdit(message)} type="button">
                    Edit
                  </button>
                  <button className="ghost-button message-action-button" onClick={() => setMessageDeleteCandidate(message)} type="button">
                    Delete
                  </button>
                </div>
              ) : null}
            </article>
          ))}
        </div>
        <div className="composer">
          {selectedAttachments.length ? (
            <div className="attachment-list">
              {visibleSelectedAttachments.map((attachment) => (
                <AttachmentPreview
                  attachment={attachment}
                  className="composer-image"
                  key={attachment.attachment_key}
                  onDelete={() => void deleteAttachment(attachment.attachment_key)}
                  onSessionChange={setSession}
                  session={session}
                />
              ))}
            </div>
          ) : null}
          <div className="composer-row">
            <button className="upload-button plus-button" disabled={!selectedConversation || busy} onClick={() => setAttachmentPopupOpen(true)} type="button">+</button>
            <input
              className="composer-input"
              placeholder="Write a message"
              value={composer.message}
              onChange={(event) => setComposer((current) => ({ ...current, message: event.target.value }))}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  if (!busy && selectedConversation) {
                    void sendMessage()
                  }
                }
              }}
            />
            <button className="primary-button composer-send" disabled={!selectedConversation || busy} onClick={() => void sendMessage()} type="button">
              Send
            </button>
          </div>
        </div>
        {isAttachmentPopupOpen ? (
          <div className="attachments-popup-backdrop" onClick={() => setAttachmentPopupOpen(false)} role="presentation">
            <section className="attachments-popup" onClick={(event) => event.stopPropagation()}>
              <label className="upload-button popup-upload-button">
                <input
                  accept="image/*"
                  disabled={busy}
                  multiple
                  onChange={(event) => void uploadAttachments(event)}
                  type="file"
                />
                <span>Upload file</span>
              </label>
              <div className="popup-attachments-list">
                {attachments.map((attachment) => {
                  const isSelected = composer.attachmentKeys.includes(attachment.attachment_key)
                  return (
                    <div className={`popup-attachment-item ${isSelected ? 'is-selected' : ''}`} key={attachment.attachment_key}>
                      <button className="popup-attachment-preview" onClick={() => toggleComposerAttachment(attachment.attachment_key)} type="button">
                        <AttachmentPreview
                          attachment={attachment}
                          className="composer-image"
                          onDelete={undefined}
                          onSessionChange={setSession}
                          session={session}
                        />
                      </button>
                      <button className="ghost-button popup-delete" onClick={() => void deleteAttachment(attachment.attachment_key)} type="button">
                        Delete
                      </button>
                    </div>
                  )
                })}
              </div>
            </section>
          </div>
        ) : null}
      </section>
    )
  }

  function renderProfileSection() {
    return (
      <section className="section-grid">
        <section className="card stack">
          <div className="card-head">
            <h2>Profile</h2>
          </div>
          <p className="profile-email">{currentUser?.email ?? ''}</p>
          <div className="privacy-block">
            <label>
              <span>Bio</span>
              <textarea
                className="profile-bio-input"
                placeholder="A few words about you"
                rows={4}
                value={profileDraft.about}
                onChange={(event) => {
                  setProfileDraftDirty(true)
                  setProfileDraft((current) => ({ ...current, about: event.target.value }))
                }}
              />
            </label>
            <label>
              <span>Birthday</span>
              <input
                type="date"
                value={profileDraft.birthDate}
                onChange={(event) => {
                  setProfileDraftDirty(true)
                  setProfileDraft((current) => ({ ...current, birthDate: event.target.value }))
                }}
              />
            </label>
            <label>
              <span>City</span>
              <input
                placeholder="Your city"
                value={profileDraft.city}
                onChange={(event) => {
                  setProfileDraftDirty(true)
                  setProfileDraft((current) => ({ ...current, city: event.target.value }))
                }}
              />
            </label>
            <label>
              <span>Gender</span>
              <select
                value={profileDraft.gender}
                onChange={(event) => {
                  setProfileDraftDirty(true)
                  setProfileDraft((current) => ({ ...current, gender: event.target.value as 'MALE' | 'FEMALE' }))
                }}
              >
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
            </label>
            <button
              className="ghost-button"
              onClick={() =>
                void (async () => {
                  clearFeedback()
                  setBusy(true)
                  try {
                    await request<UserProfile>('/api/account/profile', {
                      method: 'PATCH',
                      body: JSON.stringify({
                        about: profileDraft.about,
                        birthDate: profileDraft.birthDate || null,
                        city: profileDraft.city,
                        gender: profileDraft.gender,
                      }),
                    })
                    setProfileDraftDirty(false)
                    await bootstrap()
                    setSuccessMessage('Profile saved')
                  } catch (error) {
                    handleError(error)
                  } finally {
                    setBusy(false)
                  }
                })()
              }
              type="button"
            >
              Save
            </button>
          </div>
        </section>
        <section className="card stack">
          <div className="card-head">
            <h2>Privacy</h2>
          </div>
          {(['canMessageMe', 'canInviteMe'] as const).map((key) => (
            <div className="privacy-block" key={key}>
              <h3>{privacyLabel(key)}</h3>
              <select
                value={privacyDrafts[key]?.mode ?? 'ALL_EXCEPT_SELECTED'}
                onChange={(event) => {
                  setPrivacyDirtyByKey((current) => ({ ...current, [key]: true }))
                  setPrivacyDrafts((current) => ({
                    ...current,
                    [key]: {
                      ...(current[key] ?? { userIds: [] }),
                      mode: event.target.value as PrivacyMode,
                    },
                  }))
                }}
              >
                <option value="NOBODY">Nobody</option>
                <option value="ONLY_SELECTED">Only selected</option>
                <option value="ALL_EXCEPT_SELECTED">All except selected</option>
              </select>
              <div className="privacy-selected-users">
                {(privacyDrafts[key]?.userIds ?? []).length ? (
                  (privacyDrafts[key]?.userIds ?? []).map((userId) => (
                    <button
                      className="popup-row popup-row-button"
                      key={`privacy-user-${key}-${userId}`}
                      onClick={() => togglePrivacyUserId(key, userId)}
                      type="button"
                    >
                      <strong>{userNamesById[userId] ?? 'Unknown user'}</strong>
                      <span>Tap to remove</span>
                    </button>
                  ))
                ) : (
                  <span className="privacy-users-empty">No users selected</span>
                )}
              </div>
              <button
                className="ghost-button"
                disabled={(privacyDrafts[key]?.mode ?? 'ALL_EXCEPT_SELECTED') === 'NOBODY'}
                onClick={() => openPrivacyUsersPopup(key)}
                type="button"
              >
                Select users
              </button>
              <button className="ghost-button" onClick={() => void savePrivacy(key)} type="button">
                Save
              </button>
            </div>
          ))}
        </section>
      </section>
    )
  }

  function renderChatsSection() {
    const isConversationOpen = selectedConversation && (selectedConversation.kind === 'chat' || selectedConversation.kind === 'group')
    return (
      <section className="section-stack">
        {isConversationOpen ? (
          selectedConversation?.kind === 'group' ? (
            <section className="section-stack">
              <section className="card stack">
                <div className="conversation-toolbar">
                  <h2>{selectedConversation.title}</h2>
                  <div className="toolbar-actions">
                    <button className="ghost-button" disabled={busy || (!isSelectedGroupAdmin && groupPrivacy?.canUsersInvite === false)} onClick={() => setGroupInvitePopupOpen(true)} type="button">Invite</button>
                    {(isSelectedGroupAdmin || canOpenMembers) ? <button className="ghost-button" onClick={() => setGroupMembersPopupOpen(true)} type="button">Members</button> : null}
                    {isSelectedGroupAdmin ? <button className="ghost-button" onClick={() => setGroupInvitesPopupOpen(true)} type="button">Join Requests</button> : null}
                    {isSelectedGroupAdmin ? <button className="ghost-button" onClick={() => setGroupSettingsPopupOpen(true)} type="button">Settings</button> : null}
                    <button className="ghost-button danger-button" onClick={() => setGroupLeaveChoicePopupOpen(true)} type="button">Leave</button>
                  </div>
                </div>
              </section>
              {renderConversationCard(true)}
            </section>
          ) : (
            <section className="section-stack">
              <section className="card stack">
                <div className="conversation-toolbar">
                  <h2>{selectedConversation?.title}</h2>
                  <div className="toolbar-actions">
                    <button className="ghost-button danger-button" onClick={() => setChatClearPopupOpen(true)} type="button">Clear</button>
                  </div>
                </div>
              </section>
              {renderConversationCard(true)}
            </section>
          )
        ) : (
          <section className="card stack">
            <div className="chat-search-row">
              <input
                placeholder="Search in started chats"
                value={chatQuery}
                onChange={(event) => setChatQuery(event.target.value)}
              />
              <button className="primary-button chat-create-group-button" onClick={() => setCreateGroupPopupOpen(true)} type="button">Create Group</button>
            </div>
            <div className="list-column tall-list">
              {chatListItems.map((item) => (
                <button
                  className={`list-item ${item.kind === 'group' ? 'is-group' : 'is-chat'}`}
                  key={`${item.kind}-${item.id}`}
                  onClick={() =>
                    void selectConversation({
                      kind: item.kind,
                      id: item.id,
                      title: item.title,
                      peerId: item.peerId,
                    })
                  }
                    type="button"
                >
                  <strong>{item.title}</strong>
                  <span>{item.subtitle}</span>
                </button>
              ))}
            </div>
          </section>
        )}
        {isGroupInvitePopupOpen && selectedConversation?.kind === 'group' ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupInvitePopupOpen(false)} role="presentation">
            <section className="attachments-popup group-invite-popup" onClick={(event) => event.stopPropagation()}>
              <div className="group-invite-search-row">
                <input
                  placeholder="Search users"
                  value={groupInviteQuery}
                  onChange={(event) => setGroupInviteQuery(event.target.value)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                />
                <button className="primary-button" disabled={busy || !groupInviteQuery.trim()} onClick={() => void searchGroupUsersForInvite()} type="button">
                  Search
                </button>
              </div>
              <div className="global-search-filters">
                <input
                  inputMode="numeric"
                  placeholder="Age from"
                  value={groupInviteAgeFrom}
                  onChange={(event) => setGroupInviteAgeFrom(event.target.value)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                />
                <input
                  inputMode="numeric"
                  placeholder="Age to"
                  value={groupInviteAgeTo}
                  onChange={(event) => setGroupInviteAgeTo(event.target.value)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                />
                <input
                  type="date"
                  value={groupInviteBirthDate}
                  onChange={(event) => setGroupInviteBirthDate(event.target.value)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                />
                <select
                  value={groupInviteGender}
                  onChange={(event) => setGroupInviteGender(event.target.value as UserGenderFilter)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                >
                  <option value="ANY">All</option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                </select>
                <input
                  placeholder="City"
                  value={groupInviteCity}
                  onChange={(event) => setGroupInviteCity(event.target.value)}
                  onKeyDown={(event) => event.key === 'Enter' && void searchGroupUsersForInvite()}
                />
              </div>
              <div className="list-column">
                {groupInviteSearchResults.map((user) => (
                  <button className="popup-row popup-row-button" key={`invite-search-${user.id}`} onClick={() => void inviteApplicantToSelectedGroup(user.id)} type="button">
                    <strong>{user.title}</strong>
                    <span>User</span>
                  </button>
                ))}
              </div>
            </section>
          </div>
        ) : null}
        {isGroupInvitesPopupOpen && selectedConversation?.kind === 'group' ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupInvitesPopupOpen(false)} role="presentation">
            <section className="attachments-popup" onClick={(event) => event.stopPropagation()}>
              <div className="list-column">
                {groupInvites.map((userId) => (
                  <button className="popup-row popup-row-button" key={`invite-${userId}`} onClick={() => void inviteApplicantToSelectedGroup(userId)} type="button">
                    <strong>{userNamesById[userId] ?? `User ${userId}`}</strong>
                    <span>User</span>
                  </button>
                ))}
              </div>
            </section>
          </div>
        ) : null}
        {isGroupSettingsPopupOpen && selectedConversation?.kind === 'group' && isSelectedGroupAdmin ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupSettingsPopupOpen(false)} role="presentation">
            <section className="attachments-popup" onClick={(event) => event.stopPropagation()}>
              <label className="toggle-row">
                <span>Members visible</span>
                <input
                  checked={groupPrivacy?.membersVisible ?? false}
                  onChange={(event) =>
                    setGroupPrivacy((current) =>
                      current
                        ? { ...current, membersVisible: event.target.checked }
                        : { membersVisible: event.target.checked, canUsersInvite: false },
                    )
                  }
                  type="checkbox"
                />
              </label>
              <label className="toggle-row">
                <span>Users can invite</span>
                <input
                  checked={groupPrivacy?.canUsersInvite ?? false}
                  onChange={(event) =>
                    setGroupPrivacy((current) =>
                      current
                        ? { ...current, canUsersInvite: event.target.checked }
                        : { membersVisible: false, canUsersInvite: event.target.checked },
                    )
                  }
                  type="checkbox"
                />
              </label>
              <button className="primary-button" onClick={() => void saveGroupPrivacy()} type="button">Save</button>
            </section>
          </div>
        ) : null}
        {isGroupMembersPopupOpen && selectedConversation?.kind === 'group' ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupMembersPopupOpen(false)} role="presentation">
            <section className="attachments-popup" onClick={(event) => event.stopPropagation()}>
              <div className="list-column">
                {groupMembers.map((userId) => (
                  <div className="popup-row popup-row-member" key={`member-${userId}`}>
                    <div className="popup-row-text">
                      <strong>{userNamesById[userId] ?? `User ${userId}`}</strong>
                      <span>User</span>
                    </div>
                    {isSelectedGroupAdmin && currentUser?.id !== userId ? (
                      <button className="ghost-button" disabled={busy} onClick={() => void kickMemberFromSelectedGroup(userId)} type="button">Kick</button>
                    ) : null}
                  </div>
                ))}
              </div>
            </section>
          </div>
        ) : null}
        {isGroupLeaveChoicePopupOpen && selectedConversation?.kind === 'group' ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupLeaveChoicePopupOpen(false)} role="presentation">
            <section className="attachments-popup group-delete-popup" onClick={(event) => event.stopPropagation()}>
              <h3>Delete group?</h3>
              <div className="message-actions">
                <button className="primary-button danger-button" disabled={busy} onClick={() => void deleteSelectedGroup()} type="button">Yes</button>
                <button
                  className="ghost-button"
                  onClick={() => {
                    setGroupLeaveChoicePopupOpen(false)
                    setGroupLeavePopupOpen(true)
                  }}
                  type="button"
                >
                  No
                </button>
              </div>
            </section>
          </div>
        ) : null}
        {isGroupLeavePopupOpen && selectedConversation?.kind === 'group' ? (
          <div className="attachments-popup-backdrop" onClick={() => setGroupLeavePopupOpen(false)} role="presentation">
            <section className="attachments-popup group-leave-popup" onClick={(event) => event.stopPropagation()}>
              <h3>Are you sure?</h3>
              <label className="toggle-row">
                <span>Delete all my messages</span>
                <input checked={leaveDeleteMessages} onChange={(event) => setLeaveDeleteMessages(event.target.checked)} type="checkbox" />
              </label>
              <div className="message-actions">
                <button className="primary-button danger-button" onClick={() => void leaveSelectedGroup()} type="button">Leave</button>
                <button className="ghost-button" onClick={() => setGroupLeavePopupOpen(false)} type="button">Cancel</button>
              </div>
            </section>
          </div>
        ) : null}
        {isChatClearPopupOpen && selectedConversation?.kind === 'chat' ? (
          <div className="attachments-popup-backdrop" onClick={() => setChatClearPopupOpen(false)} role="presentation">
            <section className="attachments-popup chat-clear-popup" onClick={(event) => event.stopPropagation()}>
              <h3>Clear chat</h3>
              <div className="message-actions">
                <button className="ghost-button" onClick={() => void clearSelectedChat('mine')} type="button">Delete My Messages</button>
                <button className="primary-button danger-button" onClick={() => void clearSelectedChat('all')} type="button">Delete Whole Chat</button>
              </div>
            </section>
          </div>
        ) : null}
        {isCreateGroupPopupOpen ? (
          <div className="attachments-popup-backdrop" onClick={() => setCreateGroupPopupOpen(false)} role="presentation">
            <section className="attachments-popup" onClick={(event) => event.stopPropagation()}>
              <input
                placeholder="Group title"
                value={groupTitle}
                onChange={(event) => setGroupTitle(event.target.value)}
              />
              <div className="message-actions">
                <button
                  className="primary-button"
                  disabled={busy || !groupTitle.trim()}
                  onClick={() =>
                    void (async () => {
                      await createGroup()
                      setCreateGroupPopupOpen(false)
                    })()
                  }
                  type="button"
                >
                  Create
                </button>
                <button className="ghost-button" onClick={() => setCreateGroupPopupOpen(false)} type="button">Cancel</button>
              </div>
            </section>
          </div>
        ) : null}
        {messageDeleteCandidate ? (
          <div className="attachments-popup-backdrop" onClick={() => setMessageDeleteCandidate(null)} role="presentation">
            <section className="attachments-popup message-delete-popup" onClick={(event) => event.stopPropagation()}>
              <h3>Delete message?</h3>
              <div className="message-actions">
                <button className="primary-button danger-button" onClick={() => void confirmDeleteMessage()} type="button">Delete</button>
                <button className="ghost-button" onClick={() => setMessageDeleteCandidate(null)} type="button">Cancel</button>
              </div>
            </section>
          </div>
        ) : null}
      </section>
    )
  }

  function renderGlobalSearchSection() {
    const globalResults = [
      ...globalUsers.map((user) => ({ ...user, resultType: 'user' as const })),
      ...globalGroups.map((group) => ({ ...group, resultType: 'group' as const })),
    ]
    return (
      <section className="card stack">
        <div className="global-search-main-row">
          <input
            placeholder="Search users and groups"
            value={globalQuery}
            onChange={(event) => setGlobalQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                runGlobalSearch()
              }
            }}
          />
          <select value={globalTypeFilter} onChange={(event) => setGlobalTypeFilter(event.target.value as GlobalSearchTypeFilter)}>
            <option value="users">Users</option>
            <option value="groups">Groups</option>
          </select>
          <button className="primary-button" onClick={runGlobalSearch} type="button">Search</button>
        </div>
        {globalTypeFilter === 'users' ? (
          <div className="global-search-filters">
            <input
              inputMode="numeric"
              placeholder="Age from"
              value={globalAgeFrom}
              onChange={(event) => setGlobalAgeFrom(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && runGlobalSearch()}
            />
            <input
              inputMode="numeric"
              placeholder="Age to"
              value={globalAgeTo}
              onChange={(event) => setGlobalAgeTo(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && runGlobalSearch()}
            />
            <input
              type="date"
              value={globalBirthDate}
              onChange={(event) => setGlobalBirthDate(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && runGlobalSearch()}
            />
            <select
              value={globalGender}
              onChange={(event) => setGlobalGender(event.target.value as UserGenderFilter)}
              onKeyDown={(event) => event.key === 'Enter' && runGlobalSearch()}
            >
              <option value="ANY">All</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
            </select>
            <input
              placeholder="City"
              value={globalCity}
              onChange={(event) => setGlobalCity(event.target.value)}
              onKeyDown={(event) => event.key === 'Enter' && runGlobalSearch()}
            />
          </div>
        ) : null}
        <div className="list-column tall-list">
          {globalResults.map((result) => (
            <button
              className={`search-result ${result.resultType === 'group' ? 'is-group' : 'is-user'}`}
              key={`${result.resultType}-${result.id}`}
              onClick={() =>
                void (result.resultType === 'user'
                  ? previewGlobalUser(result.id)
                  : applyToGroupById(result.id))
              }
              type="button"
            >
              <strong>{result.title}</strong>
              <span>{result.resultType === 'user' ? 'User' : 'Group'}</span>
            </button>
          ))}
        </div>
      </section>
    )
  }

  function renderActiveSection() {
    switch (activeSection) {
      case 'profile':
        return renderProfileSection()
      case 'chats':
        return renderChatsSection()
      case 'global-search':
        return renderGlobalSearchSection()
      default:
        return renderProfileSection()
    }
  }

  useEffect(() => {
    isProfileDraftDirtyRef.current = isProfileDraftDirty
  }, [isProfileDraftDirty])

  useEffect(() => {
    privacyDirtyByKeyRef.current = privacyDirtyByKey
  }, [privacyDirtyByKey])

  useEffect(() => {
    if (session) {
      window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
    } else {
      window.localStorage.removeItem(SESSION_STORAGE_KEY)
    }
  }, [session])

  /* eslint-disable react-hooks/set-state-in-effect, react-hooks/exhaustive-deps */
  useEffect(() => {
    if (!session) {
      return
    }
    void bootstrap()
  }, [session])

  useEffect(() => {
    if (!session) {
      return
    }
    const timer = window.setInterval(() => {
      void bootstrap(false)
    }, 5000)
    return () => window.clearInterval(timer)
  }, [session])

  useEffect(() => {
    if (!session || !selectedConversation) {
      return
    }
    const timer = window.setInterval(() => {
      if (messageEditState) {
        return
      }
      void loadHistory(selectedConversation, 1, true)
    }, 5000)
    return () => window.clearInterval(timer)
  }, [session, selectedConversation, messageEditState])

  useEffect(() => {
    const handlePopState = () => {
      const state = window.history.state as HistoryStatePayload | null
      if (!session) {
        return
      }
      void applyHistoryState(state, true)
    }

    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [session, chats, groups])

  useEffect(() => {
    if (!session) {
      hasAppliedInitialHistoryRef.current = false
      return
    }
    if (hasAppliedInitialHistoryRef.current) {
      return
    }
    if (!chats.length && !groups.length) {
      return
    }

    hasAppliedInitialHistoryRef.current = true
    const params = new URLSearchParams(window.location.search)
    const sectionParam = params.get('section')
    const kindParam = params.get('kind')
    const idParam = params.get('id')
    const section = sectionParam === 'groups'
      ? 'chats'
      : (isAppSection(sectionParam) ? sectionParam : 'profile')
    const state: HistoryStatePayload = { section }

    if ((kindParam === 'chat' || kindParam === 'group') && idParam) {
      state.conversationKind = kindParam
      state.conversationId = Number(idParam)
    }

    void applyHistoryState(state, true)
  }, [session, chats, groups])

  useEffect(() => {
    if (!session) {
      return
    }
    void searchChats(deferredChatQuery)
  }, [deferredChatQuery, session])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeAllPopups()
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  useEffect(() => {
    if (!session || !currentUser || !messages.length) {
      return
    }
    const unknownUserIds = Array.from(new Set(messages
      .map((message) => message.from_id)
      .filter((id) => id !== currentUser.id && !userNamesById[id])))
    if (!unknownUserIds.length) {
      return
    }
    void (async () => {
      const loaded = await Promise.allSettled(
        unknownUserIds.map((id) => request<UserProfile>(`/api/users/${id}`)),
      )
      const next: Record<number, string> = {}
      loaded.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          next[unknownUserIds[index]] = formatUserName(result.value)
        }
      })
      if (Object.keys(next).length) {
        setUserNamesById((current) => ({ ...current, ...next }))
      }
    })()
  }, [session, currentUser, messages, userNamesById])

  useEffect(() => {
    if (!session || !groupInvites.length) {
      return
    }
    const unknownUserIds = groupInvites.filter((id) => !userNamesById[id])
    if (!unknownUserIds.length) {
      return
    }
    void (async () => {
      const loaded = await Promise.allSettled(
        unknownUserIds.map((id) => request<UserProfile>(`/api/users/${id}`)),
      )
      const next: Record<number, string> = {}
      loaded.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          next[unknownUserIds[index]] = formatUserName(result.value)
        }
      })
      if (Object.keys(next).length) {
        setUserNamesById((current) => ({ ...current, ...next }))
      }
    })()
  }, [session, groupInvites, userNamesById])

  useEffect(() => {
    if (!session || !groupMembers.length) {
      return
    }
    const unknownUserIds = groupMembers.filter((id) => !userNamesById[id])
    if (!unknownUserIds.length) {
      return
    }
    void (async () => {
      const loaded = await Promise.allSettled(
        unknownUserIds.map((id) => request<UserProfile>(`/api/users/${id}`)),
      )
      const next: Record<number, string> = {}
      loaded.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          next[unknownUserIds[index]] = formatUserName(result.value)
        }
      })
      if (Object.keys(next).length) {
        setUserNamesById((current) => ({ ...current, ...next }))
      }
    })()
  }, [session, groupMembers, userNamesById])

  useEffect(() => {
    if (!session || !privacySettings) {
      return
    }
    const allPrivacyUserIds = Array.from(new Set([
      ...privacySettings.canMessageMe.userIds,
      ...privacySettings.canSeeInfo.userIds,
      ...privacySettings.canInviteMe.userIds,
    ])).filter((id) => id !== currentUser?.id && !userNamesById[id])
    if (!allPrivacyUserIds.length) {
      return
    }
    void (async () => {
      const loaded = await Promise.allSettled(
        allPrivacyUserIds.map((id) => request<UserProfile>(`/api/users/${id}`)),
      )
      const next: Record<number, string> = {}
      loaded.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          next[allPrivacyUserIds[index]] = formatUserName(result.value)
        }
      })
      if (Object.keys(next).length) {
        setUserNamesById((current) => ({ ...current, ...next }))
      }
    })()
  }, [session, currentUser, privacySettings, userNamesById])

  /* eslint-enable react-hooks/set-state-in-effect, react-hooks/exhaustive-deps */

  if (!session || !currentUser) {
    return (
      <main className="auth-shell">
        <section className="auth-card">
          <h1 className="auth-title">Welcome</h1>
          <div className="auth-switch">
            <button
              className={authMode === 'login' ? 'is-active' : ''}
              onClick={() => setAuthMode('login')}
              type="button"
            >
              Login
            </button>
            <button
              className={authMode === 'register' ? 'is-active' : ''}
              onClick={() => setAuthMode('register')}
              type="button"
            >
              Register
            </button>
          </div>

          <form className="stack" onSubmit={submitAuth}>
            {authMode === 'register' ? (
              <>
                <label>
                  <span>First name</span>
                  <input
                    value={registerForm.firstName}
                    onChange={(event) =>
                      setRegisterForm((current) => ({ ...current, firstName: event.target.value }))
                    }
                  />
                </label>
                <label>
                  <span>Last name</span>
                  <input
                    value={registerForm.lastName}
                    onChange={(event) =>
                      setRegisterForm((current) => ({ ...current, lastName: event.target.value }))
                    }
                  />
                </label>
                <label>
                  <span>Email</span>
                  <input
                    type="email"
                    value={registerForm.email}
                    onChange={(event) =>
                      setRegisterForm((current) => ({ ...current, email: event.target.value }))
                    }
                  />
                </label>
                <label>
                  <span>Password</span>
                  <input
                    type="password"
                    value={registerForm.password}
                    onChange={(event) =>
                      setRegisterForm((current) => ({ ...current, password: event.target.value }))
                    }
                  />
                </label>
              </>
            ) : (
              <>
                <label>
                  <span>Email</span>
                  <input type="email" value={loginEmail} onChange={(event) => setLoginEmail(event.target.value)} />
                </label>
                <label>
                  <span>Password</span>
                  <input
                    type="password"
                    value={loginPassword}
                    onChange={(event) => setLoginPassword(event.target.value)}
                  />
                </label>
              </>
            )}

            <button className="primary-button" disabled={busy} type="submit">
              {busy ? 'Working...' : authMode === 'login' ? 'Enter workspace' : 'Create account'}
            </button>
          </form>
        </section>
        {errorMessage ? (
          <div className="attachments-popup-backdrop" onClick={() => setErrorMessage(null)} role="presentation">
            <section className="attachments-popup error-popup" onClick={(event) => event.stopPropagation()}>
              <p>{errorMessage}</p>
              <button className="primary-button" onClick={() => setErrorMessage(null)} type="button">OK</button>
            </section>
          </div>
        ) : null}
        {successMessage ? (
          <div className="attachments-popup-backdrop" onClick={() => setSuccessMessage(null)} role="presentation">
            <section className="attachments-popup success-popup" onClick={(event) => event.stopPropagation()}>
              <p>{successMessage}</p>
              <button className="primary-button" onClick={() => setSuccessMessage(null)} type="button">OK</button>
            </section>
          </div>
        ) : null}
      </main>
    )
  }

  return (
    <main className="app-shell">
      <header className="hero-strip">
        <div>
          <h1>{currentUser.firstName} {currentUser.lastName}</h1>
        </div>
        <div className="hero-actions">
          <button className="ghost-button" onClick={() => void logout()} type="button">
            Logout
          </button>
        </div>
      </header>

      <section className="layout-grid">
        <aside className="panel sidebar-panel">
          <nav className="sidebar-menu">
            <button className={activeSection === 'profile' ? 'nav-button is-active' : 'nav-button'} onClick={() => openSection('profile')} type="button">Profile</button>
            <button className={activeSection === 'chats' ? 'nav-button is-active' : 'nav-button'} onClick={() => openSection('chats')} type="button">Chats</button>
            <button className={activeSection === 'global-search' ? 'nav-button is-active' : 'nav-button'} onClick={() => openSection('global-search')} type="button">Global Search</button>
          </nav>
          {ownedGroups.length ? (
            <div className="sidebar-owned-groups">
              {ownedGroups.map((group) => (
                <button
                  className="owned-group-button"
                  key={`owned-${group.id}`}
                  onClick={() =>
                    void selectConversation({
                      kind: 'group',
                      id: group.id,
                      title: group.title,
                    })
                  }
                  type="button"
                >
                  {group.title}
                </button>
              ))}
            </div>
          ) : null}
        </aside>
        <section className="panel content-panel">
          {renderActiveSection()}
        </section>
      </section>
      {isPrivacyUsersPopupOpen && privacyUsersPopupKey ? (
        <div className="attachments-popup-backdrop" onClick={closePrivacyUsersPopup} role="presentation">
          <section className="attachments-popup group-invite-popup" onClick={(event) => event.stopPropagation()}>
            <div className="group-invite-search-row">
              <input
                placeholder="Search users"
                value={privacyUsersQuery}
                onChange={(event) => setPrivacyUsersQuery(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              />
              <button className="primary-button" disabled={busy || !privacyUsersQuery.trim()} onClick={() => void searchPrivacyUsers()} type="button">
                Search
              </button>
            </div>
            <div className="global-search-filters">
              <input
                inputMode="numeric"
                placeholder="Age from"
                value={privacyUsersAgeFrom}
                onChange={(event) => setPrivacyUsersAgeFrom(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              />
              <input
                inputMode="numeric"
                placeholder="Age to"
                value={privacyUsersAgeTo}
                onChange={(event) => setPrivacyUsersAgeTo(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              />
              <input
                type="date"
                value={privacyUsersBirthDate}
                onChange={(event) => setPrivacyUsersBirthDate(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              />
              <select
                value={privacyUsersGender}
                onChange={(event) => setPrivacyUsersGender(event.target.value as UserGenderFilter)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              >
                <option value="ANY">All</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
              </select>
              <input
                placeholder="City"
                value={privacyUsersCity}
                onChange={(event) => setPrivacyUsersCity(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && void searchPrivacyUsers()}
              />
            </div>
            <div className="list-column">
              {privacyUsersSearchResults.map((user) => {
                const selectedUserIds = privacyDrafts[privacyUsersPopupKey]?.userIds ?? []
                const isSelected = selectedUserIds.includes(user.id)
                return (
                  <button
                    className="popup-row popup-row-button"
                    key={`privacy-search-${privacyUsersPopupKey}-${user.id}`}
                    onClick={() => togglePrivacyUserId(privacyUsersPopupKey, user.id)}
                    type="button"
                  >
                    <strong>{user.title}</strong>
                    <span>{isSelected ? 'Selected' : 'Tap to select'}</span>
                  </button>
                )
              })}
            </div>
          </section>
        </div>
      ) : null}
      {errorMessage ? (
        <div className="attachments-popup-backdrop" onClick={() => setErrorMessage(null)} role="presentation">
          <section className="attachments-popup error-popup" onClick={(event) => event.stopPropagation()}>
            <p>{errorMessage}</p>
            <button className="primary-button" onClick={() => setErrorMessage(null)} type="button">OK</button>
          </section>
        </div>
      ) : null}
      {successMessage ? (
        <div className="attachments-popup-backdrop" onClick={() => setSuccessMessage(null)} role="presentation">
          <section className="attachments-popup success-popup" onClick={(event) => event.stopPropagation()}>
            <p>{successMessage}</p>
            <button className="primary-button" onClick={() => setSuccessMessage(null)} type="button">OK</button>
          </section>
        </div>
      ) : null}
      {globalUserPreview ? (
        <div className="attachments-popup-backdrop" onClick={() => setGlobalUserPreview(null)} role="presentation">
          <section className="attachments-popup user-preview-popup" onClick={(event) => event.stopPropagation()}>
            <header className="user-preview-header">
              <h3>{formatUserName(globalUserPreview)}</h3>
              <p>User profile</p>
            </header>
            <div className="user-preview-field">
              <span>Bio</span>
              <div className="user-preview-value">{globalUserPreview.about?.trim() ? globalUserPreview.about : ''}</div>
            </div>
            <div className="user-preview-field">
              <span>Birthday</span>
              <div className="user-preview-value">{globalUserPreview.bdate ?? ''}</div>
            </div>
            <div className="user-preview-field">
              <span>City</span>
              <div className="user-preview-value">{globalUserPreview.city?.trim() ? globalUserPreview.city : ''}</div>
            </div>
            <div className="user-preview-field">
              <span>Gender</span>
              <div className="user-preview-value">{globalUserPreview.gender === 'MALE' ? 'Male' : globalUserPreview.gender === 'FEMALE' ? 'Female' : ''}</div>
            </div>
            <div className="message-actions">
              <button
                className="primary-button"
                onClick={() =>
                  void (async () => {
                    await openChat(globalUserPreview.id, formatUserName(globalUserPreview))
                    setGlobalUserPreview(null)
                  })()
                }
                type="button"
              >
                Message
              </button>
              <button className="ghost-button" onClick={() => setGlobalUserPreview(null)} type="button">Close</button>
            </div>
          </section>
        </div>
      ) : null}
    </main>
  )
}

function isAppSection(value: string | null): value is AppSection {
  return value === 'profile' || value === 'chats' || value === 'global-search'
}

type AttachmentPreviewProps = {
  attachment: Attachment
  session: Session | null
  onSessionChange: (session: Session | null) => void
  onDelete?: (() => void) | undefined
  className: string
}

function AttachmentPreview({ attachment, session, onSessionChange, onDelete, className }: AttachmentPreviewProps) {
  const [src, setSrc] = useState<string | null>(null)

  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    let active = true
    let objectUrl: string | null = null

    const previewPath = attachment.preview_url
    if (!previewPath) {
      setSrc(null)
      return
    }

    void (async () => {
      try {
        const blob = await fetchAuthorizedBlob(previewPath, session, onSessionChange)
        if (!active) {
          return
        }
        objectUrl = URL.createObjectURL(blob)
        setSrc(objectUrl)
      } catch {
        if (active) {
          setSrc(null)
        }
      }
    })()

    return () => {
      active = false
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl)
      }
    }
  }, [attachment.preview_url, onSessionChange, session])
  /* eslint-enable react-hooks/set-state-in-effect */

  return (
    <button
      className={`attachment-thumb ${className}`}
      onClick={onDelete}
      type="button"
    >
      {src ? (
        <img alt={attachment.file_name} src={src} />
      ) : (
        <span className="attachment-fallback">{attachment.file_name}</span>
      )}
      {onDelete ? <span className="attachment-delete">×</span> : null}
    </button>
  )
}

function privacyLabel(key: keyof PrivacySettings) {
  if (key === 'canMessageMe') {
    return 'Who can message me'
  }
  if (key === 'canSeeInfo') {
    return 'Who can see optional info'
  }
  return 'Who can invite me'
}

function toDraft(rule: { mode: PrivacyMode; userIds: number[] }): PrivacyDraft {
  return {
    mode: rule.mode,
    userIds: rule.userIds,
  }
}

export default App
