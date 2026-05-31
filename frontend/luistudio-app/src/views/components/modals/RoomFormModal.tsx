import type { FormEvent } from 'react'
import type { RoomDraft, ScheduleDay } from '../../../models/types'

interface RoomFormModalProps {
  mode: 'add' | 'edit'
  draft: RoomDraft
  notice: string
  targetRoomId: string | null
  campusOptions: string[]
  venueOptionsByCampus: Map<string, string[]>
  onChange: (next: RoomDraft) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

const dayLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const defaultSchedule: ScheduleDay[] = [
  { dayOfWeek: 1, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 2, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 3, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 4, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 5, openTime: '06:00', closeTime: '22:00', closed: false },
  { dayOfWeek: 6, openTime: '06:00', closeTime: '12:00', closed: false },
  { dayOfWeek: 7, openTime: null, closeTime: null, closed: true },
]

export function RoomFormModal({
  mode,
  draft,
  notice,
  targetRoomId,
  campusOptions,
  venueOptionsByCampus,
  onChange,
  onCancel,
  onSubmit,
}: RoomFormModalProps) {
  const schedule = draft.schedule.length > 0 ? draft.schedule : defaultSchedule
  const venues = venueOptionsByCampus.get(draft.campus) ?? []

  const updateScheduleDay = (dayOfWeek: number, patch: Partial<ScheduleDay>) => {
    const nextSchedule = schedule.map((day) =>
      day.dayOfWeek === dayOfWeek ? { ...day, ...patch } : day,
    )
    onChange({ ...draft, schedule: nextSchedule })
  }

  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card text-left room-form-modal-card">
        <h2>{mode === 'add' ? 'Agregar Sala' : 'Editar Sala'}</h2>
        {mode === 'edit' && targetRoomId && <p className="meta-id">ID: {targetRoomId}</p>}

        <form className="stack" onSubmit={onSubmit}>
          <label htmlFor="room-name">Nombre interno (EN)</label>
          <input
            id="room-name"
            type="text"
            value={draft.name}
            onChange={(event) => onChange({ ...draft, name: event.target.value })}
          />

          <div className="form-grid two-cols">
            <div>
              <label htmlFor="room-campus">Campus</label>
              <select
                id="room-campus"
                value={draft.campus}
                onChange={(event) => {
                  const nextCampus = event.target.value
                  const nextVenue = venueOptionsByCampus.get(nextCampus)?.[0] ?? draft.location
                  onChange({
                    ...draft,
                    campus: nextCampus,
                    location: nextVenue,
                    pabellonCode: `${nextCampus}-${nextVenue}`.replace(/[^A-Za-z0-9]/g, '-').toUpperCase(),
                  })
                }}
              >
                {campusOptions.map((campus) => (
                  <option key={campus} value={campus}>
                    {campus}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label htmlFor="room-location">Ubicación (EN)</label>
              <select
                id="room-location"
                value={draft.location}
                onChange={(event) =>
                  onChange({
                    ...draft,
                    location: event.target.value,
                    pabellonCode: `${draft.campus}-${event.target.value}`.replace(/[^A-Za-z0-9]/g, '-').toUpperCase(),
                  })
                }
              >
                {venues.map((venue) => (
                  <option key={venue} value={venue}>
                    {venue}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-grid three-cols">
            <div>
              <label htmlFor="room-capacity">Capacidad</label>
              <input
                id="room-capacity"
                type="number"
                min={1}
                value={draft.capacity}
                onChange={(event) => {
                  const capacity = Number(event.target.value)
                  onChange({
                    ...draft,
                    capacity,
                    maxPeople: Math.min(Math.max(1, draft.maxPeople), capacity),
                  })
                }}
              />
            </div>

            <div>
              <label htmlFor="room-min-people">Mínimo personas</label>
              <input
                id="room-min-people"
                type="number"
                min={1}
                value={draft.minPeople}
                onChange={(event) => onChange({ ...draft, minPeople: Number(event.target.value) })}
              />
            </div>

            <div>
              <label htmlFor="room-max-people">Máximo personas</label>
              <input
                id="room-max-people"
                type="number"
                min={1}
                max={draft.capacity}
                value={draft.maxPeople}
                onChange={(event) => onChange({ ...draft, maxPeople: Number(event.target.value) })}
              />
            </div>
          </div>

          <label className="remember-check">
            <input
              type="checkbox"
              checked={draft.minPeopleRequired}
              onChange={(event) => onChange({ ...draft, minPeopleRequired: event.target.checked })}
            />
            Mínimo obligatorio para reservar
          </label>

          <div>
            <p className="m-0 mb-2 text-xs font-semibold text-slate-700">Horario de disponibilidad por sala</p>
            <div className="room-schedule-grid">
              {schedule.map((day) => (
                <div key={day.dayOfWeek} className="room-schedule-row">
                  <span className="text-xs font-semibold text-slate-700">{dayLabels[day.dayOfWeek - 1]}</span>
                  <label className="remember-check m-0">
                    <input
                      type="checkbox"
                      checked={day.closed}
                      onChange={(event) =>
                        updateScheduleDay(day.dayOfWeek, {
                          closed: event.target.checked,
                          openTime: event.target.checked ? null : day.openTime ?? '06:00',
                          closeTime: event.target.checked ? null : day.closeTime ?? '22:00',
                        })
                      }
                    />
                    Cerrado
                  </label>
                  <input
                    type="time"
                    value={day.openTime ?? ''}
                    disabled={day.closed}
                    onChange={(event) => updateScheduleDay(day.dayOfWeek, { openTime: event.target.value })}
                  />
                  <input
                    type="time"
                    value={day.closeTime ?? ''}
                    disabled={day.closed}
                    onChange={(event) => updateScheduleDay(day.dayOfWeek, { closeTime: event.target.value })}
                  />
                </div>
              ))}
            </div>
          </div>

          {notice && <p className="error-text">{notice}</p>}

          <div className="modal-actions">
            <button
              type="button"
              className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              onClick={onCancel}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {mode === 'add' ? 'Agregar' : 'Guardar'}
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
