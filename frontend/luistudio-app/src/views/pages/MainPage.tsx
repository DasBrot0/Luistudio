import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getRouteFromPath, resolveRouteByAuth, routePaths } from '../../viewmodels/routes'
import type {
  AuthUser,
  Booking,
  BookingStatus,
  CampusSchedule,
  Profile,
  ReservationCompanion,
  ReservationForm,
  Role,
  ScheduleDay,
  Room,
  RoomDraft,
  RoomStatus,
  RouteKey,
  SystemConfig,
} from '../../models/types'
import { buildPabellonCode, getDefaultReservationForm, minutesBetween } from '../../utils/helpers'
import { LoginPage } from './LoginPage'
import { ResetPasswordPage } from './ResetPasswordPage'
import { ReservasPage } from './ReservasPage'
import { MisReservasPage } from './MisReservasPage'
import { SalasPage } from './SalasPage'
import { PerfilesPage } from './PerfilesPage'
import { AdminReservasPage } from './AdminReservasPage'
import { BookingSuccessModal } from '../components/modals/BookingSuccessModal'
import { EditBookingModal } from '../components/modals/EditBookingModal'
import { RoomFormModal } from '../components/modals/RoomFormModal'
import { RoomSuccessModal } from '../components/modals/RoomSuccessModal'
import { DeleteRoomModal } from '../components/modals/DeleteRoomModal'
import { ForgotPasswordModal } from '../components/modals/ForgotPasswordModal'
import { TwoFactorModal } from '../components/modals/TwoFactorModal'
import { MessageModal } from '../components/modals/MessageModal'
import { ConfirmCancelBookingModal } from '../components/modals/ConfirmCancelBookingModal'
import { GlobalTopbar } from '../components/layout/GlobalTopbar'
import { api, type ApiCampusSchedule, type ApiPreferences } from '../../services/api'

interface NotificationItem {
  id: number
  message: string
  createdAt: string
}

type NotificationPreferenceKey =
  | 'BOOKING_CONFIRMATION'
  | 'BOOKING_UPDATE'
  | 'BOOKING_CANCELLATION'
  | 'BOOKING_REMINDER'
  | 'ROOM_MAINTENANCE'
  | 'PROFILE_STATUS'

type NotificationChannelSettings = { app: boolean; email: boolean }
type NotificationSettings = Record<string, NotificationChannelSettings>

interface NotificationPreferenceOption {
  key: NotificationPreferenceKey
  group: string
  label: string
  app: boolean
  email: boolean
}

type LoginLandingViewCode =
  | 'STUDENT_MY_BOOKINGS'
  | 'STUDENT_RESERVE'
  | 'ADMIN_ROOMS'
  | 'ADMIN_PROFILES'
  | 'ADMIN_BOOKINGS'

const LOCAL_STORAGE_THEME_KEY = 'luistudio_dark_mode'
const LOCAL_STORAGE_FONT_SCALE_KEY = 'luistudio_font_scale'
const LOCAL_STORAGE_LANDING_KEY = 'luistudio_login_landing_route'
const LOCAL_STORAGE_SESSION_HINT_KEY = 'luistudio_session_hint'
const SESSION_STORAGE_SESSION_HINT_KEY = 'luistudio_session_hint'
const SESSION_STORAGE_NOTIFICATIONS_KEY = 'luistudio_notifications'

const clampFontScale = (value: number) => Math.min(1.3, Math.max(0.85, Number.isFinite(value) ? value : 1))

const getStoredSessionHint = () =>
  localStorage.getItem(LOCAL_STORAGE_SESSION_HINT_KEY) === '1' ||
  sessionStorage.getItem(SESSION_STORAGE_SESSION_HINT_KEY) === '1'

const persistSessionHint = (rememberMe: boolean) => {
  localStorage.removeItem(LOCAL_STORAGE_SESSION_HINT_KEY)
  sessionStorage.removeItem(SESSION_STORAGE_SESSION_HINT_KEY)
  if (rememberMe) {
    localStorage.setItem(LOCAL_STORAGE_SESSION_HINT_KEY, '1')
    return
  }
  sessionStorage.setItem(SESSION_STORAGE_SESSION_HINT_KEY, '1')
}

const clearSessionHint = () => {
  localStorage.removeItem(LOCAL_STORAGE_SESSION_HINT_KEY)
  sessionStorage.removeItem(SESSION_STORAGE_SESSION_HINT_KEY)
}

