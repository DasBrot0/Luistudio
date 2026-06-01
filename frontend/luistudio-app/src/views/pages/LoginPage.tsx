import { useState, type FormEvent } from 'react'
import loginLogo from '../../assets/logos/logo_modo_claro.png'

interface LoginPageProps {
  loginEmail: string
  loginPassword: string
  rememberMe: boolean
  loginError: string
  onLoginEmailChange: (value: string) => void
  onLoginPasswordChange: (value: string) => void
  onRememberMeChange: (value: boolean) => void
  onOpenForgotModal: () => void
  onSubmitLogin: (event: FormEvent<HTMLFormElement>) => void
}

export function LoginPage({
  loginEmail,
  loginPassword,
  rememberMe,
  loginError,
  onLoginEmailChange,
  onLoginPasswordChange,
  onRememberMeChange,
  onOpenForgotModal,
  onSubmitLogin,
}: LoginPageProps) {
  const [showPassword, setShowPassword] = useState(false)

  return (
    <main className="page auth-page">
      <div className="auth-shell">
        <section className="auth-card" aria-label="Inicio de sesión">
          <div className="login-logo-wrap">
            <img src={loginLogo} alt="Logo de Luistudio" className="login-logo" />
          </div>
          <h1>Bienvenido a Luistudio</h1>

          <form onSubmit={onSubmitLogin} className="stack">
            <label htmlFor="email">Correo institucional</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={loginEmail}
              onChange={(event) => onLoginEmailChange(event.target.value)}
              placeholder="correo@universidad.edu.pe"
            />

            <label htmlFor="password">Contraseña</label>
            <div className="password-field">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                value={loginPassword}
                onChange={(event) => onLoginPasswordChange(event.target.value)}
                placeholder="••••••••••••"
              />
              <button
                type="button"
                className="password-visibility-btn"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-pressed={showPassword}
              >
                {showPassword ? (
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M3 3l18 18M10.7 10.7a2 2 0 0 0 2.6 2.6M9.9 5.1A10.3 10.3 0 0 1 12 4c5.3 0 8.8 4.4 9.8 6-0.5 0.9-1.5 2.4-3 3.8M6.6 6.6C4.7 8 3.5 9.8 2.9 10.8c1 1.6 4.5 6 9.1 6 1.7 0 3.2-0.5 4.5-1.2"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M2.9 12c1-1.6 4.5-6 9.1-6s8.1 4.4 9.1 6c-1 1.6-4.5 6-9.1 6s-8.1-4.4-9.1-6z"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                    <circle cx="12" cy="12" r="2.6" fill="none" stroke="currentColor" strokeWidth="1.8" />
                  </svg>
                )}
              </button>
            </div>

            <div className="between-row">
              <label className="remember-check">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(event) => onRememberMeChange(event.target.checked)}
                />
                Recordarme
              </label>

              <button type="button" className="link-btn" onClick={onOpenForgotModal}>
                Olvidé mi contraseña
              </button>
            </div>

            <button
              type="submit"
              className="inline-flex min-h-10 items-center justify-center rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60"
            >
              Iniciar sesión
            </button>
          </form>

          {loginError && <p className="error-text">{loginError}</p>}

          <div className="auth-tip">
            <div>Usuarios seed BD:</div>
            <div>
              estudiante <strong>20224692@aloe.ulima.edu.pe</strong> / <strong>Student123!</strong>
            </div>
            <div>
              admin <strong>20233916@aloe.ulima.edu.pe</strong> / <strong>Admin123!</strong>
            </div>
          </div>
        </section>
      </div>
    </main>
  )
}
