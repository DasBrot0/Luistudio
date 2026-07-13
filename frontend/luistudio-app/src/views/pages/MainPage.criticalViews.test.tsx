import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../App'
import { api, type ApiRoom } from '../../services/api'

/**
 * Pruebas complementarias de vistas críticas del frontend.
 *
 * Cubre, con mocks de los servicios HTTP (`api.*`) y sin red real:
 *   - Login: renderizado del formulario y estado de error ante credenciales inválidas.
 *   - Restauración de sesión: estado de carga ("Restaurando sesión").
 *   - Reservas (estudiante): renderizado tras un login exitoso con datos simulados.
 *   - Salas (admin): renderizado con datos mockeados y estado de error (toast) si falla la carga.
 *   - Perfil: estado de carga de actividad y renderizado posterior con datos mockeados.
 *   - Vista administrativa principal (Reservas registradas): renderizado con datos mockeados.
 */

vi.mock('../../services/api', () => ({
  api: {
    login: vi.fn(),
    verify2fa: vi.fn(),
    me: vi.fn(),
    logout: vi.fn(),
    requestReset: vi.fn(),
    confirmReset: vi.fn(),
    enroll2fa: vi.fn(),
    confirm2faEnrollment: vi.fn(),
    disable2fa: vi.fn(),
    confirmDisable2fa: vi.fn(),
    getRooms: vi.fn(),
    getAvailableRooms: vi.fn(),
    createRoom: vi.fn(),
    updateRoom: vi.fn(),
    deleteRoom: vi.fn(),
    getBookingsMe: vi.fn(),
    getRoomBookings: vi.fn(),
    createBooking: vi.fn(),
    updateBooking: vi.fn(),
    cancelBooking: vi.fn(),
    downloadBookingIcs: vi.fn(),
    getAdminBookings: vi.fn(),
    getAdminConfig: vi.fn(),
    updateAdminConfig: vi.fn(),
    getCampusSchedules: vi.fn(),
    updateCampusSchedule: vi.fn(),
    getUsers: vi.fn(),
    updateUserStatus: vi.fn(),
    unlockUser: vi.fn(),
    getPreferences: vi.fn(),
    updatePreferences: vi.fn(),
    getCampusMap: vi.fn(),
    lookupUserByCode: vi.fn(),
    getSessions: vi.fn(),
    revokeSession: vi.fn(),
    revokeAllSessions: vi.fn(),
    getMyActivity: vi.fn(),
    requestSensitiveChange: vi.fn(),
    confirmSensitiveChange: vi.fn(),
    getLoginAttempts: vi.fn(),
    subscribeToRoom: vi.fn(),
    unsubscribeFromRoom: vi.fn(),
    getMyAvailabilitySubscriptions: vi.fn(),
    publishAnnouncement: vi.fn(),
  },
}))

const studentUser = {
  id: 5,
  code: '20224692',
  firstName: 'Luis',
  lastName: 'García',
  email: '20224692@aloe.ulima.edu.pe',
  role: 'ESTUDIANTE' as const,
  status: 'HABILITADO',
  has2fa: false,
}

const adminUser = {
  id: 1,
  code: '20233916',
  firstName: 'Ana',
  lastName: 'Admin',
  email: '20233916@aloe.ulima.edu.pe',
  role: 'ADMIN' as const,
  status: 'HABILITADO',
  has2fa: false,
}

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 1 }

const sampleRoom: ApiRoom = {
  id: 1,
  code: 'A-101',
  name: 'Sala A-101',
  resourceLabel: 'Sala de estudio',
  campus: 'monterrico',
  campusLabel: 'Monterrico',
  venue: 'biblioteca',
  venueLabel: 'Biblioteca',
  capacity: 6,
  location: 'Biblioteca Central',
  minPeople: 1,
  minPeopleRequired: false,
  maxPeople: 6,
  slotMinutes: 30,
  schedule: [],
  status: 'DISPONIBLE',
  pabellonCode: 'BC',
  noiseLevel: 'MEDIO',
  supportsConcentration: true,
  roomType: 'GENERAL',
  equipment: [],
}

const sampleBooking = {
  id: 10,
  userId: studentUser.id,
  userEmail: studentUser.email,
  roomId: sampleRoom.id,
  roomCode: sampleRoom.code,
  roomName: sampleRoom.name,
  location: sampleRoom.location,
  people: 3,
  date: '2026-07-15',
  start: '10:00:00',
  end: '11:00:00',
  status: 'ACTIVA' as const,
  googleCalendarUrl: '',
  icsUrl: '',
}

function loginResponse(user: typeof studentUser | typeof adminUser, twoFactorRequired = false) {
  return {
    token: null,
    provisionalToken: twoFactorRequired ? 'provisional-token' : null,
    twoFactorRequired,
    user,
    message: '',
  }
}

function renderApp() {
  render(
    <MemoryRouter initialEntries={['/']}>
      <App />
    </MemoryRouter>,
  )
}

