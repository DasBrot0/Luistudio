import { AppHeader } from '../components/layout/AppHeader'
import type { Room } from '../../models/types'

interface SalasPageProps {
  filteredRooms: Room[]
  campusOptions: string[]
  locationOptions: string[]
  roomFilterLocation: string
  roomNotice: string
  onRoomFilterChange: (value: string) => void
  onOpenAddRoom: () => void
  onOpenEditRoom: (room: Room) => void
  onAskDeleteRoom: (roomId: string) => void
}

export function SalasPage({
  filteredRooms,
  campusOptions,
  locationOptions,
  roomFilterLocation,
  roomNotice,
  onRoomFilterChange,
  onOpenAddRoom,
  onOpenEditRoom,
  onAskDeleteRoom,
}: SalasPageProps) {
  return (
    <main className="page dashboard-page">
      <AppHeader title="Salas" roleLabel="Administrador" />

      <section className="dashboard-grid single-grid">
        <article className="card">
          <div className="card-head">
            <h2>Salas</h2>
            <div className="inline-filters">
              <select value={roomFilterLocation} onChange={(event) => onRoomFilterChange(event.target.value)}>
                <option value="Todas">Todas</option>
                {campusOptions.map((campus) => (
                  <option key={campus} value={campus}>
                    {campus}
                  </option>
                ))}
                {locationOptions.map((location) => (
                  <option key={location} value={location}>{location}</option>
                ))}
              </select>

              <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" onClick={onOpenAddRoom}>
                Agregar
              </button>
            </div>
          </div>

          <div className="table-wrap desktop-table-only">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Campus</th>
                  <th>Ubicación</th>
                  <th>Recurso</th>
                  <th>Nombre</th>
                  <th>Personas</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filteredRooms.map((room) => (
                  <tr key={room.id}>
                    <td data-label="ID">{room.id}</td>
                    <td data-label="Campus">{room.campusLabel}</td>
                    <td data-label="Ubicación">{room.venueLabel}</td>
                    <td data-label="Recurso">{room.resourceLabel}</td>
                    <td data-label="Nombre">{room.name}</td>
                    <td data-label="Personas">
                      {room.minPeopleRequired ? `${room.minPeople} (min obligatorio)` : `${room.minPeople} (min opcional)`} / {room.maxPeople} max
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

          <div className="mobile-list-only">
            {filteredRooms.map((room) => (
              <article key={`room-mobile-${room.id}`} className="mobile-record-card">
                <div className="mobile-record-grid">
                  <p><strong>ID:</strong> {room.id}</p>
                  <p><strong>Campus:</strong> {room.campusLabel}</p>
                  <p><strong>Ubicación:</strong> {room.venueLabel}</p>
                  <p><strong>Recurso:</strong> {room.resourceLabel}</p>
                  <p><strong>Nombre:</strong> {room.name}</p>
                  <p>
                    <strong>Personas:</strong>{' '}
                    {room.minPeopleRequired ? `${room.minPeople} (min obligatorio)` : `${room.minPeople} (min opcional)`} / {room.maxPeople} max
                  </p>
                </div>
                <div className="actions-inline mt-2">
                  <button type="button" className="inline-flex min-h-8 items-center justify-center rounded-md bg-slate-200 px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-300 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onOpenEditRoom(room)}>Editar</button>
                  <button type="button" className="danger-btn inline-flex min-h-8 items-center justify-center rounded-md px-3 text-xs font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onAskDeleteRoom(room.id)}>Eliminar</button>
                </div>
              </article>
            ))}
          </div>

          {roomNotice && <p className="error-text">{roomNotice}</p>}
        </article>
      </section>
    </main>
  )
}

