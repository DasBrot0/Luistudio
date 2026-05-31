import { useMemo } from 'react'
import type { FormEvent } from 'react'
import type { AuthUser, ReservationCompanion, ReservationForm, Room } from '../../../models/types'

interface EditBookingModalProps {
  form: ReservationForm
  campusOptions: string[]
  locationOptionsByCampus: Map<string, string[]>
  activeRooms: Room[]
  currentUser: AuthUser | null
  companions: ReservationCompanion[]
  companionCodeInput: string
  errorMessage: string
  onChange: (next: ReservationForm) => void
  onCompanionCodeInputChange: (value: string) => void
  onAddCompanion: () => void
  onRemoveCompanion: (index: number) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
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

function dayOfWeekFromIsoDate(isoDate: string) {
  const date = new Date(`${isoDate}T00:00:00`)
  return ((date.getDay() + 6) % 7) + 1
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m4.5 12.5 4.6 4.6L19.5 6.8" />
    </svg>
  )
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="8.2" r="3.2" />
      <path d="M5.2 18.2c1.4-2.7 3.8-4.1 6.8-4.1s5.4 1.4 6.8 4.1" />
    </svg>
  )
}

function MinusUserIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3.6 17.5c1-2.5 3-3.8 5.4-3.8s4.4 1.3 5.4 3.8" />
      <path d="M15.5 10.5H21" />
    </svg>
  )
}

function SaveIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5 3h11l3 3v15H5z" />
      <path d="M8 3v6h8V3" />
      <path d="M8 16h8" />
    </svg>
  )
}

