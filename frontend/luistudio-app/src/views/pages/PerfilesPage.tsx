import { AppHeader } from '../components/layout/AppHeader'
import type { Profile } from '../../models/types'

interface PerfilesPageProps {
  paginatedProfiles: Profile[]
  profilesPage: number
  totalProfilePages: number
  searchQuery: string
  onSearchQueryChange: (value: string) => void
  onToggleProfileStatus: (profileId: string) => void
  onPrevPage: () => void
  onNextPage: () => void
}

export function PerfilesPage({
  paginatedProfiles,
  profilesPage,
  totalProfilePages,
  searchQuery,
  onSearchQueryChange,
  onToggleProfileStatus,
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
            <input
              type="search"
              value={searchQuery}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              placeholder="Buscar por código o correo"
            />
          </div>

          <div className="table-wrap desktop-table-only">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Código</th>
                  <th>Correo</th>
                  <th>Nombre</th>
                  <th>Apellidos</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {paginatedProfiles.map((profile) => (
                  <tr key={profile.id}>
                    <td data-label="ID">{profile.id}</td>
                    <td data-label="Código">{profile.code}</td>
                    <td data-label="Correo">{profile.email}</td>
                    <td data-label="Nombre">{profile.firstName}</td>
                    <td data-label="Apellidos">{profile.lastName}</td>
                    <td data-label="Estado">
                      <button
                        type="button"
                        className={`status-pill clickable ${profile.status === 'Habilitado' ? 'ok' : 'cancelled'}`}
                        onClick={() => onToggleProfileStatus(profile.id)}
                      >
                        {profile.status}
                      </button>
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
                  <p className="mobile-record-state">
                    <strong>Estado:</strong>{' '}
                    <button
                      type="button"
                      className={`status-pill clickable ${profile.status === 'Habilitado' ? 'ok' : 'cancelled'}`}
                      onClick={() => onToggleProfileStatus(profile.id)}
                    >
                      {profile.status}
                    </button>
                  </p>
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

