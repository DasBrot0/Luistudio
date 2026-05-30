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
  const activeRooms = rooms.filter((room) => room.active)
  const firstRoom = activeRooms[0]

  if (!firstRoom) {
    return {
      location: '',
      roomId: '',
      people: 2,
      date: '',
      start: '16:30',
      end: '17:30',
    }
  }

  return {
    location: firstRoom.location,
    roomId: firstRoom.id,
    people: 2,
    date: '',
    start: '16:30',
    end: '17:30',
  }
}