export function EditBookingModal({
  form,
  campusOptions,
  locationOptionsByCampus,
  activeRooms,
  currentUser,
  companions,
  companionCodeInput,
  errorMessage,
  onChange,
  onCompanionCodeInputChange,
  onAddCompanion,
  onRemoveCompanion,
  onCancel,
  onSubmit,
}: EditBookingModalProps) {
  const roomsByCampus = useMemo(
    () => activeRooms.filter((room) => room.campusLabel === form.campus),
    [activeRooms, form.campus],
  )

  const locationsForCampus = useMemo(
    () => locationOptionsByCampus.get(form.campus) ?? [],
    [locationOptionsByCampus, form.campus],
  )

  const roomsByLocation = useMemo(
    () => roomsByCampus.filter((room) => room.venueLabel === form.location),
    [roomsByCampus, form.location],
  )

  const selectedRoom = useMemo(
    () => activeRooms.find((room) => room.id === form.roomId) ?? null,
    [activeRooms, form.roomId],
  )

  const slotMinutes = selectedRoom?.slotMinutes ?? 60
  const totalPeople = (currentUser ? 1 : 0) + companions.length
  const peopleLabel = `${totalPeople} ${totalPeople === 1 ? 'persona' : 'personas'}`
  const maxPeople = selectedRoom?.maxPeople ?? 12
  const canAddCompanion = Boolean(selectedRoom && currentUser && companionCodeInput.trim() && totalPeople < maxPeople)

  const availableStartSlots = useMemo(() => {
    if (!selectedRoom || !form.date) return []
    const weekday = dayOfWeekFromIsoDate(form.date)
    const schedule = selectedRoom.schedule.find((day) => day.dayOfWeek === weekday)
    if (!schedule || schedule.closed || !schedule.openTime || !schedule.closeTime) return []

    const open = toMinutes(schedule.openTime)
    const close = toMinutes(schedule.closeTime)
    const slots: string[] = []
    for (let minute = open; minute + slotMinutes <= close; minute += slotMinutes) {
      slots.push(toTime(minute))
    }
    return slots
  }, [selectedRoom, form.date, slotMinutes])

  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card room-form-modal-card">
        <h2>Editar reserva</h2>

        <form className="booking-form" onSubmit={onSubmit}>
          <section className="booking-block">
            <p className="booking-block-kicker">Recurso</p>
            <div className="booking-resource-grid">
              <div className="booking-field booking-field-campus">
                <label htmlFor="edit-campus">Campus</label>
                <select
                  id="edit-campus"
                  value={form.campus}
                  onChange={(event) => {
                    const nextCampus = event.target.value
                    onChange({
                      ...form,
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
                  <option value="">--</option>
                  {campusOptions.map((campus) => (
                    <option key={campus} value={campus}>
                      {campus}
                    </option>
                  ))}
                </select>
              </div>

              <div className="booking-field">
                <label htmlFor="edit-location">Ubicación</label>
                <select
                  id="edit-location"
                  value={form.location}
                  onChange={(event) => {
                    const nextLocation = event.target.value
                    onChange({
                      ...form,
                      location: nextLocation,
                      roomId: '',
                      people: 0,
                      date: '',
                      start: '',
                      end: '',
                    })
                  }}
                  disabled={!form.campus}
                >
                  <option value="">--</option>
                  {locationsForCampus.map((location) => (
                    <option key={location} value={location}>
                      {location}
                    </option>
                  ))}
                </select>
              </div>

              <div className="booking-field">
                <label htmlFor="edit-room">Recurso</label>
                <select
                  id="edit-room"
                  value={form.roomId}
                  onChange={(event) =>
                    onChange({
                      ...form,
                      roomId: event.target.value,
                      people: 0,
                      date: '',
                      start: '',
                      end: '',
                    })
                  }
                  disabled={!form.location}
                >
                  <option value="">--</option>
                  {roomsByLocation.map((room) => (
                    <option key={room.id} value={room.id}>
                      {room.resourceLabel}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            {selectedRoom && (
              <p className="booking-resource-meta">
                Cap. {selectedRoom.capacity} · Min. {selectedRoom.minPeople}
                {selectedRoom.minPeopleRequired ? ' obligatorio' : ' opcional'} · Max. {selectedRoom.maxPeople} · Bloques de {slotMinutes} min
              </p>
            )}
          </section>

          {selectedRoom && (
            <>
              <section className="booking-block">
                <p className="booking-block-kicker">Fecha y hora</p>
                <div className="reservation-time-row booking-time-grid">
                  <div className="compact-field">
                    <label htmlFor="edit-date">Fecha</label>
                    <input
                      id="edit-date"
                      type="date"
                      value={form.date}
                      onChange={(event) =>
                        onChange({
                          ...form,
                          date: event.target.value,
                          start: '',
                          end: '',
                        })
                      }
                    />
                  </div>
                  <div className="compact-field">
                    <label htmlFor="edit-start">Inicio</label>
                    <select
                      id="edit-start"
                      value={form.start}
                      disabled={!form.date}
                      onChange={(event) =>
                        onChange({
                          ...form,
                          start: event.target.value,
                          end: event.target.value ? toTime(toMinutes(event.target.value) + slotMinutes) : '',
                        })
                      }
                    >
                      <option value="">--</option>
                      {availableStartSlots.map((start) => (
                        <option key={start} value={start}>
                          {start}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="compact-field">
                    <label htmlFor="edit-end">Fin</label>
                    <input id="edit-end" type="time" value={form.end} readOnly />
                  </div>
                </div>
              </section>

              <section className="booking-block">
                <div className="participants-panel participants-panel-modern">
                  <div className="participants-modern-head">
                    <p className="booking-block-kicker">Personas de la reserva</p>
                    <p className="people-total-compact" aria-live="polite">
                      {peopleLabel}
                    </p>
                  </div>

                  {currentUser && (
                    <>
                      <div className="participant-modern-row participant-modern-row-owner">
                        <span className="participant-modern-check" aria-hidden="true">
                          <CheckIcon />
                        </span>
                        <div className="participant-modern-copy">
                          <strong>{`${currentUser.firstName} ${currentUser.lastName}`.trim()}</strong>
                          <span>{currentUser.code}</span>
                        </div>
                      </div>

                      {companions.map((companion, index) => (
                        <div key={`edit-companion-${index}`} className="participant-modern-row">
                          <span className="participant-modern-avatar" aria-hidden="true">
                            <UserIcon />
                          </span>
                          <div className="participant-modern-copy">
                            <strong>{companion.fullName}</strong>
                            <span>{companion.code}</span>
                          </div>
                          <button
                            type="button"
                            className="person-action-btn person-action-btn-danger inline-flex min-h-8 items-center justify-center gap-2 rounded-md border bg-white px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => onRemoveCompanion(index)}
                          >
                            <span className="btn-icon" aria-hidden="true">
                              <MinusUserIcon />
                            </span>
                            Quitar
                          </button>
                        </div>
                      ))}

                      <div className="participant-modern-row participant-modern-row-add">
                        <span className="participant-modern-avatar participant-modern-avatar-muted" aria-hidden="true">
                          <UserIcon />
                        </span>
                        <input
                          type="text"
                          value={companionCodeInput}
                          onChange={(event) => onCompanionCodeInputChange(event.target.value)}
                          placeholder="Código"
                          aria-label="Código de persona a agregar"
                        />
                        <button
                          type="button"
                          className="person-action-btn inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                          onClick={onAddCompanion}
                          disabled={!canAddCompanion}
                        >
                          + Agregar
                        </button>
                      </div>
                    </>
                  )}
                </div>
              </section>
            </>
          )}

          {errorMessage && <p className="error-text">{errorMessage}</p>}

          <div className="modal-actions">
            <button type="button" className="ghost-btn" onClick={onCancel}>
              Cancelar
            </button>
            <button
              type="submit"
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              <span className="btn-icon" aria-hidden="true">
                <SaveIcon />
              </span>
              Guardar cambios
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
