import { useEffect } from 'react'
import type { CampusMapPavilion } from '../../../models/campusMap'

export function PavilionDetailsDrawer({ pavilion, onClose, onReserve }: { pavilion: CampusMapPavilion | null; onClose: () => void; onReserve: (id: number) => void }) {
  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => event.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])
  if (!pavilion) return null
  return (
    <aside className="pavilion-drawer" aria-label={`Espacios de ${pavilion.code}`}>
      <div className="pavilion-drawer-head">
        <div><h2>{pavilion.code} · {pavilion.name}</h2><p>{pavilion.summary.free} libres · {pavilion.summary.occupied} ocupadas · {pavilion.summary.maintenance} en mantenimiento</p></div>
        <button type="button" className="pavilion-close-btn" onClick={onClose} aria-label="Cerrar">×</button>
      </div>
      {pavilion.locations.map((location) => (
        <section key={location.name} className="pavilion-location">
          <p className="booking-block-kicker">{location.name}</p>
          {location.rooms.map((room) => (
            <article key={room.id} className="pavilion-room">
              <div className="pavilion-room-head"><div><strong>{room.name}</strong><p>{room.code} · {room.venue} · Cap. {room.capacity}</p></div><span className={`status-pill map-room-status status-${room.status.toLowerCase()}`}><i />{room.status}</span></div>
              {!room.withinSchedule && <p className="pavilion-room-warning">Fuera de horario</p>}
              {room.reservableNow && <button type="button" className="primary-btn pavilion-reserve-btn" onClick={() => onReserve(room.id)}>Reservar</button>}
            </article>
          ))}
        </section>
      ))}
    </aside>
  )
}
