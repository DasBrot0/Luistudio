import { useState } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { Booking, Room } from '../../models/types'
import type { AvailabilitySubscription } from './ReservasPage'
import { formatDate } from '../../utils/helpers'

interface MisReservasPageProps {
  myBookings: Booking[]
  activeRooms: Room[]
  mySubscriptions: AvailabilitySubscription[]
  onEditBooking: (booking: Booking) => void
  onCancelBooking: (bookingId: string) => void
  onCreateFirstReservation: () => void
  onDownloadIcs: (booking: Booking) => void
  onUnsubscribeFromSlot: (subscriptionId: number) => void
}

function PanelIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="3.5" y="5" width="7.5" height="14" rx="1.7" />
      <rect x="13" y="5" width="7.5" height="14" rx="1.7" />
    </svg>
  )
}

function ExportIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3v11.5" />
      <path d="m7.8 10.7 4.2 4.3 4.2-4.3" />
      <path d="M5 20h14" />
    </svg>
  )
}

const formatGoogleCalendarDate = (date: string, time: string) => {
  const utcDate = new Date(`${date}T${time}:00-05:00`)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${utcDate.getUTCFullYear()}${pad(utcDate.getUTCMonth() + 1)}${pad(utcDate.getUTCDate())}T${pad(
    utcDate.getUTCHours(),
  )}${pad(utcDate.getUTCMinutes())}${pad(utcDate.getUTCSeconds())}Z`
}

const buildGoogleCalendarUrl = (booking: Booking, roomName: string) => {
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: `Reserva - ${roomName}`,
    details: `Reserva Luistudio ID ${booking.backendId}`,
    location: booking.location,
    dates: `${formatGoogleCalendarDate(booking.date, booking.start)}/${formatGoogleCalendarDate(booking.date, booking.end)}`,
  })
  return `https://calendar.google.com/calendar/render?${params.toString()}`
}

function BellSlashIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5.5 9.5A6.5 6.5 0 0 1 18 8.5v3.5l2 3H4l2-3V9.5" />
      <path d="M9.5 20a2.5 2.5 0 0 0 5 0" />
      <path d="M3 3l18 18" />
    </svg>
  )
}

