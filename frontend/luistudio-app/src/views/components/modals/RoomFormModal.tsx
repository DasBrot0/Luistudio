import type { FormEvent } from 'react'
import type { RoomDraft } from '../../../models/types'

interface RoomFormModalProps {
  mode: 'add' | 'edit'
  draft: RoomDraft
  notice: string
  targetRoomId: string | null
  onChange: (next: RoomDraft) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function RoomFormModal({
  mode,
  draft,
  notice,
  targetRoomId,
  onChange,
  onCancel,
  onSubmit,
}: RoomFormModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card">
        <h2>{mode === 'add' ? 'Agregar Sala' : 'Editar Sala'}</h2>
        {mode === 'edit' && targetRoomId && <p className="meta-id">ID: {targetRoomId}</p>}

        <form className="stack" onSubmit={onSubmit}>
          <label htmlFor="room-name">Nombre</label>
          <input
            id="room-name"
            type="text"
            value={draft.name}
            onChange={(event) => onChange({ ...draft, name: event.target.value })}
          />

          <label htmlFor="room-location">Ubicación</label>
          <input
            id="room-location"
            type="text"
            value={draft.location}
            onChange={(event) => onChange({ ...draft, location: event.target.value })}
          />

          <label htmlFor="room-capacity">Capacidad</label>
          <input
            id="room-capacity"
            type="number"
            min={1}
            value={draft.capacity}
            onChange={(event) => onChange({ ...draft, capacity: Number(event.target.value) })}
          />

          {notice && <p className="error-text">{notice}</p>}

          <div className="modal-actions">
            <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="inline-flex min-h-10 items-center justify-center rounded-full border border-amber-300 bg-amber-200 px-4 text-sm font-semibold text-amber-900 transition hover:-translate-y-px hover:bg-amber-300">
              {mode === 'add' ? 'Agregar' : 'Aceptar'}
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
