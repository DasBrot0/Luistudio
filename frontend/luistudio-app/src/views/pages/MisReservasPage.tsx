import { AppHeader } from '../components/layout/AppHeader'
import type { Booking } from '../../models/types'
import { formatDate } from '../../utils/helpers'

interface MisReservasPageProps {
  myBookings: Booking[]
  onEditBooking: (booking: Booking) => void
  onCancelBooking: (bookingId: string) => void
  onCreateFirstReservation: () => void
}

export function MisReservasPage({
  myBookings,
  onEditBooking,
  onCancelBooking,
  onCreateFirstReservation,
}: MisReservasPageProps) {
  return (
    <main className="page dashboard-page">
      <AppHeader title="Mis reservas" roleLabel="Estudiante" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Reservas</h2>
          </div>

          {myBookings.length === 0 ? (
            <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 p-6 text-center">
              <p className="m-0 text-sm font-semibold text-slate-700">Aun no tienes reservas.</p>
              <p className="mb-0 mt-1 text-xs text-slate-600">Cuando confirmes tu primera reserva, aparecera en este listado.</p>
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
              <div className="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Sala</th>
                      <th>Fecha</th>
                      <th>Horario</th>
                      <th>Estado</th>
                      <th>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {myBookings.map((booking) => (
                      <tr key={booking.id}>
                        <td data-label="Sala">{booking.roomId}</td>
                        <td data-label="Fecha">{formatDate(booking.date)}</td>
                        <td data-label="Horario">{booking.start}-{booking.end}</td>
                        <td data-label="Estado">
                          <span className={`status-pill ${booking.status === 'Confirmado' ? 'ok' : 'cancelled'}`}>
                            {booking.status}
                          </span>
                        </td>
                        <td data-label="Acciones" className="actions-cell">
                          <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onEditBooking(booking)}>Editar</button>
                          <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" disabled={booking.status === 'Cancelado'} onClick={() => onCancelBooking(booking.id)}>Cancelar</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mt-3 flex justify-end">
                <button
                  type="button"
                  className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  Exportar
                </button>
              </div>
            </>
          )}
        </article>
      </section>
    </main>
  )
}
