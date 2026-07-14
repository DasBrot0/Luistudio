import { useState } from 'react'
import type { Room } from '../../models/types'
import { FilterBar } from '../components/filters/FilterBar'
import { AppHeader } from '../components/layout/AppHeader'
import { Pagination } from '../components/layout/Pagination'

const PAGE_SIZE = 10

interface SalasPageProps {
  filteredRooms: Room[]
  campusOptions: string[]
  venueOptions: string[]
  locationOptions: string[]
  roomSearchQuery: string
  roomFilterCampus: string
  roomFilterVenue: string
  roomFilterLocation: string
  roomStatusFilter: 'Todos' | 'Disponible' | 'En mantenimiento'
  roomSort: string
  roomNotice: string
  onRoomSearchChange: (value: string) => void
  onRoomFilterCampusChange: (value: string) => void
  onRoomFilterVenueChange: (value: string) => void
  onRoomFilterLocationChange: (value: string) => void
  onRoomStatusFilterChange: (value: 'Todos' | 'Disponible' | 'En mantenimiento') => void
  onRoomSortChange: (value: string) => void
  onResetRoomFilters: () => void
  onOpenAddRoom: () => void
  onOpenEditRoom: (room: Room) => void
  onAskDeleteRoom: (roomId: string) => void
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

export function SalasPage({
  filteredRooms,
  campusOptions,
  venueOptions,
  locationOptions,
  roomSearchQuery,
  roomFilterCampus,
  roomFilterVenue,
  roomFilterLocation,
  roomStatusFilter,
  roomSort,
  roomNotice,
  onRoomSearchChange,
  onRoomFilterCampusChange,
  onRoomFilterVenueChange,
  onRoomFilterLocationChange,
  onRoomStatusFilterChange,
  onRoomSortChange,
  onResetRoomFilters,
  onOpenAddRoom,
  onOpenEditRoom,
  onAskDeleteRoom,
}: SalasPageProps) {
  const [page, setPage] = useState(1)
  const totalPages = Math.max(1, Math.ceil(filteredRooms.length / PAGE_SIZE))
  const safePage = Math.min(page, totalPages)
  const visibleRooms = filteredRooms.slice((safePage - 1) * PAGE_SIZE, safePage * PAGE_SIZE)

  return (
    <main className="page dashboard-page">
      <AppHeader title="Salas" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Listado de salas</h2>
          </div>

          <FilterBar
            searchPlaceholder="Buscar por nombre, código o ubicación"
            searchValue={roomSearchQuery}
            onSearchChange={(value) => { setPage(1); onRoomSearchChange(value) }}
            fieldsClassName="filter-bar-fields-three"
            filters={[
              {
                id: 'room-campus-filter',
                value: roomFilterCampus,
                onChange: (value) => { setPage(1); onRoomFilterCampusChange(value) },
                options: [
                  { value: 'Todos', label: 'Campus: Todos' },
                  ...campusOptions.map((campus) => ({ value: campus, label: `Campus: ${campus}` })),
                ],
              },
              {
                id: 'room-venue-filter',
                value: roomFilterVenue,
                onChange: (value) => { setPage(1); onRoomFilterVenueChange(value) },
                disabled: roomFilterCampus === 'Todos',
                options: [
                  { value: 'Todos', label: 'Recinto: Todos' },
                  ...venueOptions.map((venue) => ({ value: venue, label: `Recinto: ${venue}` })),
                ],
              },
              {
                id: 'room-location-filter',
                value: roomFilterLocation,
                onChange: (value) => { setPage(1); onRoomFilterLocationChange(value) },
                disabled: roomFilterCampus === 'Todos',
                options: [
                  { value: 'Todas', label: 'Ubicación: Todas' },
                  ...locationOptions.map((location) => ({ value: location, label: `Ubicación: ${location}` })),
                ],
              },
            ]}
            sortControls={[
              {
                id: 'room-sort-filter',
                value: roomSort,
                onChange: (value) => { setPage(1); onRoomSortChange(value) },
                options: [
                  { value: 'name:asc', label: 'Nombre A-Z' },
                  { value: 'name:desc', label: 'Nombre Z-A' },
                  { value: 'code:asc', label: 'Código ↑' },
                  { value: 'code:desc', label: 'Código ↓' },
                ],
              },
            ]}
            quickChips={[
              {
                id: 'room-status-all',
                label: 'Todas',
                active: roomStatusFilter === 'Todos',
                onClick: () => { setPage(1); onRoomStatusFilterChange('Todos') },
              },
              {
                id: 'room-status-available',
                label: 'Disponible',
                active: roomStatusFilter === 'Disponible',
                onClick: () => { setPage(1); onRoomStatusFilterChange('Disponible') },
              },
              {
                id: 'room-status-maintenance',
                label: 'Mantenimiento',
                active: roomStatusFilter === 'En mantenimiento',
                onClick: () => { setPage(1); onRoomStatusFilterChange('En mantenimiento') },
              },
            ]}
            actions={
              <button
                type="button"
                className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
                onClick={() => { setPage(1); onResetRoomFilters() }}
              >
                <span className="btn-icon" aria-hidden="true">
                  <ResetFiltersIcon />
                </span>
                Reiniciar filtros
              </button>
            }
          />

          <div className="filter-standalone-actions">
            <button
              type="button"
              className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
              onClick={onOpenAddRoom}
            >
                  <span className="btn-icon" aria-hidden="true">
                    <svg viewBox="0 0 24 24">
                      <path d="M12 5v14M5 12h14" />
                    </svg>
                  </span>
                  Agregar
            </button>
          </div>

          {filteredRooms.length === 0 && (
            <div className="empty-state">
              <p>No hay salas para los filtros seleccionados.</p>
            </div>
          )}

          {filteredRooms.length > 0 && (
          <div className="table-wrap desktop-table-only">
            <table>
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nombre</th>
                  <th>Campus</th>
                  <th>Recinto</th>
                  <th>Ubicación</th>
                  <th>Estado</th>
                  <th>Personas</th>
                  <th>Unidades físicas</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {visibleRooms.map((room) => (
                  <tr key={room.id}>
                    <td data-label="Código">{room.id}</td>
                    <td data-label="Nombre">{room.name}</td>
                    <td data-label="Campus">{room.campusLabel}</td>
                    <td data-label="Recinto">{room.venueLabel}</td>
                    <td data-label="Ubicación">{room.location}</td>
                    <td data-label="Estado">
                      <span className={`status-pill ${room.status === 'En mantenimiento' ? 'cancelled' : 'ok'}`}>{room.status}</span>
                    </td>
                    <td data-label="Personas">
                      {room.minPeople}-{room.maxPeople}
                    </td>
                    <td data-label="Unidades físicas">
                      <details>
                        <summary>{room.inventoryCount} {room.inventoryCount === 1 ? 'unidad' : 'unidades'}</summary>
                        <div className="actions-inline mt-2">
                          {Array.from({ length: room.inventoryCount }, (_, index) => <span className="status-pill ok" key={`${room.id}-unit-${index + 1}`}>Unidad {index + 1}</span>)}
                        </div>
                      </details>
                    </td>
                    <td data-label="Acciones" className="actions-cell">
                      <div className="actions-inline">
                        <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onOpenEditRoom(room)}>Editar</button>
                        <button type="button" className="danger-btn inline-flex min-h-8 items-center justify-center rounded-md px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onAskDeleteRoom(room.id)}>Eliminar</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          )}

          {filteredRooms.length > 0 && (
          <div className="mobile-list-only">
            {visibleRooms.map((room) => (
              <article key={`room-mobile-${room.id}`} className="mobile-record-card">
                <div className="mobile-record-grid">
                  <p><strong>Código:</strong> {room.id}</p>
                  <p><strong>Nombre:</strong> {room.name}</p>
                  <p><strong>Campus:</strong> {room.campusLabel}</p>
                  <p><strong>Recinto:</strong> {room.venueLabel}</p>
                  <p><strong>Ubicación:</strong> {room.location}</p>
                  <p><strong>Estado:</strong> {room.status}</p>
                  <p>
                    <strong>Personas:</strong>{' '}
                    {room.minPeople}-{room.maxPeople}
                  </p>
                  <p><strong>Unidades físicas:</strong> {Array.from({ length: room.inventoryCount }, (_, index) => `Unidad ${index + 1}`).join(', ')}</p>
                </div>
                <div className="actions-inline mobile-card-actions mt-2">
                  <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onOpenEditRoom(room)}>Editar</button>
                  <button type="button" className="danger-btn inline-flex min-h-8 items-center justify-center rounded-md px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onAskDeleteRoom(room.id)}>Eliminar</button>
                </div>
              </article>
            ))}
          </div>
          )}

          {filteredRooms.length > 0 && (
            <Pagination
              page={safePage}
              totalPages={totalPages}
              onPrev={() => setPage(Math.max(1, safePage - 1))}
              onNext={() => setPage(Math.min(totalPages, safePage + 1))}
            />
          )}

          {roomNotice && <p className="error-text">{roomNotice}</p>}
        </article>
      </section>
    </main>
  )
}
