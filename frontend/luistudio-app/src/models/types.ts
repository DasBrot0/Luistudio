export type Role = 'student' | 'admin'

export type RouteKey =
  | 'login'
  | 'reset-password'
  | 'confirm-change'
  | 'reservas'
  | 'disponibilidad'
  | 'busqueda-inteligente'
  | 'misreservas'
  | 'mapa'
  | 'dashboard'
  | 'salas'
  | 'perfiles'
  | 'admin-reservas'
  | 'asistencias'
  | 'seguridad'
  | 'comunicados'
  | 'profile'

export type BookingStatus = 'Confirmado' | 'Cancelado'
export type ProfileStatus = 'Habilitado' | 'Deshabilitado' | 'Bloqueado'
export type RoomStatus = 'Disponible' | 'En mantenimiento' | 'Inactiva'

export interface AuthUser {
  id: number
  role: Role
  code: string
  firstName: string
  lastName: string
  email: string
  status: string
  has2fa: boolean
}

export interface Room {
  backendId: number
  id: string
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
  active: boolean
  status: RoomStatus
  noiseLevel?: 'BAJO' | 'MEDIO' | 'ALTO'
  supportsConcentration?: boolean
  roomType?: string
  equipment?: string[]
  description?: string
  allowedActivities?: string[]
  nearbyServices?: string[]
  accessibilityFeatures?: string[]
  inventoryCount: number
}

export interface ScheduleDay {
  dayOfWeek: number
  openTime: string | null
  closeTime: string | null
  closed: boolean
  override?: boolean
}

export interface ReservationForm {
  campus: string
  location: string
  roomId: string
  people: number
  date: string
  start: string
  end: string
}

export interface ReservationCompanion {
  code: string
  fullName: string
}

export interface Booking {
  id: string
  backendId: number
  userId: number
  userEmail?: string
  location: string
  roomId: string
  roomBackendId?: number
  people: number
  date: string
  start: string
  end: string
  status: BookingStatus
  attendanceStatus?: 'ASISTIO' | 'INASISTIO' | null
  observation?: string
  googleCalendarUrl?: string
  icsUrl?: string
  roomUnitNumber?: number | null
  roomUnitLabel?: string | null
}

export interface Profile {
  id: string
  code: string
  email: string
  firstName: string
  lastName: string
  status: ProfileStatus
  blocked: boolean
}

export interface SystemConfig {
  maxActiveBookings: number
  maxDurationMinutes: number
}

export interface RoomDraft {
  name: string
  campus: string
  location: string
  capacity: number
  minPeople: number
  minPeopleRequired: boolean
  maxPeople: number
  status: RoomStatus
  schedule: ScheduleDay[]
  pabellonCode: string
  noiseLevel: 'BAJO' | 'MEDIO' | 'ALTO'
  supportsConcentration: boolean
  roomType: 'ESTUDIO_INDIVIDUAL' | 'ESTUDIO_GRUPAL' | 'REUNION' | 'PRESENTACION' | 'GENERAL'
  equipment: string[]
  description: string
  allowedActivities: string[]
  nearbyServices: string[]
  accessibilityFeatures: string[]
}

export interface CampusSchedule {
  campus: string
  campusLabel: string
  slotMinutes: number
  days: ScheduleDay[]
  warnings: string[]
}
