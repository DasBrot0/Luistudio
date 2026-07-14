import { useState } from 'react'
import type { FormEvent } from 'react'
import type { RoomDraft, ScheduleDay } from '../../../models/types'
import { buildPabellonCode } from '../../../utils/helpers'

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
  const [equipmentInput, setEquipmentInput] = useState('')
  const schedule = draft.schedule.length > 0 ? draft.schedule : defaultSchedule
  const venues = venueOptionsByCampus.get(draft.campus) ?? []

  const addEquipment = () => {
    const normalized = equipmentInput.trim().toLowerCase()
    if (!normalized || draft.equipment.includes(normalized)) return
    onChange({ ...draft, equipment: [...draft.equipment, normalized] })
    setEquipmentInput('')
  }

  const updateScheduleDay = (dayOfWeek: number, patch: Partial<ScheduleDay>) => {
    const nextSchedule = schedule.map((day) =>
      day.dayOfWeek === dayOfWeek ? { ...day, ...patch } : day,
    )
    onChange({ ...draft, schedule: nextSchedule })
  }

  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card text-left room-form-modal-card">
        <h2>{mode === 'add' ? 'Agregar sala' : 'Editar sala'}</h2>
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
                    pabellonCode: buildPabellonCode(nextCampus, nextVenue),
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
                    pabellonCode: buildPabellonCode(draft.campus, event.target.value),
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

          <div>
            <label htmlFor="room-status">Estado</label>
            <select
              id="room-status"
              value={draft.status}
              onChange={(event) =>
                onChange({
                  ...draft,
                  status: event.target.value as RoomDraft['status'],
                })
              }
            >
              <option value="Disponible">Disponible</option>
              <option value="En mantenimiento">En mantenimiento</option>
              <option value="Inactiva">Inactiva</option>
            </select>
          </div>

          <label className="remember-check">
            <input
              type="checkbox"
              checked={draft.minPeopleRequired}
              onChange={(event) => onChange({ ...draft, minPeopleRequired: event.target.checked })}
            />
            Mínimo obligatorio para reservar
          </label>

          <fieldset className="rounded-xl border border-slate-200 p-4">
            <legend className="px-2 text-sm font-semibold text-slate-800">Datos para búsqueda inteligente</legend>
            <p className="mb-4 mt-0 text-xs text-slate-500">
              Estos atributos se guardan por sala y se usan para filtrar y ordenar las recomendaciones.
            </p>

            <div className="form-grid two-cols">
              <div>
                <label htmlFor="room-type">Tipo de sala</label>
                <select
                  id="room-type"
                  value={draft.roomType}
                  onChange={(event) => onChange({ ...draft, roomType: event.target.value as RoomDraft['roomType'] })}
                >
                  <option value="GENERAL">General</option>
                  <option value="ESTUDIO_INDIVIDUAL">Estudio individual</option>
                  <option value="ESTUDIO_GRUPAL">Estudio grupal</option>
                  <option value="REUNION">Reunión</option>
                  <option value="PRESENTACION">Presentación</option>
                </select>
              </div>

              <div>
                <label htmlFor="room-noise-level">Nivel de ruido</label>
                <select
                  id="room-noise-level"
                  value={draft.noiseLevel}
                  onChange={(event) => onChange({ ...draft, noiseLevel: event.target.value as RoomDraft['noiseLevel'] })}
                >
                  <option value="BAJO">Bajo</option>
                  <option value="MEDIO">Medio</option>
                  <option value="ALTO">Alto</option>
                </select>
              </div>
            </div>

            <label className="remember-check mt-3">
              <input
                type="checkbox"
                checked={draft.supportsConcentration}
                onChange={(event) => onChange({ ...draft, supportsConcentration: event.target.checked })}
              />
              Apta para actividades que requieren concentración
            </label>

            <div className="mt-3">
              <label htmlFor="room-equipment">Equipamiento</label>
              <div className="flex flex-col gap-2 sm:flex-row">
                <input
                  id="room-equipment"
                  type="text"
                  maxLength={50}
                  value={equipmentInput}
                  placeholder="Ej.: proyector, pizarra, computadora"
                  onChange={(event) => setEquipmentInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault()
                      addEquipment()
                    }
                  }}
                />
                <button
                  type="button"
                  className="inline-flex min-h-10 shrink-0 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={!equipmentInput.trim()}
                  onClick={addEquipment}
                >
                  Agregar
                </button>
              </div>
              {draft.equipment.length > 0 ? (
                <div className="mt-3 flex flex-wrap gap-2" aria-label="Equipamiento registrado">
                  {draft.equipment.map((item) => (
                    <span key={item} className="inline-flex items-center gap-2 rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                      {item}
                      <button
                        type="button"
                        className="text-base leading-none text-slate-500 hover:text-red-600"
                        aria-label={`Quitar ${item}`}
                        onClick={() => onChange({ ...draft, equipment: draft.equipment.filter((value) => value !== item) })}
                      >
                        ×
                      </button>
                    </span>
                  ))}
                </div>
              ) : (
                <p className="mb-0 mt-2 text-xs text-slate-500">Sin equipamiento registrado.</p>
              )}
            </div>
          </fieldset>

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
