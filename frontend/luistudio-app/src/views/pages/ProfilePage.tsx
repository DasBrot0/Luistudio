import { useState } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { AuthUser } from '../../models/types'

export interface SessionItem {
  id: number
  ip: string | null
  userAgent: string | null
  deviceLabel: string | null
  createdAt: string
  lastSeenAt: string
  current: boolean
}

export interface ActivityItem {
  id: number
  action: string
  detail: string | null
  createdAt: string
}

interface ProfilePageProps {
  user: AuthUser
  sessions: SessionItem[]
  sessionsLoading: boolean
  activity: ActivityItem[]
  activityLoading: boolean
  onRevokeSession: (sessionId: number) => void
  onRevokeAllSessions: () => void
  onRequestChangePassword: () => void
  onRequestDisable2fa: () => void
  onRequestEnable2fa: () => void
  onRequestRevokeAll: () => void
}

type ProfileTab = 'profile' | 'activity' | 'sessions' | 'security'

const ACTION_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'Inicio de sesión',
  LOGIN_UNUSUAL_ACCESS: 'Acceso inusual detectado',
  LOGOUT_CURRENT: 'Cierre de sesión',
  LOGOUT_REMOTE: 'Sesión remota cerrada',
  LOGOUT_ALL: 'Todas las sesiones cerradas',
  SENSITIVE_CHANGE_CONFIRMED: 'Cambio de seguridad confirmado',
}

const ACTION_COLORS: Record<string, string> = {
  LOGIN_SUCCESS: '#16a34a',
  LOGIN_UNUSUAL_ACCESS: '#dc2626',
  LOGOUT_CURRENT: '#6b7280',
  LOGOUT_REMOTE: '#d97706',
  LOGOUT_ALL: '#d97706',
  SENSITIVE_CHANGE_CONFIRMED: '#2563eb',
}

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}

function RoleBadge({ role }: { role: 'student' | 'admin' }) {
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 999,
        fontSize: 12,
        fontWeight: 700,
        background: role === 'admin' ? '#dbeafe' : '#dcfce7',
        color: role === 'admin' ? '#1d4ed8' : '#15803d',
        border: `1px solid ${role === 'admin' ? '#93c5fd' : '#86efac'}`,
      }}
    >
      {role === 'admin' ? 'Administrador' : 'Estudiante'}
    </span>
  )
}

