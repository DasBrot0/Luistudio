import { useEffect, useMemo, useState } from 'react'
import type { CSSProperties } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import { Pagination } from '../components/layout/Pagination'
import { api, type ApiAdminDashboard } from '../../services/api'

interface Props {
  data: ApiAdminDashboard | null
  loading: boolean
  error: string
  from: string
  to: string
  onFromChange: (value: string) => void
  onToChange: (value: string) => void
  onApply: () => void
  onReset: () => void
}

export function AdminDashboardPage({ token }: { token: string }) {
  const today = new Date()
  const monthAgo = new Date(today); monthAgo.setDate(today.getDate() - 30)
  const iso = (date: Date) => date.toISOString().slice(0, 10)
  const [from, setFrom] = useState(iso(monthAgo))
  const [to, setTo] = useState(iso(today))
  const [data, setData] = useState<ApiAdminDashboard | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const load = async () => { setLoading(true); setError(''); try { setData(await api.getAdminDashboard(token, from, to)) } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo cargar el dashboard') } finally { setLoading(false) } }
  useEffect(() => { void load() }, [])
  const reset = () => { setFrom(iso(monthAgo)); setTo(iso(today)) }
  return <AdminDashboardContent data={data} loading={loading} error={error} from={from} to={to} onFromChange={setFrom} onToChange={setTo} onApply={() => void load()} onReset={reset} />
}

const RANKING_PAGE_SIZE = 5
const DAY_LABELS = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']
const hours = (minutes: number) => `${(minutes / 60).toFixed(minutes % 60 === 0 ? 0 : 1)} h`
const percent = (value: number) => `${value.toFixed(1)}%`
const formatShortDate = (value: string) => new Date(`${value}T00:00:00`).toLocaleDateString('es-PE', { day: '2-digit', month: 'short' })

function MetricIcon({ type }: { type: 'occupancy' | 'bookings' | 'peak' | 'absence' }) {
  const paths = {
    occupancy: <><rect x="4" y="4" width="6" height="16" rx="1" /><rect x="14" y="8" width="6" height="12" rx="1" /></>,
    bookings: <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M8 3v4M16 3v4M4 10h16" /></>,
    peak: <><circle cx="12" cy="12" r="8" /><path d="M12 7v5l3 2" /></>,
    absence: <><path d="M12 3 3.5 19h17L12 3Z" /><path d="M12 9v4M12 16h.01" /></>,
  }
  return <span className={`dashboard-metric-icon ${type}`} aria-hidden="true"><svg viewBox="0 0 24 24">{paths[type]}</svg></span>
}

function OccupancyTrend({ data }: { data: ApiAdminDashboard['dailyOccupancy'] }) {
  const width = 760
  const height = 230
  const padX = 36
  const padY = 24
  const chartWidth = width - padX * 2
  const chartHeight = height - padY * 2
  const points = data.map((item, index) => ({
    ...item,
    x: data.length <= 1 ? width / 2 : padX + (index / (data.length - 1)) * chartWidth,
    y: padY + chartHeight - (Math.min(100, item.occupancyRate) / 100) * chartHeight,
  }))
  const line = points.map((point) => `${point.x},${point.y}`).join(' ')
  const area = points.length ? `${padX},${padY + chartHeight} ${line} ${width - padX},${padY + chartHeight}` : ''
  const labelStep = Math.max(1, Math.ceil(data.length / 6))

  return (
    <div className="dashboard-trend-chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Tendencia diaria del porcentaje de ocupación">
        {[0, 25, 50, 75, 100].map((tick) => {
          const y = padY + chartHeight - (tick / 100) * chartHeight
          return <g key={tick}><line className="dashboard-chart-grid" x1={padX} x2={width - padX} y1={y} y2={y} /><text x="2" y={y + 4}>{tick}%</text></g>
        })}
        {area && <polygon className="dashboard-chart-area" points={area} />}
        {line && <polyline className="dashboard-chart-line" points={line} />}
        {points.map((point, index) => <g key={point.date}>
          <circle className="dashboard-chart-point" cx={point.x} cy={point.y} r="4"><title>{formatShortDate(point.date)}: {percent(point.occupancyRate)}, {point.reservedMinutes} min reservados</title></circle>
          {(index % labelStep === 0 || index === points.length - 1) && <text className="dashboard-chart-date" x={point.x} y={height - 2} textAnchor="middle">{formatShortDate(point.date)}</text>}
        </g>)}
      </svg>
      {data.length === 0 && <p className="dashboard-empty">No hay información diaria en este rango.</p>}
    </div>
  )
}

function AttendanceDonut({ data }: { data: ApiAdminDashboard }) {
  const rawTotal = data.attendanceCount + data.absenceCount + data.pendingAttendanceCount
  const total = Math.max(1, rawTotal)
  const attendedEnd = (data.attendanceCount / total) * 100
  const absentEnd = attendedEnd + (data.absenceCount / total) * 100
  return (
    <div className="attendance-layout">
      <div className="attendance-donut" style={{ background: rawTotal === 0 ? 'var(--surface-150)' : `conic-gradient(var(--ok-ink) 0 ${attendedEnd}%, var(--danger-ink) ${attendedEnd}% ${absentEnd}%, var(--warning-ink) ${absentEnd}% 100%)` }} role="img" aria-label={`Asistencias ${data.attendanceCount}, inasistencias ${data.absenceCount}, pendientes ${data.pendingAttendanceCount}`}>
        <div><strong>{percent(data.absenceRate)}</strong><span>inasistencia</span></div>
      </div>
      <ul className="attendance-legend">
        <li><i className="attended" /><span>Asistencias</span><strong>{data.attendanceCount}</strong></li>
        <li><i className="absent" /><span>Inasistencias</span><strong>{data.absenceCount}</strong></li>
        <li><i className="pending" /><span>Pendientes</span><strong>{data.pendingAttendanceCount}</strong></li>
      </ul>
    </div>
  )
}

function WeeklyHeatmap({ cells }: { cells: ApiAdminDashboard['weeklyHeatmap'] }) {
  const days = [1, 2, 3, 4, 5, 6]
  const activeHours = [...new Set(cells.map((cell) => cell.hour))].sort((a, b) => a - b)
  const byKey = new Map(cells.map((cell) => [`${cell.dayOfWeek}-${cell.hour}`, cell]))
  return (
    <div className="heatmap-scroll">
      <div className="dashboard-heatmap" style={{ '--heatmap-columns': days.length } as CSSProperties}>
        <span aria-hidden="true" />{days.map((day) => <strong key={day}>{DAY_LABELS[day - 1]}</strong>)}
        {activeHours.map((hour) => <div className="heatmap-row" key={hour}>
          <b>{String(hour).padStart(2, '0')}:00</b>
          {days.map((day) => {
            const cell = byKey.get(`${day}-${hour}`)
            const rate = cell?.occupancyRate ?? 0
            const intensity = Math.min(4, Math.ceil(rate / 25))
            return <span className={`heatmap-cell heat-${intensity}`} key={`${day}-${hour}`} title={`${DAY_LABELS[day - 1]} ${String(hour).padStart(2, '0')}:00 · ${percent(rate)}`}>{Math.round(rate)}%</span>
          })}
        </div>)}
      </div>
      {activeHours.length === 0 && <p className="dashboard-empty">No hay horarios configurados en este rango.</p>}
    </div>
  )
}

function AdminDashboardContent({ data, loading, error, from, to, onFromChange, onToChange, onApply, onReset }: Props) {
  const [occupancyMode, setOccupancyMode] = useState<'highest' | 'lowest'>('highest')
  const [rankingQuery, setRankingQuery] = useState('')
  const [rankingPage, setRankingPage] = useState(1)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  useEffect(() => { if (data) setLastUpdated(new Date()) }, [data])

  const totalReserved = data?.occupancyByRoom.reduce((sum, room) => sum + room.reservedMinutes, 0) ?? 0
  const totalAvailable = data?.occupancyByRoom.reduce((sum, room) => sum + room.availableMinutes, 0) ?? 0
  const occupancyRate = totalAvailable ? (totalReserved * 100) / totalAvailable : 0
  const peak = data?.peakHours[0]
  const sortedRooms = useMemo(() => {
    const rooms = [...(data?.occupancyByRoom ?? [])]
    return rooms.sort((a, b) => occupancyMode === 'highest' ? b.occupancyRate - a.occupancyRate : a.occupancyRate - b.occupancyRate).slice(0, 7)
  }, [data, occupancyMode])
  const filteredRanking = useMemo(() => (data?.topStudents ?? []).filter((student) => `${student.fullName} ${student.code} ${student.email}`.toLowerCase().includes(rankingQuery.toLowerCase())), [data, rankingQuery])
  const rankingTotalPages = Math.max(1, Math.ceil(filteredRanking.length / RANKING_PAGE_SIZE))
  const safeRankingPage = Math.min(rankingPage, rankingTotalPages)
  const visibleRanking = filteredRanking.slice((safeRankingPage - 1) * RANKING_PAGE_SIZE, safeRankingPage * RANKING_PAGE_SIZE)

  const exportCsv = () => {
    if (!data) return
    const rows = [['Puesto', 'Estudiante', 'Código', 'Reservas', 'Horas reservadas', 'Inasistencias'], ...data.topStudents.map((student, index) => [index + 1, student.fullName, student.code, student.reservationCount, (student.reservedMinutes / 60).toFixed(1), student.absenceCount])]
    const csv = rows.map((row) => row.map((value) => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = `dashboard-${from}-${to}.csv`; anchor.click(); URL.revokeObjectURL(url)
  }

  return (
    <main className="page dashboard-page admin-analytics-page">
      <AppHeader title="Dashboard administrativo" roleLabel="Administrador" />
      <section className="dashboard-metrics-shell" aria-busy={loading}>
        <div className="analytics-toolbar">
          <div className="dashboard-date-filter">
            <label>Desde<input type="date" value={from} max={to} onChange={(event) => onFromChange(event.target.value)} /></label>
            <label>Hasta<input type="date" value={to} min={from} onChange={(event) => onToChange(event.target.value)} /></label>
          </div>
          <div className="analytics-toolbar-actions">
            <span className="last-updated">Última actualización<br /><strong>{lastUpdated ? lastUpdated.toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }) : '—'}</strong></span>
            <button type="button" className="secondary-btn analytics-action" onClick={onReset} disabled={loading}>Restablecer</button>
            <button type="button" className="secondary-btn analytics-action" onClick={exportCsv} disabled={!data}>Exportar CSV</button>
            <button type="button" className="primary-btn analytics-action" disabled={loading || !from || !to || from > to} onClick={onApply}>{loading ? 'Actualizando…' : 'Actualizar'}</button>
          </div>
        </div>

        {error && <div className="dashboard-error" role="alert">{error}</div>}
        {loading && !data && <div className="dashboard-empty">Cargando métricas…</div>}
        {data && <>
          <div className="dashboard-summary-grid">
            <article><div><span>Tasa de ocupación</span><MetricIcon type="occupancy" /></div><strong>{percent(occupancyRate)}</strong><small>{hours(totalReserved)} de {hours(totalAvailable)} disponibles</small></article>
            <article><div><span>Reservas realizadas</span><MetricIcon type="bookings" /></div><strong>{data.totalReservations.toLocaleString('es-PE')}</strong><small>Activas y completadas en el periodo</small></article>
            <article><div><span>Hora pico</span><MetricIcon type="peak" /></div><strong>{peak ? `${String(peak.hour).padStart(2, '0')}:00–${String((peak.hour + 1) % 24).padStart(2, '0')}:00` : '—'}</strong><small>{peak ? `${peak.reservationCount} cruces · ${hours(peak.reservedMinutes)}` : 'Sin uso registrado'}</small></article>
            <article><div><span>Tasa de inasistencia</span><MetricIcon type="absence" /></div><strong>{percent(data.absenceRate)}</strong><small>{data.absenceCount} de {data.attendanceEligibleCount} reservas evaluadas</small></article>
          </div>

          <div className="analytics-primary-grid">
            <article className="dashboard-panel trend-panel"><div className="dashboard-panel-head"><div><h3>Tendencia de ocupación</h3><p>Evolución diaria dentro del rango seleccionado.</p></div><span className="panel-badge">{formatShortDate(data.from)} – {formatShortDate(data.to)}</span></div><OccupancyTrend data={data.dailyOccupancy} /></article>
            <article className="dashboard-panel attendance-panel"><div className="dashboard-panel-head"><div><h3>Asistencia</h3><p>Reservas evaluadas y pendientes.</p></div></div><AttendanceDonut data={data} /></article>
          </div>

          <div className="analytics-secondary-grid">
            <article className="dashboard-panel heatmap-panel"><div className="dashboard-panel-head"><div><h3>Horas pico por día</h3><p>Porcentaje de ocupación; cada celda incluye su valor.</p></div></div><WeeklyHeatmap cells={data.weeklyHeatmap} /></article>
            <article className="dashboard-panel room-bars-panel"><div className="dashboard-panel-head"><div><h3>Ocupación por sala</h3><p>Horas reservadas frente al horario disponible.</p></div><select value={occupancyMode} onChange={(event) => setOccupancyMode(event.target.value as 'highest' | 'lowest')} aria-label="Orden de ocupación"><option value="highest">Mayor ocupación</option><option value="lowest">Menor ocupación</option></select></div><div className="occupancy-list">{sortedRooms.map((room) => <div className="occupancy-row" key={room.roomId}><div><strong>{room.roomCode}</strong><span>{room.roomName}</span></div><div className="occupancy-track"><span style={{ width: `${Math.min(room.occupancyRate, 100)}%` }} /></div><b>{percent(room.occupancyRate)}</b></div>)}{sortedRooms.length === 0 && <p className="dashboard-empty">No hay salas activas.</p>}</div></article>
          </div>

          <article className="dashboard-panel dashboard-ranking-panel">
            <div className="dashboard-panel-head ranking-head"><div><h3>Estudiantes con más reservas</h3><p>Ordenados por cantidad y horas reservadas.</p></div><input type="search" value={rankingQuery} onChange={(event) => { setRankingQuery(event.target.value); setRankingPage(1) }} placeholder="Buscar estudiante o código" aria-label="Buscar en ranking" /></div>
            <div className="table-wrap"><table className="analytics-ranking-table"><thead><tr><th>Puesto</th><th>Estudiante</th><th>Código</th><th>Reservas</th><th>Horas reservadas</th><th>Inasistencias</th></tr></thead><tbody>{visibleRanking.map((student, index) => <tr key={student.userId}><td data-label="Puesto"><span className="ranking-position">{(safeRankingPage - 1) * RANKING_PAGE_SIZE + index + 1}</span></td><td data-label="Estudiante"><strong>{student.fullName}</strong><small>{student.email}</small></td><td data-label="Código">{student.code}</td><td data-label="Reservas">{student.reservationCount}</td><td data-label="Horas reservadas">{hours(student.reservedMinutes)}</td><td data-label="Inasistencias"><span className={`status-pill ${student.absenceCount ? 'cancelled' : 'ok'}`}>{student.absenceCount}</span></td></tr>)}</tbody></table>{visibleRanking.length === 0 && <p className="dashboard-empty">No hay estudiantes para este filtro.</p>}</div>
            <Pagination page={safeRankingPage} totalPages={rankingTotalPages} onPrev={() => setRankingPage(Math.max(1, safeRankingPage - 1))} onNext={() => setRankingPage(Math.min(rankingTotalPages, safeRankingPage + 1))} />
          </article>
        </>}
      </section>
    </main>
  )
}
