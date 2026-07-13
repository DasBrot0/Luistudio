import { useState } from 'react'
import type { ReactNode } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { AuthUser } from '../../models/types'

export interface SessionItem { id: number; ip: string | null; userAgent: string | null; deviceLabel: string | null; createdAt: string; lastSeenAt: string; current: boolean }
export interface ActivityItem { id: number; action: string; detail: string | null; createdAt: string }
interface ProfilePageProps { user: AuthUser; sessions: SessionItem[]; sessionsLoading: boolean; activity: ActivityItem[]; activityLoading: boolean; onRevokeSession: (sessionId: number) => void; onRevokeAllSessions: () => void; onRequestDisable2fa: () => void; onRequestEnable2fa: () => void }
type ProfileTab = 'profile' | 'activity' | 'sessions' | 'security'

const ACTION_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'Inicio de sesión', LOGIN_UNUSUAL_ACCESS: 'Acceso inusual detectado', LOGOUT_CURRENT: 'Cierre de sesión',
  LOGOUT_REMOTE: 'Sesión remota cerrada', LOGOUT_ALL: 'Todas las sesiones cerradas', SENSITIVE_CHANGE_CONFIRMED: 'Cambio de seguridad confirmado',
}
const ACTION_CLASSES: Record<string, string> = {
  LOGIN_SUCCESS: 'success', LOGIN_UNUSUAL_ACCESS: 'danger', LOGOUT_CURRENT: 'neutral',
  LOGOUT_REMOTE: 'warning', LOGOUT_ALL: 'warning', SENSITIVE_CHANGE_CONFIRMED: 'info',
}
function formatDate(iso: string) {
  try { return new Date(iso).toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) } catch { return iso }
}

function activityMetadata(detail: string | null) {
  if (!detail) return []
  const labels: Record<string, string> = { ip: 'IP', device: 'Dispositivo', ua: 'Navegador' }
  return detail.split(';').map((part) => {
    const separator = part.indexOf('=')
    if (separator < 0) return { label: 'Detalle', value: part }
    const key = part.slice(0, separator)
    return { label: labels[key] ?? key, value: part.slice(separator + 1) }
  }).filter((item) => item.value)
}

