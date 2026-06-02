interface ConfirmCancelBookingModalProps {
  bookingLabel: string
  roomLabel: string
  location: string
  dateLabel: string
  timeLabel: string
  actor: 'admin' | 'student'
  onConfirm: () => void
  onClose: () => void
}

export function ConfirmCancelBookingModal({
  bookingLabel,
  roomLabel,
  location,
  dateLabel,
  timeLabel,
  actor,
  onConfirm,
  onClose,
}: ConfirmCancelBookingModalProps) {
  const notice =
    actor === 'admin'
      ? 'Se enviara una notificacion automatica al estudiante.'
      : 'La cancelacion se registrara de inmediato.'

  return (
    <section className="modal-layer" role="dialog" aria-modal="true" aria-labelledby="cancel-booking-title">
      <div className="modal-card slim-modal text-left">
        <h2 id="cancel-booking-title">Confirmar cancelacion</h2>
        <div className="confirm-booking-copy">
          <p>Vas a cancelar {bookingLabel}.</p>
          <dl className="confirm-booking-details">
            <div>
              <dt>Sala</dt>
              <dd>{roomLabel}</dd>
            </div>
            <div>
              <dt>Ubicacion</dt>
              <dd>{location}</dd>
            </div>
            <div>
              <dt>Fecha</dt>
              <dd>{dateLabel}</dd>
            </div>
            <div>
              <dt>Horario</dt>
              <dd>{timeLabel}</dd>
            </div>
          </dl>
          <p className="confirm-booking-notice">{notice}</p>
        </div>
        <div className="modal-actions">
          <button
            type="button"
            className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            onClick={onClose}
          >
            Volver
          </button>
          <button
            type="button"
            className="danger-btn inline-flex min-h-10 items-center justify-center rounded-full px-4 text-sm font-semibold transition hover:-translate-y-px disabled:cursor-not-allowed disabled:opacity-60"
            onClick={onConfirm}
          >
            Confirmar cancelacion
          </button>
        </div>
      </div>
    </section>
  )
}
