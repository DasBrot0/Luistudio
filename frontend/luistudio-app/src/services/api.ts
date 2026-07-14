const rawApiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'
const normalizedApiBase = rawApiBase.replace(/\/+$/, '')
const API_BASE = normalizedApiBase.endsWith('/api') ? normalizedApiBase : `${normalizedApiBase}/api`

export interface ApiAuthUser {
  id: number
  code: string
  firstName: string
  lastName: string
  email: string
  role: 'ADMIN' | 'ESTUDIANTE'
  status: string
  has2fa: boolean
}

interface ApiLoginResponse {
  token: string | null
  provisionalToken: string | null
  twoFactorRequired: boolean
  user: ApiAuthUser
  message: string
}

export interface ApiRoom {
  id: number
  code: string
  name: string
  resourceLabel: string
  campus: string
  campusLabel: string
  venue: string
  venueLabel: string
  capacity: number
  location: string
  minPeople: number
  minPeopleRequired: boolean
  maxPeople: number
  slotMinutes: number
  schedule: ApiScheduleDay[]
  status: string
  pabellonCode: string
  noiseLevel: 'BAJO' | 'MEDIO' | 'ALTO'
  supportsConcentration: boolean
  roomType: string
  equipment: string[]
  description: string | null
  allowedActivities: string[]
  nearbyServices: string[]
  accessibilityFeatures: string[]
  inventoryCount?: number
}

export interface ApiRoomSearchIntent {
  roomType: string
  minimumCapacity: number
  maximumNoise: 'BAJO' | 'MEDIO' | 'ALTO'
  requiresConcentration: boolean
  requiredEquipment: string[]
}

export interface ApiIntelligentRoomSearchResponse {
  intent: ApiRoomSearchIntent
  recommendations: Array<{ room: ApiRoom; score: number; reasons: string[] }>
  message?: string | null
}

export interface ApiAdminDashboard {
  from: string
  to: string
  totalReservations: number
  absenceRate: number
  absenceCount: number
  attendanceEligibleCount: number
  attendanceCount: number
  pendingAttendanceCount: number
  occupancyByRoom: Array<{ roomId: number; roomCode: string; roomName: string; reservedMinutes: number; availableMinutes: number; occupancyRate: number }>
  peakHours: Array<{ hour: number; reservedMinutes: number; reservationCount: number }>
  dailyOccupancy: Array<{ date: string; reservedMinutes: number; availableMinutes: number; occupancyRate: number }>
  weeklyHeatmap: Array<{ dayOfWeek: number; hour: number; reservedMinutes: number; availableMinutes: number; occupancyRate: number }>
  topStudents: Array<{ userId: number; code: string; fullName: string; email: string; reservationCount: number; reservedMinutes: number; absenceCount: number }>
}

export interface ApiAdminAttendance {
  bookingId: number
  userId: number
  studentCode: string
  studentName: string
  studentEmail: string
  roomId: number
  roomCode: string
  roomName: string
  campus: string
  pavilionCode: string
  pavilionName: string
  location: string
  date: string
  startTime: string
  endTime: string
  bookingStatus: string
  attendanceStatus: 'PENDIENTE' | 'ASISTIO' | 'INASISTIO'
}

export interface ApiScheduleDay {
  dayOfWeek: number
  openTime: string | null
  closeTime: string | null
  closed: boolean
  override?: boolean
}

interface ApiBooking {
  id: number
  userId: number
  userEmail: string
  roomId: number
  roomCode: string
  roomName: string
  location: string
  people: number
  date: string
  start: string
  end: string
  status: 'ACTIVA' | 'CANCELADA' | 'COMPLETADA'
  attendanceStatus?: 'ASISTIO' | 'INASISTIO' | null
  observation?: string
  googleCalendarUrl: string
  icsUrl: string
  roomUnitNumber?: number | null
  roomUnitLabel?: string | null
}

interface ApiPage<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

interface ApiConfig {
  maxActiveBookings: number
  maxDurationMinutes: number
}

export interface ApiCampusSchedule {
  campus: string
  campusLabel: string
  slotMinutes: number
  days: ApiScheduleDay[]
  warnings: string[]
}

interface ApiCampusScheduleListResponse {
  campuses: ApiCampusSchedule[]
}

interface ApiUser {
  id: number
  code: string
  email: string
  firstName: string
  lastName: string
  status: 'HABILITADO' | 'DESHABILITADO'
  role: 'ADMIN' | 'ESTUDIANTE'
  blocked: boolean
}

export interface ApiUserLookup {
  code: string
  firstName: string
  lastName: string
  fullName: string
}

