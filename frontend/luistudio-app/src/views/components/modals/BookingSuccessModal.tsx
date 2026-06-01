interface BookingSuccessModalProps {
  onClose: () => void
}

export function BookingSuccessModal({ onClose }: BookingSuccessModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card">
        <h2>Reserva registrada con éxito</h2>
        <p className="modal-copy">Tu reserva fue registrada correctamente.</p>
        <button
          type="button"
          className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
          onClick={onClose}
        >
          Aceptar
        </button>
      </div>
    </section>
  )
}
