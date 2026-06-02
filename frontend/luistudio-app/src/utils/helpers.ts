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

export function getDefaultReservationForm(rooms: Room[]): ReservationForm {
  void rooms

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

export function buildPabellonCode(campus: string, venue: string): string {
  const words = `${campus} ${venue}`
    .trim()
    .split(/[^A-Za-z0-9]+/)
    .filter(Boolean)

  const initials = words.map((word) => word[0]).join('').toUpperCase()
  const fallback = `${campus}-${venue}`.replace(/[^A-Za-z0-9]/g, '-').toUpperCase()
  return (initials || fallback).slice(0, 20)
}