const getInitialDarkMode = () => {
  const saved = localStorage.getItem(LOCAL_STORAGE_THEME_KEY)
  if (saved !== null) return saved === '1'
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

const getInitialFontScale = () => {
  const savedFontScale = Number(localStorage.getItem(LOCAL_STORAGE_FONT_SCALE_KEY) ?? '1')
  if (!Number.isNaN(savedFontScale) && savedFontScale >= 0.85 && savedFontScale <= 1.3) {
    return clampFontScale(savedFontScale)
  }
  return 1
}

const toUiRole = (role: 'ADMIN' | 'ESTUDIANTE'): Role => (role === 'ADMIN' ? 'admin' : 'student')

const toUiUser = (user: {
  id: number
  code: string
  firstName: string
  lastName: string
  email: string
  role: 'ADMIN' | 'ESTUDIANTE'
  has2fa: boolean
}): AuthUser => ({
  id: user.id,
  role: toUiRole(user.role),
  code: user.code,
  firstName: user.firstName,
  lastName: user.lastName,
  email: user.email,
  has2fa: user.has2fa,
})

const toUiRoom = (room: {
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
  schedule: ScheduleDay[]
  status: string
}): Room => ({
  backendId: room.id,
  id: room.code,
  name: room.name,
  resourceLabel: room.resourceLabel,
  campus: room.campus,
  campusLabel: room.campusLabel,
  venue: room.venue,
  venueLabel: room.venueLabel,
  capacity: room.capacity,
  location: room.location,
  minPeople: room.minPeople,
  minPeopleRequired: room.minPeopleRequired,
  maxPeople: room.maxPeople,
  slotMinutes: room.slotMinutes,
  schedule: room.schedule,
  active: room.status !== 'INACTIVA',
  status:
    room.status === 'EN_MANTENIMIENTO'
      ? 'En mantenimiento'
      : room.status === 'INACTIVA'
        ? 'Inactiva'
        : 'Disponible',
})

const toApiRoomStatus = (status: RoomStatus): 'DISPONIBLE' | 'EN_MANTENIMIENTO' | 'INACTIVA' => {
  if (status === 'En mantenimiento') return 'EN_MANTENIMIENTO'
  if (status === 'Inactiva') return 'INACTIVA'
  return 'DISPONIBLE'
}

const toUiCampusSchedule = (schedule: ApiCampusSchedule): CampusSchedule => ({
  campus: schedule.campus,
  campusLabel: schedule.campusLabel,
  slotMinutes: schedule.slotMinutes,
  days: schedule.days,
  warnings: schedule.warnings,
})

const defaultWeeklySchedule: ScheduleDay[] = [
  { dayOfWeek: 1, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 2, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 3, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 4, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 5, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 6, openTime: '06:00', closeTime: '12:00', closed: false },
  { dayOfWeek: 7, openTime: null, closeTime: null, closed: true },
]

const normalizeScheduleDays = (days: ScheduleDay[]) =>
  days.map((day) => ({
    dayOfWeek: day.dayOfWeek,
    openTime: day.closed ? null : day.openTime,
    closeTime: day.closed ? null : day.closeTime,
    closed: day.closed,
  }))

const dayLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const timeToMinutes = (value: string | null) => {
  if (!value) return Number.NaN
  const [hours, minutes] = value.split(':').map(Number)
  if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return Number.NaN
  return hours * 60 + minutes
}

const findInvalidScheduleDay = (days: ScheduleDay[]) =>
  days.find((day) => !day.closed && (!day.openTime || !day.closeTime || day.closeTime <= day.openTime))

const findUnalignedScheduleDay = (days: ScheduleDay[], slotMinutes: number) =>
  days.find((day) => {
    if (day.closed) return false
    const open = timeToMinutes(day.openTime)
    const close = timeToMinutes(day.closeTime)
    return !Number.isFinite(open) || !Number.isFinite(close) || open % slotMinutes !== 0 || close % slotMinutes !== 0
  })

const toHourMinute = (value: string) => value.split(':').slice(0, 2).join(':')

const toUiBooking = (booking: {
  id: number
  userId: number
  userEmail: string
  roomId: number
  roomCode: string
  location: string
  people: number
  date: string
  start: string
  end: string
  status: 'ACTIVA' | 'CANCELADA' | 'COMPLETADA'
  observation?: string
  googleCalendarUrl?: string
  icsUrl?: string
}): Booking => ({
  id: `RES-${booking.id}`,
  backendId: booking.id,
  userId: booking.userId,
  userEmail: booking.userEmail,
  location: booking.location,
  roomId: booking.roomCode,
  roomBackendId: booking.roomId,
  people: booking.people,
  date: booking.date,
  start: toHourMinute(booking.start),
  end: toHourMinute(booking.end),
  status: booking.status === 'CANCELADA' ? 'Cancelado' : 'Confirmado',
  observation: booking.observation,
  googleCalendarUrl: booking.googleCalendarUrl,
  icsUrl: booking.icsUrl,
})

const formatDisplayDate = (isoDate: string) => {
  const [year, month, day] = isoDate.split('-')
  if (!year || !month || !day) return isoDate
  return `${day}/${month}/${year}`
}

const getTodayIso = () => new Date().toISOString().slice(0, 10)

const isDateInCurrentWeek = (isoDate: string) => {
  const today = new Date()
  const start = new Date(today)
  const weekday = (today.getDay() + 6) % 7
  start.setHours(0, 0, 0, 0)
  start.setDate(today.getDate() - weekday)
  const end = new Date(start)
  end.setDate(start.getDate() + 7)
  const target = new Date(`${isoDate}T00:00:00`)
  return target >= start && target < end
}

const dedupeBookingsByIdentity = (items: Booking[]) => {
  const latestByIdentity = new Map<string, Booking>()
  for (const booking of items) {
    const identity = [
      booking.userId,
      booking.roomId,
      booking.location,
      booking.date,
      booking.start,
      booking.end,
    ].join('|')
    const current = latestByIdentity.get(identity)
    if (!current || booking.backendId > current.backendId) {
      latestByIdentity.set(identity, booking)
    }
  }
  return Array.from(latestByIdentity.values())
}

const parseParticipantsFromObservation = (observation?: string): ReservationCompanion[] => {
  if (!observation) return []
  const parts = observation.split('Participantes:')[1]?.trim()
  if (!parts) return []
  return parts
    .split('|')
    .map((segment) => segment.trim())
    .filter(Boolean)
    .map((segment) => {
      const [rawCode, ...rest] = segment.split('-')
      const code = rawCode?.trim() ?? ''
      const fullName = rest.join('-').trim()
      return { code, fullName }
    })
    .filter((item) => item.code && item.fullName)
}

const buildParticipantsObservation = (participants: Array<{ code: string; fullName: string }>) =>
  `Participantes: ${participants.map((item) => `${item.code} - ${item.fullName}`).join(' | ')}`.slice(0, 255)

const toProfile = (user: {
  id: number
  code: string
  email: string
  firstName: string
  lastName: string
  status: 'HABILITADO' | 'DESHABILITADO'
  blocked: boolean
}): Profile => ({
  id: String(user.id),
  code: user.code,
  email: user.email,
  firstName: user.firstName,
  lastName: user.lastName,
  status: user.blocked ? 'Bloqueado' : user.status === 'HABILITADO' ? 'Habilitado' : 'Deshabilitado',
  blocked: user.blocked,
})

const defaultLandingRoute = (role: Role): RouteKey => (role === 'admin' ? 'salas' : 'misreservas')

const routeToLandingViewCode = (role: Role, route: RouteKey): LoginLandingViewCode => {
  if (role === 'admin') {
    if (route === 'perfiles') return 'ADMIN_PROFILES'
    return route === 'admin-reservas' ? 'ADMIN_BOOKINGS' : 'ADMIN_ROOMS'
  }
  return route === 'reservas' ? 'STUDENT_RESERVE' : 'STUDENT_MY_BOOKINGS'
}

const landingViewCodeToRoute = (role: Role, code: LoginLandingViewCode): RouteKey => {
  if (role === 'admin') {
    if (code === 'ADMIN_PROFILES') return 'perfiles'
    return code === 'ADMIN_BOOKINGS' ? 'admin-reservas' : 'salas'
  }
  return code === 'STUDENT_RESERVE' ? 'reservas' : 'misreservas'
}

const notificationPreferenceOptions = (role: Role): NotificationPreferenceOption[] => {
  const studentOptions: NotificationPreferenceOption[] = [
    { key: 'BOOKING_CONFIRMATION', group: 'Reservas', label: 'Confirmacion de reserva', app: true, email: true },
    { key: 'BOOKING_UPDATE', group: 'Reservas', label: 'Modificacion de reserva', app: true, email: true },
    { key: 'BOOKING_CANCELLATION', group: 'Reservas', label: 'Cancelacion de reserva', app: true, email: true },
    { key: 'BOOKING_REMINDER', group: 'Reservas', label: 'Recordatorio de reserva', app: true, email: true },
  ]
  if (role === 'admin') {
    return [
      { key: 'ROOM_MAINTENANCE', group: 'Mantenimiento de salas', label: 'Sala en mantenimiento', app: true, email: true },
      { key: 'PROFILE_STATUS', group: 'Perfiles', label: 'Cambios de estado de usuarios', app: true, email: false },
    ]
  }
  return studentOptions
}

const defaultNotificationSettings = (role: Role): NotificationSettings =>
  notificationPreferenceOptions(role).reduce<NotificationSettings>((acc, option) => {
    acc[option.key] = { app: option.app, email: option.email }
    return acc
  }, {})

const normalizeNotificationSettings = (
  role: Role,
  settings?: NotificationSettings,
  legacy?: Pick<ApiPreferences, 'emailEnabled' | 'reminderEnabled' | 'bookingChangesEnabled'>,
): NotificationSettings =>
  notificationPreferenceOptions(role).reduce<NotificationSettings>((acc, option) => {
    const saved = settings?.[option.key]
    let email = saved?.email ?? option.email
    if (!saved && legacy) {
      if (option.key === 'BOOKING_REMINDER') email = legacy.emailEnabled && legacy.reminderEnabled
      else if (option.key === 'BOOKING_UPDATE' || option.key === 'BOOKING_CANCELLATION') {
        email = legacy.emailEnabled && legacy.bookingChangesEnabled
      } else {
        email = legacy.emailEnabled && option.email
      }
    }
    acc[option.key] = { app: saved?.app ?? option.app, email }
    return acc
  }, {})

const getInitialNotifications = (): NotificationItem[] => {
  try {
    const raw = sessionStorage.getItem(SESSION_STORAGE_NOTIFICATIONS_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter(
        (item): item is NotificationItem =>
          item &&
          typeof item.id === 'number' &&
          typeof item.message === 'string' &&
          typeof item.createdAt === 'string',
      )
      .slice(0, 100)
  } catch {
    return []
  }
}

function AuthRestoreScreen() {
  return (
    <main className="page auth-page auth-loading-page" aria-busy="true" aria-live="polite">
      <section className="auth-card auth-loading-card" aria-label="Restaurando sesion">
        <div className="auth-loading-spinner" aria-hidden="true" />
        <p className="auth-loading-eyebrow">Sesion en curso</p>
        <h1>Restaurando sesion</h1>
        <p>Estamos validando tu sesion para devolverte a la vista en la que estabas.</p>
      </section>
    </main>
  )
}

export function MainPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [route, setRoute] = useState<RouteKey>(() => getRouteFromPath(location.pathname))

  const [token, setToken] = useState('')
  const [authenticatedUser, setAuthenticatedUser] = useState<AuthUser | null>(null)
  const [authHydrated, setAuthHydrated] = useState(false)
  const [hasStoredSession, setHasStoredSession] = useState(() => getStoredSessionHint())

  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [loginError, setLoginError] = useState('')
  const [twoFactorError, setTwoFactorError] = useState('')
  const [showTwoFactorModal, setShowTwoFactorModal] = useState(false)
  const [twoFactorCode, setTwoFactorCode] = useState('')

  const [showForgotModal, setShowForgotModal] = useState(false)
  const [forgotEmail, setForgotEmail] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [resetPassword, setResetPassword] = useState('')
  const [resetPasswordConfirm, setResetPasswordConfirm] = useState('')
  const [resetError, setResetError] = useState('')
  const [showResetSuccess, setShowResetSuccess] = useState(false)

  const [rooms, setRooms] = useState<Room[]>([])
  const [roomDirectory, setRoomDirectory] = useState<Room[]>([])
  const [bookings, setBookings] = useState<Booking[]>([])
  const [profiles, setProfiles] = useState<Profile[]>([])

  const [config, setConfig] = useState<SystemConfig>({
    maxActiveBookings: 1,
    maxDurationMinutes: 120,
  })
  const [configDraft, setConfigDraft] = useState<SystemConfig>({
    maxActiveBookings: 1,
    maxDurationMinutes: 120,
  })
  const [configNotice, setConfigNotice] = useState('')

  const [reservationForm, setReservationForm] = useState<ReservationForm>({
    campus: '',
    location: '',
    roomId: '',
    people: 0,
    date: '',
    start: '',
    end: '',
  })
  const [reservationError, setReservationError] = useState('')
  const [reservationCompanions, setReservationCompanions] = useState<ReservationCompanion[]>([])
  const [reservationCompanionCodeInput, setReservationCompanionCodeInput] = useState('')
  const [reservationWeekOffset, setReservationWeekOffset] = useState(0)
  const [roomBookingsWindow, setRoomBookingsWindow] = useState<Booking[]>([])
  const [showBookingSuccess, setShowBookingSuccess] = useState(false)

  const [editingBookingId, setEditingBookingId] = useState<string | null>(null)
  const [editBookingForm, setEditBookingForm] = useState<ReservationForm>({
    campus: '',
    location: '',
    roomId: '',
    people: 2,
    date: '',
    start: '16:30',
    end: '17:30',
  })
  const [editBookingOwner, setEditBookingOwner] = useState<ReservationCompanion | null>(null)
  const [editBookingCompanions, setEditBookingCompanions] = useState<ReservationCompanion[]>([])
  const [editCompanionCodeInput, setEditCompanionCodeInput] = useState('')

  const [roomSearchQuery, setRoomSearchQuery] = useState('')
  const [roomFilterCampus, setRoomFilterCampus] = useState('Todos')
  const [roomFilterVenue, setRoomFilterVenue] = useState('Todos')
  const [roomFilterLocation, setRoomFilterLocation] = useState('Todas')
  const [roomStatusFilter, setRoomStatusFilter] = useState<'Todos' | 'Disponible' | 'En mantenimiento'>('Todos')
  const [roomSort, setRoomSort] = useState('name:asc')
  const [roomModalMode, setRoomModalMode] = useState<'none' | 'add' | 'edit'>('none')
  const [roomModalTargetId, setRoomModalTargetId] = useState<string | null>(null)
  const [roomDraft, setRoomDraft] = useState<RoomDraft>({
    name: '',
    campus: 'Monterrico',
    location: 'University Wellness Center',
    capacity: 6,
    minPeople: 1,
    minPeopleRequired: false,
    maxPeople: 6,
    status: 'Disponible',
    schedule: [],
    pabellonCode: '',
  })
  const [roomNotice, setRoomNotice] = useState('')
  const [roomSuccessId, setRoomSuccessId] = useState('')
  const [pendingDeleteRoomId, setPendingDeleteRoomId] = useState<string | null>(null)

  const [profilesPage, setProfilesPage] = useState(1)
  const [profilesTotalPages, setProfilesTotalPages] = useState(1)
  const [adminSearchQuery, setAdminSearchQuery] = useState('')
  const [adminStatusFilter, setAdminStatusFilter] = useState<'Todos' | BookingStatus>('Todos')
  const [adminCampusFilter, setAdminCampusFilter] = useState('Todos')
  const [adminDateFilter, setAdminDateFilter] = useState('')
  const [adminDateQuickFilter, setAdminDateQuickFilter] = useState<'none' | 'today' | 'week'>('none')
  const [adminSort, setAdminSort] = useState('date:desc')
  const [adminPage, setAdminPage] = useState(1)
  const [profilesQuery, setProfilesQuery] = useState('')
  const [profilesYearFilter, setProfilesYearFilter] = useState('')
  const [profilesStatusFilter, setProfilesStatusFilter] = useState<'Todos' | 'Habilitado' | 'Deshabilitado' | 'Bloqueado'>('Todos')
  const [profilesSortBy, setProfilesSortBy] = useState('firstName')
  const [profilesSortDir, setProfilesSortDir] = useState<'asc' | 'desc'>('asc')

  const [toastMessage, setToastMessage] = useState('')
  const [modalMessage, setModalMessage] = useState<{ title: string; message: string; variant: 'error' | 'success' } | null>(null)
  const [pendingCancelBooking, setPendingCancelBooking] = useState<{ bookingId: string; actor: Role } | null>(null)
  const [pendingProfileAction, setPendingProfileAction] = useState<{ profileId: string; action: 'status' | 'unlock' } | null>(null)
  const [notifications, setNotifications] = useState<NotificationItem[]>(getInitialNotifications)
  const [isNotificationsModalOpen, setIsNotificationsModalOpen] = useState(false)
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false)
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState<boolean>(getInitialDarkMode)
  const [fontScale, setFontScale] = useState<number>(getInitialFontScale)
  const [twoFactorAction, setTwoFactorAction] = useState<'none' | 'enable' | 'disable'>('none')
  const [preferencesLoaded, setPreferencesLoaded] = useState(false)
  const [lastPreferencesSignature, setLastPreferencesSignature] = useState('')
  const [notificationPrefs, setNotificationPrefs] = useState<Pick<ApiPreferences, 'emailEnabled' | 'reminderEnabled' | 'bookingChangesEnabled' | 'notificationSettings'>>({
    emailEnabled: true,
    reminderEnabled: true,
    bookingChangesEnabled: true,
    notificationSettings: defaultNotificationSettings('student'),
  })
  const [loginLandingRoute, setLoginLandingRoute] = useState<RouteKey | null>(null)
  const [campusSchedules, setCampusSchedules] = useState<CampusSchedule[]>([])

  const effectiveRoute = useMemo(
    () => resolveRouteByAuth(route, authenticatedUser, loginLandingRoute),
    [route, authenticatedUser, loginLandingRoute],
  )
  const activeRooms = useMemo(() => rooms.filter((room) => room.active), [rooms])
  const campusValueOptions = useMemo(() => [...new Set(activeRooms.map((room) => room.campus))], [activeRooms])
  const campusOptions = useMemo(() => [...new Set(activeRooms.map((room) => room.campusLabel))], [activeRooms])
  const roomCampusOptions = useMemo(() => [...new Set(roomDirectory.map((room) => room.campusLabel))], [roomDirectory])
  const venueOptions = useMemo(() => {
    if (roomFilterCampus === 'Todos') return []
    return [
      ...new Set(
        roomDirectory
          .filter((room) => room.campusLabel === roomFilterCampus)
          .map((room) => room.venueLabel),
      ),
    ]
  }, [roomDirectory, roomFilterCampus])
  const roomLocationOptions = useMemo(() => {
    if (roomFilterCampus === 'Todos') return []
    return [
      ...new Set(
        roomDirectory
          .filter((room) => room.campusLabel === roomFilterCampus)
          .filter((room) => roomFilterVenue === 'Todos' || room.venueLabel === roomFilterVenue)
          .map((room) => room.location),
      ),
    ]
  }, [roomDirectory, roomFilterCampus, roomFilterVenue])
  const locationOptionsByCampus = useMemo(() => {
    const grouped = new Map<string, string[]>()
    for (const room of activeRooms) {
      const key = room.campusLabel
      const current = grouped.get(key) ?? []
      if (!current.includes(room.venueLabel)) current.push(room.venueLabel)
      grouped.set(key, current)
    }
    return grouped
  }, [activeRooms])
  const venueOptionsByCampus = useMemo(() => {
    const grouped = new Map<string, string[]>()
    for (const room of activeRooms) {
      const key = room.campus
      const current = grouped.get(key) ?? []
      if (!current.includes(room.venue)) current.push(room.venue)
      grouped.set(key, current)
    }
    return grouped
  }, [activeRooms])
  const myBookings = useMemo(() => {
    if (!authenticatedUser) return []
    return bookings
      .filter((booking) => booking.userId === authenticatedUser.id)
      .sort((a, b) => `${a.date} ${a.start}`.localeCompare(`${b.date} ${b.start}`))
  }, [authenticatedUser, bookings])
  const adminBookings = useMemo(() => {
    return bookings
      .filter((booking) => {
        const room = activeRooms.find((item) => item.id === booking.roomId)
        const normalizedQuery = adminSearchQuery.trim().toLowerCase()
        if (normalizedQuery) {
          const ownerMatch = (booking.userEmail ?? '').toLowerCase().includes(normalizedQuery)
          const roomMatch = `${booking.roomId} ${room?.name ?? ''} ${room?.resourceLabel ?? ''}`.toLowerCase().includes(normalizedQuery)
          if (!ownerMatch && !roomMatch) return false
        }
        if (adminStatusFilter !== 'Todos' && booking.status !== adminStatusFilter) return false
        if (adminCampusFilter !== 'Todos' && room?.campusLabel !== adminCampusFilter) return false
        if (adminDateQuickFilter === 'today' && booking.date !== getTodayIso()) return false
        if (adminDateQuickFilter === 'week' && !isDateInCurrentWeek(booking.date)) return false
        if (adminDateQuickFilter === 'none' && adminDateFilter && booking.date !== adminDateFilter) return false
        return true
      })
      .sort((a, b) => {
        if (adminSort === 'date:asc') return `${a.date} ${a.start}`.localeCompare(`${b.date} ${b.start}`)
        if (adminSort === 'room:asc') return a.roomId.localeCompare(b.roomId)
        if (adminSort === 'student:asc') return (a.userEmail ?? '').localeCompare(b.userEmail ?? '')
        return `${b.date} ${b.start}`.localeCompare(`${a.date} ${a.start}`)
      })
  }, [activeRooms, adminCampusFilter, adminDateFilter, adminDateQuickFilter, adminSearchQuery, adminSort, adminStatusFilter, bookings])
  const pageSize = 5
  const totalAdminPages = Math.max(1, Math.ceil(adminBookings.length / pageSize))
  const adminBookingsPage = adminBookings.slice((adminPage - 1) * pageSize, adminPage * pageSize)
  const filteredRooms = useMemo(() => {
    return roomDirectory.filter((room) => {
      const normalizedQuery = roomSearchQuery.trim().toLowerCase()
      if (normalizedQuery) {
        const matchesQuery = `${room.id} ${room.name} ${room.location}`.toLowerCase().includes(normalizedQuery)
        if (!matchesQuery) return false
      }
      if (roomFilterCampus !== 'Todos' && room.campusLabel !== roomFilterCampus) return false
      if (roomFilterVenue !== 'Todos' && room.venueLabel !== roomFilterVenue) return false
      if (roomFilterLocation !== 'Todas' && room.location !== roomFilterLocation) return false
      if (roomStatusFilter !== 'Todos' && room.status !== roomStatusFilter) return false
      return true
    }).sort((a, b) => {
      if (roomSort === 'name:desc') return b.name.localeCompare(a.name)
      if (roomSort === 'code:asc') return a.id.localeCompare(b.id)
      if (roomSort === 'code:desc') return b.id.localeCompare(a.id)
      return a.name.localeCompare(b.name)
    })
  }, [roomDirectory, roomFilterCampus, roomFilterLocation, roomFilterVenue, roomSearchQuery, roomSort, roomStatusFilter])
  const totalProfilePages = profilesTotalPages
  const paginatedProfiles = profiles
  const pendingCancelTarget = useMemo(() => {
    if (!pendingCancelBooking) return null
    const booking = bookings.find((item) => item.id === pendingCancelBooking.bookingId)
    if (!booking) return null
    const room = activeRooms.find((item) => item.id === booking.roomId)
    return { booking, room, actor: pendingCancelBooking.actor }
  }, [activeRooms, bookings, pendingCancelBooking])
  const pendingProfileTarget = useMemo(() => {
    if (!pendingProfileAction) return null
    const profile = profiles.find((item) => item.id === pendingProfileAction.profileId)
    if (!profile) return null
    const nextStatus = profile.status === 'Habilitado' ? 'Deshabilitado' : 'Habilitado'
    return { profile, action: pendingProfileAction.action, nextStatus }
  }, [pendingProfileAction, profiles])
  const selectedReservationRoom = useMemo(
    () => activeRooms.find((room) => room.id === reservationForm.roomId) ?? null,
    [activeRooms, reservationForm.roomId],
  )
  const editBookingOwnerUser: AuthUser | null = editBookingOwner
    ? {
        id: 0,
        role: 'student',
        code: editBookingOwner.code,
        firstName: editBookingOwner.fullName,
        lastName: '',
        email: '',
        has2fa: false,
      }
    : authenticatedUser

  const pushNotification = (message: string, type: NotificationPreferenceKey = 'BOOKING_CONFIRMATION') => {
    const preference = notificationPrefs.notificationSettings[type]
    if (preference && !preference.app) return
    setToastMessage(message)
    setNotifications((current) => [
      {
        id: Date.now(),
        message,
        createdAt: new Date().toLocaleString('es-PE'),
      },
      ...current,
    ])
  }

  const setNotificationChannel = (key: NotificationPreferenceKey, channel: keyof NotificationChannelSettings, enabled: boolean) => {
    if (!authenticatedUser) return
    const nextSettings = normalizeNotificationSettings(authenticatedUser.role, notificationPrefs.notificationSettings)
    nextSettings[key] = {
      ...(nextSettings[key] ?? { app: true, email: true }),
      [channel]: enabled,
    }
    setNotificationPrefs({
      emailEnabled: Object.values(nextSettings).some((item) => item.email),
      reminderEnabled: Boolean(nextSettings.BOOKING_REMINDER?.email),
      bookingChangesEnabled: Boolean(nextSettings.BOOKING_UPDATE?.email || nextSettings.BOOKING_CANCELLATION?.email),
      notificationSettings: nextSettings,
    })
  }

  const setAllNotificationChannels = (enabled: boolean) => {
    const role = authenticatedUser?.role ?? 'student'
    const nextSettings = notificationPreferenceOptions(role).reduce<NotificationSettings>((acc, option) => {
      acc[option.key] = { app: enabled, email: option.email ? enabled : false }
      return acc
    }, {})
    setNotificationPrefs({
      emailEnabled: Object.values(nextSettings).some((item) => item.email),
      reminderEnabled: Boolean(nextSettings.BOOKING_REMINDER?.email),
      bookingChangesEnabled: Boolean(nextSettings.BOOKING_UPDATE?.email || nextSettings.BOOKING_CANCELLATION?.email),
      notificationSettings: nextSettings,
    })
  }

  const navigateToRoute = (nextRoute: RouteKey, options?: { replace?: boolean }) => {
    const nextPath = routePaths[nextRoute]
    if (location.pathname !== nextPath) navigate(nextPath, { replace: options?.replace ?? false })
    setRoute(nextRoute)
  }

  const handleReservationChange = (next: ReservationForm) => {
    setReservationForm(next)
    const selectionChanged =
      next.campus !== reservationForm.campus ||
      next.location !== reservationForm.location ||
      next.roomId !== reservationForm.roomId
    if (selectionChanged) {
      setReservationCompanions([])
      setReservationCompanionCodeInput('')
    }
  }

  const handleAddReservationCompanion = async () => {
    if (!token || !authenticatedUser || !selectedReservationRoom) return
    const code = reservationCompanionCodeInput.trim()
    if (!code) {
      setModalMessage({
        title: 'Código faltante',
        message: 'Ingresa un código antes de agregar a la persona.',
        variant: 'error',
      })
      return
    }
    const currentPeople = 1 + reservationCompanions.length
    if (currentPeople >= selectedReservationRoom.maxPeople) {
      setModalMessage({
        title: 'Límite alcanzado',
        message: `Esta sala permite como máximo ${selectedReservationRoom.maxPeople} personas.`,
        variant: 'error',
      })
      return
    }
    try {
      const found = await api.lookupUserByCode(token, code)
      if (found.code.toLowerCase() === authenticatedUser.code.toLowerCase()) {
        setModalMessage({
          title: 'Código duplicado',
          message: 'Tu código ya cuenta como la persona 1 de la reserva.',
          variant: 'error',
        })
        return
      }
      const duplicate = reservationCompanions.some(
        (item) => item.code.trim().toLowerCase() === found.code.toLowerCase(),
      )
      if (duplicate) {
        setModalMessage({
          title: 'Código repetido',
          message: 'Esa persona ya está agregada en la reserva.',
          variant: 'error',
        })
        return
      }
      setReservationCompanions((current) => [...current, { code: found.code, fullName: found.fullName }])
      setReservationCompanionCodeInput('')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo validar el código'
      setModalMessage({ title: 'Código inválido', message, variant: 'error' })
    }
  }

  const handleRemoveReservationCompanion = (index: number) => {
    setReservationCompanions((current) => current.filter((_, itemIndex) => itemIndex !== index))
  }

  const loadRooms = async (authToken: string) => {
    const result = await api.getRooms(authToken)
    const mapped = result.map(toUiRoom)
    setRooms(mapped)
    setRoomDirectory(mapped)
  }

  const loadRoomDirectory = async (authToken: string) => {
    const result = await api.getRooms(authToken)
    setRoomDirectory(result.map(toUiRoom))
  }

  const loadMyBookings = async (authToken: string) => {
    const result = await api.getBookingsMe(authToken)
    setBookings(dedupeBookingsByIdentity(result.map(toUiBooking)))
  }

  const loadAdminBookings = async (authToken: string) => {
    const result = await api.getAdminBookings(authToken, adminPage, adminStatusFilter, adminDateFilter)
    setBookings(dedupeBookingsByIdentity(result.content.map(toUiBooking)))
  }

  const loadAdminConfig = async (authToken: string) => {
    const result = await api.getAdminConfig(authToken)
    const nextConfig = {
      maxActiveBookings: result.maxActiveBookings,
      maxDurationMinutes: result.maxDurationMinutes,
    }
    setConfig(nextConfig)
    setConfigDraft(nextConfig)
  }

  const loadCampusSchedules = async (authToken: string) => {
    const result = await api.getCampusSchedules(authToken)
    setCampusSchedules(result.campuses.map(toUiCampusSchedule))
  }

  const loadProfiles = async (authToken: string) => {
    const result = await api.getUsers(authToken, profilesPage, {
      query: profilesQuery,
      year: profilesYearFilter,
      status: profilesStatusFilter,
      sortBy: profilesSortBy,
      sortDir: profilesSortDir,
    })
    setProfiles(result.content.map(toProfile))
    setProfilesTotalPages(Math.max(1, result.totalPages))
  }

  const toIsoDate = (date: Date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

  const getWeekRangeForOffset = (weekOffset: number) => {
    const base = new Date()
    const mondayShift = (base.getDay() + 6) % 7
    const monday = new Date(base)
    monday.setDate(base.getDate() - mondayShift + weekOffset * 7)
    const sunday = new Date(monday)
    sunday.setDate(monday.getDate() + 6)
    return { from: toIsoDate(monday), to: toIsoDate(sunday) }
  }

  const loadRoomBookingsWindow = async (authToken: string, roomBackendId: number, weekOffset: number) => {
    const { from, to } = getWeekRangeForOffset(weekOffset)
    const result = await api.getRoomBookings(authToken, roomBackendId, from, to)
    setRoomBookingsWindow(result.map(toUiBooking))
  }

  const bootstrap = async (authToken: string, user: AuthUser) => {
    await loadRooms(authToken)
    const landingRoute = await loadUserPreferences(authToken, user)
    if (user.role === 'admin') {
      await Promise.all([
        loadAdminBookings(authToken),
        loadAdminConfig(authToken),
        loadProfiles(authToken),
        loadCampusSchedules(authToken),
      ])
    } else {
      await loadMyBookings(authToken)
    }
    return landingRoute
  }

  const loadUserPreferences = async (authToken: string, user: AuthUser) => {
    try {
      const result = await api.getPreferences(authToken)
      setNotificationPrefs({
        emailEnabled: result.emailEnabled,
        reminderEnabled: result.reminderEnabled,
        bookingChangesEnabled: result.bookingChangesEnabled,
        notificationSettings: normalizeNotificationSettings(user.role, result.notificationSettings, result),
      })
      const nextDarkMode = result.themeMode === 'DARK'
      const nextFontScale = clampFontScale(result.fontScale)
      setIsDarkMode(nextDarkMode)
      setFontScale(nextFontScale)
      const landing = landingViewCodeToRoute(user.role, result.loginLandingView)
      setLoginLandingRoute(landing)
      localStorage.setItem(LOCAL_STORAGE_THEME_KEY, nextDarkMode ? '1' : '0')
      localStorage.setItem(LOCAL_STORAGE_FONT_SCALE_KEY, String(nextFontScale))
      localStorage.setItem(LOCAL_STORAGE_LANDING_KEY, landing)
      setPreferencesLoaded(true)
      return landing
    } catch {
      const cachedLanding = localStorage.getItem(LOCAL_STORAGE_LANDING_KEY) as RouteKey | null
      const allowedRoutes =
        user.role === 'admin'
          ? (['salas', 'admin-reservas'] as RouteKey[])
          : (['misreservas', 'reservas'] as RouteKey[])
      const fallbackLanding =
        cachedLanding && allowedRoutes.includes(cachedLanding)
          ? cachedLanding
          : defaultLandingRoute(user.role)
      setLoginLandingRoute(fallbackLanding)
      setPreferencesLoaded(true)
      return fallbackLanding
    }
  }

  useEffect(() => {
    setRoute(getRouteFromPath(location.pathname))
  }, [location.pathname])

  useEffect(() => {
    if (route !== 'reset-password') return
    const params = new URLSearchParams(location.search)
    const tokenFromLink = params.get('token') ?? ''
    if (!tokenFromLink) {
      setResetError('Debes abrir el enlace de recuperación enviado al correo')
      navigateToRoute('login', { replace: true })
      return
    }
    setResetToken(tokenFromLink)
  }, [route, location.search])

  useEffect(() => {
    const expectedPath = routePaths[effectiveRoute]
    if (route === 'login' && authenticatedUser && !preferencesLoaded) return
    if (!authHydrated && route !== 'login' && route !== 'reset-password') return
    if (location.pathname !== expectedPath) navigate(expectedPath, { replace: true })
  }, [authHydrated, effectiveRoute, route, authenticatedUser, preferencesLoaded, location.pathname, navigate])

  useEffect(() => {
    if (!toastMessage) return
    const timeout = window.setTimeout(() => setToastMessage(''), 3500)
    return () => window.clearTimeout(timeout)
  }, [toastMessage])

  useEffect(() => {
    sessionStorage.setItem(SESSION_STORAGE_NOTIFICATIONS_KEY, JSON.stringify(notifications))
  }, [notifications])

  useEffect(() => {
    setIsNotificationsModalOpen(false)
    setIsSettingsModalOpen(false)
  }, [effectiveRoute])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDarkMode)
    localStorage.setItem(LOCAL_STORAGE_THEME_KEY, isDarkMode ? '1' : '0')
  }, [isDarkMode])

  useEffect(() => {
    document.documentElement.style.fontSize = `${fontScale * 100}%`
    localStorage.setItem(LOCAL_STORAGE_FONT_SCALE_KEY, String(fontScale))
  }, [fontScale])

  useEffect(() => {
    if (!loginLandingRoute) return
    localStorage.setItem(LOCAL_STORAGE_LANDING_KEY, loginLandingRoute)
  }, [loginLandingRoute])

  useEffect(() => {
    if (!token || !authenticatedUser || !preferencesLoaded) return
    const payload: ApiPreferences = {
      ...notificationPrefs,
      themeMode: isDarkMode ? 'DARK' : 'LIGHT',
      fontScale: clampFontScale(fontScale),
      loginLandingView: routeToLandingViewCode(
        authenticatedUser.role,
        loginLandingRoute ?? defaultLandingRoute(authenticatedUser.role),
      ),
    }
    const signature = JSON.stringify(payload)
    if (!lastPreferencesSignature) {
      setLastPreferencesSignature(signature)
      return
    }
    if (signature === lastPreferencesSignature) return
    setLastPreferencesSignature(signature)
    const timeout = window.setTimeout(() => {
      api
        .updatePreferences(token, payload)
        .then(() => setToastMessage('Preferencias guardadas.'))
        .catch((error) => {
          const message = error instanceof Error ? error.message : 'No se pudieron guardar tus preferencias.'
          setToastMessage(message)
        })
    }, 300)
    return () => window.clearTimeout(timeout)
  }, [
    token,
    authenticatedUser,
    preferencesLoaded,
    notificationPrefs,
    isDarkMode,
    fontScale,
    loginLandingRoute,
    lastPreferencesSignature,
  ])

  useEffect(() => {
    if (authenticatedUser) {
      setAuthHydrated(true)
      return
    }
    if (!hasStoredSession) {
      setHasStoredSession(false)
      setAuthHydrated(true)
      return
    }
    api
      .me()
      .then((me) => {
        const user = toUiUser(me)
        setAuthenticatedUser(user)
        setToken('session')
        setHasStoredSession(true)
        bootstrap('session', user).catch(() => undefined)
      })
      .catch(() => {
        clearSessionHint()
        setHasStoredSession(false)
        setToken('')
      })
      .finally(() => setAuthHydrated(true))
  }, [authenticatedUser, hasStoredSession])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'admin') return
    loadAdminBookings(token).catch((error) => {
      const message = error instanceof Error ? error.message : 'No se pudieron cargar las reservas registradas.'
      setToastMessage(message)
    })
  }, [adminPage, adminCampusFilter, adminSearchQuery, adminStatusFilter, adminDateFilter, token, authenticatedUser])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'admin') return
    loadRoomDirectory(token).catch((error) => {
      const message = error instanceof Error ? error.message : 'No se pudo cargar el directorio de salas.'
      setToastMessage(message)
    })
  }, [authenticatedUser, token])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'admin') return
    loadProfiles(token).catch((error) => {
      const message = error instanceof Error ? error.message : 'No se pudieron cargar los perfiles.'
      setToastMessage(message)
    })
  }, [
    profilesPage,
    profilesQuery,
    profilesYearFilter,
    profilesStatusFilter,
    profilesSortBy,
    profilesSortDir,
    token,
    authenticatedUser,
  ])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'student') return
    const room = activeRooms.find((item) => item.id === reservationForm.roomId)
    if (!room) {
      setRoomBookingsWindow([])
      return
    }
    loadRoomBookingsWindow(token, room.backendId, reservationWeekOffset).catch(() => setRoomBookingsWindow([]))
  }, [token, authenticatedUser, activeRooms, reservationForm.roomId, reservationWeekOffset])

  useEffect(() => {
    if (!showResetSuccess) return
    const timeout = window.setTimeout(() => {
      setShowResetSuccess(false)
      setResetToken('')
      setResetPassword('')
      setResetPasswordConfirm('')
      setResetError('')
      navigateToRoute('login', { replace: true })
    }, 1500)
    return () => window.clearTimeout(timeout)
  }, [showResetSuccess])

  const clearMessages = () => {
    setLoginError('')
    setTwoFactorError('')
    setResetError('')
    setReservationError('')
    setRoomNotice('')
    setConfigNotice('')
  }

  const logout = () => {
    api.logout().catch(() => undefined)
    clearMessages()
    setAuthenticatedUser(null)
    setToken('')
    setPreferencesLoaded(false)
    setLastPreferencesSignature('')
    setShowTwoFactorModal(false)
    setTwoFactorCode('')
    setLoginLandingRoute(null)
    setNotifications([])
    sessionStorage.removeItem(SESSION_STORAGE_NOTIFICATIONS_KEY)
    clearSessionHint()
    setHasStoredSession(false)
    setAuthHydrated(true)
    navigateToRoute('login')
  }

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    clearMessages()
    try {
      const response = await api.login(loginEmail.trim(), loginPassword, rememberMe)
      if (response.twoFactorRequired) {
        setTwoFactorCode('')
        setTwoFactorError('')
        setShowTwoFactorModal(true)
        return
      }
      const user = toUiUser(response.user)
      setAuthenticatedUser(user)
      setToken('session')
      persistSessionHint(rememberMe)
      setHasStoredSession(true)
      const landingRoute = await bootstrap('session', user)
      navigateToRoute(landingRoute)
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Error al iniciar sesión')
    }
  }

  const handleTwoFactorSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoginError('')
    setTwoFactorError('')

    const code = twoFactorCode.trim()
    if (!code) {
      setTwoFactorError('Ingresa el código 2FA.')
      return
    }
    try {
      if (twoFactorAction === 'enable') {
        if (!token) {
          setTwoFactorError('Tu sesión expiró. Vuelve a iniciar sesión.')
          return
        }
        await api.confirm2faEnrollment(token, code)
        setAuthenticatedUser((current) => (current ? { ...current, has2fa: true } : current))
        setShowTwoFactorModal(false)
        setTwoFactorCode('')
        setTwoFactorAction('none')
        setModalMessage({
          title: '2FA activado',
          message: 'La autenticación en dos pasos quedó activada correctamente.',
          variant: 'success',
        })
        return
      }

      if (twoFactorAction === 'disable') {
        if (!token) {
          setTwoFactorError('Tu sesión expiró. Vuelve a iniciar sesión.')
          return
        }
        await api.confirmDisable2fa(token, code)
        setAuthenticatedUser((current) => (current ? { ...current, has2fa: false } : current))
        setShowTwoFactorModal(false)
        setTwoFactorCode('')
        setTwoFactorAction('none')
        setModalMessage({
          title: '2FA desactivado',
          message: 'La autenticación en dos pasos quedó desactivada correctamente.',
          variant: 'success',
        })
        return
      }

      const verification = await api.verify2fa(code, rememberMe)
      const user = toUiUser(verification.user)
      setAuthenticatedUser(user)
      setToken('session')
      persistSessionHint(rememberMe)
      setHasStoredSession(true)
      setShowTwoFactorModal(false)
      setTwoFactorCode('')
      const landingRoute = await bootstrap('session', user)
      navigateToRoute(landingRoute)
    } catch (error) {
      setTwoFactorError(error instanceof Error ? error.message : 'No se pudo verificar el código 2FA.')
    }
  }

  const handleToggleTwoFactor = async () => {
    if (!token || !authenticatedUser) return
    setTwoFactorError('')
    setTwoFactorCode('')
    try {
      if (authenticatedUser.has2fa) {
        await api.disable2fa(token)
        setTwoFactorAction('disable')
        setIsSettingsModalOpen(false)
        setShowTwoFactorModal(true)
        return
      }
      await api.enroll2fa(token)
      setTwoFactorAction('enable')
      setIsSettingsModalOpen(false)
      setShowTwoFactorModal(true)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo iniciar la verificación de 2FA'
      setModalMessage({ title: 'Error de seguridad', message, variant: 'error' })
    }
  }

  const handleForgotPasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      await api.requestReset(forgotEmail)
      setShowForgotModal(false)
      setModalMessage({
        title: 'Enlace enviado',
        message: 'Te enviamos un enlace de recuperación al correo institucional.',
        variant: 'success',
      })
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo procesar la solicitud'
      setModalMessage({ title: 'No se pudo enviar el enlace', message, variant: 'error' })
    }
  }

  const handleResetPasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setResetError('')
    if (!resetToken) {
      setResetError('El enlace de recuperación no es válido o no incluye token')
      return
    }
    if (resetPassword !== resetPasswordConfirm) {
      setResetError('Las contraseñas no coinciden')
      return
    }
    try {
      await api.confirmReset(resetToken, resetPassword)
      setShowResetSuccess(true)
    } catch (error) {
      setResetError(error instanceof Error ? error.message : 'No se pudo actualizar la contraseña')
    }
  }

  const getRoomById = (roomId: string) => rooms.find((room) => room.id === roomId)

  const handleCreateReservation = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    if (!reservationForm.campus || !reservationForm.location || !reservationForm.roomId || !reservationForm.date || !reservationForm.start || !reservationForm.end) {
      setModalMessage({
        title: 'Datos incompletos',
        message: 'Completa campus, ubicación, recurso, fecha y horario para reservar.',
        variant: 'error',
      })
      return
    }
    if (!authenticatedUser) return
    const selectedRoom = getRoomById(reservationForm.roomId)
    if (!selectedRoom) return
    const derivedPeople = 1 + reservationCompanions.length
    const requiredMinPeople = selectedRoom.minPeopleRequired ? selectedRoom.minPeople : 1
    const allowedMaxPeople = selectedRoom.maxPeople
    if (derivedPeople < requiredMinPeople || derivedPeople > allowedMaxPeople) {
      setModalMessage({
        title: 'Cantidad de personas inválida',
        message: `Esta sala permite entre ${requiredMinPeople} y ${allowedMaxPeople} personas para reservar.`,
        variant: 'error',
      })
      return
    }
    const allCodes = [authenticatedUser.code, ...reservationCompanions.map((item) => item.code.trim())].map((code) => code.toLowerCase())
    if (new Set(allCodes).size !== allCodes.length) {
      setModalMessage({
        title: 'Códigos repetidos',
        message: 'No se permiten códigos duplicados en la misma reserva.',
        variant: 'error',
      })
      return
    }
    if (minutesBetween(reservationForm.start, reservationForm.end) <= 0) {
      setModalMessage({
        title: 'Horario inválido',
        message: 'La hora de fin debe ser posterior a la hora de inicio.',
        variant: 'error',
      })
      return
    }
    try {
      const participants = [
        {
          code: authenticatedUser.code,
          fullName: `${authenticatedUser.firstName} ${authenticatedUser.lastName}`.trim(),
        },
        ...reservationCompanions,
      ]
      const observation = buildParticipantsObservation(participants)
      const created = await api.createBooking(token, {
        roomId: selectedRoom.backendId,
        location: reservationForm.location,
        people: derivedPeople,
        date: reservationForm.date,
        start: reservationForm.start,
        end: reservationForm.end,
        observation,
      })
      const newBooking = toUiBooking(created)
      setBookings((current) => dedupeBookingsByIdentity([newBooking, ...current.filter((booking) => booking.id !== newBooking.id)]))
      setShowBookingSuccess(true)
      setReservationForm(getDefaultReservationForm(activeRooms))
      setReservationWeekOffset(0)
      setReservationError('')
      setReservationCompanions([])
      setReservationCompanionCodeInput('')
      pushNotification(
        `Reserva confirmada: ${selectedRoom.resourceLabel} (${reservationForm.location}), ${formatDisplayDate(reservationForm.date)} ${reservationForm.start}-${reservationForm.end}.`,
        'BOOKING_CONFIRMATION',
      )
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo crear la reserva'
      setReservationError(message)
      setModalMessage({ title: 'No se pudo crear la reserva', message, variant: 'error' })
    }
  }

  const openEditBooking = (booking: Booking) => {
    const room = activeRooms.find((item) => item.id === booking.roomId)
    const participants = parseParticipantsFromObservation(booking.observation)
    const fallbackOwner = {
      code: `USER-${booking.userId}`,
      fullName: booking.userEmail ?? 'Usuario de la reserva',
    }
    const currentUserOwner = authenticatedUser
      ? {
          code: authenticatedUser.code,
          fullName: `${authenticatedUser.firstName} ${authenticatedUser.lastName}`.trim(),
        }
      : null
    const owner =
      authenticatedUser?.role === 'admin'
        ? participants[0] ?? fallbackOwner
        : currentUserOwner ?? participants.find((item) => item.code.toLowerCase() === authenticatedUser?.code.toLowerCase()) ?? fallbackOwner
    const companions =
      authenticatedUser?.role === 'admin'
        ? participants.slice(1)
        : participants.filter((item) => item.code.toLowerCase() !== owner.code.toLowerCase())
    setEditingBookingId(booking.id)
    setEditBookingOwner(owner)
    setReservationError('')
    setEditBookingForm({
      campus: room?.campusLabel ?? '',
      location: room?.venueLabel ?? booking.location,
      roomId: booking.roomId,
      people: booking.people,
      date: booking.date,
      start: booking.start,
      end: booking.end,
    })
    setEditBookingCompanions(companions)
    setEditCompanionCodeInput('')
  }

  const handleAddEditCompanion = async () => {
    if (!token || !editBookingOwner) return
    const selectedRoom = getRoomById(editBookingForm.roomId)
    if (!selectedRoom) return
    const code = editCompanionCodeInput.trim()
    if (!code) {
      setModalMessage({
        title: 'Código faltante',
        message: 'Ingresa un código antes de agregar a la persona.',
        variant: 'error',
      })
      return
    }
    const currentPeople = 1 + editBookingCompanions.length
    if (currentPeople >= selectedRoom.maxPeople) {
      setModalMessage({
        title: 'Límite alcanzado',
        message: `Esta sala permite como máximo ${selectedRoom.maxPeople} personas.`,
        variant: 'error',
      })
      return
    }
    try {
      const found = await api.lookupUserByCode(token, code)
      if (found.code.toLowerCase() === editBookingOwner.code.toLowerCase()) {
        setModalMessage({
          title: 'Código duplicado',
          message: 'Ese código ya corresponde a la persona titular de la reserva.',
          variant: 'error',
        })
        return
      }
      const duplicate = editBookingCompanions.some(
        (item) => item.code.trim().toLowerCase() === found.code.toLowerCase(),
      )
      if (duplicate) {
        setModalMessage({
          title: 'Código repetido',
          message: 'Esa persona ya está agregada en la reserva.',
          variant: 'error',
        })
        return
      }
      setEditBookingCompanions((current) => [...current, { code: found.code, fullName: found.fullName }])
      setEditCompanionCodeInput('')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo validar el código'
      setModalMessage({ title: 'Código inválido', message, variant: 'error' })
    }
  }

  const handleRemoveEditCompanion = (index: number) => {
    setEditBookingCompanions((current) => current.filter((_, itemIndex) => itemIndex !== index))
  }

  const handleSaveEditedBooking = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!editingBookingId || !token) return
    const selectedRoom = getRoomById(editBookingForm.roomId)
    const target = bookings.find((booking) => booking.id === editingBookingId)
    if (!selectedRoom || !target) return
    if (
      !editBookingForm.campus ||
      !editBookingForm.location ||
      !editBookingForm.roomId ||
      !editBookingForm.date ||
      !editBookingForm.start ||
      !editBookingForm.end
    ) {
      setModalMessage({
        title: 'Datos incompletos',
        message: 'Completa campus, ubicación, recurso, fecha y horario para editar.',
        variant: 'error',
      })
      return
    }
    const derivedPeople = 1 + editBookingCompanions.length
    const requiredMinPeople = selectedRoom.minPeopleRequired ? selectedRoom.minPeople : 1
    const allowedMaxPeople = selectedRoom.maxPeople
    if (derivedPeople < requiredMinPeople || derivedPeople > allowedMaxPeople) {
      setModalMessage({
        title: 'Cantidad de personas inválida',
        message: `Esta sala permite entre ${requiredMinPeople} y ${allowedMaxPeople} personas para reservar.`,
        variant: 'error',
      })
      return
    }
    const allCodes = [editBookingOwner?.code ?? '', ...editBookingCompanions.map((item) => item.code.trim())]
      .map((code) => code.toLowerCase())
      .filter(Boolean)
    if (new Set(allCodes).size !== allCodes.length) {
      setModalMessage({
        title: 'Códigos repetidos',
        message: 'No se permiten códigos duplicados en la misma reserva.',
        variant: 'error',
      })
      return
    }
    if (minutesBetween(editBookingForm.start, editBookingForm.end) <= 0) {
      setModalMessage({
        title: 'Horario inválido',
        message: 'La hora de fin debe ser posterior a la hora de inicio.',
        variant: 'error',
      })
      return
    }
    try {
      const participants = [
        editBookingOwner ?? { code: `USER-${target.userId}`, fullName: target.userEmail ?? 'Usuario de la reserva' },
        ...editBookingCompanions,
      ]
      const observation = buildParticipantsObservation(participants)
      const updated = await api.updateBooking(token, target.backendId, {
        roomId: selectedRoom.backendId,
        location: editBookingForm.location,
        people: derivedPeople,
        date: editBookingForm.date,
        start: editBookingForm.start,
        end: editBookingForm.end,
        observation,
      })
      const next = toUiBooking(updated)
      setBookings((current) => current.map((booking) => (booking.id === editingBookingId ? next : booking)))
      setEditingBookingId(null)
      setEditBookingOwner(null)
      setEditBookingCompanions([])
      setEditCompanionCodeInput('')
      pushNotification(
        `Reserva editada: ${selectedRoom.resourceLabel} (${editBookingForm.location}), ${formatDisplayDate(editBookingForm.date)} ${editBookingForm.start}-${editBookingForm.end}.`,
        'BOOKING_UPDATE',
      )
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo actualizar la reserva'
      setReservationError(message)
      setModalMessage({ title: 'No se pudo actualizar la reserva', message, variant: 'error' })
    }
  }

  const requestCancelBooking = (bookingId: string, actor: Role) => {
    setPendingCancelBooking({ bookingId, actor })
  }

  const cancelBooking = async () => {
    if (!token) return
    if (!pendingCancelBooking) return
    const { bookingId, actor } = pendingCancelBooking
    const target = bookings.find((booking) => booking.id === bookingId)
    if (!target) return
    const targetRoom = activeRooms.find((room) => room.id === target.roomId)
    const targetRoomLabel = targetRoom?.resourceLabel ?? target.roomId
    try {
      await api.cancelBooking(token, target.backendId)
      setBookings((current) =>
        current.map((booking) =>
          booking.id === bookingId ? { ...booking, status: 'Cancelado' } : booking,
        ),
      )
      pushNotification(
        actor === 'admin'
          ? `Reserva cancelada por administrador: ${targetRoomLabel} (${target.location}), ${formatDisplayDate(target.date)} ${target.start}-${target.end}.`
          : `Reserva cancelada: ${targetRoomLabel} (${target.location}), ${formatDisplayDate(target.date)} ${target.start}-${target.end}.`,
        'BOOKING_CANCELLATION',
      )
      setModalMessage({ title: 'Reserva cancelada', message: 'La cancelación se registró correctamente.', variant: 'success' })
      setPendingCancelBooking(null)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo cancelar la reserva'
      setModalMessage({ title: 'No se pudo cancelar', message, variant: 'error' })
    }
  }

  const downloadBookingIcs = async (booking: Booking) => {
    if (!token) return
    if (authenticatedUser?.role !== 'student' || booking.status !== 'Confirmado') {
      setModalMessage({
        title: 'No se puede exportar',
        message: 'Solo puedes exportar tus propias reservas confirmadas como estudiante.',
        variant: 'error',
      })
      return
    }

    try {
      const blob = await api.downloadBookingIcs(token, booking.backendId)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `reserva-${booking.backendId}.ics`
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo descargar el calendario'
      setModalMessage({ title: 'No se pudo exportar', message, variant: 'error' })
    }
  }

  const openAddRoom = () => {
    const firstCampus = campusValueOptions[0] ?? 'Monterrico'
    const firstLocation = venueOptionsByCampus.get(firstCampus)?.[0] ?? 'University Wellness Center'
    setRoomDraft({
      name: '',
      campus: firstCampus,
      location: firstLocation,
      capacity: 6,
      minPeople: 1,
      minPeopleRequired: false,
      maxPeople: 6,
      status: 'Disponible',
      schedule: [],
      pabellonCode: buildPabellonCode(firstCampus, firstLocation),
    })
    setRoomModalTargetId(null)
    setRoomModalMode('add')
  }

  const openEditRoom = (room: Room) => {
    setRoomDraft({
      name: room.name,
      campus: room.campus,
      location: room.venue,
      capacity: room.capacity,
      minPeople: room.minPeople,
      minPeopleRequired: room.minPeopleRequired,
      maxPeople: room.maxPeople,
      status: room.status,
      schedule: room.schedule,
      pabellonCode: buildPabellonCode(room.campus, room.venue),
    })
    setRoomModalTargetId(room.id)
    setRoomModalMode('edit')
  }

  const handleSaveRoom = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    const effectiveSchedule = roomDraft.schedule.length > 0 ? roomDraft.schedule : defaultWeeklySchedule
    const invalidScheduleDay = findInvalidScheduleDay(effectiveSchedule)
    if (!roomDraft.name.trim() || !roomDraft.campus.trim() || !roomDraft.location.trim()) {
      const message = 'Completa nombre, campus y ubicación de la sala.'
      setRoomNotice(message)
      setModalMessage({ title: 'Datos incompletos', message, variant: 'error' })
      return
    }
    if (
      !Number.isFinite(roomDraft.capacity) ||
      !Number.isFinite(roomDraft.minPeople) ||
      !Number.isFinite(roomDraft.maxPeople) ||
      roomDraft.capacity < 1 ||
      roomDraft.minPeople < 1 ||
      roomDraft.maxPeople < 1 ||
      roomDraft.maxPeople > roomDraft.capacity ||
      roomDraft.minPeople > roomDraft.maxPeople
    ) {
      const message = 'Revisa capacidad, mínimo y máximo de personas antes de guardar.'
      setRoomNotice(message)
      setModalMessage({ title: 'Cantidad de personas inválida', message, variant: 'error' })
      return
    }
    if (invalidScheduleDay) {
      const message = `El horario del día ${invalidScheduleDay.dayOfWeek} debe tener apertura y cierre válidos.`
      setRoomNotice(message)
      setModalMessage({ title: 'Horario inválido', message, variant: 'error' })
      return
    }
    const payload = {
      ...roomDraft,
      name: roomDraft.name.trim(),
      campus: roomDraft.campus.trim(),
      location: roomDraft.location.trim(),
      pabellonCode: roomDraft.pabellonCode.trim(),
      status: toApiRoomStatus(roomDraft.status),
      schedule: normalizeScheduleDays(effectiveSchedule),
    }
    try {
      if (roomModalMode === 'add') {
        const created = await api.createRoom(token, payload)
        setRooms((current) => [toUiRoom(created), ...current])
        setRoomSuccessId(created.code)
      } else if (roomModalMode === 'edit' && roomModalTargetId) {
        const target = rooms.find((room) => room.id === roomModalTargetId)
        if (!target) return
        const updated = await api.updateRoom(token, target.backendId, payload)
        setRooms((current) => current.map((room) => (room.id === roomModalTargetId ? toUiRoom(updated) : room)))
        pushNotification(`Sala actualizada: ${updated.resourceLabel}.`, 'ROOM_MAINTENANCE')
      }
      await loadRoomDirectory(token)
      setRoomModalMode('none')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo guardar la sala'
      setRoomNotice(message)
      setModalMessage({ title: 'Error al guardar sala', message, variant: 'error' })
    }
  }

  const handleDeleteRoomConfirmed = async () => {
    if (!pendingDeleteRoomId || !token) return
    const target = rooms.find((room) => room.id === pendingDeleteRoomId)
    if (!target) return
    try {
      await api.deleteRoom(token, target.backendId)
      setRooms((current) =>
        current.map((room) =>
          room.id === pendingDeleteRoomId ? { ...room, active: false } : room,
        ),
      )
      await loadRoomDirectory(token)
      pushNotification('Sala desactivada correctamente.', 'ROOM_MAINTENANCE')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo eliminar la sala'
      setModalMessage({ title: 'No se pudo eliminar la sala', message, variant: 'error' })
    } finally {
      setPendingDeleteRoomId(null)
    }
  }

  const requestProfileStatusChange = (profileId: string) => {
    const target = profiles.find((profile) => profile.id === profileId)
    const nextStatus = target?.status === 'Habilitado' ? 'Deshabilitado' : 'Habilitado'
    if (authenticatedUser?.id === Number(profileId) && nextStatus === 'Deshabilitado') {
      setModalMessage({
        title: 'Acción no permitida',
        message: 'No puedes deshabilitar tu propia cuenta mientras estás administrando el sistema.',
        variant: 'error',
      })
      return
    }
    setPendingProfileAction({ profileId, action: 'status' })
  }

  const requestProfileUnlock = (profileId: string) => {
    setPendingProfileAction({ profileId, action: 'unlock' })
  }

  const confirmProfileAction = async () => {
    if (!token || !pendingProfileAction) return
    const target = profiles.find((profile) => profile.id === pendingProfileAction.profileId)
    if (!target) {
      setPendingProfileAction(null)
      return
    }
    const nextStatus = target.status === 'Habilitado' ? 'Deshabilitado' : 'Habilitado'
    try {
      if (pendingProfileAction.action === 'unlock') {
        await api.unlockUser(token, Number(target.id))
        setToastMessage('Perfil desbloqueado correctamente.')
      } else {
        if (authenticatedUser?.id === Number(target.id) && nextStatus === 'Deshabilitado') {
          setModalMessage({
            title: 'Acción no permitida',
            message: 'No puedes deshabilitar tu propia cuenta mientras estás administrando el sistema.',
            variant: 'error',
          })
          return
        }
        await api.updateUserStatus(token, Number(target.id), nextStatus)
        setToastMessage(`Perfil ${nextStatus.toLowerCase()} correctamente.`)
      }
      await loadProfiles(token)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo actualizar el perfil'
      setModalMessage({ title: 'Error al actualizar perfil', message, variant: 'error' })
    } finally {
      setPendingProfileAction(null)
    }
  }

  const saveAdminConfig = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    if (
      !Number.isInteger(configDraft.maxActiveBookings) ||
      !Number.isInteger(configDraft.maxDurationMinutes) ||
      configDraft.maxActiveBookings < 1 ||
      configDraft.maxDurationMinutes < 30
    ) {
      setModalMessage({
        title: 'Configuración inválida',
        message: 'Revisa el máximo de reservas activas y la duración máxima antes de guardar.',
        variant: 'error',
      })
      return
    }
    try {
      const updated = await api.updateAdminConfig(token, {
        maxActiveBookings: configDraft.maxActiveBookings,
        maxDurationMinutes: configDraft.maxDurationMinutes,
      })
      setConfig({
        maxActiveBookings: updated.maxActiveBookings,
        maxDurationMinutes: updated.maxDurationMinutes,
      })
      setConfigNotice('Configuración actualizada correctamente.')
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo actualizar la configuración'
      setModalMessage({ title: 'Error de configuración', message, variant: 'error' })
    }
  }

  const saveCampusSchedule = async (campus: CampusSchedule) => {
    if (!token) return
    const invalidScheduleDay = findInvalidScheduleDay(campus.days)
    const unalignedScheduleDay = findUnalignedScheduleDay(campus.days, campus.slotMinutes)
    if (!campus.campus.trim() || !Number.isInteger(campus.slotMinutes) || campus.slotMinutes < 5) {
      setModalMessage({
        title: 'Horario inválido',
        message: 'Revisa el campus y la duración por reserva antes de guardar.',
        variant: 'error',
      })
      return
    }
    if (invalidScheduleDay) {
      setModalMessage({
        title: 'Horario inválido',
        message: `El horario del día ${invalidScheduleDay.dayOfWeek} debe tener apertura y cierre válidos.`,
        variant: 'error',
      })
      return
    }
    if (unalignedScheduleDay) {
      setModalMessage({
        title: 'Horario inválido',
        message: `En ${dayLabels[unalignedScheduleDay.dayOfWeek - 1]}, la apertura y el cierre deben ser múltiplos de ${campus.slotMinutes} minutos. Ajusta las horas antes de guardar.`,
        variant: 'error',
      })
      return
    }
    try {
      const updated = await api.updateCampusSchedule(token, {
        campus: campus.campus.trim(),
        slotMinutes: campus.slotMinutes,
        days: normalizeScheduleDays(campus.days),
      })
      setCampusSchedules((current) =>
        current.map((item) => (item.campus === updated.campus ? toUiCampusSchedule(updated) : item)),
      )
      if (updated.warnings.length > 0) {
        setModalMessage({
          title: 'Advertencia de horarios',
          message: updated.warnings.join('\n'),
          variant: 'error',
        })
      } else {
        setModalMessage({
          title: 'Horarios actualizados',
          message: `Se actualizó el horario general del campus ${updated.campusLabel}.`,
          variant: 'success',
        })
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No se pudo actualizar el horario del campus'
      setModalMessage({ title: 'Error al actualizar horario', message, variant: 'error' })
    }
  }

  const roomBookingsForSelectedRoom = useMemo(
    () => roomBookingsWindow.filter((booking) => booking.roomId === reservationForm.roomId),
    [roomBookingsWindow, reservationForm.roomId],
  )

  const showGlobalTopbar =
    Boolean(authenticatedUser) && effectiveRoute !== 'login' && effectiveRoute !== 'reset-password'
  const contentOffsetClass = showGlobalTopbar
    ? isSidebarCollapsed
      ? 'md:pl-24'
      : 'md:pl-64'
    : ''
  const mobileNavPaddingClass = showGlobalTopbar ? 'pt-16 md:pt-0 pb-20 md:pb-0' : ''
  const shouldShowAuthRestoreScreen = hasStoredSession && !authHydrated && route !== 'reset-password'
  const settingsRole = authenticatedUser?.role ?? 'student'
  const settingsNotificationOptions = notificationPreferenceOptions(settingsRole)
  const settingsNotificationGroups = settingsNotificationOptions.reduce<Record<string, NotificationPreferenceOption[]>>((acc, option) => {
    acc[option.group] = [...(acc[option.group] ?? []), option]
    return acc
  }, {})
  const allNotificationsEnabled = settingsNotificationOptions.every((option) => {
    const preference = notificationPrefs.notificationSettings[option.key] ?? { app: option.app, email: option.email }
    return preference.app && (!option.email || preference.email)
  })

  return (
    <>
      {shouldShowAuthRestoreScreen ? (
        <AuthRestoreScreen />
      ) : (
        <>
          {showGlobalTopbar && authenticatedUser && (
            <GlobalTopbar
              role={authenticatedUser.role}
              activeRoute={effectiveRoute}
              notifications={notifications}
              isSidebarCollapsed={isSidebarCollapsed}
              onNavigate={(nextRoute) => {
                setIsNotificationsModalOpen(false)
                setIsSettingsModalOpen(false)
                navigateToRoute(nextRoute)
              }}
              onToggleSidebar={() => setIsSidebarCollapsed((current) => !current)}
              onOpenNotifications={() => setIsNotificationsModalOpen(true)}
              onOpenSettings={() => setIsSettingsModalOpen(true)}
            />
          )}

          <div className={`${contentOffsetClass} ${mobileNavPaddingClass} min-h-screen`}>
            {effectiveRoute === 'login' && (
              <LoginPage
                loginEmail={loginEmail}
                loginPassword={loginPassword}
                rememberMe={rememberMe}
                loginError={loginError}
                onLoginEmailChange={setLoginEmail}
                onLoginPasswordChange={setLoginPassword}
                onRememberMeChange={setRememberMe}
                onOpenForgotModal={() => {
                  setForgotEmail(loginEmail)
                  setShowForgotModal(true)
                  setResetError('')
                }}
                onSubmitLogin={handleLoginSubmit}
              />
            )}
            {effectiveRoute === 'reset-password' && (
              <ResetPasswordPage
                resetPassword={resetPassword}
                resetPasswordConfirm={resetPasswordConfirm}
                resetError={resetError}
                showResetSuccess={showResetSuccess}
                onResetPasswordChange={setResetPassword}
                onResetPasswordConfirmChange={setResetPasswordConfirm}
                onSubmitResetPassword={handleResetPasswordSubmit}
                onBackToLogin={() => navigateToRoute('login')}
              />
            )}
            {effectiveRoute === 'reservas' && (
              <ReservasPage
                reservationForm={reservationForm}
                reservationError={reservationError}
                campusOptions={roomCampusOptions}
                locationOptionsByCampus={locationOptionsByCampus}
                activeRooms={activeRooms}
                selectedRoomCapacity={selectedReservationRoom?.capacity ?? null}
                roomBookings={roomBookingsForSelectedRoom}
                weekOffset={reservationWeekOffset}
                onReservationChange={handleReservationChange}
                onAddCompanion={handleAddReservationCompanion}
                currentUser={authenticatedUser}
                companions={reservationCompanions}
                companionCodeInput={reservationCompanionCodeInput}
                onCompanionCodeInputChange={setReservationCompanionCodeInput}
                onRemoveCompanion={handleRemoveReservationCompanion}
                onWeekOffsetChange={setReservationWeekOffset}
                onClearReservationForm={() => {
                  setReservationForm(getDefaultReservationForm(activeRooms))
                  setReservationCompanions([])
                  setReservationCompanionCodeInput('')
                }}
                onSubmitReservation={handleCreateReservation}
              />
            )}
            {effectiveRoute === 'misreservas' && (
              <MisReservasPage
                myBookings={myBookings}
                activeRooms={activeRooms}
                onEditBooking={openEditBooking}
                onCancelBooking={(bookingId) => requestCancelBooking(bookingId, 'student')}
                onCreateFirstReservation={() => navigateToRoute('reservas')}
                onDownloadIcs={downloadBookingIcs}
              />
            )}
            {effectiveRoute === 'salas' && (
              <SalasPage
                filteredRooms={filteredRooms}
                campusOptions={campusOptions}
                venueOptions={venueOptions}
                locationOptions={roomLocationOptions}
                roomSearchQuery={roomSearchQuery}
                roomFilterCampus={roomFilterCampus}
                roomFilterVenue={roomFilterVenue}
                roomFilterLocation={roomFilterLocation}
                roomStatusFilter={roomStatusFilter}
                roomSort={roomSort}
                roomNotice={roomNotice}
                onRoomSearchChange={setRoomSearchQuery}
                onRoomFilterCampusChange={(value) => {
                  setRoomFilterCampus(value)
                  setRoomFilterVenue('Todos')
                  setRoomFilterLocation('Todas')
                }}
                onRoomFilterVenueChange={(value) => {
                  setRoomFilterVenue(value)
                  setRoomFilterLocation('Todas')
                }}
                onRoomFilterLocationChange={setRoomFilterLocation}
                onRoomStatusFilterChange={setRoomStatusFilter}
                onRoomSortChange={setRoomSort}
                onResetRoomFilters={() => {
                  setRoomSearchQuery('')
                  setRoomFilterCampus('Todos')
                  setRoomFilterVenue('Todos')
                  setRoomFilterLocation('Todas')
                  setRoomStatusFilter('Todos')
                  setRoomSort('name:asc')
                }}
                onOpenAddRoom={openAddRoom}
                onOpenEditRoom={openEditRoom}
                onAskDeleteRoom={setPendingDeleteRoomId}
              />
            )}
            {effectiveRoute === 'perfiles' && (
              <PerfilesPage
                paginatedProfiles={paginatedProfiles}
                profilesPage={profilesPage}
                totalProfilePages={totalProfilePages}
                searchQuery={profilesQuery}
                yearFilter={profilesYearFilter}
                statusFilter={profilesStatusFilter}
                sortBy={profilesSortBy}
                sortDir={profilesSortDir}
                onSearchQueryChange={(value) => {
                  setProfilesPage(1)
                  setProfilesQuery(value)
                }}
                onYearFilterChange={(value) => {
                  setProfilesPage(1)
                  setProfilesYearFilter(value.replace(/\D/g, '').slice(0, 4))
                }}
                onStatusFilterChange={(value) => {
                  setProfilesPage(1)
                  setProfilesStatusFilter(value)
                }}
                onSortChange={(value) => {
                  setProfilesPage(1)
                  setProfilesSortBy(value)
                }}
                onSortDirectionToggle={() => {
                  setProfilesPage(1)
                  setProfilesSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
                }}
                onResetFilters={() => {
                  setProfilesPage(1)
                  setProfilesQuery('')
                  setProfilesYearFilter('')
                  setProfilesStatusFilter('Todos')
                  setProfilesSortBy('firstName')
                  setProfilesSortDir('asc')
                }}
                onToggleProfileStatus={requestProfileStatusChange}
                onUnlockProfile={requestProfileUnlock}
                onPrevPage={() => setProfilesPage((current) => Math.max(1, current - 1))}
                onNextPage={() => setProfilesPage((current) => Math.min(totalProfilePages, current + 1))}
              />
            )}
            {effectiveRoute === 'admin-reservas' && (
              <AdminReservasPage
                bookings={adminBookingsPage}
                users={[]}
                adminSearchQuery={adminSearchQuery}
                adminStatusFilter={adminStatusFilter}
                adminCampusFilter={adminCampusFilter}
                adminDateFilter={adminDateFilter}
                adminDateQuickFilter={adminDateQuickFilter}
                adminSort={adminSort}
                adminPage={adminPage}
                totalAdminPages={totalAdminPages}
                campusOptions={campusOptions}
                config={config}
                configDraft={configDraft}
                configNotice={configNotice}
                campusSchedules={campusSchedules}
                onSearchQueryChange={(value) => {
                  setAdminSearchQuery(value)
                  setAdminPage(1)
                }}
                onStatusFilterChange={(value) => {
                  setAdminStatusFilter(value)
                  setAdminPage(1)
                }}
                onCampusFilterChange={(value) => {
                  setAdminCampusFilter(value)
                  setAdminPage(1)
                }}
                onDateFilterChange={(value) => {
                  setAdminDateQuickFilter('none')
                  setAdminDateFilter(value)
                  setAdminPage(1)
                }}
                onTodayFilter={() => {
                  setAdminDateQuickFilter('today')
                  setAdminDateFilter(getTodayIso())
                  setAdminPage(1)
                }}
                onWeekFilter={() => {
                  setAdminDateQuickFilter('week')
                  setAdminDateFilter('')
                  setAdminPage(1)
                }}
                onClearDateFilter={() => {
                  setAdminSearchQuery('')
                  setAdminStatusFilter('Todos')
                  setAdminCampusFilter('Todos')
                  setAdminDateQuickFilter('none')
                  setAdminDateFilter('')
                  setAdminSort('date:desc')
                  setAdminPage(1)
                }}
                onSortChange={(value) => {
                  setAdminSort(value)
                  setAdminPage(1)
                }}
                onPrevPage={() => setAdminPage((current) => current - 1)}
                onNextPage={() => setAdminPage((current) => current + 1)}
                onEditBooking={openEditBooking}
                onCancelBooking={(bookingId) => requestCancelBooking(bookingId, 'admin')}
                onConfigDraftChange={setConfigDraft}
                onSaveConfig={saveAdminConfig}
                onCampusScheduleChange={(nextCampus) =>
                  setCampusSchedules((current) =>
                    current.map((item) => (item.campus === nextCampus.campus ? nextCampus : item)),
                  )
                }
                onSaveCampusSchedule={saveCampusSchedule}
              />
            )}
          </div>
        </>
      )}

      {showBookingSuccess && (
        <BookingSuccessModal
          onClose={() => {
            setShowBookingSuccess(false)
            navigateToRoute('misreservas')
          }}
        />
      )}
      {editingBookingId && (
        <EditBookingModal
          form={editBookingForm}
          campusOptions={campusOptions}
          locationOptionsByCampus={locationOptionsByCampus}
          activeRooms={activeRooms}
          currentUser={editBookingOwnerUser}
          companions={editBookingCompanions}
          companionCodeInput={editCompanionCodeInput}
          errorMessage={reservationError}
          onChange={setEditBookingForm}
          onCompanionCodeInputChange={setEditCompanionCodeInput}
          onAddCompanion={handleAddEditCompanion}
          onRemoveCompanion={handleRemoveEditCompanion}
          onCancel={() => {
            setEditingBookingId(null)
            setEditBookingOwner(null)
            setEditBookingCompanions([])
            setEditCompanionCodeInput('')
          }}
          onSubmit={handleSaveEditedBooking}
        />
      )}
      {roomModalMode !== 'none' && (
        <RoomFormModal
          mode={roomModalMode}
          draft={roomDraft}
          notice={roomNotice}
          targetRoomId={roomModalTargetId}
          campusOptions={campusValueOptions}
          venueOptionsByCampus={venueOptionsByCampus}
          onChange={setRoomDraft}
          onCancel={() => setRoomModalMode('none')}
          onSubmit={handleSaveRoom}
        />
      )}
      {roomSuccessId && <RoomSuccessModal roomId={roomSuccessId} onClose={() => setRoomSuccessId('')} />}
      {pendingDeleteRoomId && (
        <DeleteRoomModal
          roomId={pendingDeleteRoomId}
          onCancel={() => setPendingDeleteRoomId(null)}
          onConfirm={handleDeleteRoomConfirmed}
        />
      )}
      {showForgotModal && (
        <ForgotPasswordModal
          forgotEmail={forgotEmail}
          onForgotEmailChange={setForgotEmail}
          onCancel={() => setShowForgotModal(false)}
          onSubmit={handleForgotPasswordSubmit}
        />
      )}
      {showTwoFactorModal && (
        <TwoFactorModal
          code={twoFactorCode}
          errorMessage={twoFactorError}
          title={
            twoFactorAction === 'enable'
              ? 'Confirmar activación de 2FA'
              : twoFactorAction === 'disable'
                ? 'Confirmar desactivación de 2FA'
                : 'Verificar inicio de sesión'
          }
          description={
            twoFactorAction === 'none'
              ? 'Ingresa el código 2FA enviado a tu correo.'
              : 'Te enviamos un código de confirmación a tu correo. Ingrésalo para continuar.'
          }
          submitLabel={twoFactorAction === 'none' ? 'Verificar' : 'Confirmar'}
          onCodeChange={setTwoFactorCode}
          onCancel={() => {
            setShowTwoFactorModal(false)
            setTwoFactorCode('')
            setTwoFactorError('')
            setTwoFactorAction('none')
          }}
          onSubmit={handleTwoFactorSubmit}
        />
      )}
      {isNotificationsModalOpen && (
        <div className="modal-layer" onClick={() => setIsNotificationsModalOpen(false)}>
          <div className="modal-card slim-modal text-left" onClick={(event) => event.stopPropagation()}>
            <h2>Notificaciones</h2>
            {notifications.length === 0 ? (
              <p className="modal-copy">Aún no hay notificaciones</p>
            ) : (
              <ul className="m-0 mt-2 max-h-72 list-none space-y-2 overflow-y-auto p-0">
                {notifications.map((item) => (
                  <li key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 p-2">
                    <p className="m-0 text-xs font-medium text-slate-800">{item.message}</p>
                    <p className="m-0 mt-1 text-[11px] text-slate-500">{item.createdAt}</p>
                  </li>
                ))}
              </ul>
            )}
            <div className="modal-actions">
              <button type="button" className="ghost-btn" onClick={() => setIsNotificationsModalOpen(false)}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
      {isSettingsModalOpen && (
        <div className="modal-layer" onClick={() => setIsSettingsModalOpen(false)}>
          <div className="modal-card slim-modal text-left settings-dialog" onClick={(event) => event.stopPropagation()}>
            <div className="settings-header">
              <h2>Configuración</h2>
              <button
                type="button"
                className="settings-close-btn"
                onClick={() => setIsSettingsModalOpen(false)}
                aria-label="Cerrar configuración"
              >
                ×
              </button>
            </div>

            <div className="settings-section">
              <p className="settings-badge-ink">Apariencia</p>
              <div className="settings-appearance-row">
                <p className="settings-field-title">Tema</p>
                <div className={`theme-toggle ${isDarkMode ? 'dark-active' : 'light-active'}`} role="group" aria-label="Seleccionar tema">
                  <span className="theme-toggle-thumb" aria-hidden="true" />
                  <button
                    type="button"
                    className={`theme-toggle-btn ${!isDarkMode ? 'active' : ''}`}
                    onClick={() => setIsDarkMode(false)}
                    aria-pressed={!isDarkMode}
                    aria-label="Activar tema claro"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <circle cx="12" cy="12" r="4" />
                      <path d="M12 2v2.2M12 19.8V22M4.9 4.9l1.6 1.6M17.5 17.5l1.6 1.6M2 12h2.2M19.8 12H22M4.9 19.1l1.6-1.6M17.5 6.5l1.6-1.6" />
                    </svg>
                    Claro
                  </button>
                  <button
                    type="button"
                    className={`theme-toggle-btn ${isDarkMode ? 'active' : ''}`}
                    onClick={() => setIsDarkMode(true)}
                    aria-pressed={isDarkMode}
                    aria-label="Activar tema oscuro"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M20 14.2A8 8 0 1 1 9.8 4a7 7 0 1 0 10.2 10.2z" />
                    </svg>
                    Oscuro
                  </button>
                </div>
              </div>
              <div className="settings-appearance-row">
                <p className="settings-field-title">Tamaño</p>
                <div className="settings-scale-control">
                  <input
                    type="range"
                    className="settings-range"
                    min={0.85}
                    max={1.3}
                    step={0.05}
                    value={fontScale}
                    onChange={(event) => setFontScale(clampFontScale(Number(event.target.value)))}
                  />
                  <span className="settings-scale-value">{Math.round(fontScale * 100)}%</span>
                </div>
              </div>
            </div>

            <div className="settings-divider" />

            <div className="settings-section">
              <p className="settings-badge-ink">Notificaciones</p>
              <div className="notification-preference-list">
                {Object.entries(settingsNotificationGroups).map(([group, options]) => (
                  <div className="notification-preference-group" key={group}>
                    <h3>{group}</h3>
                    {group === 'Reservas' && (
                      <div className="notification-preference-row notification-master-row">
                        <span className="notification-preference-label">Activar todas las notificaciones</span>
                        <button
                          type="button"
                          className={`settings-switch ${allNotificationsEnabled ? 'active' : ''}`}
                          role="switch"
                          aria-checked={allNotificationsEnabled}
                          onClick={() => setAllNotificationChannels(!allNotificationsEnabled)}
                        >
                          <span />
                        </button>
                      </div>
                    )}
                    {options.map((option) => {
                      const preference = notificationPrefs.notificationSettings[option.key] ?? {
                        app: option.app,
                        email: option.email,
                      }
                      return (
                        <div className="notification-preference-row" key={option.key}>
                          <span className="notification-preference-label">{option.label}</span>
                          <div className="notification-channel-controls">
                            <span className="notification-channel-badge app">App</span>
                            <button
                              type="button"
                              className={`settings-switch compact ${preference.app ? 'active' : ''}`}
                              role="switch"
                              aria-checked={preference.app}
                              aria-label={`${option.label} por App`}
                              onClick={() => setNotificationChannel(option.key, 'app', !preference.app)}
                            >
                              <span />
                            </button>
                            {option.email && (
                              <>
                                <span className="notification-channel-badge email">Email</span>
                                <button
                                  type="button"
                                  className={`settings-switch compact ${preference.email ? 'active' : ''}`}
                                  role="switch"
                                  aria-checked={preference.email}
                                  aria-label={`${option.label} por Email`}
                                  onClick={() => setNotificationChannel(option.key, 'email', !preference.email)}
                                >
                                  <span />
                                </button>
                              </>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                ))}
              </div>
            </div>

            <div className="settings-divider" />

            <div className="settings-section">
              <p className="settings-badge-ink">Seguridad</p>
              <div className="two-factor-title-row">
                <p className="settings-field-title">Autenticación en dos pasos (2FA)</p>
                <span className={`two-factor-status-pill ${authenticatedUser?.has2fa ? 'active' : 'inactive'}`}>
                  {authenticatedUser?.has2fa ? 'Activado' : 'Desactivado'}
                </span>
              </div>
              <button
                type="button"
                className={`settings-action-btn ${authenticatedUser?.has2fa ? 'danger' : ''}`}
                onClick={handleToggleTwoFactor}
                disabled={!authenticatedUser}
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                  <rect x="3" y="11" width="18" height="10" rx="2" />
                  <path d="M7 11V8a5 5 0 0 1 10 0v3" />
                </svg>
                {authenticatedUser?.has2fa ? 'Desactivar 2FA' : 'Activar 2FA'}
              </button>
            </div>

            <div className="settings-divider" />

            <div className="settings-section">
              <p className="settings-badge-ink">Sesión</p>
              <p className="settings-field-title">Vista inicial al iniciar sesión</p>
              <select
                className="settings-select"
                value={loginLandingRoute ?? (authenticatedUser ? defaultLandingRoute(authenticatedUser.role) : 'misreservas')}
                onChange={(event) => setLoginLandingRoute(event.target.value as RouteKey)}
                disabled={!authenticatedUser}
              >
                {authenticatedUser?.role === 'admin' ? (
                  <>
                    <option value="salas">Salas</option>
                    <option value="perfiles">Perfiles</option>
                    <option value="admin-reservas">Reservas</option>
                  </>
                ) : (
                  <>
                    <option value="misreservas">Mis reservas</option>
                    <option value="reservas">Reservar</option>
                  </>
                )}
              </select>
              <p className="settings-note">Esta vista se abrirá automáticamente cuando inicies sesión.</p>
              <button type="button" className="settings-logout-btn" onClick={logout}>
                Cerrar sesión
              </button>
            </div>

          </div>
        </div>
      )}
      {modalMessage && (
        <MessageModal
          title={modalMessage.title}
          message={modalMessage.message}
          variant={modalMessage.variant}
          onClose={() => setModalMessage(null)}
        />
      )}
      {pendingCancelTarget && (
        <ConfirmCancelBookingModal
          bookingLabel={pendingCancelTarget.actor === 'admin' ? 'esta reserva' : 'tu reserva'}
          roomLabel={pendingCancelTarget.room?.resourceLabel ?? pendingCancelTarget.booking.roomId}
          location={pendingCancelTarget.booking.location}
          dateLabel={formatDisplayDate(pendingCancelTarget.booking.date)}
          timeLabel={`${pendingCancelTarget.booking.start}-${pendingCancelTarget.booking.end}`}
          actor={pendingCancelTarget.actor}
          onConfirm={cancelBooking}
          onClose={() => setPendingCancelBooking(null)}
        />
      )}
      {pendingProfileTarget && (
        <section className="modal-layer" role="dialog" aria-modal="true" aria-labelledby="profile-action-title">
          <div className="modal-card slim-modal text-left">
            <h2 id="profile-action-title">
              {pendingProfileTarget.action === 'unlock'
                ? 'Confirmar desbloqueo'
                : `Confirmar ${pendingProfileTarget.nextStatus.toLowerCase()}`}
            </h2>
            <p className="modal-copy">
              {pendingProfileTarget.action === 'unlock'
                ? `Vas a desbloquear la cuenta de ${pendingProfileTarget.profile.email}.`
                : `Vas a cambiar el estado de ${pendingProfileTarget.profile.email} a ${pendingProfileTarget.nextStatus}.`}
            </p>
            <div className="modal-actions">
              <button
                type="button"
                className="ghost-btn"
                onClick={() => setPendingProfileAction(null)}
              >
                Cancelar
              </button>
              <button
                type="button"
                className={pendingProfileTarget.nextStatus === 'Deshabilitado' ? 'danger-btn' : 'primary-btn'}
                onClick={confirmProfileAction}
              >
                Confirmar
              </button>
            </div>
          </div>
        </section>
      )}
      {toastMessage && <div className="toast">{toastMessage}</div>}
    </>
  )
}
