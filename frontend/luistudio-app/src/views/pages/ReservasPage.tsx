import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { AuthUser, Booking, ReservationCompanion, ReservationForm, Room } from '../../models/types'
import type { ApiIntelligentRoomSearchResponse } from '../../services/api'

export interface AvailabilitySubscription {
  id: number
  roomId: number
  roomName: string
  targetDate: string
  startTime: string
  endTime: string
  status: string
}

interface ReservasPageProps {
  reservationForm: ReservationForm
  reservationError: string
  campusOptions: string[]
  locationOptionsByCampus: Map<string, string[]>
  activeRooms: Room[]
  selectedRoomCapacity: number | null
  roomBookings: Booking[]
  weekOffset: number
  currentUser: AuthUser | null
  companions: ReservationCompanion[]
  companionCodeInput: string
  mySubscriptions: AvailabilitySubscription[]
  onReservationChange: (next: ReservationForm) => void
  onAddCompanion: () => void
  onCompanionCodeInputChange: (value: string) => void
  onRemoveCompanion: (index: number) => void
  onWeekOffsetChange: (value: number) => void
  onClearReservationForm: () => void
  onSubmitReservation: (event: FormEvent<HTMLFormElement>) => void
  onSubscribeToSlot: (roomId: number, date: string, start: string, end: string) => void
  onUnsubscribeFromSlot: (subscriptionId: number) => void
  intelligentSearchResult: ApiIntelligentRoomSearchResponse | null
  intelligentSearchLoading: boolean
  intelligentSearchError: string
  onIntelligentSearch: (query: string, date: string, start: string, end: string) => void
  onSelectRecommendation: (roomId: number) => void
}

interface CalendarDay {
  isoDate: string
  weekdayLabel: string
  dateLabel: string
  weekday: number
}

function SweepIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m4 18 6.2-6.2 2.4 2.4L6.4 20.4H4z" />
      <path d="m11.4 10.6 1.9-1.9 2.4 2.4-1.9 1.9z" />
      <path d="m14.6 7.4 2-2a2 2 0 0 1 2.8 2.8l-2 2z" />
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

function ArrowLeftIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M20 12H6.5" />
      <path d="m11.2 17-5-5 5-5" />
    </svg>
  )
}

function ArrowRightIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 12h13.5" />
      <path d="m12.8 7 5 5-5 5" />
    </svg>
  )
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
    const weekdayLabel = current
      .toLocaleDateString('es-PE', { weekday: 'short' })
      .replace('.', '')
      .toLowerCase()
    const dateLabel = current.toLocaleDateString('es-PE', { day: '2-digit', month: '2-digit' })
    const weekday = ((current.getDay() + 6) % 7) + 1
    return { isoDate, weekdayLabel, dateLabel, weekday }
  })
}