export function ProfilePage({
  user,
  sessions,
  sessionsLoading,
  activity,
  activityLoading,
  onRevokeSession,
  onRevokeAllSessions,
  onRequestChangePassword,
  onRequestDisable2fa,
  onRequestEnable2fa,
  onRequestRevokeAll,
}: ProfilePageProps) {
  const [activeTab, setActiveTab] = useState<ProfileTab>('profile')

  const tabs: { key: ProfileTab; label: string }[] = [
    { key: 'profile', label: 'Mi perfil' },
    { key: 'activity', label: 'Actividad' },
    { key: 'sessions', label: 'Sesiones activas' },
    { key: 'security', label: 'Seguridad' },
  ]

  return (
    <main className="page dashboard-page">
      <AppHeader title="Mi perfil" subtitle="Información y configuración de tu cuenta" />

      {/* Tab Navigation */}
      <div
        style={{
          display: 'flex',
          gap: 4,
          borderBottom: '1px solid var(--color-border, #e5e7eb)',
          marginBottom: 24,
          overflowX: 'auto',
        }}
      >
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => setActiveTab(tab.key)}
            style={{
              padding: '8px 16px',
              fontSize: 14,
              fontWeight: activeTab === tab.key ? 700 : 500,
              color: activeTab === tab.key ? 'var(--color-accent, #2563eb)' : 'var(--color-text-secondary, #6b7280)',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab.key ? '2px solid var(--color-accent, #2563eb)' : '2px solid transparent',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
              marginBottom: -1,
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Mi perfil */}
      {activeTab === 'profile' && (
        <div style={{ maxWidth: 520 }}>
          <div
            style={{
              background: 'var(--color-surface, #f7f8fa)',
              border: '1px solid var(--color-border, #e5e7eb)',
              borderRadius: 12,
              padding: 20,
              display: 'flex',
              flexDirection: 'column',
              gap: 16,
            }}
          >
            <ProfileField label="Código" value={user.code} />
            <ProfileField label="Nombres" value={user.firstName} />
            <ProfileField label="Apellidos" value={user.lastName} />
            <ProfileField label="Correo" value={user.email} />
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: 13, color: 'var(--color-text-secondary, #6b7280)', minWidth: 100, fontWeight: 600 }}>Rol</span>
              <RoleBadge role={user.role} />
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: 13, color: 'var(--color-text-secondary, #6b7280)', minWidth: 100, fontWeight: 600 }}>2FA</span>
              <span
                style={{
                  display: 'inline-block',
                  padding: '2px 10px',
                  borderRadius: 999,
                  fontSize: 12,
                  fontWeight: 700,
                  background: user.has2fa ? '#dcfce7' : '#fef9c3',
                  color: user.has2fa ? '#15803d' : '#92400e',
                  border: `1px solid ${user.has2fa ? '#86efac' : '#fde68a'}`,
                }}
              >
                {user.has2fa ? 'Activado' : 'Desactivado'}
              </span>
            </div>
          </div>
          <p style={{ fontSize: 12, color: 'var(--color-text-secondary, #6b7280)', marginTop: 12 }}>
            Los datos personales son de solo lectura. Para modificarlos, contacta al administrador.
          </p>
        </div>
      )}

      {/* Actividad */}
      {activeTab === 'activity' && (
        <div>
          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 12 }}>Historial de actividad</h3>
          {activityLoading ? (
            <p style={{ color: 'var(--color-text-secondary, #6b7280)', fontSize: 14 }}>Cargando...</p>
          ) : activity.length === 0 ? (
            <p style={{ color: 'var(--color-text-secondary, #6b7280)', fontSize: 14 }}>Sin actividad registrada.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {activity.map((item) => (
                <div
                  key={item.id}
                  style={{
                    background: 'var(--color-surface, #f7f8fa)',
                    border: '1px solid var(--color-border, #e5e7eb)',
                    borderRadius: 10,
                    padding: '10px 14px',
                    display: 'flex',
                    gap: 12,
                    alignItems: 'flex-start',
                  }}
                >
                  <span
                    style={{
                      display: 'inline-block',
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: ACTION_COLORS[item.action] ?? '#6b7280',
                      marginTop: 5,
                      flexShrink: 0,
                    }}
                  />
                  <div>
                    <p style={{ margin: 0, fontWeight: 600, fontSize: 13 }}>
                      {ACTION_LABELS[item.action] ?? item.action}
                    </p>
                    {item.detail && (
                      <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--color-text-secondary, #6b7280)' }}>
                        {item.detail}
                      </p>
                    )}
                    <p style={{ margin: '2px 0 0', fontSize: 11, color: 'var(--color-text-secondary, #6b7280)' }}>
                      {formatDate(item.createdAt)}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Sesiones activas */}
      {activeTab === 'sessions' && (
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <h3 style={{ fontSize: 15, fontWeight: 700, margin: 0 }}>Sesiones activas</h3>
            {sessions.length > 1 && (
              <button
                type="button"
                onClick={onRevokeAllSessions}
                style={{
                  padding: '6px 14px',
                  fontSize: 13,
                  fontWeight: 600,
                  background: '#fef2f2',
                  color: '#dc2626',
                  border: '1px solid #fecaca',
                  borderRadius: 8,
                  cursor: 'pointer',
                }}
              >
                Cerrar todas
              </button>
            )}
          </div>
          {sessionsLoading ? (
            <p style={{ color: 'var(--color-text-secondary, #6b7280)', fontSize: 14 }}>Cargando...</p>
          ) : sessions.length === 0 ? (
            <p style={{ color: 'var(--color-text-secondary, #6b7280)', fontSize: 14 }}>Sin sesiones activas.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
              {sessions.map((session) => (
                <div
                  key={session.id}
                  style={{
                    background: 'var(--color-surface, #f7f8fa)',
                    border: `1px solid ${session.current ? 'var(--color-accent, #2563eb)' : 'var(--color-border, #e5e7eb)'}`,
                    borderRadius: 10,
                    padding: '12px 16px',
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    gap: 12,
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 4 }}>
                      <span style={{ fontWeight: 700, fontSize: 13 }}>{session.deviceLabel ?? 'Dispositivo'}</span>
                      {session.current && (
                        <span
                          style={{
                            fontSize: 11,
                            fontWeight: 700,
                            padding: '1px 7px',
                            borderRadius: 999,
                            background: '#dbeafe',
                            color: '#1d4ed8',
                          }}
                        >
                          Sesión actual
                        </span>
                      )}
                    </div>
                    <p style={{ margin: 0, fontSize: 12, color: 'var(--color-text-secondary, #6b7280)' }}>
                      IP: {session.ip ?? 'Desconocida'}
                    </p>
                    <p style={{ margin: '2px 0 0', fontSize: 11, color: 'var(--color-text-secondary, #6b7280)' }}>
                      Última actividad: {formatDate(session.lastSeenAt)}
                    </p>
                  </div>
                  {!session.current && (
                    <button
                      type="button"
                      onClick={() => onRevokeSession(session.id)}
                      style={{
                        padding: '4px 12px',
                        fontSize: 12,
                        fontWeight: 600,
                        background: '#fef2f2',
                        color: '#dc2626',
                        border: '1px solid #fecaca',
                        borderRadius: 7,
                        cursor: 'pointer',
                        flexShrink: 0,
                      }}
                    >
                      Revocar
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Seguridad */}
      {activeTab === 'security' && (
        <div style={{ maxWidth: 480 }}>
          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 16 }}>Configuración de seguridad</h3>
          <p style={{ fontSize: 13, color: 'var(--color-text-secondary, #6b7280)', marginBottom: 20 }}>
            Para realizar cualquier cambio de seguridad, recibirás un correo de confirmación. Debes confirmarlo para que el cambio sea efectivo.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <SecurityAction
              label="Cambiar contraseña"
              description="Se enviará un correo para confirmar el cambio."
              onClick={onRequestChangePassword}
            />
            {user.has2fa ? (
              <SecurityAction
                label="Desactivar 2FA"
                description="Recibirás un correo de confirmación antes de desactivarlo."
                onClick={onRequestDisable2fa}
                danger
              />
            ) : (
              <SecurityAction
                label="Activar 2FA"
                description="Activa la autenticación de dos factores para mayor seguridad."
                onClick={onRequestEnable2fa}
              />
            )}
            <SecurityAction
              label="Cerrar todas las sesiones"
              description="Cerrará todas tus sesiones activas en todos los dispositivos."
              onClick={onRequestRevokeAll}
              danger
            />
          </div>
        </div>
      )}
    </main>
  )
}

function ProfileField({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <span style={{ fontSize: 13, color: 'var(--color-text-secondary, #6b7280)', minWidth: 100, fontWeight: 600 }}>
        {label}
      </span>
      <span style={{ fontSize: 14, color: 'var(--color-text, #1f2328)' }}>{value}</span>
    </div>
  )
}

function SecurityAction({
  label,
  description,
  onClick,
  danger,
}: {
  label: string
  description: string
  onClick: () => void
  danger?: boolean
}) {
  return (
    <div
      style={{
        background: 'var(--color-surface, #f7f8fa)',
        border: '1px solid var(--color-border, #e5e7eb)',
        borderRadius: 10,
        padding: '12px 16px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        gap: 12,
      }}
    >
      <div>
        <p style={{ margin: 0, fontWeight: 700, fontSize: 14 }}>{label}</p>
        <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--color-text-secondary, #6b7280)' }}>
          {description}
        </p>
      </div>
      <button
        type="button"
        onClick={onClick}
        style={{
          padding: '6px 14px',
          fontSize: 13,
          fontWeight: 700,
          background: danger ? '#fef2f2' : 'var(--color-accent, #2563eb)',
          color: danger ? '#dc2626' : '#fff',
          border: danger ? '1px solid #fecaca' : 'none',
          borderRadius: 8,
          cursor: 'pointer',
          flexShrink: 0,
        }}
      >
        Solicitar
      </button>
    </div>
  )
}
