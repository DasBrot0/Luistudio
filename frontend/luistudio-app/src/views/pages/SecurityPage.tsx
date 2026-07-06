import { AppHeader } from '../components/layout/AppHeader'
import { FilterBar } from '../components/filters/FilterBar'

export interface LoginAttemptItem {
  id: number
  userId: number
  userEmail: string
  ip: string | null
  userAgent: string | null
  attemptedAt: string
  success: boolean
  lockedUntil: string | null
}

interface SecurityPageProps {
  attempts: LoginAttemptItem[]
  loading: boolean
  emailFilter: string
  statusFilter: 'todos' | 'fallido' | 'exitoso'
  fromFilter: string
  toFilter: string
  page: number
  totalPages: number
  totalElements: number
  onEmailFilterChange: (value: string) => void
  onStatusFilterChange: (value: 'todos' | 'fallido' | 'exitoso') => void
  onFromFilterChange: (value: string) => void
  onToFilterChange: (value: string) => void
  onPrevPage: () => void
  onNextPage: () => void
  onClearFilters: () => void
}

function formatDateTime(iso: string) {
  try {
    return new Date(iso).toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  } catch {
    return iso
  }
}

function formatLockedUntil(iso: string | null) {
  if (!iso) return null
  try {
    const until = new Date(iso)
    if (until <= new Date()) return null
    return until.toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}

function ClearIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M17.5 8.5A6.2 6.2 0 0 0 7 6.8L5.5 8.3" />
      <path d="M5.5 4.8v3.5H9" />
      <path d="M6.5 15.5A6.2 6.2 0 0 0 17 17.2l1.5-1.5" />
      <path d="M18.5 19.2v-3.5H15" />
    </svg>
  )
}

export function SecurityPage({
  attempts,
  loading,
  emailFilter,
  statusFilter,
  fromFilter,
  toFilter,
  page,
  totalPages,
  totalElements,
  onEmailFilterChange,
  onStatusFilterChange,
  onFromFilterChange,
  onToFilterChange,
  onPrevPage,
  onNextPage,
  onClearFilters,
}: SecurityPageProps) {
  const noFiltersActive =
    emailFilter === '' &&
    statusFilter === 'todos' &&
    fromFilter === '' &&
    toFilter === ''

  return (
    <main className="page dashboard-page">
      <AppHeader title="Seguridad" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Historial de intentos de acceso</h2>
          </div>

          <FilterBar
            searchPlaceholder="Buscar por correo"
            searchValue={emailFilter}
            onSearchChange={onEmailFilterChange}
            filters={[
              {
                id: 'sec-status-filter',
                value: statusFilter,
                onChange: (value) => onStatusFilterChange(value as 'todos' | 'fallido' | 'exitoso'),
                options: [
                  { value: 'todos', label: 'Estado: Todos' },
                  { value: 'fallido', label: 'Estado: Fallido' },
                  { value: 'exitoso', label: 'Estado: Exitoso' },
                ],
              },
              {
                id: 'sec-from-filter',
                type: 'date',
                value: fromFilter,
                onChange: onFromFilterChange,
                ariaLabel: 'Desde fecha',
              },
              {
                id: 'sec-to-filter',
                type: 'date',
                value: toFilter,
                onChange: onToFilterChange,
                ariaLabel: 'Hasta fecha',
              },
            ]}
            quickChips={[]}
            actions={
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                onClick={onClearFilters}
                disabled={noFiltersActive}
              >
                <span className="btn-icon" aria-hidden="true">
                  <ClearIcon />
                </span>
                Reiniciar filtros
              </button>
            }
          />

          {loading && (
            <div className="empty-state">
              <p>Cargando intentos de acceso...</p>
            </div>
          )}

          {!loading && attempts.length === 0 && (
            <div className="empty-state">
              <p>No hay registros para los filtros seleccionados.</p>
            </div>
          )}

          {!loading && attempts.length > 0 && (
            <>
              <p style={{ margin: '0 0 8px', fontSize: 12, color: 'var(--color-text-secondary, #6b7280)' }}>
                {totalElements} registro{totalElements !== 1 ? 's' : ''} encontrado{totalElements !== 1 ? 's' : ''}
              </p>

              <div className="table-wrap desktop-table-only">
                <table>
                  <thead>
                    <tr>
                      <th>Correo</th>
                      <th>IP</th>
                      <th>Dispositivo</th>
                      <th>Fecha y hora</th>
                      <th>Estado</th>
                      <th>Bloqueo</th>
                    </tr>
                  </thead>
                  <tbody>
                    {attempts.map((attempt) => {
                      const lockedUntilLabel = formatLockedUntil(attempt.lockedUntil)
                      return (
                        <tr key={attempt.id}>
                          <td data-label="Correo">{attempt.userEmail}</td>
                          <td data-label="IP">{attempt.ip ?? '—'}</td>
                          <td data-label="Dispositivo" style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={attempt.userAgent ?? undefined}>
                            {attempt.userAgent ? attempt.userAgent.slice(0, 40) + (attempt.userAgent.length > 40 ? '…' : '') : '—'}
                          </td>
                          <td data-label="Fecha y hora">{formatDateTime(attempt.attemptedAt)}</td>
                          <td data-label="Estado">
                            <span className={`status-pill ${attempt.success ? 'ok' : 'cancelled'}`}>
                              {attempt.success ? 'Exitoso' : 'Fallido'}
                            </span>
                          </td>
                          <td data-label="Bloqueo">
                            {lockedUntilLabel ? (
                              <span className="status-pill cancelled" title={`Bloqueado hasta ${lockedUntilLabel}`}>
                                Hasta {lockedUntilLabel}
                              </span>
                            ) : (
                              <span style={{ color: 'var(--color-text-secondary, #6b7280)', fontSize: 12 }}>—</span>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              <div className="mobile-list-only">
                {attempts.map((attempt) => {
                  const lockedUntilLabel = formatLockedUntil(attempt.lockedUntil)
                  return (
                    <article key={`sec-mobile-${attempt.id}`} className="mobile-record-card">
                      <div className="mobile-record-grid">
                        <p><strong>Correo:</strong> {attempt.userEmail}</p>
                        <p><strong>IP:</strong> {attempt.ip ?? '—'}</p>
                        <p><strong>Fecha:</strong> {formatDateTime(attempt.attemptedAt)}</p>
                        <p className="mobile-record-state">
                          <strong>Estado:</strong>{' '}
                          <span className={`status-pill ${attempt.success ? 'ok' : 'cancelled'}`}>
                            {attempt.success ? 'Exitoso' : 'Fallido'}
                          </span>
                        </p>
                        {lockedUntilLabel && (
                          <p><strong>Bloqueado hasta:</strong> {lockedUntilLabel}</p>
                        )}
                      </div>
                    </article>
                  )
                })}
              </div>
            </>
          )}

          {!loading && attempts.length > 0 && (
            <div className="pagination">
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={page === 1}
                onClick={onPrevPage}
              >
                Anterior
              </button>
              <p>Página {page} de {totalPages}</p>
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={page === totalPages}
                onClick={onNextPage}
              >
                Siguiente
              </button>
            </div>
          )}
        </article>
      </section>
    </main>
  )
}
