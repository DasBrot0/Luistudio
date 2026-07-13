import { AppHeader } from '../components/layout/AppHeader'
import { Pagination } from '../components/layout/Pagination'
import { FilterBar } from '../components/filters/FilterBar'
import type { Profile } from '../../models/types'

interface PerfilesPageProps {
  paginatedProfiles: Profile[]
  profilesPage: number
  totalProfilePages: number
  searchQuery: string
  yearFilter: string
  statusFilter: 'Todos' | 'Habilitado' | 'Deshabilitado' | 'Bloqueado'
  sortBy: string
  sortDir: 'asc' | 'desc'
  onSearchQueryChange: (value: string) => void
  onYearFilterChange: (value: string) => void
  onStatusFilterChange: (value: 'Todos' | 'Habilitado' | 'Deshabilitado' | 'Bloqueado') => void
  onSortChange: (value: string) => void
  onSortDirectionToggle: () => void
  onResetFilters: () => void
  onToggleProfileStatus: (profileId: string) => void
  onUnlockProfile: (profileId: string) => void
  onPrevPage: () => void
  onNextPage: () => void
}

function ResetFiltersIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M17.5 8.5A6.2 6.2 0 0 0 7 6.8L5.5 8.3" />
      <path d="M5.5 4.8v3.5H9" />
      <path d="M6.5 15.5A6.2 6.2 0 0 0 17 17.2l1.5-1.5" />
      <path d="M18.5 19.2v-3.5H15" />
    </svg>
  )
}

