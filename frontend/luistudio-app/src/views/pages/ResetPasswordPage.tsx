import type { FormEvent } from 'react'

interface ResetPasswordPageProps {
  resetToken: string
  resetPassword: string
  resetPasswordConfirm: string
  resetError: string
  showResetSuccess: boolean
  onResetTokenChange: (value: string) => void
  onResetPasswordChange: (value: string) => void
  onResetPasswordConfirmChange: (value: string) => void
  onSubmitResetPassword: (event: FormEvent<HTMLFormElement>) => void
  onBackToLogin: () => void
}

export function ResetPasswordPage({
  resetToken,
  resetPassword,
  resetPasswordConfirm,
  resetError,
  showResetSuccess,
  onResetTokenChange,
  onResetPasswordChange,
  onResetPasswordConfirmChange,
  onSubmitResetPassword,
  onBackToLogin,
}: ResetPasswordPageProps) {
  return (
    <main className="page auth-page">
      <section className="auth-card reset-card">
        <h1>Nueva contraseña</h1>

        <form onSubmit={onSubmitResetPassword} className="stack">
          <label htmlFor="reset-token">Token de recuperación</label>
          <input
            id="reset-token"
            type="text"
            value={resetToken}
            onChange={(event) => onResetTokenChange(event.target.value)}
            placeholder="Ingresa el token"
          />

          <label htmlFor="new-password">Nueva contraseña</label>
          <input
            id="new-password"
            type="password"
            value={resetPassword}
            onChange={(event) => onResetPasswordChange(event.target.value)}
            placeholder="Mínimo 8 caracteres"
          />

          <label htmlFor="confirm-password">Confirmar contraseña</label>
          <input
            id="confirm-password"
            type="password"
            value={resetPasswordConfirm}
            onChange={(event) => onResetPasswordConfirmChange(event.target.value)}
            placeholder="Repite la contraseña"
          />

          <button type="submit" className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60">
            Guardar nueva contraseña
          </button>
        </form>

        {resetError && <p className="error-text">{resetError}</p>}
        {showResetSuccess && (
          <p className="success-text">Contraseña actualizada. Ya puedes iniciar sesión.</p>
        )}

        <button type="button" className="link-btn centered" onClick={onBackToLogin}>
          Volver al login
        </button>
      </section>
    </main>
  )
}
