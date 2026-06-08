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

interface ApiRoom {
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
  observation?: string
  googleCalendarUrl: string
  icsUrl: string
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
  loginLandingView: 'STUDENT_MY_BOOKINGS' | 'STUDENT_RESERVE' | 'ADMIN_ROOMS' | 'ADMIN_PROFILES' | 'ADMIN_BOOKINGS'
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
    if (params?.campus && params.campus !== 'Todas') queryParams.set('campus', params.campus)
    if (params?.venue && params.venue !== 'Todos') queryParams.set('recinto', params.venue)
    if (params?.location && params.location !== 'Todas') queryParams.set('ubicacion', params.location)
    if (params?.query?.trim()) queryParams.set('q', params.query.trim())
    const query = queryParams.toString() ? `?${queryParams.toString()}` : ''
    return http<ApiPage<ApiRoom>>(`/rooms${query}`, {}, token)
  },

  getAvailableRooms(token: string, date: string, start: string, end: string) {
    const query = `?fecha=${encodeURIComponent(date)}&horaInicio=${encodeURIComponent(
      start,
    )}&horaFin=${encodeURIComponent(end)}`
    return http<ApiRoom[]>(`/rooms/available${query}`, {}, token)
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

  getAdminConfig(token: string) {
    return http<ApiConfig>('/admin/config', {}, token)
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

  getCampusMap(token: string) {
    return http('/campus/map', {}, token)
  },

  lookupUserByCode(token: string, code: string) {
    const query = `?code=${encodeURIComponent(code)}`
    return http<ApiUserLookup>(`/users/lookup${query}`, {}, token)
  },
}
