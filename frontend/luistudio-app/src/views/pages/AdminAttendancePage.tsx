import type { ApiAdminAttendance } from '../../services/api'
import type { Room } from '../../models/types'
import { formatDate } from '../../utils/helpers'
import { FilterBar } from '../components/filters/FilterBar'
import { AppHeader } from '../components/layout/AppHeader'
import { Pagination } from '../components/layout/Pagination'

interface AdminAttendancePageProps {
  items: ApiAdminAttendance[]
  rooms: Room[]
  loading: boolean
  query: string
  campus: string
  pavilion: string
  status: string
  from: string
  to: string
  sort: string
  page: number
  totalPages: number
  totalElements: number
  onQueryChange: (value: string) => void
  onCampusChange: (value: string) => void
  onPavilionChange: (value: string) => void
  onStatusChange: (value: string) => void
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
  onSortChange: (value: string) => void
  onClear: () => void
  onPrev: () => void
  onNext: () => void
  onMark: (bookingId: number, status: 'ASISTIO' | 'INASISTIO') => void
}

const statusLabel = (status: ApiAdminAttendance['attendanceStatus']) => {
  if (status === 'ASISTIO') return 'Asistió'
  if (status === 'INASISTIO') return 'Inasistencia'
  return 'Pendiente'
}

const statusClass = (status: ApiAdminAttendance['attendanceStatus']) => {
  if (status === 'ASISTIO') return 'ok'
  if (status === 'INASISTIO') return 'cancelled'
  return 'pending'
}

const hasStarted = (item: ApiAdminAttendance) => new Date(`${item.date}T${item.startTime}`).getTime() <= Date.now()