export function PerfilesPage({
  paginatedProfiles,
  profilesPage,
  totalProfilePages,
  searchQuery,
  yearFilter,
  statusFilter,
  sortBy,
  sortDir,
  onSearchQueryChange,
  onYearFilterChange,
  onStatusFilterChange,
  onSortChange,
  onSortDirectionToggle,
  onResetFilters,
  onToggleProfileStatus,
  onUnlockProfile,
  onPrevPage,
  onNextPage,
}: PerfilesPageProps) {
  const showMobileFilters = false
  const sortValue = `${sortBy}:${sortDir}`

  const handleCombinedSortChange = (value: string) => {
    const [nextSortBy, nextSortDir] = value.split(':') as [string, 'asc' | 'desc']
    if (nextSortBy !== sortBy) onSortChange(nextSortBy)
    if (nextSortDir !== sortDir) onSortDirectionToggle()
  }

  return (
    <main className="page dashboard-page profiles-page">
      <AppHeader title="Perfiles" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Listado de perfiles</h2>
          </div>

          <FilterBar
            searchPlaceholder="Buscar por código, correo o nombre"
            searchValue={searchQuery}
            onSearchChange={onSearchQueryChange}
            fieldsClassName="filter-bar-fields-three"
            filters={[
              {
                id: 'profiles-status-filter',
                value: statusFilter,
                onChange: (value) => onStatusFilterChange(value as 'Todos' | 'Habilitado' | 'Deshabilitado' | 'Bloqueado'),
                options: [
                  { value: 'Todos', label: 'Estado: Todos' },
                  { value: 'Habilitado', label: 'Estado: Habilitado' },
                  { value: 'Deshabilitado', label: 'Estado: Deshabilitado' },
                  { value: 'Bloqueado', label: 'Estado: Bloqueado' },
                ],
              },
              {
                id: 'profiles-year-filter',
                type: 'text',
                value: yearFilter,
                onChange: onYearFilterChange,
                placeholder: 'Año',
                ariaLabel: 'Filtrar por año',
                inputMode: 'numeric',
              },
              {
                id: 'profiles-sort-filter',
                value: sortValue,
                onChange: handleCombinedSortChange,
                options: [
                  { value: 'firstName:asc', label: 'Nombre A-Z' },
                  { value: 'firstName:desc', label: 'Nombre Z-A' },
                  { value: 'lastName:asc', label: 'Apellidos A-Z' },
                  { value: 'lastName:desc', label: 'Apellidos Z-A' },
                  { value: 'code:asc', label: 'Código ↑' },
                  { value: 'code:desc', label: 'Código ↓' },
                ],
              },
            ]}
            sortControls={[
              {
                id: 'profiles-sort-control',
                value: sortValue,
                onChange: handleCombinedSortChange,
                options: [
                  { value: 'firstName:asc', label: 'Nombre A-Z' },
                  { value: 'firstName:desc', label: 'Nombre Z-A' },
                  { value: 'lastName:asc', label: 'Apellidos A-Z' },
                  { value: 'lastName:desc', label: 'Apellidos Z-A' },
                  { value: 'code:asc', label: 'Código ↑' },
                  { value: 'code:desc', label: 'Código ↓' },
                ],
              },
            ]}
            quickChips={[]}
            actions={
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
                onClick={onResetFilters}
              >
                <span className="btn-icon" aria-hidden="true">
                  <ResetFiltersIcon />
                </span>
                Reiniciar filtros
              </button>
            }
          />

          <div className={`profiles-secondary-filters ${showMobileFilters ? 'open' : ''}`}>
            <select value={sortValue} onChange={(event) => handleCombinedSortChange(event.target.value)} aria-label="Ordenar perfiles">
              <option value="firstName:asc">Nombre A-Z</option>
              <option value="firstName:desc">Nombre Z-A</option>
              <option value="lastName:asc">Apellidos A-Z</option>
              <option value="lastName:desc">Apellidos Z-A</option>
              <option value="code:asc">Código ascendente</option>
              <option value="code:desc">Código descendente</option>
              <option value="status:asc">Estado A-Z</option>
              <option value="status:desc">Estado Z-A</option>
            </select>
            <input
              type="text"
              value={yearFilter}
              onChange={(event) => onYearFilterChange(event.target.value)}
              placeholder="Año"
              aria-label="Filtrar por año"
              inputMode="numeric"
            />
          </div>

          <div className="table-wrap desktop-table-only">
            <table className="profiles-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Código</th>
                  <th>Correo</th>
                  <th>Nombre</th>
                  <th>Apellidos</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {paginatedProfiles.map((profile) => (
                  <tr key={profile.id}>
                    <td data-label="ID">{profile.id}</td>
                    <td data-label="Código">{profile.code}</td>
                    <td data-label="Correo">{profile.email}</td>
                    <td data-label="Nombre">{profile.firstName}</td>
                    <td data-label="Apellidos" className="profiles-last-name-cell">{profile.lastName}</td>
                    <td data-label="Estado">
                      <span className={`status-pill ${profile.status === 'Habilitado' ? 'ok' : 'cancelled'}`}>
                        {profile.status}
                      </span>
                    </td>
                    <td data-label="Acciones" className="actions-cell">
                      <div className="actions-inline">
                        {profile.blocked ? (
                          <button
                            type="button"
                            className="unlock-profile-btn inline-flex min-h-8 items-center justify-center rounded-md px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => onUnlockProfile(profile.id)}
                          >
                            Desbloquear
                          </button>
                        ) : (
                          <button
                            type="button"
                            className={`status-pill clickable ${profile.status === 'Habilitado' ? 'cancelled' : 'ok'}`}
                            onClick={() => onToggleProfileStatus(profile.id)}
                          >
                            {profile.status === 'Habilitado' ? 'Deshabilitar' : 'Habilitar'}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mobile-list-only">
            {paginatedProfiles.map((profile) => (
              <article key={`profile-mobile-${profile.id}`} className="mobile-record-card">
                <div className="mobile-record-grid">
                  <p><strong>ID:</strong> {profile.id}</p>
                  <p><strong>Código:</strong> {profile.code}</p>
                  <p><strong>Correo:</strong> {profile.email}</p>
                  <p><strong>Nombre:</strong> {profile.firstName}</p>
                  <p><strong>Apellidos:</strong> {profile.lastName}</p>
                  <p><strong>Estado:</strong> {profile.status}</p>
                </div>
                <div className="actions-inline mt-2">
                  {profile.blocked ? (
                    <button
                      type="button"
                      className="unlock-profile-btn inline-flex min-h-8 items-center justify-center rounded-md px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60"
                      onClick={() => onUnlockProfile(profile.id)}
                    >
                      Desbloquear
                    </button>
                  ) : (
                    <button
                      type="button"
                      className={`status-pill clickable ${profile.status === 'Habilitado' ? 'cancelled' : 'ok'}`}
                      onClick={() => onToggleProfileStatus(profile.id)}
                    >
                      {profile.status === 'Habilitado' ? 'Deshabilitar' : 'Habilitar'}
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>

          <Pagination page={profilesPage} totalPages={totalProfilePages} onPrev={onPrevPage} onNext={onNextPage} />
        </article>
      </section>
    </main>
  )
}
