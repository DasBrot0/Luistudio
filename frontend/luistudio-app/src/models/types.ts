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
  capacity: number
  location: string
  active: boolean
}

export interface ReservationForm {
  location: string
  roomId: string
  people: number
  date: string
  start: string
  end: string
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
  location: string
  capacity: number
}
