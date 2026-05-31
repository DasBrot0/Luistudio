import { useMemo } from 'react'
import type { FormEvent } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { Booking, ReservationForm, Room } from '../../models/types'

interface ReservasPageProps {
  reservationForm: ReservationForm
  reservationError: string
  campusOptions: string[]
  locationOptionsByCampus: Map<string, string[]>
  activeRooms: Room[]
  selectedRoomCapacity: number | null
  roomBookings: Booking[]
  weekOffset: number
  onReservationChange: (next: ReservationForm) => void
  onWeekOffsetChange: (value: number) => void
  onClearReservationForm: () => void
  onSubmitReservation: (event: FormEvent<HTMLFormElement>) => void
}

interface CalendarDay {
  isoDate: string
  label: string
  weekday: number
}

function toMinutes(time: string) {
  const [hours, minutes] = time.split(':').map(Number)
  return hours * 60 + minutes
}

function toTime(minutes: number) {
  const safe = Math.max(0, minutes)
  const hours = Math.floor(safe / 60)
  const rest = safe % 60
  return `${String(hours).padStart(2, '0')}:${String(rest).padStart(2, '0')}`
}

function toIsoDate(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

function getWeekDays(weekOffset: number): CalendarDay[] {
  const today = new Date()
  const mondayOffset = (today.getDay() + 6) % 7
  const monday = new Date(today)
  monday.setDate(today.getDate() - mondayOffset + weekOffset * 7)

  return Array.from({ length: 7 }, (_, index) => {
    const current = new Date(monday)
    current.setDate(monday.getDate() + index)
    const isoDate = toIsoDate(current)
    const label = current.toLocaleDateString('es-PE', { weekday: 'short', day: '2-digit', month: '2-digit' })
    const weekday = ((current.getDay() + 6) % 7) + 1
    return { isoDate, label, weekday }
  })
}

export function ReservasPage({
  reservationForm,
  reservationError,
  campusOptions,
  locationOptionsByCampus,
  activeRooms,
  selectedRoomCapacity,
  roomBookings,
  weekOffset,
  onReservationChange,
  onWeekOffsetChange,
  onClearReservationForm,
  onSubmitReservation,
}: ReservasPageProps) {
  const roomsByCampus = useMemo(
    () => activeRooms.filter((room) => room.campusLabel === reservationForm.campus),
    [activeRooms, reservationForm.campus],
  )

  const locationsForCampus = useMemo(
    () => locationOptionsByCampus.get(reservationForm.campus) ?? [],
    [locationOptionsByCampus, reservationForm.campus],
  )

  const roomsByLocation = useMemo(
    () => roomsByCampus.filter((room) => room.venueLabel === reservationForm.location),
    [roomsByCampus, reservationForm.location],
  )

  const selectedRoom = useMemo(
    () => activeRooms.find((room) => room.id === reservationForm.roomId) ?? null,
    [activeRooms, reservationForm.roomId],
  )

  const slotMinutes = selectedRoom?.slotMinutes ?? 60
  const weekDays = useMemo(() => getWeekDays(weekOffset), [weekOffset])
  const scheduleByDay = useMemo(() => {
    const values = new Map<number, { open: string | null; close: string | null; closed: boolean }>()
    for (const day of selectedRoom?.schedule ?? []) {
      values.set(day.dayOfWeek, {
        open: day.openTime,
        close: day.closeTime,
        closed: day.closed,
      })
    }
    return values
  }, [selectedRoom])

  const now = new Date()
  const todayIso = toIsoDate(now)
  const nowMinutes = now.getHours() * 60 + now.getMinutes()
  const weekendToday = now.getDay() === 6 || now.getDay() === 0
  const maxWeekOffset = weekendToday ? 1 : 0

  const overlapsBooking = (date: string, start: string, end: string) =>
    roomBookings.some(
      (booking) =>
        booking.date === date &&
        toMinutes(start) < toMinutes(booking.end) &&
        toMinutes(end) > toMinutes(booking.start) &&
        booking.status === 'Confirmado',
    )

  const isInsideSchedule = (weekday: number, start: string, end: string) => {
    const schedule = scheduleByDay.get(weekday)
    if (!schedule || schedule.closed || !schedule.open || !schedule.close) return false
    return toMinutes(start) >= toMinutes(schedule.open) && toMinutes(end) <= toMinutes(schedule.close)
  }

  const isInsideWindow = (date: string, start: string) => {
    if (weekOffset < 0 || weekOffset > maxWeekOffset) return false
    if (date < todayIso) return false
    if (date === todayIso && toMinutes(start) <= nowMinutes) return false
    return true
  }

  const timeSlots = useMemo(() => {
    if (!selectedRoom) return []
    const starts = selectedRoom.schedule
      .filter((day) => !day.closed && day.openTime && day.closeTime)
      .map((day) => toMinutes(day.openTime as string))
    const ends = selectedRoom.schedule
      .filter((day) => !day.closed && day.openTime && day.closeTime)
      .map((day) => toMinutes(day.closeTime as string))
    if (starts.length === 0 || ends.length === 0) return []

    const minStart = Math.min(...starts)
    const maxEnd = Math.max(...ends)
    const slots: string[] = []
    for (let minute = minStart; minute + slotMinutes <= maxEnd; minute += slotMinutes) {
      slots.push(toTime(minute))
    }
    return slots
  }, [selectedRoom, slotMinutes])

  const availableStartSlotsForSelectedDate = useMemo(() => {
    if (!reservationForm.date || !selectedRoom) return []
    const selectedDay = weekDays.find((day) => day.isoDate === reservationForm.date)
    if (!selectedDay) return []
    return timeSlots.filter((start) => {
      const end = toTime(toMinutes(start) + slotMinutes)
      if (!isInsideWindow(reservationForm.date, start)) return false
      if (!isInsideSchedule(selectedDay.weekday, start, end)) return false
      if (overlapsBooking(reservationForm.date, start, end)) return false
      return true
    })
  }, [reservationForm.date, selectedRoom, weekDays, timeSlots, slotMinutes, roomBookings])

  return (
    <main className="page dashboard-page">
      <AppHeader title="Reservar" roleLabel="Estudiante" />

      <section className="dashboard-grid single-grid">
        <article className="card booking-card">
          <form onSubmit={onSubmitReservation} className="booking-form">
            <div className="card-head slim-head">
              <h2>Nueva reserva</h2>
              <div className="inline-filters quick-links">
                <button
                  type="button"
                  className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={onClearReservationForm}
                >
                  Limpiar
                </button>
              </div>
            </div>

            <div className="form-grid top-grid">
              <div>
                <label htmlFor="campus">Campus</label>
                <select
                  id="campus"
                  value={reservationForm.campus}
                  onChange={(event) => {
                    const nextCampus = event.target.value
                    onReservationChange({
                      ...reservationForm,
                      campus: nextCampus,
                      location: '',
                      roomId: '',
                      people: 0,
                      date: '',
                      start: '',
                      end: '',
                    })
                  }}
                >
                  <option value="">Seleccionar campus</option>
                  {campusOptions.map((campus) => (
                    <option key={campus} value={campus}>
                      {campus}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="location">Ubicación</label>
                <select
                  id="location"
                  value={reservationForm.location}
                  onChange={(event) => {
                    const nextLocation = event.target.value
                    onReservationChange({
                      ...reservationForm,
                      location: nextLocation,
                      roomId: '',
                      people: 0,
                      date: '',
                      start: '',
                      end: '',
                    })
                  }}
                  disabled={!reservationForm.campus}
                >
                  <option value="">Seleccionar ubicación</option>
                  {locationsForCampus.map((location) => (
                    <option key={location} value={location}>
                      {location}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="room">Recurso</label>
                <select
                  id="room"
                  value={reservationForm.roomId}
                  onChange={(event) => {
                    const nextRoom = roomsByLocation.find((room) => room.id === event.target.value)
                    onReservationChange({
                      ...reservationForm,
                      roomId: event.target.value,
                      people: nextRoom?.minPeople ?? 0,
                      date: '',
                      start: '',
                      end: '',
                    })
                  }}
                  disabled={!reservationForm.location}
                >
                  <option value="">Seleccionar recurso</option>
                  {roomsByLocation.map((room) => (
                    <option key={room.id} value={room.id}>
                      {room.resourceLabel}
                    </option>
                  ))}
                </select>
                {selectedRoom && (
                  <p className="mt-1 text-xs font-medium text-slate-600">
                    Capacidad: {selectedRoom.capacity} | Min: {selectedRoom.minPeople}
                    {selectedRoom.minPeopleRequired ? ' (obligatorio)' : ' (opcional)'} | Max: {selectedRoom.maxPeople} | Bloque: {slotMinutes} min
                  </p>
                )}
              </div>
            </div>

            <div className="form-grid bottom-grid">
              <div>
                <label htmlFor="people">Personas</label>
                <input
                  id="people"
                  type="number"
                  min={selectedRoom?.minPeopleRequired ? selectedRoom.minPeople : 1}
                  max={selectedRoom?.maxPeople ?? selectedRoomCapacity ?? 12}
                  value={reservationForm.people > 0 ? reservationForm.people : ''}
                  onChange={(event) =>
                    onReservationChange({
                      ...reservationForm,
                      people: Number(event.target.value),
                    })
                  }
                  disabled={!selectedRoom}
                />
              </div>
              <div>
                <label htmlFor="date">Fecha</label>
                <input id="date" type="date" value={reservationForm.date} readOnly />
              </div>
              <div>
                <label htmlFor="start">Inicio</label>
                <select
                  id="start"
                  value={reservationForm.start}
                  disabled={!reservationForm.date}
                  onChange={(event) =>
                    onReservationChange({
                      ...reservationForm,
                      start: event.target.value,
                      end: event.target.value ? toTime(toMinutes(event.target.value) + slotMinutes) : '',
                    })
                  }
                >
                  <option value="">Seleccionar inicio</option>
                  {availableStartSlotsForSelectedDate.map((start) => (
                    <option key={start} value={start}>
                      {start}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label htmlFor="end">Fin</label>
                <input id="end" type="time" value={reservationForm.end} readOnly />
              </div>
            </div>

            <div className="reservation-week-nav">
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => onWeekOffsetChange(Math.max(0, weekOffset - 1))}
                disabled={weekOffset <= 0}
              >
                Semana anterior
              </button>
              <p className="m-0 text-xs font-semibold text-slate-600">
                {weekOffset === 0 ? 'Semana actual' : 'Semana siguiente'}
              </p>
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => onWeekOffsetChange(Math.min(maxWeekOffset, weekOffset + 1))}
                disabled={weekOffset >= maxWeekOffset}
              >
                Semana siguiente
              </button>
            </div>

            <div className="calendar-grid-wrap">
              <table className="calendar-grid">
                <thead>
                  <tr>
                    <th>Hora</th>
                    {weekDays.map((day) => (
                      <th key={day.isoDate}>{day.label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {timeSlots.map((slot) => {
                    const slotEnd = toTime(toMinutes(slot) + slotMinutes)
                    return (
                      <tr key={slot}>
                        <td>{slot}</td>
                        {weekDays.map((day) => {
                          const outsideWindow = !isInsideWindow(day.isoDate, slot)
                          if (outsideWindow) {
                            return <td key={`${day.isoDate}-${slot}`} className="calendar-empty-slot" />
                          }

                          const blockedBySchedule = !isInsideSchedule(day.weekday, slot, slotEnd)
                          const occupied = overlapsBooking(day.isoDate, slot, slotEnd)
                          const selected = reservationForm.date === day.isoDate && reservationForm.start === slot
                          const disabled = blockedBySchedule || occupied

                          return (
                            <td key={`${day.isoDate}-${slot}`}>
                              <button
                                type="button"
                                className={`calendar-cell-btn ${selected ? 'selected' : ''} ${disabled ? 'blocked' : 'available'}`}
                                disabled={disabled}
                                aria-label={`${day.label} ${slot} ${slotEnd}`}
                                onClick={() =>
                                  onReservationChange({
                                    ...reservationForm,
                                    date: day.isoDate,
                                    start: slot,
                                    end: slotEnd,
                                  })
                                }
                              />
                            </td>
                          )
                        })}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            <div className="action-row items-center">
              <button
                type="submit"
                className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
              >
                Confirmar reserva
              </button>
            </div>

            {reservationError && <p className="error-text">{reservationError}</p>}
          </form>
        </article>
      </section>
    </main>
  )
}
