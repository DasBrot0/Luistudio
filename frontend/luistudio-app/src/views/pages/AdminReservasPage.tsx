import type { FormEvent } from 'react'
import type { AuthUser, Booking, BookingStatus, CampusSchedule, ScheduleDay, SystemConfig } from '../../models/types'
import { formatDate } from '../../utils/helpers'
import { FilterBar } from '../components/filters/FilterBar'
import { AppHeader } from '../components/layout/AppHeader'

interface AdminReservasPageProps {
  bookings: Booking[]
  users: AuthUser[]
  adminSearchQuery: string
  adminStatusFilter: 'Todos' | BookingStatus
  adminCampusFilter: string
  adminDateFilter: string
  adminDateQuickFilter: 'none' | 'today' | 'week'
  adminSort: string
  adminPage: number
  totalAdminPages: number
  campusOptions: string[]
  config: SystemConfig
  configDraft: SystemConfig
  configNotice: string
  campusSchedules: CampusSchedule[]
  onSearchQueryChange: (value: string) => void
  onStatusFilterChange: (value: 'Todos' | BookingStatus) => void
  onCampusFilterChange: (value: string) => void
  onDateFilterChange: (value: string) => void
  onTodayFilter: () => void
  onWeekFilter: () => void
  onClearDateFilter: () => void
  onSortChange: (value: string) => void
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

function ClearDateIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M17.5 8.5A6.2 6.2 0 0 0 7 6.8L5.5 8.3" />
      <path d="M5.5 4.8v3.5H9" />
      <path d="M6.5 15.5A6.2 6.2 0 0 0 17 17.2l1.5-1.5" />
      <path d="M18.5 19.2v-3.5H15" />
    </svg>
  )
}

const patchDay = (days: ScheduleDay[], dayOfWeek: number, patch: Partial<ScheduleDay>) =>
  days.map((day) => (day.dayOfWeek === dayOfWeek ? { ...day, ...patch } : day))

