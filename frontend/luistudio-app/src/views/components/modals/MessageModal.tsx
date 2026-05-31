interface MessageModalProps {
  title: string
  message: string
  variant?: 'error' | 'success'
  onClose: () => void
}

export function MessageModal({ title, message, variant = 'error', onClose }: MessageModalProps) {
  const isSuccess = variant === 'success'
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card slim-modal text-left">
        <h2>{title}</h2>
        <p className={isSuccess ? 'success-text whitespace-pre-line' : 'error-text whitespace-pre-line'}>{message}</p>
        <div className="modal-actions">
          <button
            type="button"
            className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
            onClick={onClose}
          >
            Cerrar
          </button>
        </div>
      </div>
    </section>
  )
}