export function AdminAttendancePage({
  items,
  rooms,
  loading,
  query,
  campus,
  pavilion,
  status,
  from,
  to,
  sort,
  page,
  totalPages,
  totalElements,
  onQueryChange,
  onCampusChange,
  onPavilionChange,
  onStatusChange,
  onFromChange,
  onToChange,
  onSortChange,
  onClear,
  onPrev,
  onNext,
  onMark,
}: AdminAttendancePageProps) {
  const campuses = Array.from(new Set(rooms.map((room) => room.campusLabel))).sort()
  const pavilions = Array.from(
    new Map(
      rooms
        .filter((room) => campus === 'Todos' || room.campusLabel === campus)
        .map((room) => [room.venue, room.venueLabel]),
    ).entries(),
  ).sort((a, b) => a[1].localeCompare(b[1], 'es-PE'))
  const noFilters = query === '' && campus === 'Todos' && pavilion === 'Todos'
    && status === 'Todos' && from === '' && to === '' && sort === 'date:desc'

  return (
    <main className="page dashboard-page">
      <AppHeader title="Asistencias" roleLabel="Administrador" />
      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <div>
              <h2>Control de asistencias</h2>
              <p>Marca la asistencia de una reserva iniciada o corrige una inasistencia registrada.</p>
            </div>
          </div>

          <FilterBar
            searchPlaceholder="Buscar estudiante, código o sala"
            searchValue={query}
            onSearchChange={onQueryChange}
            filters={[
              {
                id: 'attendance-campus-filter',
                value: campus,
                onChange: onCampusChange,
                options: [
                  { value: 'Todos', label: 'Campus: Todos' },
                  ...campuses.map((value) => ({ value, label: `Campus: ${value}` })),
                ],
              },
              {
                id: 'attendance-pavilion-filter',
                value: pavilion,
                onChange: onPavilionChange,
                options: [
                  { value: 'Todos', label: 'Pabellón: Todos' },
                  ...pavilions.map(([code, name]) => ({ value: code, label: `Pabellón: ${name}` })),
                ],
              },
              {
                id: 'attendance-status-filter',
                value: status,
                onChange: onStatusChange,
                options: [
                  { value: 'Todos', label: 'Asistencia: Todas' },
                  { value: 'PENDIENTE', label: 'Asistencia: Pendiente' },
                  { value: 'ASISTIO', label: 'Asistencia: Asistió' },
                  { value: 'INASISTIO', label: 'Asistencia: Inasistencia' },
                ],
              },
              { id: 'attendance-from-filter', type: 'date', value: from, onChange: onFromChange, ariaLabel: 'Desde fecha' },
              { id: 'attendance-to-filter', type: 'date', value: to, onChange: onToChange, ariaLabel: 'Hasta fecha' },
            ]}
            sortControls={[{
              id: 'attendance-sort',
              value: sort,
              onChange: onSortChange,
              options: [
                { value: 'date:desc', label: 'Fecha reciente' },
                { value: 'date:asc', label: 'Fecha antigua' },
                { value: 'pavilion:asc', label: 'Pabellón A-Z' },
                { value: 'student:asc', label: 'Estudiante A-Z' },
                { value: 'room:asc', label: 'Sala A-Z' },
                { value: 'status:asc', label: 'Estado de asistencia' },
              ],
            }]}
            quickChips={[]}
            actions={<button type="button" className="secondary-btn" onClick={onClear} disabled={noFilters}>Reiniciar filtros</button>}
          />

          {loading ? (
            <div className="empty-state"><p>Cargando asistencias...</p></div>
          ) : items.length === 0 ? (
            <div className="empty-state"><p>No hay reservas para los filtros seleccionados.</p></div>
          ) : (
            <>
              <p className="mb-2 text-xs text-slate-500">{totalElements} registro{totalElements === 1 ? '' : 's'} encontrado{totalElements === 1 ? '' : 's'}.</p>
              <div className="table-wrap desktop-table-only">
                <table>
                  <thead><tr><th>Estudiante</th><th>Pabellón / sala</th><th>Fecha y horario</th><th>Estado</th><th>Acciones</th></tr></thead>
                  <tbody>{items.map((item) => {
                    const started = hasStarted(item)
                    return (
                    <tr key={item.bookingId}>
                      <td data-label="Estudiante"><strong>{item.studentName}</strong><br /><small>{item.studentCode} · {item.studentEmail}</small></td>
                      <td data-label="Pabellón / sala"><strong>{item.pavilionName}</strong><br /><small>{item.roomName} · {item.location}</small></td>
                      <td data-label="Fecha y horario">{formatDate(item.date)}<br /><small>{item.startTime}-{item.endTime}</small></td>
                      <td data-label="Estado"><span className={`status-pill ${statusClass(item.attendanceStatus)}`}>{statusLabel(item.attendanceStatus)}</span></td>
                      <td data-label="Acciones" className="actions-cell">
                        <button type="button" className="secondary-btn" title={started ? undefined : 'La reserva todavía no ha iniciado'} disabled={!started || item.attendanceStatus === 'ASISTIO'} onClick={() => onMark(item.bookingId, 'ASISTIO')}>Marcar asistencia</button>
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 disabled:opacity-50" title={started ? undefined : 'La reserva todavía no ha iniciado'} disabled={!started || item.attendanceStatus === 'INASISTIO'} onClick={() => onMark(item.bookingId, 'INASISTIO')}>Marcar inasistencia</button>
                      </td>
                    </tr>
                  )})}</tbody>
                </table>
              </div>

              <div className="mobile-card-list mobile-table-only">
                {items.map((item) => {
                  const started = hasStarted(item)
                  return (
                  <article className="mobile-data-card" key={`mobile-attendance-${item.bookingId}`}>
                    <div className="mobile-data-card-head"><strong>{item.studentName}</strong><span className={`status-pill ${statusClass(item.attendanceStatus)}`}>{statusLabel(item.attendanceStatus)}</span></div>
                    <p>{item.studentCode} · {item.studentEmail}</p>
                    <p><strong>Pabellón:</strong> {item.pavilionName}</p>
                    <p><strong>Sala:</strong> {item.roomName} · {item.location}</p>
                    <p><strong>Reserva:</strong> {formatDate(item.date)} · {item.startTime}-{item.endTime}</p>
                    <div className="mobile-card-actions">
                      <button type="button" className="secondary-btn" title={started ? undefined : 'La reserva todavía no ha iniciado'} disabled={!started || item.attendanceStatus === 'ASISTIO'} onClick={() => onMark(item.bookingId, 'ASISTIO')}>Marcar asistencia</button>
                      <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 disabled:opacity-50" title={started ? undefined : 'La reserva todavía no ha iniciado'} disabled={!started || item.attendanceStatus === 'INASISTIO'} onClick={() => onMark(item.bookingId, 'INASISTIO')}>Marcar inasistencia</button>
                    </div>
                  </article>
                )})}
              </div>
            </>
          )}

          <Pagination page={page} totalPages={totalPages} onPrev={onPrev} onNext={onNext} />
        </article>
      </section>
    </main>
  )
}
