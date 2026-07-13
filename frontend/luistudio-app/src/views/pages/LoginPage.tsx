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
  const [copiedCredential, setCopiedCredential] = useState<string | null>(null)

  const copyCredential = async (key: string, value: string) => {
    await navigator.clipboard.writeText(value)
    setCopiedCredential(key)
    window.setTimeout(() => setCopiedCredential((current) => current === key ? null : current), 1800)
  }

  return (
    <main className="page auth-page">
      <div className="auth-shell login-shell">
        <aside className="login-context-panel" aria-label="Acerca de Luistudio">
          <div className="login-context-brand">
            <span className="login-context-logo"><img src={loginLogo} alt="" /></span>
            <div><strong>Luistudio</strong></div>
          </div>

          <div className="login-context-copy">
            <p className="login-context-eyebrow">Espacios que se adaptan a ti</p>
            <h2>Tu lugar para estudiar, reunirte y crear.</h2>
            <p>Encuentra espacios disponibles y organiza tus reservas desde una experiencia simple y centralizada.</p>
          </div>

          <ul className="login-benefit-list">
            <li><span><CalendarIcon /></span><div><strong>Reserva con claridad</strong><small>Consulta horarios y disponibilidad en tiempo real.</small></div></li>
            <li><span><SearchIcon /></span><div><strong>Encuentra el espacio ideal</strong><small>Busca por ubicación o describe lo que necesitas.</small></div></li>
            <li><span><ShieldIcon /></span><div><strong>Acceso institucional</strong><small>Tu cuenta y actividad permanecen protegidas.</small></div></li>
          </ul>

          <p className="login-context-foot">Sistema de gestión de espacios académicos</p>
        </aside>

        <section className="auth-card login-card" aria-label="Inicio de sesión">
          <div className="login-mobile-brand">
            <img src={loginLogo} alt="Logo de Luistudio" className="login-logo" />
            <span>Luistudio</span>
          </div>
          <div className="login-form-heading">
            <p>Bienvenido de vuelta</p>
            <h1>Inicia sesión</h1>
            <span>Usa las credenciales de tu cuenta institucional.</span>
          </div>

          <form onSubmit={onSubmitLogin} className="stack login-form">
            <label htmlFor="email">Correo institucional</label>
            <div className="login-input-field">
              <span aria-hidden="true"><MailIcon /></span>
              <input id="email" type="email" autoComplete="email" value={loginEmail} onChange={(event) => onLoginEmailChange(event.target.value)} placeholder="código@universidad.edu.pe" required />
            </div>

            <label htmlFor="password">Contraseña</label>
            <div className="password-field login-input-field">
              <span aria-hidden="true"><LockIcon /></span>
              <input id="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" value={loginPassword} onChange={(event) => onLoginPasswordChange(event.target.value)} placeholder="••••••••••••" required />
              <button type="button" className="password-visibility-btn" onClick={() => setShowPassword((current) => !current)} aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'} aria-pressed={showPassword}>
                {showPassword ? <EyeOffIcon /> : <EyeIcon />}
              </button>
            </div>

            <div className="between-row login-options-row">
              <label className="remember-check"><input type="checkbox" checked={rememberMe} onChange={(event) => onRememberMeChange(event.target.checked)} />Recordarme</label>
              <button type="button" className="link-btn login-forgot-link" onClick={onOpenForgotModal}>Olvidé mi contraseña</button>
            </div>

            {loginError && <p className="login-error" role="alert"><span>!</span>{loginError}</p>}

            <button type="submit" className="login-submit-btn"><span>Iniciar sesión</span><ArrowIcon /></button>
          </form>

          <details className="auth-tip">
            <summary>Credenciales para entorno de demostración</summary>
            <div className="demo-credentials">
              <DemoAccount
                label="Estudiante"
                email="20224692@aloe.ulima.edu.pe"
                password="Student123!"
                copiedCredential={copiedCredential}
                onCopy={copyCredential}
              />
              <DemoAccount
                label="Administrador"
                email="20233916@aloe.ulima.edu.pe"
                password="Admin123!"
                copiedCredential={copiedCredential}
                onCopy={copyCredential}
              />
            </div>
          </details>

          <p className="login-form-foot"><ShieldIcon /> Conexión segura · Acceso exclusivo para usuarios autorizados</p>
        </section>
      </div>
    </main>
  )
}

function DemoAccount({ label, email, password, copiedCredential, onCopy }: { label: string; email: string; password: string; copiedCredential: string | null; onCopy: (key: string, value: string) => Promise<void> }) {
  const emailKey = `${label}-email`
  const passwordKey = `${label}-password`
  return (
    <section className="demo-account" aria-label={`Credenciales de ${label}`}>
      <h3>{label}</h3>
      <div className="demo-credential-row">
        <div><span>Correo</span><strong>{email}</strong></div>
        <button type="button" onClick={() => void onCopy(emailKey, email)} aria-label={`Copiar correo de ${label}`}>
          <CopyIcon />{copiedCredential === emailKey ? 'Copiado' : 'Copiar'}
        </button>
      </div>
      <div className="demo-credential-row">
        <div><span>Contraseña</span><code>{password}</code></div>
        <button type="button" onClick={() => void onCopy(passwordKey, password)} aria-label={`Copiar contraseña de ${label}`}>
          <CopyIcon />{copiedCredential === passwordKey ? 'Copiado' : 'Copiar'}
        </button>
      </div>
    </section>
  )
}

function MailIcon() { return <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m4 7 8 6 8-6"/></svg> }
function LockIcon() { return <svg viewBox="0 0 24 24"><rect x="4" y="10" width="16" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3"/></svg> }
function ArrowIcon() { return <svg viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></svg> }
function CalendarIcon() { return <svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 10h18"/></svg> }
function SearchIcon() { return <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg> }
function ShieldIcon() { return <svg viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/></svg> }
function EyeIcon() { return <svg viewBox="0 0 24 24"><path d="M2.9 12c1-1.6 4.5-6 9.1-6s8.1 4.4 9.1 6c-1 1.6-4.5 6-9.1 6s-8.1-4.4-9.1-6z"/><circle cx="12" cy="12" r="2.6"/></svg> }
function EyeOffIcon() { return <svg viewBox="0 0 24 24"><path d="M3 3l18 18M10.7 10.7a2 2 0 0 0 2.6 2.6M9.9 5.1A10.3 10.3 0 0 1 12 4c5.3 0 8.8 4.4 9.8 6-.5.9-1.5 2.4-3 3.8M6.6 6.6C4.7 8 3.5 9.8 2.9 10.8c1 1.6 4.5 6 9.1 6 1.7 0 3.2-.5 4.5-1.2"/></svg> }
function CopyIcon() { return <svg viewBox="0 0 24 24"><rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/></svg> }
