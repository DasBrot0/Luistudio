import type { FormEvent } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { AuthUser, Booking, BookingStatus, CampusSchedule, ScheduleDay, SystemConfig } from '../../models/types'
import { formatDate } from '../../utils/helpers'

interface AdminReservasPageProps {
  bookings: Booking[]
  users: AuthUser[]
  adminStatusFilter: 'Todos' | BookingStatus
  adminDateFilter: string
  adminPage: number
  totalAdminPages: number
  config: SystemConfig
  configDraft: SystemConfig
  configNotice: string
  campusSchedules: CampusSchedule[]
  onStatusFilterChange: (value: 'Todos' | BookingStatus) => void
  onDateFilterChange: (value: string) => void
  onPrevPage: () => void
  onNextPage: () => void
  onEditBooking: (booking: Booking) => void
  onCancelBooking: (bookingId: string) => void
  onConfigDraftChange: (draft: SystemConfig) => void
  onSaveConfig: (event: FormEvent<HTMLFormElement>) => void
  onCampusScheduleChange: (campus: CampusSchedule) => void
  onSaveCampusSchedule: (campus: CampusSchedule) => void
}

const dayLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

const patchDay = (days: ScheduleDay[], dayOfWeek: number, patch: Partial<ScheduleDay>) =>
  days.map((day) => (day.dayOfWeek === dayOfWeek ? { ...day, ...patch } : day))

