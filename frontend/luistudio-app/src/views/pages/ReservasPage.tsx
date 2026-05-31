import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { ReservationForm, Room } from '../../models/types'

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

interface ReservasPageProps {
  reservationForm: ReservationForm
  reservationError: string
  locationOptions: string[]
  activeRooms: Room[]
  selectedRoomCapacity: number | null
  availabilitySlots: AvailabilitySlot[]
  availabilityByDay: AvailabilityDay[]
  onReservationChange: (next: ReservationForm) => void
  onClearReservationForm: () => void
  onSubmitReservation: (event: FormEvent<HTMLFormElement>) => void
}

export function ReservasPage({
  reservationForm,
  reservationError,
  locationOptions,
  activeRooms,
  selectedRoomCapacity,
  availabilitySlots,
  availabilityByDay,
  onReservationChange,
  onClearReservationForm,
  onSubmitReservation,
}: ReservasPageProps) {
  const [showAvailability, setShowAvailability] = useState(false)

  const availableRoomsForReservation = useMemo(
    () => activeRooms.filter((room) => room.location === reservationForm.location),
    [activeRooms, reservationForm.location],
  )

  const roomsByCapacity = useMemo(
    () => availableRoomsForReservation.filter((room) => room.capacity >= reservationForm.people),
    [availableRoomsForReservation, reservationForm.people],
  )

  const selectableRooms = roomsByCapacity.length > 0 ? roomsByCapacity : availableRoomsForReservation

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
                  onClick={() => setShowAvailability((current) => !current)}
                >
                  {showAvailability ? 'Ocultar disponibilidad' : 'Ver disponibilidad'}
                </button>
              </div>
            </div>

            <div className="form-grid top-grid">
              <div>
                <label htmlFor="location">Ubicación</label>
                <select
                  id="location"
                  value={reservationForm.location}
                  onChange={(event) => {
                    const nextLocation = event.target.value
                    const firstRoomByLocation = activeRooms.find((room) => room.location === nextLocation)

                    onReservationChange({
                      ...reservationForm,
                      location: nextLocation,
                      roomId: firstRoomByLocation?.id ?? '',
                      people:
                        firstRoomByLocation && reservationForm.people > firstRoomByLocation.capacity
                          ? firstRoomByLocation.capacity
                          : reservationForm.people,
                    })
                  }}
                >
                  {locationOptions.map((location) => (
                    <option key={location} value={location}>
                      {location}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor="room">Sala</label>
                <select
                  id="room"
                  value={reservationForm.roomId}
                  onChange={(event) => onReservationChange({ ...reservationForm, roomId: event.target.value })}
                >
                  {selectableRooms.map((room) => (
                    <option key={room.id} value={room.id}>
                      {room.id}
                    </option>
                  ))}
                </select>
                {selectedRoomCapacity !== null && (
                  <p className="mt-1 text-xs font-medium text-slate-600">
                    Capacidad de la sala: {selectedRoomCapacity} personas
                  </p>
                )}
              </div>

              <div>
                <label htmlFor="people">Personas</label>
                <input
                  id="people"
                  type="number"
                  min={1}
                  max={selectedRoomCapacity ?? 12}
                  value={reservationForm.people}
                  onChange={(event) => {
                    const nextPeople = Number(event.target.value)
                    const currentRoom = availableRoomsForReservation.find((room) => room.id === reservationForm.roomId)
                    const firstCompatibleRoom = availableRoomsForReservation.find((room) => room.capacity >= nextPeople)

                    const nextRoomId =
                      currentRoom && currentRoom.capacity >= nextPeople
                        ? reservationForm.roomId
                        : firstCompatibleRoom?.id ?? reservationForm.roomId

                    onReservationChange({
                      ...reservationForm,
                      people: nextPeople,
                      roomId: nextRoomId,
                    })
                  }}
                />
              </div>
            </div>

            <div className="form-grid bottom-grid">
              <div>
                <label htmlFor="date">Fecha</label>
                <input
                  id="date"
                  type="date"
                  value={reservationForm.date}
                  onChange={(event) => onReservationChange({ ...reservationForm, date: event.target.value })}
                />
              </div>

              <div>
                <label htmlFor="start">Inicio</label>
                <input
                  id="start"
                  type="time"
                  value={reservationForm.start}
                  onChange={(event) => onReservationChange({ ...reservationForm, start: event.target.value })}
                />
              </div>

              <div>
                <label htmlFor="end">Fin</label>
                <input
                  id="end"
                  type="time"
                  value={reservationForm.end}
                  onChange={(event) => onReservationChange({ ...reservationForm, end: event.target.value })}
                />
              </div>
            </div>

            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
              <p className="m-0 text-xs font-semibold text-slate-700">Estado de disponibilidad (próximos 7 días)</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {availabilityByDay.map((day) => (
                  <span
                    key={day.date}
                    className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                      day.hasAvailability ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                    }`}
                  >
                    {day.label}
                  </span>
                ))}
              </div>
            </div>

            {showAvailability && (
              <div className="rounded-lg border border-slate-200 bg-white p-3">
                <div className="mb-2 flex items-center justify-between gap-2">
                  <p className="m-0 text-sm font-semibold text-slate-800">Disponibilidad por bloques de 30 minutos</p>
                  {reservationForm.date && <p className="m-0 text-xs font-medium text-slate-500">{reservationForm.date}</p>}
                </div>

                {!reservationForm.date ? (
                  <p className="m-0 text-xs text-slate-600">Selecciona una fecha para ver los horarios ocupados y libres.</p>
                ) : (
                  <div className="grid grid-cols-2 gap-2 md:grid-cols-3">
                    {availabilitySlots.map((slot) => (
                      <div
                        key={`${slot.start}-${slot.end}`}
                        className={`rounded-md border px-2 py-1.5 text-xs font-semibold ${
                          slot.isAvailable ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-rose-200 bg-rose-50 text-rose-700'
                        }`}
                      >
                        {slot.start} - {slot.end}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            <div className="action-row items-center">
              <button
                type="submit"
                className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
              >
                Confirmar
              </button>
              <button type="button" className="link-btn" onClick={onClearReservationForm}>
                Limpiar formulario
              </button>
            </div>

            {reservationError && <p className="error-text">{reservationError}</p>}
          </form>
        </article>
      </section>
    </main>
  )
}