export interface ApiPreferences {
  emailEnabled: boolean
  reminderEnabled: boolean
  bookingChangesEnabled: boolean
  notificationSettings: Record<string, { app: boolean; email: boolean }>
  themeMode: 'LIGHT' | 'DARK'
  fontScale: number
  loginLandingView: 'STUDENT_MY_BOOKINGS' | 'STUDENT_RESERVE' | 'ADMIN_DASHBOARD' | 'ADMIN_ROOMS' | 'ADMIN_PROFILES' | 'ADMIN_BOOKINGS'
}

const fieldLabels: Record<string, string> = {
  campus: 'campus',
  capacity: 'capacidad',
  closeTime: 'hora de cierre',
  date: 'fecha',
  days: 'días del horario',
  email: 'correo',
  end: 'hora de fin',
  location: 'ubicación',
  maxActiveBookings: 'máximo de reservas activas',
  maxDurationMinutes: 'duración máxima',
  maxPeople: 'máximo de personas',
  minPeople: 'mínimo de personas',
  minPeopleRequired: 'mínimo obligatorio',
  name: 'nombre',
  newPassword: 'nueva contraseña',
  openTime: 'hora de apertura',
  password: 'contraseña',
  people: 'cantidad de personas',
  roomId: 'sala',
  slotMinutes: 'duración por reserva',
  start: 'hora de inicio',
  status: 'estado',
  token: 'enlace de recuperación',
}

const cleanApiMessage = (message: string) => {
  const normalized = message.trim()
  if (!normalized) return 'No se pudo completar la acción. Revisa los datos e intenta nuevamente.'

  const parts = normalized
    .split(';')
    .map((part) => part.trim())
    .filter(Boolean)

  if (parts.length > 0) {
    const friendlyParts = parts.map((part) => {
      const [rawField, ...rest] = part.split(':')
      const detail = rest.join(':').trim()
      const field = rawField.trim()
      const label = fieldLabels[field] ?? field

      if (!detail) return part
      if (detail === 'must not be null' || detail === 'no debe ser nulo') return `Completa ${label}.`
      if (detail === 'must not be blank' || detail === 'no debe estar vacío') return `Completa ${label}.`
      if (detail.startsWith('must be greater than or equal to') || detail.startsWith('debe ser mayor que o igual a')) {
        const minimum = detail.match(/\d+/)?.[0]
        return minimum ? `${label} debe ser al menos ${minimum}.` : `Revisa ${label}.`
      }
      if (detail.startsWith('must be less than or equal to') || detail.startsWith('debe ser menor que o igual a')) {
        const maximum = detail.match(/\d+/)?.[0]
        return maximum ? `${label} debe ser como máximo ${maximum}.` : `Revisa ${label}.`
      }
      if (detail.includes('size must be between') || detail.includes('el tamaño debe estar entre')) {
        return `Revisa la longitud de ${label}.`
      }
      return `${label}: ${detail}`
    })
    return [...new Set(friendlyParts)].join(' ')
  }

  if (normalized.startsWith('Error ')) return 'No se pudo completar la acción. Intenta nuevamente.'
  return normalized
}

const readErrorMessage = async (response: Response) => {
  let message = `Error ${response.status}`
  try {
    const data = await response.json()
    message = data.message ?? data.error ?? message
  } catch {
    // no-op
  }
  return cleanApiMessage(message)
}