async function fillAndSubmitLogin(email: string, password: string) {
  fireEvent.change(screen.getByLabelText('Correo institucional'), { target: { value: email } })
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }))
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()

  vi.mocked(api.login).mockReset()
  vi.mocked(api.verify2fa).mockReset().mockResolvedValue(loginResponse(studentUser))
  vi.mocked(api.me).mockReset().mockRejectedValue(new Error('No autenticado'))
  vi.mocked(api.logout).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.requestReset).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.confirmReset).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.enroll2fa).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.confirm2faEnrollment).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.disable2fa).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.confirmDisable2fa).mockReset().mockResolvedValue({ message: 'ok' })
  vi.mocked(api.getRooms).mockReset().mockResolvedValue({ ...emptyPage, size: 50 })
  vi.mocked(api.getAvailableRooms).mockReset().mockResolvedValue([])
  vi.mocked(api.createRoom).mockReset()
  vi.mocked(api.updateRoom).mockReset()
  vi.mocked(api.deleteRoom).mockReset().mockResolvedValue(undefined)
  vi.mocked(api.getBookingsMe).mockReset().mockResolvedValue(emptyPage)
  vi.mocked(api.getRoomBookings).mockReset().mockResolvedValue([])
  vi.mocked(api.createBooking).mockReset()
  vi.mocked(api.updateBooking).mockReset()
  vi.mocked(api.cancelBooking).mockReset()
  vi.mocked(api.downloadBookingIcs).mockReset()
  vi.mocked(api.getAdminBookings).mockReset().mockResolvedValue(emptyPage)
  vi.mocked(api.getAdminConfig).mockReset().mockResolvedValue({ maxActiveBookings: 1, maxDurationMinutes: 120 })
  vi.mocked(api.updateAdminConfig).mockReset()
  vi.mocked(api.getCampusSchedules).mockReset().mockResolvedValue({ campuses: [] })
  vi.mocked(api.updateCampusSchedule).mockReset()
  vi.mocked(api.getUsers).mockReset().mockResolvedValue(emptyPage)
  vi.mocked(api.updateUserStatus).mockReset()
  vi.mocked(api.unlockUser).mockReset()
  // Sin mock explícito de preferencias: la app usa el aterrizaje por defecto o el
  // guardado en localStorage (comportamiento real de degradación documentado en MainPage).
  vi.mocked(api.getPreferences).mockReset().mockRejectedValue(new Error('Sin preferencias'))
  vi.mocked(api.updatePreferences).mockReset().mockResolvedValue({} as never)
  vi.mocked(api.getCampusMap).mockReset().mockResolvedValue({
    generatedAt: '2026-07-12T00:00:00Z',
    refreshAfterSeconds: 300,
    campuses: [],
  })
  vi.mocked(api.lookupUserByCode).mockReset()
  vi.mocked(api.getSessions).mockReset().mockResolvedValue({ sessions: [] })
  vi.mocked(api.revokeSession).mockReset()
  vi.mocked(api.revokeAllSessions).mockReset()
  vi.mocked(api.getMyActivity).mockReset().mockResolvedValue({ content: [], page: 0, totalPages: 1 })
  vi.mocked(api.requestSensitiveChange).mockReset()
  vi.mocked(api.confirmSensitiveChange).mockReset()
  vi.mocked(api.getLoginAttempts).mockReset().mockResolvedValue({ content: [], page: 0, totalPages: 1, totalElements: 0 })
  vi.mocked(api.subscribeToRoom).mockReset()
  vi.mocked(api.unsubscribeFromRoom).mockReset()
  vi.mocked(api.getMyAvailabilitySubscriptions).mockReset().mockResolvedValue({ subscriptions: [] })
  vi.mocked(api.publishAnnouncement).mockReset()
})

afterEach(() => {
  localStorage.clear()
  sessionStorage.clear()
})

describe('Vista crítica: Login', () => {
  it('renderiza el formulario de inicio de sesión', () => {
    renderApp()

    expect(screen.getByRole('heading', { name: 'Inicia sesión' })).toBeInTheDocument()
    expect(screen.getByLabelText('Correo institucional')).toBeInTheDocument()
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Iniciar sesión' })).toBeInTheDocument()
  })

  it('muestra un mensaje de error (estado de error) cuando el login es rechazado por la API mockeada', async () => {
    vi.mocked(api.login).mockRejectedValueOnce(new Error('Credenciales inválidas'))
    renderApp()

    await fillAndSubmitLogin('user@test.com', 'WrongPass1!')

    expect(await screen.findByText('Credenciales inválidas')).toBeInTheDocument()
    expect(api.login).toHaveBeenCalledWith('user@test.com', 'WrongPass1!', false)
  })
})

