import type { FormEvent } from 'react'

interface TwoFactorModalProps {
  code: string
  errorMessage: string
  title?: string
  description?: string
  submitLabel?: string
  onCodeChange: (value: string) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function TwoFactorModal({
  code,
  errorMessage,
  title,
  description,
  submitLabel,
  onCodeChange,
  onCancel,
  onSubmit,
}: TwoFactorModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true" aria-label="Verificación de dos factores">
      <div className="modal-card slim-modal">
        <h2>{title ?? 'Verificar inicio de sesión'}</h2>
        <p className="modal-copy">{description ?? 'Ingresa el código 2FA enviado a tu correo.'}</p>
        <form onSubmit={onSubmit} className="stack">
          <label htmlFor="two-factor-code">Código 2FA</label>
          <input
            id="two-factor-code"
            type="text"
            inputMode="numeric"
            value={code}
            onChange={(event) => onCodeChange(event.target.value)}
            placeholder="000000"
            autoFocus
          />
          {errorMessage && <p className="error-text">{errorMessage}</p>}
          <div className="modal-actions">
            <button
              type="button"
              className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60"
              onClick={onCancel}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitLabel ?? 'Verificar'}
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
