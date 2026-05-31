interface DeleteRoomModalProps {
  roomId: string
  onCancel: () => void
  onConfirm: () => void
}

export function DeleteRoomModal({ roomId, onCancel, onConfirm }: DeleteRoomModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card slim-modal">
        <h2>Eliminar sala</h2>
        <p className="modal-copy">
          ¿Seguro que deseas eliminar la sala {roomId}? Esta acción puede bloquearse si tiene
          reservas futuras.
        </p>
        <div className="modal-actions">
          <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onCancel}>
            Cancel
          </button>
          <button type="button" className="danger-btn inline-flex min-h-10 items-center justify-center rounded-full px-4 text-sm font-semibold transition hover:-translate-y-px" onClick={onConfirm}>
            Eliminar
          </button>
        </div>
      </div>
    </section>
  )
}
