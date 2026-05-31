import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getRouteFromPath, resolveRouteByAuth, routePaths } from '../../viewmodels/routes'
import type {
  AuthUser,
  Booking,
  BookingStatus,
  Profile,
  ReservationForm,
  Role,
  Room,
  RoomDraft,
  RouteKey,
  SystemConfig,
} from '../../models/types'
import { getDefaultReservationForm, minutesBetween, toMinutes } from '../../utils/helpers'
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
import { GlobalTopbar } from '../components/layout/GlobalTopbar'
import { api, type ApiPreferences } from '../../services/api'

interface NotificationItem {
  id: number
  message: string
  createdAt: string
}

interface AvailabilitySlot {
  start: string
  end: string
  isAvailable: boolean
}

interface AvailabilityDay {
  date: string
  label: string
  hasAvailability: boolean
}

const clampFontScale = (value: number) => Math.min(1.3, Math.max(0.85, Number.isFinite(value) ? value : 1))

const toUiRole = (role: 'ADMIN' | 'ESTUDIANTE'): Role => (role === 'ADMIN' ? 'admin' : 'student')

const toUiUser = (user: {
  id: number
  code: string
  firstName: string
  lastName: string
  email: string
  role: 'ADMIN' | 'ESTUDIANTE'
}): AuthUser => ({
  id: user.id,
  role: toUiRole(user.role),
  code: user.code,
  firstName: user.firstName,
  lastName: user.lastName,
  email: user.email,
})

const toUiRoom = (room: {
  id: number
  code: string
  name: string
  capacity: number
  location: string
  status: string
}): Room => ({
  backendId: room.id,
  id: room.code,
  name: room.name,
  capacity: room.capacity,
  location: room.location,
  active: room.status !== 'INACTIVA',
})

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
  start: booking.start,
  end: booking.end,
  status: booking.status === 'CANCELADA' ? 'Cancelado' : 'Confirmado',
})

const toProfile = (user: {
  id: number
  code: string
  email: string
  firstName: string
  lastName: string
  status: 'HABILITADO' | 'DESHABILITADO'
}): Profile => ({
  id: String(user.id),
  code: user.code,
  email: user.email,
  firstName: user.firstName,
  lastName: user.lastName,
  status: user.status === 'HABILITADO' ? 'Habilitado' : 'Deshabilitado',
})