export function ProfilePage({ user, sessions, sessionsLoading, activity, activityLoading, onRevokeSession, onRevokeAllSessions, onRequestDisable2fa, onRequestEnable2fa }: ProfilePageProps) {
  const [activeTab, setActiveTab] = useState<ProfileTab>('profile')
  const tabs: { key: ProfileTab; label: string }[] = [
    { key: 'profile', label: 'Mi perfil' }, { key: 'activity', label: 'Actividad' },
    { key: 'sessions', label: 'Sesiones activas' }, { key: 'security', label: 'Seguridad' },
  ]
  return (
    <main className="page dashboard-page">
      <AppHeader title="Mi perfil" roleLabel={user.role === 'admin' ? 'Administrador' : 'Estudiante'} subtitle="Información y configuración de tu cuenta" />
      <section className="dashboard-grid single-grid">
        <article className="card account-card">
          <nav className="account-tabs" aria-label="Secciones del perfil">
            {tabs.map((tab) => <button key={tab.key} type="button" className={activeTab === tab.key ? 'active' : ''} onClick={() => setActiveTab(tab.key)} aria-current={activeTab === tab.key ? 'page' : undefined}>{tab.label}</button>)}
          </nav>

          {activeTab === 'profile' && <section className="account-section" aria-labelledby="personal-data-title">
            <div className="account-section-head"><div><p className="booking-block-kicker">Cuenta</p><h2 id="personal-data-title">Datos personales</h2><p>Información asociada a tu cuenta institucional.</p></div></div>
            <div className="account-details">
              <ProfileField label="Código" value={user.code} /><ProfileField label="Nombres" value={user.firstName} /><ProfileField label="Apellidos" value={user.lastName} /><ProfileField label="Correo" value={user.email} />
              <ProfileField label="Rol" value={<span className="account-badge info">{user.role === 'admin' ? 'Administrador' : 'Estudiante'}</span>} />
              <ProfileField label="Estado" value={<span className={`account-badge ${user.status === 'HABILITADO' ? 'success' : 'warning'}`}>{user.status === 'HABILITADO' ? 'Habilitado' : 'Deshabilitado'}</span>} />
            </div>
            <p className="account-note">Los datos personales son de solo lectura. Para modificarlos, contacta al administrador.</p>
          </section>}

          {activeTab === 'activity' && <section className="account-section" aria-labelledby="activity-title">
            <div className="account-section-head"><div><p className="booking-block-kicker">Cuenta</p><h2 id="activity-title">Historial de actividad</h2><p>Acciones recientes relacionadas con tu cuenta.</p></div></div>
            {activityLoading ? <EmptyCopy text="Cargando actividad…" /> : activity.length === 0 ? <EmptyCopy text="Sin actividad registrada." /> : <div className="account-list">{activity.map((item) => <article className="account-list-item" key={item.id}><i className={`account-event-dot ${ACTION_CLASSES[item.action] ?? 'neutral'}`} /><div><strong>{ACTION_LABELS[item.action] ?? item.action}</strong><div className="account-event-meta">{activityMetadata(item.detail).map((meta) => <span key={`${meta.label}-${meta.value}`}><b>{meta.label}:</b> {meta.value}</span>)}</div><time>{formatDate(item.createdAt)}</time></div></article>)}</div>}
          </section>}

          {activeTab === 'sessions' && <section className="account-section" aria-labelledby="sessions-title">
            <div className="account-section-head"><div><p className="booking-block-kicker">Acceso</p><h2 id="sessions-title">Sesiones activas</h2><p>Dispositivos que tienen acceso a tu cuenta.</p></div>{sessions.length > 1 && <button type="button" className="danger-btn account-action-btn" onClick={onRevokeAllSessions}>Cerrar todas</button>}</div>
            {sessionsLoading ? <EmptyCopy text="Cargando sesiones…" /> : sessions.length === 0 ? <EmptyCopy text="Sin sesiones activas." /> : <div className="account-list">{sessions.map((session) => <article className={`account-session ${session.current ? 'current' : ''}`} key={session.id}><div><div className="account-session-title"><strong>{session.deviceLabel ?? 'Dispositivo'}</strong>{session.current && <span className="account-badge info">Sesión actual</span>}</div><p>IP: {session.ip ?? 'Desconocida'}</p><time>Inicio: {formatDate(session.createdAt)} · Última actividad: {formatDate(session.lastSeenAt)}</time></div><button type="button" className="danger-btn account-action-btn compact" onClick={() => onRevokeSession(session.id)}>{session.current ? 'Cerrar sesión' : 'Revocar'}</button></article>)}</div>}
          </section>}

          {activeTab === 'security' && <section className="account-section" aria-labelledby="security-title">
            <div className="account-section-head"><div><p className="booking-block-kicker">Protección</p><h2 id="security-title">Configuración de seguridad</h2><p>Administra la verificación en dos pasos de tu cuenta.</p></div></div>
            <div className="account-security-list">
              {user.has2fa ? <SecurityAction label="Desactivar 2FA" description="Recibirás un correo de confirmación antes de desactivarlo." onClick={onRequestDisable2fa} danger /> : <SecurityAction label="Activar 2FA" description="Añade una segunda verificación al iniciar sesión." onClick={onRequestEnable2fa} />}
            </div>
          </section>}
        </article>
      </section>
    </main>
  )
}

function ProfileField({ label, value }: { label: string; value: ReactNode }) { return <div className="account-field"><dt>{label}</dt><dd>{value}</dd></div> }
function EmptyCopy({ text }: { text: string }) { return <div className="empty-state account-empty"><p>{text}</p></div> }
function SecurityAction({ label, description, onClick, danger }: { label: string; description: string; onClick: () => void; danger?: boolean }) {
  return <article className="account-security-item"><div><strong>{label}</strong><p>{description}</p></div><button type="button" className={danger ? 'danger-btn account-action-btn' : 'primary-btn account-action-btn'} onClick={onClick}>Solicitar</button></article>
}
