interface RoomSuccessModalProps {
  roomId: string
  onClose: () => void
}

export function RoomSuccessModal({ roomId, onClose }: RoomSuccessModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card slim-modal">
        <h2>Sala registrada</h2>
        <p className="modal-copy">Se registró la sala con ID: {roomId}</p>
        <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full border border-amber-300 bg-amber-200 px-4 text-sm font-semibold text-amber-900 transition hover:-translate-y-px hover:bg-amber-300" onClick={onClose}>
          Aceptar
        </button>
      </div>
    </section>
  )
}
