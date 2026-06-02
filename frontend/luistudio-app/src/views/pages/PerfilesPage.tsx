import { AppHeader } from '../components/layout/AppHeader'
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
  onToggleProfileStatus: (profileId: string) => void
  onUnlockProfile: (profileId: string) => void
  onPrevPage: () => void
  onNextPage: () => void
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
  onToggleProfileStatus,
  onUnlockProfile,
  onPrevPage,
  onNextPage,
}: PerfilesPageProps) {
  return (
    <main className="page dashboard-page">
      <AppHeader title="Perfiles" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Perfiles</h2>
          </div>

          <FilterBar
            searchPlaceholder="Buscar por código, correo o nombre"
            searchValue={searchQuery}
            onSearchChange={onSearchQueryChange}
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
                value: sortBy,
                onChange: onSortChange,
                options: [
                  { value: 'firstName', label: 'Ordenar por: Nombre' },
                  { value: 'lastName', label: 'Ordenar por: Apellidos' },
                  { value: 'code', label: 'Ordenar por: Código' },
                  { value: 'status', label: 'Ordenar por: Estado' },
                ],
              },
            ]}
            quickChips={[
              {
                id: 'profiles-sort-asc',
                label: 'Asc',
                active: sortDir === 'asc',
                onClick: () => {
                  if (sortDir !== 'asc') onSortDirectionToggle()
                },
              },
              {
                id: 'profiles-sort-desc',
                label: 'Desc',
                active: sortDir === 'desc',
                onClick: () => {
                  if (sortDir !== 'desc') onSortDirectionToggle()
                },
              },
            ]}
          />

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
                            className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
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
                      className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60"
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

          <div className="pagination pagination-center">
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onPrevPage} disabled={profilesPage === 1}>Anterior</button>
            <p>Página {profilesPage} de {totalProfilePages}</p>
            <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onNextPage} disabled={profilesPage === totalProfilePages}>Siguiente</button>
          </div>
        </article>
      </section>
    </main>
  )
}
