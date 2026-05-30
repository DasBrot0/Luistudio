import type { FormEvent } from 'react'

interface ForgotPasswordModalProps {
  forgotEmail: string
  onForgotEmailChange: (value: string) => void
  onCancel: () => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
}

export function ForgotPasswordModal({
  forgotEmail,
  onForgotEmailChange,
  onCancel,
  onSubmit,
}: ForgotPasswordModalProps) {
  return (
    <section className="modal-layer" role="dialog" aria-modal="true">
      <div className="modal-card slim-modal">
        <h2>Restablecer contraseña</h2>
        <form onSubmit={onSubmit} className="stack">
          <label htmlFor="forgot-email">Correo institucional</label>
          <input
            id="forgot-email"
            type="email"
            value={forgotEmail}
            onChange={(event) => onForgotEmailChange(event.target.value)}
            placeholder="correo@universidad.edu.pe"
          />
          <div className="modal-actions">
            <button type="button" className="inline-flex min-h-10 items-center justify-center rounded-full border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={onCancel}>
              Cancel
            </button>
            <button type="submit" className="inline-flex min-h-10 items-center justify-center rounded-full border border-amber-300 bg-amber-200 px-4 text-sm font-semibold text-amber-900 transition hover:-translate-y-px hover:bg-amber-300">
              Enviar enlace
            </button>
          </div>
        </form>
      </div>
    </section>
  )
}
