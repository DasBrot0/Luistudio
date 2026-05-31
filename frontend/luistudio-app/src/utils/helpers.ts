import type { ReservationForm, Room } from '../models/types'

export function toMinutes(time: string): number {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

export function minutesBetween(start: string, end: string): number {
  return toMinutes(end) - toMinutes(start)
}

export function formatDate(date: string): string {
  const [year, month, day] = date.split('-')
  return `${day}-${month}-${year}`
}

export function getDefaultReservationForm(_rooms: Room[]): ReservationForm {
  return {
    campus: '',
    location: '',
    roomId: '',
    people: 0,
    date: '',
    start: '',
    end: '',
  }
}