export function AdminReservasPage({
  bookings,
  users,
  adminStatusFilter,
  adminDateFilter,
  adminPage,
  totalAdminPages,
  config,
  configDraft,
  configNotice,
  campusSchedules,
  onStatusFilterChange,
  onDateFilterChange,
  onPrevPage,
  onNextPage,
  onEditBooking,
  onCancelBooking,
  onConfigDraftChange,
  onSaveConfig,
  onCampusScheduleChange,
  onSaveCampusSchedule,
}: AdminReservasPageProps) {
  const canCancelBooking = (booking: Booking) => {
    if (booking.status === 'Cancelado') return false
    const endDateTime = new Date(`${booking.date}T${booking.end}:00`)
    if (Number.isNaN(endDateTime.getTime())) return false
    return endDateTime.getTime() > Date.now()
  }

  return (
    <main className="page dashboard-page">
      <AppHeader title="Reservas registradas" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Reservas activas</h2>
            <div className="inline-filters">
              <select value={adminStatusFilter} onChange={(event) => onStatusFilterChange(event.target.value as 'Todos' | BookingStatus)}>
                <option value="Todos">Todos</option>
                <option value="Confirmado">Confirmadas</option>
                <option value="Cancelado">Canceladas</option>
              </select>

              <input type="date" value={adminDateFilter} onChange={(event) => onDateFilterChange(event.target.value)} />
            </div>
          </div>

          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Estudiante</th>
                  <th>Sala</th>
                  <th>Fecha</th>
                  <th>Horario</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {bookings.map((booking) => {
                  const owner = users.find((user) => user.id === booking.userId)

                  return (
                    <tr key={booking.id}>
                      <td data-label="ID">{booking.id}</td>
                      <td data-label="Estudiante">{booking.userEmail ?? owner?.email ?? 'No registrado'}</td>
                      <td data-label="Sala">{booking.roomId}</td>
                      <td data-label="Fecha">{formatDate(booking.date)}</td>
                      <td data-label="Horario">{booking.start}-{booking.end}</td>
                      <td data-label="Estado">
                        <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>{booking.status}</span>
                      </td>
                      <td data-label="Acciones" className="actions-cell">
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onEditBooking(booking)}>Editar</button>
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:opacity-60" disabled={!canCancelBooking(booking)} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="pagination">
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" disabled={adminPage === 1} onClick={onPrevPage}>Anterior</button>
            <p>Página {adminPage} de {totalAdminPages}</p>
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" disabled={adminPage === totalAdminPages} onClick={onNextPage}>Siguiente</button>
          </div>
        </article>

        <article className="card config-card">
          <h2>Configuración del sistema</h2>
          <p className="description">Máximo de reservas simultáneas y duración máxima por reserva.</p>

          <form className="stack" onSubmit={onSaveConfig}>
            <label htmlFor="max-bookings">Máximo de reservas simultáneas</label>
            <input id="max-bookings" type="number" min={1} value={configDraft.maxActiveBookings} onChange={(event) => onConfigDraftChange({ ...configDraft, maxActiveBookings: Number(event.target.value) })} />

            <label htmlFor="max-duration">Duración máxima por reserva (minutos)</label>
            <input id="max-duration" type="number" min={30} step={15} value={configDraft.maxDurationMinutes} onChange={(event) => onConfigDraftChange({ ...configDraft, maxDurationMinutes: Number(event.target.value) })} />

            <button type="submit" className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60">Guardar configuración</button>
          </form>

          <dl className="config-summary">
            <div><dt>Límite actual</dt><dd>{config.maxActiveBookings} reservas</dd></div>
            <div><dt>Duración actual</dt><dd>{config.maxDurationMinutes} minutos</dd></div>
          </dl>

          {configNotice && <p className="success-text">{configNotice}</p>}
        </article>

        <article className="card config-card">
          <h2>Horario General por Campus</h2>
          <p className="description">Regla base para todas las salas. Si hay override por sala, se validará conflicto.</p>

          {campusSchedules.map((campusSchedule) => (
            <div key={campusSchedule.campus} className="campus-schedule-card">
              <h3>{campusSchedule.campusLabel}</h3>
              <div className="form-grid two-cols">
                <div>
                  <label htmlFor={`slot-${campusSchedule.campus}`}>Duración por botón</label>
                  <select
                    id={`slot-${campusSchedule.campus}`}
                    value={campusSchedule.slotMinutes}
                    onChange={(event) =>
                      onCampusScheduleChange({
                        ...campusSchedule,
                        slotMinutes: Number(event.target.value),
                      })
                    }
                  >
                    <option value={30}>30 minutos</option>
                    <option value={45}>45 minutos</option>
                    <option value={60}>1 hora</option>
                    <option value={120}>2 horas</option>
                  </select>
                </div>
              </div>
              <div className="room-schedule-grid">
                {campusSchedule.days.map((day) => (
                  <div key={`${campusSchedule.campus}-${day.dayOfWeek}`} className="room-schedule-row">
                    <span className="text-xs font-semibold text-slate-700">{dayLabels[day.dayOfWeek - 1]}</span>
                    <label className="remember-check m-0">
                      <input
                        type="checkbox"
                        checked={day.closed}
                        onChange={(event) =>
                          onCampusScheduleChange({
                            ...campusSchedule,
                            days: patchDay(campusSchedule.days, day.dayOfWeek, {
                              closed: event.target.checked,
                              openTime: event.target.checked ? null : day.openTime ?? '06:00',
                              closeTime: event.target.checked ? null : day.closeTime ?? '22:00',
                            }),
                          })
                        }
                      />
                      Cerrado
                    </label>
                    <input
                      type="time"
                      value={day.openTime ?? ''}
                      disabled={day.closed}
                      onChange={(event) =>
                        onCampusScheduleChange({
                          ...campusSchedule,
                          days: patchDay(campusSchedule.days, day.dayOfWeek, { openTime: event.target.value }),
                        })
                      }
                    />
                    <input
                      type="time"
                      value={day.closeTime ?? ''}
                      disabled={day.closed}
                      onChange={(event) =>
                        onCampusScheduleChange({
                          ...campusSchedule,
                          days: patchDay(campusSchedule.days, day.dayOfWeek, { closeTime: event.target.value }),
                        })
                      }
                    />
                  </div>
                ))}
              </div>
              <button
                type="button"
                className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                onClick={() => onSaveCampusSchedule(campusSchedule)}
              >
                Guardar horario de {campusSchedule.campusLabel}
              </button>
            </div>
          ))}
        </article>
      </section>
    </main>
  )
}