export function AdminReservasPage({
  bookings,
  users,
  adminSearchQuery,
  adminStatusFilter,
  adminCampusFilter,
  adminDateFilter,
  adminDateQuickFilter,
  adminSort,
  adminPage,
  totalAdminPages,
  campusOptions,
  config,
  configDraft,
  configNotice,
  campusSchedules,
  onSearchQueryChange,
  onStatusFilterChange,
  onCampusFilterChange,
  onDateFilterChange,
  onTodayFilter,
  onWeekFilter,
  onClearDateFilter,
  onSortChange,
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

  const bookingRows = bookings.map((booking) => {
    const owner = users.find((user) => user.id === booking.userId)
    return {
      booking,
      ownerEmail: booking.userEmail ?? owner?.email ?? 'No registrado',
    }
  })

  return (
    <main className="page dashboard-page">
      <AppHeader title="Reservas registradas" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Listado de Reservas</h2>
          </div>

          <FilterBar
            searchPlaceholder="Buscar por estudiante o sala"
            searchValue={adminSearchQuery}
            onSearchChange={onSearchQueryChange}
            filters={[
              {
                id: 'admin-bookings-status-filter',
                value: adminStatusFilter,
                onChange: (value) => onStatusFilterChange(value as 'Todos' | BookingStatus),
                options: [
                  { value: 'Todos', label: 'Estado: Todos' },
                  { value: 'Confirmado', label: 'Estado: Confirmado' },
                  { value: 'Cancelado', label: 'Estado: Cancelado' },
                ],
              },
              {
                id: 'admin-bookings-campus-filter',
                value: adminCampusFilter,
                onChange: onCampusFilterChange,
                options: [
                  { value: 'Todos', label: 'Campus: Todos' },
                  ...campusOptions.map((campus) => ({ value: campus, label: `Campus: ${campus}` })),
                ],
              },
              {
                id: 'admin-bookings-date-filter',
                type: 'date',
                value: adminDateFilter,
                onChange: onDateFilterChange,
                ariaLabel: 'Filtrar por fecha',
              },
            ]}
            sortControls={[
              {
                id: 'admin-bookings-sort-filter',
                value: adminSort,
                onChange: onSortChange,
                options: [
                  { value: 'date:desc', label: 'Fecha reciente' },
                  { value: 'date:asc', label: 'Fecha antigua' },
                  { value: 'room:asc', label: 'Sala A-Z' },
                  { value: 'student:asc', label: 'Estudiante A-Z' },
                ],
              },
            ]}
            quickChipsPlacement="sort-row"
            quickChips={[
              {
                id: 'admin-bookings-today',
                label: 'Hoy',
                active: adminDateQuickFilter === 'today',
                onClick: onTodayFilter,
              },
              {
                id: 'admin-bookings-week',
                label: 'Esta semana',
                active: adminDateQuickFilter === 'week',
                onClick: onWeekFilter,
              },
            ]}
            actions={
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={onClearDateFilter}
                disabled={adminStatusFilter === 'Todos' && adminCampusFilter === 'Todos' && adminDateQuickFilter === 'none' && adminDateFilter === '' && adminSort === 'date:desc'}
              >
                <span className="btn-icon" aria-hidden="true">
                  <ClearDateIcon />
                </span>
                Reiniciar filtros
              </button>
            }
          />

          {bookingRows.length === 0 && (
            <div className="empty-state">
              <p>No hay reservas para los filtros seleccionados.</p>
            </div>
          )}

          {bookingRows.length > 0 && (
            <>
              <div className="table-wrap desktop-table-only">
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
                    {bookingRows.map(({ booking, ownerEmail }) => (
                      <tr key={booking.id}>
                        <td data-label="ID">{booking.id}</td>
                        <td data-label="Estudiante">{ownerEmail}</td>
                        <td data-label="Sala">{booking.roomId}</td>
                        <td data-label="Fecha">{formatDate(booking.date)}</td>
                        <td data-label="Horario">{booking.start}-{booking.end}</td>
                        <td data-label="Estado">
                          <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>{booking.status}</span>
                        </td>
                        <td data-label="Acciones" className="actions-cell">
                          <div className="actions-inline">
                            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" disabled={booking.status === 'Cancelado'} onClick={() => onEditBooking(booking)}>Editar</button>
                            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:opacity-60" disabled={!canCancelBooking(booking)} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mobile-list-only">
                {bookingRows.map(({ booking, ownerEmail }) => (
                  <article key={`admin-mobile-${booking.id}`} className="mobile-record-card">
                    <div className="mobile-record-grid">
                      <p><strong>ID:</strong> {booking.id}</p>
                      <p><strong>Estudiante:</strong> {ownerEmail}</p>
                      <p><strong>Sala:</strong> {booking.roomId}</p>
                      <p><strong>Fecha:</strong> {formatDate(booking.date)}</p>
                      <p><strong>Horario:</strong> {booking.start}-{booking.end}</p>
                      <p className="mobile-record-state">
                        <strong>Estado:</strong>{' '}
                        <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>{booking.status}</span>
                      </p>
                    </div>
                    <div className="actions-inline mt-2">
                      <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" disabled={booking.status === 'Cancelado'} onClick={() => onEditBooking(booking)}>Editar</button>
                      <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500 disabled:opacity-60" disabled={!canCancelBooking(booking)} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                    </div>
                  </article>
                ))}
              </div>
            </>
          )}

          {bookingRows.length > 0 && (
          <div className="pagination">
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" disabled={adminPage === 1} onClick={onPrevPage}>Anterior</button>
            <p>Página {adminPage} de {totalAdminPages}</p>
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" disabled={adminPage === totalAdminPages} onClick={onNextPage}>Siguiente</button>
          </div>
          )}
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
          <p className="description">Regla base para todas las salas. Se validará conflicto con el horario individual de cada sala.</p>

          {campusSchedules.map((campusSchedule) => (
            <div key={campusSchedule.campus} className="campus-schedule-card">
              <h3>{campusSchedule.campusLabel}</h3>
              <div className="form-grid two-cols">
                <div>
                  <label htmlFor={`slot-${campusSchedule.campus}`}>Duración por reserva</label>
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
                      step={campusSchedule.slotMinutes * 60}
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
                      step={campusSchedule.slotMinutes * 60}
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