describe('Vista crítica: Restauración de sesión (estado de carga)', () => {
  it('muestra la pantalla de "Restaurando sesión" mientras la llamada a api.me sigue pendiente', () => {
    localStorage.setItem('luistudio_session_hint', '1')
    let resolveMe: (value: typeof studentUser) => void = () => {}
    vi.mocked(api.me).mockReset().mockReturnValue(
      new Promise((resolve) => {
        resolveMe = resolve
      }),
    )

    renderApp()

    expect(screen.getByRole('heading', { name: 'Restaurando sesión' })).toBeInTheDocument()
    expect(api.me).toHaveBeenCalled()

    // limpia la promesa pendiente para no dejar handlers colgando entre tests
    resolveMe(studentUser)
  })
})

describe('Vista crítica: Reservas (estudiante)', () => {
  it('tras un login exitoso, renderiza la vista de Reservas con datos simulados vía HTTP mockeado', async () => {
    localStorage.setItem('luistudio_login_landing_route', 'reservas')
    vi.mocked(api.login).mockResolvedValueOnce(loginResponse(studentUser))
    vi.mocked(api.getRooms).mockResolvedValue({ ...emptyPage, content: [sampleRoom], size: 50 })

    renderApp()
    await fillAndSubmitLogin(studentUser.email, 'Student123!')

    expect(await screen.findByRole('heading', { name: 'Reservar' })).toBeInTheDocument()
    await waitFor(() => expect(api.getRooms).toHaveBeenCalled())
  })
})

describe('Vista crítica: Salas / disponibilidad (admin)', () => {
  it('tras un login exitoso como admin, renderiza el listado de salas con datos mockeados', async () => {
    localStorage.setItem('luistudio_login_landing_route', 'salas')
    vi.mocked(api.login).mockResolvedValueOnce(loginResponse(adminUser))
    vi.mocked(api.getRooms).mockResolvedValue({ ...emptyPage, content: [sampleRoom], size: 50 })

    renderApp()
    await fillAndSubmitLogin(adminUser.email, 'Admin123!')

    expect(await screen.findByRole('heading', { name: 'Salas' })).toBeInTheDocument()
    const roomNameMatches = await screen.findAllByText(sampleRoom.name)
    expect(roomNameMatches.length).toBeGreaterThan(0)
  })

  it('muestra un mensaje de error (toast) cuando la carga del directorio de salas falla', async () => {
    localStorage.setItem('luistudio_login_landing_route', 'salas')
    vi.mocked(api.login).mockResolvedValueOnce(loginResponse(adminUser))
    vi.mocked(api.getRooms).mockRejectedValue(new Error('Error de red simulado'))

    renderApp()
    await fillAndSubmitLogin(adminUser.email, 'Admin123!')

    await waitFor(() => {
      const toast = document.querySelector('.toast')
      expect(toast).not.toBeNull()
      expect(toast?.textContent ?? '').toContain('Error de red simulado')
    })
  })
})

describe('Vista crítica: Perfil / configuración', () => {
  it('muestra el estado de carga y luego el historial de actividad obtenido vía HTTP mockeado', async () => {
    vi.mocked(api.login).mockResolvedValueOnce(loginResponse(studentUser))

    type ActivityResponse = Awaited<ReturnType<typeof api.getMyActivity>>
    let resolveActivity: (value: ActivityResponse) => void = () => {}
    vi.mocked(api.getMyActivity).mockReturnValue(
      new Promise<ActivityResponse>((resolve) => {
        resolveActivity = resolve
      }),
    )

    renderApp()
    await fillAndSubmitLogin(studentUser.email, 'Student123!')

    // Espera a estar dentro de la app autenticada antes de navegar al perfil
    const [profileButton] = await screen.findAllByTitle('Mi perfil')
    fireEvent.click(profileButton)
    fireEvent.click(await screen.findByRole('button', { name: 'Actividad' }))

    expect(screen.getByText('Cargando actividad…')).toBeInTheDocument()

    await act(async () => {
      resolveActivity({
        content: [{ id: 1, action: 'LOGIN_SUCCESS', detail: null, createdAt: '2026-07-11T10:00:00Z' }],
        page: 0,
        totalPages: 1,
      })
    })

    expect(await screen.findByText('Inicio de sesión')).toBeInTheDocument()
    expect(screen.queryByText('Cargando actividad…')).not.toBeInTheDocument()
  })
})

describe('Vista crítica: Vista administrativa principal (Reservas registradas)', () => {
  it('renderiza el listado de reservas administrativo con datos simulados vía HTTP mockeado', async () => {
    localStorage.setItem('luistudio_login_landing_route', 'admin-reservas')
    vi.mocked(api.login).mockResolvedValueOnce(loginResponse(adminUser))
    vi.mocked(api.getAdminBookings).mockResolvedValue({ ...emptyPage, content: [sampleBooking] })

    renderApp()
    await fillAndSubmitLogin(adminUser.email, 'Admin123!')

    expect(await screen.findByRole('heading', { name: 'Reservas registradas' })).toBeInTheDocument()
    const ownerEmailMatches = await screen.findAllByText(sampleBooking.userEmail)
    expect(ownerEmailMatches.length).toBeGreaterThan(0)
  })
})