async function http<T>(path: string, options: RequestInit = {}, _token?: string): Promise<T> {
  void _token
  const headers = new Headers(options.headers)

  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export const api = {
  login(email: string, password: string, rememberMe: boolean) {
    return http<ApiLoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password, rememberMe }),
    })
  },

  verify2fa(code: string, rememberMe: boolean) {
    return http<ApiLoginResponse>('/auth/2fa/verify', {
      method: 'POST',
      body: JSON.stringify({ code, rememberMe }),
    })
  },

  me(token?: string) {
    return http<ApiAuthUser>('/auth/me', {}, token)
  },

  logout() {
    return http<{ message: string }>('/auth/logout', { method: 'POST' })
  },

  requestReset(email: string) {
    return http<{ message: string }>('/auth/reset-request', {
      method: 'POST',
      body: JSON.stringify({ email }),
    })
  },

  confirmReset(token: string, newPassword: string) {
    return http<{ message: string }>('/auth/reset-confirm', {
      method: 'POST',
      body: JSON.stringify({ token, newPassword }),
    })
  },

  enroll2fa(token: string) {
    return http<{ message: string }>('/auth/2fa/enroll', { method: 'POST' }, token)
  },

  confirm2faEnrollment(token: string, code: string) {
    return http<{ message: string }>(
      '/auth/2fa/confirm',
      { method: 'POST', body: JSON.stringify({ code }) },
      token,
    )
  },

  disable2fa(token: string) {
    return http<{ message: string }>('/auth/2fa/disable', { method: 'POST' }, token)
  },

  confirmDisable2fa(token: string, code: string) {
    return http<{ message: string }>(
      '/auth/2fa/disable/confirm',
      { method: 'POST', body: JSON.stringify({ code }) },
      token,
    )
  },

  getRooms(
    token: string,
    params?: {
      page?: number
      size?: number
      includeSchedule?: boolean
      campus?: string
      venue?: string
      location?: string
      query?: string
    },
  ) {
    const queryParams = new URLSearchParams()
    queryParams.set('page', String(params?.page ?? 0))
    queryParams.set('size', String(params?.size ?? 50))
    queryParams.set('includeSchedule', String(params?.includeSchedule ?? false))
    if (params?.campus && params.campus !== 'Todos') queryParams.set('campus', params.campus)
    if (params?.venue && params.venue !== 'Todos') queryParams.set('recinto', params.venue)
    if (params?.location && params.location !== 'Todas') queryParams.set('ubicacion', params.location)
    if (params?.query?.trim()) queryParams.set('q', params.query.trim())
    const query = queryParams.toString() ? `?${queryParams.toString()}` : ''
    return http<ApiPage<ApiRoom>>(`/rooms${query}`, {}, token)
  },

  intelligentRoomSearch(token: string, payload: { query: string; date: string; start: string; end: string; limit?: number }) {
    return http<ApiIntelligentRoomSearchResponse>(
      '/rooms/intelligent-search',
      { method: 'POST', body: JSON.stringify(payload) },
      token,
    )
  },

  createRoom(
    token: string,
    payload: {
      name: string
      campus: string
      location: string
      capacity: number
      minPeople: number
      minPeopleRequired: boolean
      maxPeople: number
      status?: 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'INACTIVA'
      schedule: ApiScheduleDay[]
      pabellonCode: string
      noiseLevel: 'BAJO' | 'MEDIO' | 'ALTO'
      supportsConcentration: boolean
      roomType: 'ESTUDIO_INDIVIDUAL' | 'ESTUDIO_GRUPAL' | 'REUNION' | 'PRESENTACION' | 'GENERAL'
      equipment: string[]
      description: string
      allowedActivities: string[]
      nearbyServices: string[]
      accessibilityFeatures: string[]
    },
  ) {
    return http<ApiRoom>(
      '/rooms',
      { method: 'POST', body: JSON.stringify(payload) },
      token,
    )
  },

  updateRoom(
    token: string,
    roomId: number,
    payload: {
      name: string
      campus: string
      location: string
      capacity: number
      minPeople: number
      minPeopleRequired: boolean
      maxPeople: number
      status?: 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'INACTIVA'
      schedule: ApiScheduleDay[]
      pabellonCode: string
      noiseLevel: 'BAJO' | 'MEDIO' | 'ALTO'
      supportsConcentration: boolean
      roomType: 'ESTUDIO_INDIVIDUAL' | 'ESTUDIO_GRUPAL' | 'REUNION' | 'PRESENTACION' | 'GENERAL'
      equipment: string[]
      description: string
      allowedActivities: string[]
      nearbyServices: string[]
      accessibilityFeatures: string[]
    },
  ) {
    return http<ApiRoom>(
      `/rooms/${roomId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      token,
    )
  },

  deleteRoom(token: string, roomId: number) {
    return http<void>(`/rooms/${roomId}`, { method: 'DELETE' }, token)
  },

  getBookingsMe(token: string, page = 0, size = 10) {
    const params = new URLSearchParams({
      page: String(Math.max(page, 0)),
      size: String(Math.min(Math.max(size, 1), 50)),
    })
    return http<ApiPage<ApiBooking>>(`/bookings/me?${params.toString()}`, {}, token)
  },

  getRoomBookings(token: string, roomId: number, fromDate: string, toDate: string) {
    const query = `?desde=${encodeURIComponent(fromDate)}&hasta=${encodeURIComponent(toDate)}`
    return http<ApiBooking[]>(`/rooms/${roomId}/bookings${query}`, {}, token)
  },

  createBooking(
    token: string,
    payload: {
      roomId: number
      date: string
      start: string
      end: string
      people: number
      location: string
      observation?: string
    },
  ) {
    return http<ApiBooking>('/bookings', { method: 'POST', body: JSON.stringify(payload) }, token)
  },

  updateBooking(
    token: string,
    bookingId: number,
    payload: {
      roomId: number
      date: string
      start: string
      end: string
      people: number
      location: string
      observation?: string
    },
  ) {
    return http<ApiBooking>(
      `/bookings/${bookingId}`,
      { method: 'PUT', body: JSON.stringify(payload) },
      token,
    )
  },

  cancelBooking(token: string, bookingId: number) {
    return http<ApiBooking>(`/bookings/${bookingId}/cancel`, { method: 'PATCH' }, token)
  },

  async downloadBookingIcs(token: string, bookingId: number) {
    const response = await fetch(`${API_BASE}/bookings/${bookingId}/ics`, {
      credentials: 'include',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })

    if (!response.ok) {
      throw new Error(await readErrorMessage(response))
    }

    return response.blob()
  },

  getAdminBookings(token: string, page: number, status: string, date: string) {
    const params = new URLSearchParams({
      page: String(Math.max(page - 1, 0)),
      size: '10',
    })
    if (status && status !== 'Todos') params.set('status', status === 'Confirmado' ? 'ACTIVA' : 'CANCELADA')
    if (date) params.set('fecha', date)
    return http<ApiPage<ApiBooking>>(`/admin/bookings?${params.toString()}`, {}, token)
  },

  getAdminAttendance(
    token: string,
    filters: {
      query?: string
      campus?: string
      pavilion?: string
      status?: string
      from?: string
      to?: string
      sortBy?: string
      sortDir?: 'asc' | 'desc'
    },
    page = 0,
    size = 10,
  ) {
    const params = new URLSearchParams({ page: String(Math.max(0, page)), size: String(Math.min(50, Math.max(1, size))) })
    Object.entries(filters).forEach(([key, value]) => {
      if (value) params.set(key, value)
    })
    return http<ApiPage<ApiAdminAttendance>>(`/admin/attendance?${params.toString()}`, {}, token)
  },

  updateAttendance(token: string, bookingId: number, status: 'ASISTIO' | 'INASISTIO') {
    return http<ApiAdminAttendance>(
      `/admin/attendance/${bookingId}`,
      { method: 'PATCH', body: JSON.stringify({ status }) },
      token,
    )
  },

  getAdminConfig(token: string) {
    return http<ApiConfig>('/admin/config', {}, token)
  },

  getAdminDashboard(token: string, from: string, to: string) {
    const params = new URLSearchParams({ from, to })
    return http<ApiAdminDashboard>(`/admin/dashboard?${params.toString()}`, {}, token)
  },

  updateAdminConfig(token: string, config: ApiConfig) {
    return http<ApiConfig>('/admin/config', { method: 'PUT', body: JSON.stringify(config) }, token)
  },

  getCampusSchedules(token: string) {
    return http<ApiCampusScheduleListResponse>('/admin/campus-schedules', {}, token)
  },

  updateCampusSchedule(token: string, schedule: { campus: string; slotMinutes: number; days: ApiScheduleDay[] }) {
    return http<ApiCampusSchedule>(
      '/admin/campus-schedules',
      { method: 'PUT', body: JSON.stringify(schedule) },
      token,
    )
  },

  getUsers(
    token: string,
    page: number,
    filters: {
      query: string
      year: string
      status: string
      sortBy: string
      sortDir: string
    },
  ) {
    const params = new URLSearchParams({
      page: String(Math.max(page - 1, 0)),
      size: '10',
      sortBy: filters.sortBy,
      sortDir: filters.sortDir,
    })
    if (filters.query) params.set('query', filters.query)
    if (filters.year) params.set('year', filters.year)
    if (filters.status && filters.status !== 'Todos') {
      if (filters.status === 'Bloqueado') {
        params.set('status', 'BLOQUEADO')
      } else {
        params.set('status', filters.status === 'Habilitado' ? 'HABILITADO' : 'DESHABILITADO')
      }
    }
    return http<ApiPage<ApiUser>>(`/admin/users?${params.toString()}`, {}, token)
  },

  updateUserStatus(token: string, userId: number, status: 'Habilitado' | 'Deshabilitado') {
    return http<ApiUser>(
      `/admin/users/${userId}/estado`,
      { method: 'PATCH', body: JSON.stringify({ status: status === 'Habilitado' ? 'HABILITADO' : 'DESHABILITADO' }) },
      token,
    )
  },

  unlockUser(token: string, userId: number) {
    return http<ApiUser>(
      `/admin/users/${userId}/estado`,
      { method: 'PATCH', body: JSON.stringify({ status: 'HABILITADO' }) },
      token,
    )
  },

  getPreferences(token: string) {
    return http<ApiPreferences>('/me/preferences', {}, token)
  },

  updatePreferences(token: string, preferences: ApiPreferences) {
    return http<ApiPreferences>('/me/preferences', { method: 'PUT', body: JSON.stringify(preferences) }, token)
  },

  getCampusMap(token: string, campus?: string, signal?: AbortSignal) {
    const query = campus ? `?campus=${encodeURIComponent(campus)}` : ''
    return http<import('../models/campusMap').CampusMapResponse>(`/campus/map${query}`, { signal }, token)
  },

  updateBuildingLocation(token: string, buildingId: number, latitude: number, longitude: number) {
    return http<import('../models/campusMap').CampusMapPavilion>(`/admin/buildings/${buildingId}/location`, {
      method: 'PUT', body: JSON.stringify({ latitude, longitude }),
    }, token)
  },

  lookupUserByCode(token: string, code: string) {
    const query = `?code=${encodeURIComponent(code)}`
    return http<ApiUserLookup>(`/users/lookup${query}`, {}, token)
  },

  // ST-02 Sessions
  getSessions(token: string) {
    return http<{ sessions: Array<{ id: number; ip: string | null; userAgent: string | null; deviceLabel: string | null; createdAt: string; lastSeenAt: string; current: boolean }> }>('/me/sessions', {}, token)
  },

  revokeSession(token: string, sessionId: number) {
    return http<{ message: string }>(`/me/sessions/${sessionId}`, { method: 'DELETE' }, token)
  },

  revokeAllSessions(token: string) {
    return http<{ message: string }>('/me/sessions', { method: 'DELETE' }, token)
  },

  // ST-04 Activity
  getMyActivity(token: string, page = 0, size = 10, filters: { from?: string; to?: string } = {}) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters.from) params.set('from', filters.from)
    if (filters.to) params.set('to', filters.to)
    return http<{ content: Array<{ id: number; action: string; detail: string | null; createdAt: string }>; page: number; totalPages: number; totalElements: number }>(`/me/activity?${params.toString()}`, {}, token)
  },

  // ST-06 Sensitive change
  confirmSensitiveChange(token: string, confirmToken: string) {
    return http<{ message: string }>('/auth/sensitive-change/confirm', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: confirmToken }),
    }, token)
  },

  // ST-07 Admin login attempts
  getLoginAttempts(token: string, filters: { user?: string; email?: string; from?: string; to?: string; success?: boolean; blocked?: boolean; sortBy?: string; sortDir?: 'asc' | 'desc' }, page = 0, size = 10) {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters.user) params.set('user', filters.user)
    if (filters.email) params.set('email', filters.email)
    if (filters.from) params.set('from', filters.from)
    if (filters.to) params.set('to', filters.to)
    if (filters.success !== undefined) params.set('success', String(filters.success))
    if (filters.blocked !== undefined) params.set('blocked', String(filters.blocked))
    if (filters.sortBy) params.set('sortBy', filters.sortBy)
    if (filters.sortDir) params.set('sortDir', filters.sortDir)
    return http<{ content: Array<{ id: number; userId: number; userEmail: string; ip: string | null; userAgent: string | null; attemptedAt: string; success: boolean; lockedUntil: string | null }>; page: number; totalPages: number; totalElements: number }>(`/admin/security/login-attempts?${params.toString()}`, {}, token)
  },

  // ST-08 Room availability subscriptions
  subscribeToRoom(token: string, roomId: number, targetDate: string, startTime: string, endTime: string) {
    return http<{ message: string }>(`/rooms/${roomId}/availability-subscriptions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ targetDate, startTime, endTime }),
    }, token)
  },

  unsubscribeFromRoom(token: string, roomId: number) {
    return http<{ message: string }>(`/rooms/${roomId}/availability-subscriptions/me`, { method: 'DELETE' }, token)
  },

  getMyAvailabilitySubscriptions(token: string) {
    return http<{ subscriptions: Array<{ id: number; roomId: number; roomName: string; targetDate: string; startTime: string; endTime: string; status: string; createdAt: string }> }>('/me/availability-subscriptions', {}, token)
  },

  // ST-09 Announcements
  publishAnnouncement(token: string, request: { title: string; content: string; announcementType: string }) {
    return http<{ id: number; title: string; announcementType: string; createdAt: string; recipientCount: number }>('/admin/announcements', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    }, token)
  },
}
