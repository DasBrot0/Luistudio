export type Role = 'student' | 'admin'

export type RouteKey =
  | 'login'
  | 'reset-password'
  | 'reservas'
  | 'misreservas'
  | 'salas'
  | 'perfiles'
  | 'admin-reservas'

export type BookingStatus = 'Confirmado' | 'Cancelado'
export type ProfileStatus = 'Habilitado' | 'Deshabilitado'

export interface AuthUser {
  id: number
  role: Role
  code: string
  firstName: string
  lastName: string
  email: string
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
  observation?: string
}

export interface Profile {
  id: string
  code: string
  email: string
  firstName: string
  lastName: string
  status: ProfileStatus
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
  schedule: ScheduleDay[]
  pabellonCode: string
}

export interface CampusSchedule {
  campus: string
  campusLabel: string
  slotMinutes: number
  days: ScheduleDay[]
  warnings: string[]
}