export function MainPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const [route, setRoute] = useState<RouteKey>(() => getRouteFromPath(location.pathname))

  const [token, setToken] = useState('')
  const [authenticatedUser, setAuthenticatedUser] = useState<AuthUser | null>(null)

  const [loginEmail, setLoginEmail] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [loginError, setLoginError] = useState('')
  const [twoFactorError, setTwoFactorError] = useState('')
  const [showTwoFactorModal, setShowTwoFactorModal] = useState(false)
  const [provisionalToken, setProvisionalToken] = useState('')
  const [twoFactorCode, setTwoFactorCode] = useState('')

  const [showForgotModal, setShowForgotModal] = useState(false)
  const [forgotEmail, setForgotEmail] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [resetPassword, setResetPassword] = useState('')
  const [resetPasswordConfirm, setResetPasswordConfirm] = useState('')
  const [resetError, setResetError] = useState('')
  const [showResetSuccess, setShowResetSuccess] = useState(false)

  const [rooms, setRooms] = useState<Room[]>([])
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
    location: '',
    roomId: '',
    people: 2,
    date: '',
    start: '16:30',
    end: '17:30',
  })
  const [reservationError, setReservationError] = useState('')
  const [confirmationBookingId, setConfirmationBookingId] = useState('')

  const [editingBookingId, setEditingBookingId] = useState<string | null>(null)
  const [editBookingForm, setEditBookingForm] = useState<ReservationForm>({
    location: '',
    roomId: '',
    people: 2,
    date: '',
    start: '16:30',
    end: '17:30',
  })

  const [roomFilterLocation, setRoomFilterLocation] = useState('Todas')
  const [roomModalMode, setRoomModalMode] = useState<'none' | 'add' | 'edit'>('none')
  const [roomModalTargetId, setRoomModalTargetId] = useState<string | null>(null)
  const [roomDraft, setRoomDraft] = useState<RoomDraft>({
    name: '',
    location: 'E3',
    capacity: 6,
  })
  const [roomNotice, setRoomNotice] = useState('')
  const [roomSuccessId, setRoomSuccessId] = useState('')
  const [pendingDeleteRoomId, setPendingDeleteRoomId] = useState<string | null>(null)

  const [profilesPage, setProfilesPage] = useState(1)
  const [adminStatusFilter, setAdminStatusFilter] = useState<'Todos' | BookingStatus>('Todos')
  const [adminDateFilter, setAdminDateFilter] = useState('')
  const [adminPage, setAdminPage] = useState(1)
  const [profilesQuery, setProfilesQuery] = useState('')

  const [toastMessage, setToastMessage] = useState('')
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [isNotificationsModalOpen, setIsNotificationsModalOpen] = useState(false)
  const [isSettingsModalOpen, setIsSettingsModalOpen] = useState(false)
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)
  const [isDarkMode, setIsDarkMode] = useState(false)
  const [fontScale, setFontScale] = useState(1)
  const [preferencesLoaded, setPreferencesLoaded] = useState(false)
  const [notificationPrefs, setNotificationPrefs] = useState<Pick<ApiPreferences, 'emailEnabled' | 'reminderEnabled' | 'bookingChangesEnabled'>>({
    emailEnabled: true,
    reminderEnabled: true,
    bookingChangesEnabled: true,
  })

  const effectiveRoute = useMemo(
    () => resolveRouteByAuth(route, authenticatedUser),
    [route, authenticatedUser],
  )
  const activeRooms = useMemo(() => rooms.filter((room) => room.active), [rooms])
  const locationOptions = useMemo(
    () => [...new Set(activeRooms.map((room) => room.location))],
    [activeRooms],
  )
  const myBookings = useMemo(() => {
    if (!authenticatedUser) return []
    return bookings
      .filter((booking) => booking.userId === authenticatedUser.id)
      .sort((a, b) => `${a.date} ${a.start}`.localeCompare(`${b.date} ${b.start}`))
  }, [authenticatedUser, bookings])
  const adminBookings = useMemo(() => {
    return bookings
      .filter((booking) => {
        if (adminStatusFilter !== 'Todos' && booking.status !== adminStatusFilter) return false
        if (adminDateFilter && booking.date !== adminDateFilter) return false
        return true
      })
      .sort((a, b) => `${b.date} ${b.start}`.localeCompare(`${a.date} ${a.start}`))
  }, [bookings, adminDateFilter, adminStatusFilter])
  const pageSize = 5
  const totalAdminPages = Math.max(1, Math.ceil(adminBookings.length / pageSize))
  const adminBookingsPage = adminBookings.slice((adminPage - 1) * pageSize, adminPage * pageSize)
  const filteredRooms = useMemo(() => {
    if (roomFilterLocation === 'Todas') return activeRooms
    return activeRooms.filter((room) => room.location === roomFilterLocation)
  }, [activeRooms, roomFilterLocation])
  const profilesPerPage = 10
  const totalProfilePages = Math.max(1, Math.ceil(profiles.length / profilesPerPage))
  const paginatedProfiles = profiles.slice(
    (profilesPage - 1) * profilesPerPage,
    profilesPage * profilesPerPage,
  )
  const selectedReservationRoom = useMemo(
    () => activeRooms.find((room) => room.id === reservationForm.roomId) ?? null,
    [activeRooms, reservationForm.roomId],
  )

  const pushNotification = (message: string) => {
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

  const navigateToRoute = (nextRoute: RouteKey, options?: { replace?: boolean }) => {
    const nextPath = routePaths[nextRoute]
    if (location.pathname !== nextPath) navigate(nextPath, { replace: options?.replace ?? false })
    setRoute(nextRoute)
  }

  const loadRooms = async (authToken: string) => {
    const result = await api.getRooms(authToken)
    const mapped = result.map(toUiRoom)
    setRooms(mapped)
    if (!reservationForm.roomId && mapped.length > 0) setReservationForm(getDefaultReservationForm(mapped))
  }

  const loadMyBookings = async (authToken: string) => {
    const result = await api.getBookingsMe(authToken)
    setBookings(result.map(toUiBooking))
  }

  const loadAdminBookings = async (authToken: string) => {
    const result = await api.getAdminBookings(authToken, adminPage, adminStatusFilter, adminDateFilter)
    setBookings(result.content.map(toUiBooking))
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

  const loadProfiles = async (authToken: string) => {
    const result = await api.getUsers(authToken, profilesPage, profilesQuery)
    setProfiles(result.content.map(toProfile))
  }

  const bootstrap = async (authToken: string, user: AuthUser) => {
    await loadRooms(authToken)
    await loadUserPreferences(authToken)
    if (user.role === 'admin') {
      await Promise.all([loadAdminBookings(authToken), loadAdminConfig(authToken), loadProfiles(authToken)])
    } else {
      await loadMyBookings(authToken)
    }
  }

  const loadUserPreferences = async (authToken: string) => {
    try {
      const result = await api.getPreferences(authToken)
      setNotificationPrefs({
        emailEnabled: result.emailEnabled,
        reminderEnabled: result.reminderEnabled,
        bookingChangesEnabled: result.bookingChangesEnabled,
      })
      setIsDarkMode(result.themeMode === 'DARK')
      setFontScale(clampFontScale(result.fontScale))
      setPreferencesLoaded(true)
    } catch {
      setPreferencesLoaded(true)
    }
  }

  useEffect(() => {
    setRoute(getRouteFromPath(location.pathname))
  }, [location.pathname])

  useEffect(() => {
    const expectedPath = routePaths[effectiveRoute]
    if (location.pathname !== expectedPath) navigate(expectedPath, { replace: true })
  }, [effectiveRoute, location.pathname, navigate])

  useEffect(() => {
    if (!toastMessage) return
    const timeout = window.setTimeout(() => setToastMessage(''), 3500)
    return () => window.clearTimeout(timeout)
  }, [toastMessage])

  useEffect(() => {
    setIsNotificationsModalOpen(false)
    setIsSettingsModalOpen(false)
  }, [effectiveRoute])

  useEffect(() => {
    const saved = localStorage.getItem('luistudio_dark_mode')
    const initial = saved === null ? window.matchMedia('(prefers-color-scheme: dark)').matches : saved === '1'
    setIsDarkMode(initial)

    const savedFontScale = Number(localStorage.getItem('luistudio_font_scale') ?? '1')
    if (!Number.isNaN(savedFontScale) && savedFontScale >= 0.85 && savedFontScale <= 1.3) {
      setFontScale(clampFontScale(savedFontScale))
    }
  }, [])

  useEffect(() => {
    document.documentElement.classList.toggle('dark', isDarkMode)
    localStorage.setItem('luistudio_dark_mode', isDarkMode ? '1' : '0')
  }, [isDarkMode])

  useEffect(() => {
    document.documentElement.style.fontSize = `${fontScale * 100}%`
    localStorage.setItem('luistudio_font_scale', String(fontScale))
  }, [fontScale])

  useEffect(() => {
    if (!token || !authenticatedUser || !preferencesLoaded) return
    const timeout = window.setTimeout(() => {
      const payload: ApiPreferences = {
        ...notificationPrefs,
        themeMode: isDarkMode ? 'DARK' : 'LIGHT',
        fontScale: clampFontScale(fontScale),
      }
      api.updatePreferences(token, payload).catch(() => undefined)
    }, 300)
    return () => window.clearTimeout(timeout)
  }, [token, authenticatedUser, preferencesLoaded, notificationPrefs, isDarkMode, fontScale])

  useEffect(() => {
    const stored = localStorage.getItem('luistudio_token')
    if (!stored || authenticatedUser) return
    api
      .me(stored)
      .then((me) => {
        const user = toUiUser(me)
        setAuthenticatedUser(user)
        setToken(stored)
        bootstrap(stored, user).catch(() => undefined)
      })
      .catch(() => localStorage.removeItem('luistudio_token'))
  }, [authenticatedUser])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'admin') return
    loadAdminBookings(token).catch(() => undefined)
  }, [adminPage, adminStatusFilter, adminDateFilter, token, authenticatedUser])

  useEffect(() => {
    if (!token || authenticatedUser?.role !== 'admin') return
    loadProfiles(token).catch(() => undefined)
  }, [profilesPage, profilesQuery, token, authenticatedUser])

  const clearMessages = () => {
    setLoginError('')
    setTwoFactorError('')
    setResetError('')
    setReservationError('')
    setRoomNotice('')
    setConfigNotice('')
  }

  const logout = () => {
    clearMessages()
    setAuthenticatedUser(null)
    setToken('')
    setPreferencesLoaded(false)
    setShowTwoFactorModal(false)
    setTwoFactorCode('')
    setProvisionalToken('')
    localStorage.removeItem('luistudio_token')
    navigateToRoute('login')
  }

  const handleLoginSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    clearMessages()
    try {
      const response = await api.login(loginEmail.trim(), loginPassword)
      if (response.twoFactorRequired && response.provisionalToken) {
        setProvisionalToken(response.provisionalToken)
        setTwoFactorCode('')
        setTwoFactorError('')
        setShowTwoFactorModal(true)
        return
      }
      if (!response.token) return
      const user = toUiUser(response.user)
      setAuthenticatedUser(user)
      setToken(response.token)
      localStorage.setItem('luistudio_token', response.token)
      await bootstrap(response.token, user)
      navigateToRoute(user.role === 'admin' ? 'salas' : 'misreservas')
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : 'Error al iniciar sesion')
    }
  }

  const handleTwoFactorSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoginError('')
    setTwoFactorError('')

    const code = twoFactorCode.trim()
    if (!code) {
      setTwoFactorError('Ingresa el codigo 2FA.')
      return
    }
    if (!provisionalToken) {
      setTwoFactorError('No se encontro un token provisional. Inicia sesion nuevamente.')
      return
    }

    try {
      const verification = await api.verify2fa(provisionalToken, code)
      if (!verification.token) {
        setTwoFactorError('No se recibio token de sesion.')
        return
      }
      const user = toUiUser(verification.user)
      setAuthenticatedUser(user)
      setToken(verification.token)
      localStorage.setItem('luistudio_token', verification.token)
      setShowTwoFactorModal(false)
      setTwoFactorCode('')
      setProvisionalToken('')
      await bootstrap(verification.token, user)
      navigateToRoute(user.role === 'admin' ? 'salas' : 'misreservas')
    } catch (error) {
      setTwoFactorError(error instanceof Error ? error.message : 'No se pudo verificar el codigo 2FA.')
    }
  }

  const handleForgotPasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      await api.requestReset(forgotEmail)
      setShowForgotModal(false)
      pushNotification('Te enviamos un token de recuperacion al correo institucional.')
      navigateToRoute('reset-password')
    } catch (error) {
      setResetError(error instanceof Error ? error.message : 'No se pudo procesar la solicitud')
    }
  }

  const handleResetPasswordSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    try {
      await api.confirmReset(resetToken, resetPassword)
      setShowResetSuccess(true)
    } catch (error) {
      setResetError(error instanceof Error ? error.message : 'No se pudo actualizar la contrasena')
    }
  }

  const getRoomById = (roomId: string) => rooms.find((room) => room.id === roomId)

  const handleCreateReservation = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    const selectedRoom = getRoomById(reservationForm.roomId)
    if (!selectedRoom) return
    if (minutesBetween(reservationForm.start, reservationForm.end) <= 0) return
    try {
      const created = await api.createBooking(token, {
        roomId: selectedRoom.backendId,
        location: reservationForm.location,
        people: reservationForm.people,
        date: reservationForm.date,
        start: reservationForm.start,
        end: reservationForm.end,
      })
      const newBooking = toUiBooking(created)
      setBookings((current) => [newBooking, ...current])
      setConfirmationBookingId(newBooking.id)
      pushNotification('Reserva confirmada y correo encolado correctamente.')
    } catch (error) {
      setReservationError(error instanceof Error ? error.message : 'No se pudo crear la reserva')
    }
  }

  const openEditBooking = (booking: Booking) => {
    setEditingBookingId(booking.id)
    setEditBookingForm({
      location: booking.location,
      roomId: booking.roomId,
      people: booking.people,
      date: booking.date,
      start: booking.start,
      end: booking.end,
    })
  }

  const handleSaveEditedBooking = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!editingBookingId || !token) return
    const selectedRoom = getRoomById(editBookingForm.roomId)
    const target = bookings.find((booking) => booking.id === editingBookingId)
    if (!selectedRoom || !target) return
    try {
      const updated = await api.updateBooking(token, target.backendId, {
        roomId: selectedRoom.backendId,
        location: editBookingForm.location,
        people: editBookingForm.people,
        date: editBookingForm.date,
        start: editBookingForm.start,
        end: editBookingForm.end,
      })
      const next = toUiBooking(updated)
      setBookings((current) => current.map((booking) => (booking.id === editingBookingId ? next : booking)))
      setEditingBookingId(null)
    } catch (error) {
      setReservationError(error instanceof Error ? error.message : 'No se pudo actualizar la reserva')
    }
  }

  const cancelBooking = async (bookingId: string, actor: Role) => {
    if (!token) return
    const target = bookings.find((booking) => booking.id === bookingId)
    if (!target) return
    await api.cancelBooking(token, target.backendId)
    setBookings((current) =>
      current.map((booking) =>
        booking.id === bookingId ? { ...booking, status: 'Cancelado' } : booking,
      ),
    )
    pushNotification(
      actor === 'admin'
        ? 'Reserva cancelada por administrador y notificacion enviada.'
        : 'La reserva fue cancelada. Recibiras un correo de confirmacion.',
    )
  }

  const openAddRoom = () => {
    setRoomDraft({ name: '', location: locationOptions[0] ?? 'E3', capacity: 6 })
    setRoomModalTargetId(null)
    setRoomModalMode('add')
  }

  const openEditRoom = (room: Room) => {
    setRoomDraft({ name: room.name, location: room.location, capacity: room.capacity })
    setRoomModalTargetId(room.id)
    setRoomModalMode('edit')
  }

  const handleSaveRoom = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    try {
      if (roomModalMode === 'add') {
        const created = await api.createRoom(token, roomDraft)
        setRooms((current) => [toUiRoom(created), ...current])
        setRoomSuccessId(created.code)
      } else if (roomModalMode === 'edit' && roomModalTargetId) {
        const target = rooms.find((room) => room.id === roomModalTargetId)
        if (!target) return
        const updated = await api.updateRoom(token, target.backendId, roomDraft)
        setRooms((current) => current.map((room) => (room.id === roomModalTargetId ? toUiRoom(updated) : room)))
      }
      setRoomModalMode('none')
    } catch (error) {
      setRoomNotice(error instanceof Error ? error.message : 'No se pudo guardar la sala')
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
    } finally {
      setPendingDeleteRoomId(null)
    }
  }

  const toggleProfileStatus = async (profileId: string) => {
    if (!token) return
    const target = profiles.find((profile) => profile.id === profileId)
    if (!target) return
    await api.updateUserStatus(token, Number(profileId), target.status)
    await loadProfiles(token)
  }

  const saveAdminConfig = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!token) return
    const updated = await api.updateAdminConfig(token, configDraft)
    setConfig({
      maxActiveBookings: updated.maxActiveBookings,
      maxDurationMinutes: updated.maxDurationMinutes,
    })
    setConfigNotice('Configuracion actualizada correctamente.')
  }

  const availabilitySlots: AvailabilitySlot[] = useMemo(() => {
    if (!reservationForm.roomId || !reservationForm.date) return []
    const confirmedBookings = bookings.filter(
      (booking) =>
        booking.status === 'Confirmado' &&
        booking.roomId === reservationForm.roomId &&
        booking.date === reservationForm.date,
    )
    const slots: AvailabilitySlot[] = []
    for (let current = 420; current < 1320; current += 30) {
      const start = `${String(Math.floor(current / 60)).padStart(2, '0')}:${String(current % 60).padStart(2, '0')}`
      const endMin = current + 30
      const end = `${String(Math.floor(endMin / 60)).padStart(2, '0')}:${String(endMin % 60).padStart(2, '0')}`
      const overlaps = confirmedBookings.some(
        (booking) => toMinutes(start) < toMinutes(booking.end) && toMinutes(end) > toMinutes(booking.start),
      )
      slots.push({ start, end, isAvailable: !overlaps })
    }
    return slots
  }, [bookings, reservationForm.date, reservationForm.roomId])

  const availabilityByDay = useMemo<AvailabilityDay[]>(() => {
    if (!reservationForm.roomId) return []
    const baseDate = reservationForm.date ? new Date(`${reservationForm.date}T00:00:00`) : new Date()
    return Array.from({ length: 7 }, (_, offset) => {
      const nextDate = new Date(baseDate)
      nextDate.setDate(baseDate.getDate() + offset)
      const date = `${nextDate.getFullYear()}-${String(nextDate.getMonth() + 1).padStart(2, '0')}-${String(nextDate.getDate()).padStart(2, '0')}`
      const label = `${nextDate.toLocaleDateString('es-PE', { weekday: 'short' })} ${String(nextDate.getDate()).padStart(2, '0')}`
      const hasAvailability = availabilitySlots.some((slot) => slot.isAvailable)
      return { date, label, hasAvailability }
    })
  }, [availabilitySlots, reservationForm.date, reservationForm.roomId])

  const showGlobalTopbar =
    Boolean(authenticatedUser) && effectiveRoute !== 'login' && effectiveRoute !== 'reset-password'
  const contentOffsetClass = showGlobalTopbar
    ? isSidebarCollapsed
      ? 'md:pl-24'
      : 'md:pl-64'
    : ''
  const mobileNavPaddingClass = showGlobalTopbar ? 'pt-16 md:pt-0 pb-20 md:pb-0' : ''

  return (
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
            resetToken={resetToken}
            resetPassword={resetPassword}
            resetPasswordConfirm={resetPasswordConfirm}
            resetError={resetError}
            showResetSuccess={showResetSuccess}
            onResetTokenChange={setResetToken}
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
            locationOptions={locationOptions}
            activeRooms={activeRooms}
            selectedRoomCapacity={selectedReservationRoom?.capacity ?? null}
            availabilitySlots={availabilitySlots}
            availabilityByDay={availabilityByDay}
            onReservationChange={setReservationForm}
            onClearReservationForm={() => setReservationForm(getDefaultReservationForm(activeRooms))}
            onSubmitReservation={handleCreateReservation}
          />
        )}
        {effectiveRoute === 'misreservas' && (
          <MisReservasPage
            myBookings={myBookings}
            onEditBooking={openEditBooking}
            onCancelBooking={(bookingId) => cancelBooking(bookingId, 'student')}
            onCreateFirstReservation={() => navigateToRoute('reservas')}
          />
        )}
        {effectiveRoute === 'salas' && (
          <SalasPage
            filteredRooms={filteredRooms}
            locationOptions={locationOptions}
            roomFilterLocation={roomFilterLocation}
            roomNotice={roomNotice}
            onRoomFilterChange={setRoomFilterLocation}
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
            onSearchQueryChange={(value) => {
              setProfilesPage(1)
              setProfilesQuery(value)
            }}
            onToggleProfileStatus={toggleProfileStatus}
            onPrevPage={() => setProfilesPage((current) => Math.max(1, current - 1))}
            onNextPage={() => setProfilesPage((current) => Math.min(totalProfilePages, current + 1))}
          />
        )}
        {effectiveRoute === 'admin-reservas' && (
          <AdminReservasPage
            bookings={adminBookingsPage}
            users={[]}
            adminStatusFilter={adminStatusFilter}
            adminDateFilter={adminDateFilter}
            adminPage={adminPage}
            totalAdminPages={totalAdminPages}
            config={config}
            configDraft={configDraft}
            configNotice={configNotice}
            onStatusFilterChange={(value) => {
              setAdminStatusFilter(value)
              setAdminPage(1)
            }}
            onDateFilterChange={(value) => {
              setAdminDateFilter(value)
              setAdminPage(1)
            }}
            onPrevPage={() => setAdminPage((current) => current - 1)}
            onNextPage={() => setAdminPage((current) => current + 1)}
            onEditBooking={openEditBooking}
            onCancelBooking={(bookingId) => cancelBooking(bookingId, 'admin')}
            onConfigDraftChange={setConfigDraft}
            onSaveConfig={saveAdminConfig}
          />
        )}
      </div>

      {confirmationBookingId && (
        <BookingSuccessModal bookingId={confirmationBookingId} onClose={() => setConfirmationBookingId('')} />
      )}
      {editingBookingId && (
        <EditBookingModal
          form={editBookingForm}
          locationOptions={locationOptions}
          activeRooms={activeRooms}
          errorMessage={reservationError}
          onChange={setEditBookingForm}
          onCancel={() => setEditingBookingId(null)}
          onSubmit={handleSaveEditedBooking}
        />
      )}
      {roomModalMode !== 'none' && (
        <RoomFormModal
          mode={roomModalMode}
          draft={roomDraft}
          notice={roomNotice}
          targetRoomId={roomModalTargetId}
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
          onCodeChange={setTwoFactorCode}
          onCancel={() => {
            setShowTwoFactorModal(false)
            setTwoFactorCode('')
            setProvisionalToken('')
            setTwoFactorError('')
          }}
          onSubmit={handleTwoFactorSubmit}
        />
      )}
      {isNotificationsModalOpen && (
        <div className="modal-layer" onClick={() => setIsNotificationsModalOpen(false)}>
          <div className="modal-card slim-modal text-left" onClick={(event) => event.stopPropagation()}>
            <h2>Notificaciones</h2>
            {notifications.length === 0 ? (
              <p className="modal-copy">Aun no hay notificaciones.</p>
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
                </button>
              </div>
              <p className="settings-note">El tema se aplica a toda la aplicación.</p>
            </div>

            <div className="settings-divider" />

            <div className="settings-section">
              <div className="settings-scale-head">
                <p className="settings-badge-ink">Texto</p>
                <span className="settings-scale-value">{Math.round(fontScale * 100)}%</span>
              </div>
              <p className="settings-field-title">Tamaño de texto</p>
              <input
                type="range"
                className="settings-range"
                min={0.85}
                max={1.3}
                step={0.05}
                value={fontScale}
                onChange={(event) => setFontScale(clampFontScale(Number(event.target.value)))}
              />
              <div className="settings-scale-labels">
                <span>A-</span>
                <span>A</span>
                <span>A+</span>
              </div>
            </div>

            <div className="settings-divider" />

            <div className="settings-section">
              <p className="settings-badge-ink">Sesión</p>
              <button type="button" className="settings-logout-btn" onClick={logout}>
                Cerrar sesión
              </button>
            </div>

            <div className="modal-actions">
              <button type="button" className="ghost-btn" onClick={() => setIsSettingsModalOpen(false)}>
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
      {toastMessage && <div className="toast">{toastMessage}</div>}
    </>
  )
}
