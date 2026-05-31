import type { FormEvent } from 'react'
import type { ReservationForm, Room } from '../../../models/types'

interface EditBookingModalProps {
  form: ReservationForm
  locationOptions: string[]
  activeRooms: Room[]
  errorMessage: string
  onChange: (next: ReservationForm) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function EditBookingModal({
  form,
  locationOptions,
  activeRooms,
  errorMessage,
  onChange,
  onCancel,
  onSubmit,
}: EditBookingModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card">
        <h2>Editar reserva</h2>

        <form className="stack" onSubmit={onSubmit}>
          <label htmlFor="edit-location">Ubicación</label>
          <select
            id="edit-location"
            value={form.location}
            onChange={(event) => {
              const nextLocation = event.target.value
              const firstRoom = activeRooms.find((room) => room.location === nextLocation)
              onChange({
                ...form,
                location: nextLocation,
                roomId: firstRoom?.id ?? '',
              })
            }}
          >
            {locationOptions.map((location) => (
              <option key={location} value={location}>
                {location}
              </option>
            ))}
          </select>

          <label htmlFor="edit-room">Sala</label>
          <select
            id="edit-room"
            value={form.roomId}
            onChange={(event) => onChange({ ...form, roomId: event.target.value })}
          >
            {activeRooms
              .filter((room) => room.location === form.location)
              .map((room) => (
                <option key={room.id} value={room.id}>
                  {room.id}
                </option>
              ))}
          </select>

          <label htmlFor="edit-people">Personas</label>
          <input
            id="edit-people"
            type="number"
            min={1}
            max={12}
            value={form.people}
            onChange={(event) => onChange({ ...form, people: Number(event.target.value) })}
          />

          <label htmlFor="edit-date">Fecha</label>
          <input
            id="edit-date"
            type="date"
            value={form.date}
            onChange={(event) => onChange({ ...form, date: event.target.value })}
          />

          <div className="form-grid two-cols">
            <div>
              <label htmlFor="edit-start">Inicio</label>
              <input
                id="edit-start"
                type="time"
                value={form.start}
                onChange={(event) => onChange({ ...form, start: event.target.value })}
              />
            </div>
            <div>
              <label htmlFor="edit-end">Fin</label>
              <input
                id="edit-end"
                type="time"
                value={form.end}
                onChange={(event) => onChange({ ...form, end: event.target.value })}
              />
            </div>
          </div>

          {errorMessage && <p className="error-text">{errorMessage}</p>}

          <div className="modal-actions">
            <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onCancel}>
              Cancelar
            </button>
            <button type="submit" className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60">
              Guardar cambios
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
