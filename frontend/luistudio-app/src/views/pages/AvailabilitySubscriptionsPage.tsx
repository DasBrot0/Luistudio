import { useMemo, useState } from 'react'
import type { AvailabilitySubscription } from './ReservasPage'
import { FilterBar } from '../components/filters/FilterBar'
import { AppHeader } from '../components/layout/AppHeader'
import { Pagination } from '../components/layout/Pagination'
import { formatDate } from '../../utils/helpers'

const PAGE_SIZE = 10

interface AvailabilitySubscriptionsPageProps {
  subscriptions: AvailabilitySubscription[]
  onCancel: (subscriptionId: number) => void
  onGoToReserve: () => void
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

export function AvailabilitySubscriptionsPage({
  subscriptions,
  onCancel,
  onGoToReserve,
}: AvailabilitySubscriptionsPageProps) {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('Todos')
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    const normalized = query.trim().toLocaleLowerCase('es-PE')
    return subscriptions.filter((subscription) =>
      subscription.roomName.toLocaleLowerCase('es-PE').includes(normalized)
      && (status === 'Todos' || subscription.status === status),
    )
  }, [query, status, subscriptions])
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages)
  const visible = filtered.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE)

  return (
    <main className="page dashboard-page">
      <AppHeader title="Disponibilidad" roleLabel="Estudiante" />
      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head slim-head">
            <div>
              <h2>Avisos de disponibilidad</h2>
              <p>Suscríbete a una sala agotada y recibe un correo cuando vuelva a estar disponible.</p>
            </div>
          </div>

          <div className="mb-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            Solo puede existir un aviso activo por sala. Para crear uno, ve a Reservar y presiona la campana de un horario agotado. Cancelar un aviso elimina el interés activo asociado a esa sala.
          </div>

          {subscriptions.length === 0 ? (
            <div className="empty-state">
              <p>No tienes avisos de disponibilidad activos.</p>
              <button type="button" className="secondary-btn" onClick={onGoToReserve}>Ir a Reservar</button>
            </div>
          ) : (
            <>
              <FilterBar
                searchPlaceholder="Buscar sala"
                searchValue={query}
                onSearchChange={(value) => { setQuery(value); setPage(1) }}
                filters={[{
                  id: 'availability-status-filter',
                  value: status,
                  onChange: (value) => { setStatus(value); setPage(1) },
                  options: [
                    { value: 'Todos', label: 'Estado: Todos' },
                    ...Array.from(new Set(subscriptions.map((item) => item.status)))
                      .map((value) => ({ value, label: `Estado: ${value}` })),
                  ],
                }]}
                quickChips={[]}
                actions={(
                  <button type="button" className="secondary-btn" onClick={() => { setQuery(''); setStatus('Todos'); setPage(1) }}>
                    Reiniciar filtros
                  </button>
                )}
              />

              {filtered.length === 0 ? (
                <div className="empty-state"><p>No hay avisos para estos filtros.</p></div>
              ) : (
                <div className="table-wrap">
                  <table className="bookings-table">
                    <thead><tr><th>Sala</th><th>Fecha</th><th>Horario</th><th>Estado</th><th>Acción</th></tr></thead>
                    <tbody>
                      {visible.map((subscription) => (
                        <tr key={subscription.id}>
                          <td data-label="Sala">{subscription.roomName}</td>
                          <td data-label="Fecha">{formatDate(subscription.targetDate)}</td>
                          <td data-label="Horario">{subscription.startTime}-{subscription.endTime}</td>
                          <td data-label="Estado"><span className="status-pill ok">{subscription.status}</span></td>
                          <td data-label="Acción" className="actions-cell">
                            <button
                              type="button"
                              className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md bg-red-100 px-3 text-xs font-semibold text-red-700 transition hover:-translate-y-px hover:bg-red-200"
                              onClick={() => onCancel(subscription.id)}
                            >
                              <span className="btn-icon" aria-hidden="true"><BellSlashIcon /></span>
                              Cancelar aviso
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              <Pagination
                page={safePage}
                totalPages={totalPages}
                onPrev={() => setPage(Math.max(1, safePage - 1))}
                onNext={() => setPage(Math.min(totalPages, safePage + 1))}
              />
            </>
          )}
        </article>
      </section>
    </main>
  )
}