export function MisReservasPage({
  myBookings,
  activeRooms,
  mySubscriptions,
  onEditBooking,
  onCancelBooking,
  onCreateFirstReservation,
  onDownloadIcs,
  onUnsubscribeFromSlot,
}: MisReservasPageProps) {
  const [showDetailedView, setShowDetailedView] = useState(false)
  const [mobileExpanded, setMobileExpanded] = useState<Record<string, boolean>>({})
  const [exportModalOpen, setExportModalOpen] = useState(false)

  const canCancelBooking = (booking: Booking) => {
    if (booking.status === 'Cancelado') return false
    const endDateTime = new Date(`${booking.date}T${booking.end}:00`)
    if (Number.isNaN(endDateTime.getTime())) return false
    return endDateTime.getTime() > Date.now()
  }

  const canEditBooking = (booking: Booking) => booking.status !== 'Cancelado'

  const getRoomByCode = (roomCode: string) =>
    activeRooms.find((room) => room.id === roomCode) ?? null

  const bookingRows = myBookings.map((booking) => {
    const room = getRoomByCode(booking.roomId)
    const roomName = room?.name ?? booking.roomId
    return { booking, room, roomName }
  })
  const confirmedBookingRows = bookingRows.filter(({ booking }) => booking.status === 'Confirmado')

  const toggleMobileCard = (bookingId: string) => {
    setMobileExpanded((current) => ({ ...current, [bookingId]: !current[bookingId] }))
  }

  return (
    <main className="page dashboard-page">
      <AppHeader title="Mis reservas" roleLabel="Estudiante" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head mis-bookings-head">
            <h2>Reservas</h2>
            {myBookings.length > 0 && (
              <button
                type="button"
                className="mis-view-toggle desktop-toggle-only inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
                onClick={() => setShowDetailedView((current) => !current)}
              >
                <span className="btn-icon" aria-hidden="true">
                  <PanelIcon />
                </span>
                {showDetailedView ? 'Vista compacta' : 'Ver detalle'}
              </button>
            )}
          </div>

          {myBookings.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-6 text-center">
              <p className="m-0 text-sm font-semibold text-slate-700">Aún no tienes reservas.</p>
              <p className="mb-0 mt-1 text-xs text-slate-600">Cuando confirmes tu primera reserva, aparecerá en este listado.</p>
              <button
                type="button"
                className="mt-4 inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                onClick={onCreateFirstReservation}
              >
                Hacer mi primera reserva
              </button>
            </div>
          ) : (
            <>
              <div className="table-wrap desktop-bookings-only">
                <table className={`bookings-table ${showDetailedView ? 'bookings-table-detailed' : ''}`}>
                  {showDetailedView ? (
                    <colgroup>
                      <col className="col-detail-room" />
                      <col className="col-detail-campus" />
                      <col className="col-detail-venue" />
                      <col className="col-detail-location" />
                      <col className="col-detail-date" />
                      <col className="col-detail-time" />
                      <col className="col-detail-status" />
                    </colgroup>
                  ) : (
                    <colgroup>
                      <col className="col-room" />
                      <col className="col-date" />
                      <col className="col-time" />
                      <col className="col-status" />
                      <col className="col-actions" />
                    </colgroup>
                  )}
                  <thead>
                    {showDetailedView ? (
                      <tr>
                        <th>Sala</th>
                        <th>Campus</th>
                        <th>Recinto</th>
                        <th>Ubicación</th>
                        <th>Fecha</th>
                        <th>Horario</th>
                        <th>Estado</th>
                      </tr>
                    ) : (
                      <tr>
                        <th>Sala</th>
                        <th>Fecha</th>
                        <th>Horario</th>
                        <th>Estado</th>
                        <th>Acciones</th>
                      </tr>
                    )}
                  </thead>
                  <tbody>
                    {bookingRows.map(({ booking, room, roomName }) => {
                      if (showDetailedView) {
                        return (
                          <tr key={booking.id}>
                            <td data-label="Sala">{roomName}</td>
                            <td data-label="Campus">{room?.campusLabel ?? '—'}</td>
                            <td data-label="Recinto">{room?.venueLabel ?? booking.location}</td>
                            <td data-label="Ubicación">{room?.location ?? booking.location}</td>
                            <td data-label="Fecha">{formatDate(booking.date)}</td>
                            <td data-label="Horario">{booking.start}-{booking.end}</td>
                            <td data-label="Estado">
                              <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>
                                {booking.status}
                              </span>
                            </td>
                          </tr>
                        )
                      }
                      return (
                        <tr key={booking.id}>
                          <td data-label="Sala">{roomName}</td>
                          <td data-label="Fecha">{formatDate(booking.date)}</td>
                          <td data-label="Horario">{booking.start}-{booking.end}</td>
                          <td data-label="Estado">
                            <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>
                              {booking.status}
                            </span>
                          </td>
                          <td data-label="Acciones" className="actions-cell">
                            <div className="actions-inline">
                              <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" disabled={!canEditBooking(booking)} onClick={() => onEditBooking(booking)}>Editar</button>
                              <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200 disabled:cursor-not-allowed disabled:bg-red-100 disabled:text-red-400 disabled:opacity-70" disabled={!canCancelBooking(booking)} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              <div className="mobile-bookings-only">
                {bookingRows.map(({ booking, room, roomName }) => {
                  const isExpanded = Boolean(mobileExpanded[booking.id])
                  return (
                    <article key={`mobile-${booking.id}`} className="booking-mobile-card">
                      <div className="booking-mobile-top">
                        <p className="booking-mobile-title">{roomName}</p>
                        <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>
                          {booking.status}
                        </span>
                      </div>
                      <div className="booking-mobile-meta">
                        <div>
                          <span>Fecha</span>
                          <strong>{formatDate(booking.date)}</strong>
                        </div>
                        <div>
                          <span>Horario</span>
                          <strong>{booking.start}-{booking.end}</strong>
                        </div>
                      </div>
                      <div className="actions-inline mt-2">
                        <button
                          type="button"
                          className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
                          onClick={() => toggleMobileCard(booking.id)}
                        >
                          <span className="btn-icon" aria-hidden="true">
                            <PanelIcon />
                          </span>
                          {isExpanded ? 'Vista compacta' : 'Ver detalle'}
                        </button>
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" disabled={!canEditBooking(booking)} onClick={() => onEditBooking(booking)}>Editar</button>
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200 disabled:cursor-not-allowed disabled:bg-red-100 disabled:text-red-400 disabled:opacity-70" disabled={!canCancelBooking(booking)} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                      </div>

                      {isExpanded && (
                        <div className="booking-mobile-details">
                          <div className="booking-mobile-detail-item">
                            <span>Campus</span>
                            <strong>{room?.campusLabel ?? '—'}</strong>
                          </div>
                          <div className="booking-mobile-detail-item">
                            <span>Recinto</span>
                            <strong>{room?.venueLabel ?? booking.location}</strong>
                          </div>
                          <div className="booking-mobile-detail-item booking-mobile-detail-item-full">
                            <span>Ubicación</span>
                            <strong>{room?.location ?? booking.location}</strong>
                          </div>
                        </div>
                      )}
                    </article>
                  )
                })}
              </div>

              <div className="mt-3 flex justify-end">
                <button
                  type="button"
                  className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md bg-primary px-3 text-xs font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
                  onClick={() => setExportModalOpen(true)}
                >
                  <span className="btn-icon" aria-hidden="true">
                    <ExportIcon />
                  </span>
                  Exportar
                </button>
              </div>
            </>
          )}
        </article>
      </section>

      {mySubscriptions.length > 0 && (
        <section className="dashboard-grid single-grid">
          <article className="card">
            <div className="card-head slim-head">
              <h2>Avisos de disponibilidad</h2>
            </div>
            <p className="text-xs text-slate-500 mb-3">
              Recibirás un correo cuando el horario ocupado quede disponible.
            </p>
            <div className="table-wrap">
              <table className="bookings-table">
                <colgroup>
                  <col style={{ width: '30%' }} />
                  <col style={{ width: '15%' }} />
                  <col style={{ width: '18%' }} />
                  <col style={{ width: '12%' }} />
                  <col style={{ width: '15%' }} />
                </colgroup>
                <thead>
                  <tr>
                    <th>Sala</th>
                    <th>Fecha</th>
                    <th>Horario</th>
                    <th>Estado</th>
                    <th>Acción</th>
                  </tr>
                </thead>
                <tbody>
                  {mySubscriptions.map((sub) => (
                    <tr key={sub.id}>
                      <td data-label="Sala">{sub.roomName}</td>
                      <td data-label="Fecha">{formatDate(sub.targetDate)}</td>
                      <td data-label="Horario">{sub.startTime}–{sub.endTime}</td>
                      <td data-label="Estado">
                        <span className="status-pill ok">{sub.status}</span>
                      </td>
                      <td data-label="Acción" className="actions-cell">
                        <button
                          type="button"
                          className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200"
                          onClick={() => onUnsubscribeFromSlot(sub.id)}
                        >
                          <span className="btn-icon" aria-hidden="true">
                            <BellSlashIcon />
                          </span>
                          Cancelar aviso
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </article>
        </section>
      )}

      {exportModalOpen && (
        <section className="modal-layer" role="dialog" aria-modal="true" aria-labelledby="export-bookings-title">
          <div className="modal-card slim-modal text-left">
            <h2 id="export-bookings-title">Exportar reservas</h2>
            <div className="confirm-booking-copy">
              {confirmedBookingRows.length === 0 ? (
                <p>No tienes reservas confirmadas para exportar.</p>
              ) : (
                <div className="export-booking-list">
                  {confirmedBookingRows.map(({ booking, roomName }) => (
                    <div key={`export-${booking.id}`} className="export-booking-item">
                      <div>
                        <strong>{roomName}</strong>
                        <span>{formatDate(booking.date)} · {booking.start}-{booking.end}</span>
                      </div>
                      <div className="actions-inline">
                        <button
                          type="button"
                          className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300"
                          onClick={() => onDownloadIcs(booking)}
                        >
                          Exportar como .ics
                        </button>
                        <a
                          className="inline-flex min-h-8 items-center justify-center rounded-md bg-primary px-3 text-xs font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark"
                          href={buildGoogleCalendarUrl(booking, roomName)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          Exportar a Google Calendar
                        </a>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            <div className="modal-actions">
              <button
                type="button"
                className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
                onClick={() => setExportModalOpen(false)}
              >
                Cerrar
              </button>
            </div>
          </div>
        </section>
      )}
    </main>
  )
}