function toDisplayDate(isoDate: string) {
  if (!isoDate) return ''
  const [year, month, day] = isoDate.split('-')
  if (!year || !month || !day) return isoDate
  return `${day}/${month}/${year}`
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
  currentUser,
  companions,
  companionCodeInput,
  mySubscriptions,
  onReservationChange,
  onAddCompanion,
  onCompanionCodeInputChange,
  onRemoveCompanion,
  onWeekOffsetChange,
  onClearReservationForm,
  onSubmitReservation,
  onSubscribeToSlot,
  onUnsubscribeFromSlot,
  intelligentSearchResult,
  intelligentSearchLoading,
  intelligentSearchError,
  onIntelligentSearch,
  onSelectRecommendation,
}: ReservasPageProps) {
  const [naturalQuery, setNaturalQuery] = useState('')
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

  const maxPeople = selectedRoom?.maxPeople ?? selectedRoomCapacity ?? 12
  const totalPeople = (currentUser ? 1 : 0) + companions.length
  const peopleLabel = `${selectedRoom ? totalPeople : 0} ${(selectedRoom ? totalPeople : 0) === 1 ? 'persona' : 'personas'}`
  const canAddCompanion = Boolean(
    selectedRoom && currentUser && companionCodeInput.trim() && totalPeople < maxPeople,
  )
  const canShowReservationDetails =
    Boolean(reservationForm.campus) &&
    Boolean(reservationForm.location) &&
    Boolean(reservationForm.roomId)

  return (
    <main className="page dashboard-page">
      <AppHeader title="Reservar" roleLabel="Estudiante" />

      {intelligentSearchResult ? Boolean(0) && <section className="smart-search-card">
        <div className="smart-search-heading">
          <div><span className="smart-search-badge">Búsqueda inteligente</span><h2>Cuéntanos qué espacio necesitas</h2><p>Escribe tu intención con tus propias palabras; ordenaremos salas disponibles por compatibilidad.</p></div>
        </div>
        <form className="smart-search-form" onSubmit={(event) => { event.preventDefault(); onIntelligentSearch(naturalQuery, reservationForm.date, reservationForm.start, reservationForm.end) }}>
          <label className="smart-search-query">Necesidad<textarea value={naturalQuery} maxLength={500} rows={2} placeholder="Ej.: Necesito estudiar en silencio con otras 3 personas, con pizarra y proyector" onChange={(event) => setNaturalQuery(event.target.value)} /></label>
          <label>Fecha<input type="date" value={reservationForm.date} onChange={(event) => onReservationChange({ ...reservationForm, date: event.target.value })} /></label>
          <label>Inicio<input type="time" value={reservationForm.start} onChange={(event) => onReservationChange({ ...reservationForm, start: event.target.value })} /></label>
          <label>Fin<input type="time" value={reservationForm.end} onChange={(event) => onReservationChange({ ...reservationForm, end: event.target.value })} /></label>
          <button className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={intelligentSearchLoading || !naturalQuery.trim() || !reservationForm.date || !reservationForm.start || !reservationForm.end}>
            <span className="btn-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" /><path d="m16 16 4 4" /></svg></span>
            {intelligentSearchLoading ? 'Interpretando…' : 'Encontrar salas'}
          </button>
        </form>
        {intelligentSearchError && <p className="smart-search-error" role="alert">{intelligentSearchError}</p>}
        {intelligentSearchResult && (
          <div className="smart-search-results">
            <div className="interpreted-intent">
              <span>Interpretamos:</span>
              <b>{intelligentSearchResult.intent.minimumCapacity} personas</b>
              <b>Ruido {intelligentSearchResult.intent.maximumNoise.toLowerCase()}</b>
              {intelligentSearchResult.intent.requiresConcentration && <b>Concentración</b>}
              {intelligentSearchResult.intent.requiredEquipment.map((item) => <b key={item}>{item}</b>)}
            </div>
            {intelligentSearchResult.recommendations.length === 0 ? <p className="smart-search-empty">No encontramos salas compatibles disponibles en ese horario. Prueba otra hora o describe requisitos más flexibles.</p> : (
              <div className="recommendation-grid">{intelligentSearchResult.recommendations.map((recommendation, index) => (
                <article className="recommendation-card" key={recommendation.room.id}>
                  <div className="recommendation-rank">#{index + 1}</div>
                  <div><span className="recommendation-score">{recommendation.score} puntos</span><h3>{recommendation.room.resourceLabel}</h3><p>{recommendation.room.campusLabel} · {recommendation.room.venueLabel} · Cap. {recommendation.room.capacity}</p></div>
                  <ul>{recommendation.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>
                  <button type="button" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onSelectRecommendation(recommendation.room.id)}><span className="btn-icon" aria-hidden="true"><CheckIcon /></span>Elegir esta sala</button>
                </article>
              ))}</div>
            )}
          </div>
        )}
      </section> : null}

      <section className="dashboard-grid single-grid">
        <article className="card booking-card">
          <form onSubmit={onSubmitReservation} className="booking-form">
            <div className="card-head slim-head booking-form-head">
              <h2>Nueva reserva</h2>
              <div className="inline-filters quick-links">
                <button
                  type="button"
                  className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={onClearReservationForm}
                >
                  <span className="btn-icon" aria-hidden="true">
                    <SweepIcon />
                  </span>
                  Limpiar
                </button>
              </div>
            </div>

            <section className="booking-block">
              <p className="booking-block-kicker">Recurso</p>
              <div className="booking-resource-grid">
                <div className="booking-field booking-field-campus">
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
                    <option value="">--</option>
                    {campusOptions.map((campus) => (
                      <option key={campus} value={campus}>
                        {campus}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="booking-field">
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
                    <option value="">--</option>
                    {locationsForCampus.map((location) => (
                      <option key={location} value={location}>
                        {location}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="booking-field">
                  <label htmlFor="room">Recurso</label>
                  <select
                    id="room"
                    value={reservationForm.roomId}
                    onChange={(event) => {
                      onReservationChange({
                        ...reservationForm,
                        roomId: event.target.value,
                        people: 0,
                        date: '',
                        start: '',
                        end: '',
                      })
                    }}
                    disabled={!reservationForm.location}
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

            {canShowReservationDetails && (
              <>
                <section className="booking-block">
                  <div className="participants-panel participants-panel-modern">
                    <div className="participants-modern-head">
                      <p className="booking-block-kicker">Personas de la reserva</p>
                      <p className="people-total-compact" aria-live="polite">
                        {peopleLabel}
                      </p>
                    </div>

                    {currentUser && selectedRoom && (
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
                          <div key={`companion-${index}`} className="participant-modern-row">
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

                <section className="booking-block">
                  <p className="booking-block-kicker">Fecha y hora</p>
                  <div className="reservation-time-row booking-time-grid">
                    <div className="compact-field">
                      <label htmlFor="date">Fecha</label>
                      <input
                        id="date"
                        type="text"
                        value={toDisplayDate(reservationForm.date)}
                        placeholder="--/--/----"
                        readOnly
                      />
                    </div>
                    <div className="compact-field">
                      <label htmlFor="start">Inicio</label>
                      <input id="start" type="text" value={reservationForm.start} placeholder="--:--" readOnly />
                    </div>
                    <div className="compact-field">
                      <label htmlFor="end">Fin</label>
                      <input id="end" type="text" value={reservationForm.end} placeholder="--:--" readOnly />
                    </div>
                  </div>
                </section>

            <div className="reservation-week-nav">
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => onWeekOffsetChange(Math.max(0, weekOffset - 1))}
                disabled={weekOffset <= 0}
              >
                <span className="btn-icon" aria-hidden="true">
                  <ArrowLeftIcon />
                </span>
                Semana anterior
              </button>
              <p className="m-0 text-xs font-semibold text-slate-600">
                {weekOffset === 0 ? 'Semana actual' : 'Semana siguiente'}
              </p>
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => onWeekOffsetChange(Math.min(maxWeekOffset, weekOffset + 1))}
                disabled={weekOffset >= maxWeekOffset}
              >
                Semana siguiente
                <span className="btn-icon" aria-hidden="true">
                  <ArrowRightIcon />
                </span>
              </button>
            </div>

            <div className="calendar-grid-wrap">
              <table className="calendar-grid">
                <thead>
                  <tr>
                    <th>Hora</th>
                    {weekDays.map((day) => (
                      <th key={day.isoDate}>
                        <span className="calendar-day-header">
                          <span>{day.weekdayLabel}</span>
                          <span>{day.dateLabel}</span>
                        </span>
                      </th>
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

                          if (occupied && selectedRoom) {
                            const existingSub = mySubscriptions.find(
                              (s) =>
                                s.roomId === selectedRoom.backendId &&
                                s.targetDate === day.isoDate &&
                                s.startTime === slot &&
                                s.endTime === slotEnd,
                            )
                            return (
                              <td key={`${day.isoDate}-${slot}`}>
                                <button
                                  type="button"
                                  className={`calendar-cell-btn blocked calendar-cell-btn-notify ${existingSub ? 'subscribed' : ''}`}
                                  title={existingSub ? 'Cancelar aviso de disponibilidad' : 'Avisarme cuando esté disponible'}
                                  aria-label={existingSub ? `Cancelar suscripción ${day.weekdayLabel} ${day.dateLabel} ${slot}` : `Suscribirse a disponibilidad ${day.weekdayLabel} ${day.dateLabel} ${slot}`}
                                  onClick={() => {
                                    if (existingSub) {
                                      onUnsubscribeFromSlot(existingSub.id)
                                    } else {
                                      onSubscribeToSlot(selectedRoom.backendId, day.isoDate, slot, slotEnd)
                                    }
                                  }}
                                >
                                  <span className="calendar-cell-notify-icon" aria-hidden="true">
                                    {existingSub ? '🔔' : '🔕'}
                                  </span>
                                </button>
                              </td>
                            )
                          }

                          return (
                            <td key={`${day.isoDate}-${slot}`}>
                              <button
                                type="button"
                                className={`calendar-cell-btn ${selected ? 'selected' : ''} ${disabled ? 'blocked' : 'available'}`}
                                disabled={disabled}
                                aria-label={`${day.weekdayLabel} ${day.dateLabel} ${slot} ${slotEnd}`}
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

            <div className="action-row items-center justify-end">
              <button
                type="submit"
                className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
              >
                <span className="btn-icon" aria-hidden="true">
                  <CheckIcon />
                </span>
                Confirmar reserva
              </button>
            </div>
              </>
            )}

            {reservationError && <p className="error-text">{reservationError}</p>}
          </form>
        </article>
      </section>
    </main>
  )
}
