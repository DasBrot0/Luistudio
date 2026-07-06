import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../services/api'

type Status = 'loading' | 'success' | 'error'

const ACTION_LABELS: Record<string, string> = {
  CHANGE_PASSWORD: 'cambio de contraseña',
  DISABLE_2FA: 'desactivación de 2FA',
  REVOKE_ALL_SESSIONS: 'cierre de todas las sesiones',
}

export function ConfirmSensitiveChangePage() {
  const [searchParams] = useSearchParams()
  const [status, setStatus] = useState<Status>('loading')
  const [message, setMessage] = useState('')
  const [actionLabel, setActionLabel] = useState('')

  useEffect(() => {
    const token = searchParams.get('token')
    const action = searchParams.get('action') ?? ''
    setActionLabel(ACTION_LABELS[action] ?? 'cambio de seguridad')

    if (!token) {
      setStatus('error')
      setMessage('El enlace de confirmación no es válido o está incompleto.')
      return
    }

    api
      .confirmSensitiveChange('', token)
      .then(() => {
        setStatus('success')
        setMessage('Tu ' + (ACTION_LABELS[action] ?? 'cambio de seguridad') + ' fue confirmado exitosamente.')
      })
      .catch((err: unknown) => {
        setStatus('error')
        setMessage(err instanceof Error ? err.message : 'El enlace expiró o ya fue usado. Solicita uno nuevo desde tu perfil.')
      })
  }, [searchParams])

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f1f5f9', padding: 24 }}>
      <div style={{ background: '#fff', borderRadius: 16, border: '1px solid #e5e7eb', padding: '40px 32px', maxWidth: 440, width: '100%', textAlign: 'center' }}>
        <h1 style={{ margin: '0 0 6px', fontSize: 22, fontWeight: 800, color: '#1e3a8a' }}>Luistudio</h1>
        <p style={{ margin: '0 0 28px', fontSize: 13, color: '#64748b' }}>Confirmación de cambio de seguridad</p>

        {status === 'loading' && (
          <>
            <div style={{ fontSize: 36, marginBottom: 16 }}>⏳</div>
            <p style={{ color: '#334155', fontSize: 15 }}>Confirmando tu {actionLabel}...</p>
          </>
        )}

        {status === 'success' && (
          <>
            <div style={{ fontSize: 36, marginBottom: 16 }}>✅</div>
            <h2 style={{ margin: '0 0 10px', fontSize: 18, fontWeight: 700, color: '#15803d' }}>¡Confirmado!</h2>
            <p style={{ color: '#334155', fontSize: 14, marginBottom: 24 }}>{message}</p>
            <a href="/" style={{ display: 'inline-block', background: '#2563eb', color: '#fff', textDecoration: 'none', fontWeight: 700, padding: '10px 24px', borderRadius: 10, fontSize: 14 }}>
              Ir al inicio
            </a>
          </>
        )}

        {status === 'error' && (
          <>
            <div style={{ fontSize: 36, marginBottom: 16 }}>❌</div>
            <h2 style={{ margin: '0 0 10px', fontSize: 18, fontWeight: 700, color: '#dc2626' }}>Enlace inválido</h2>
            <p style={{ color: '#334155', fontSize: 14, marginBottom: 24 }}>{message}</p>
            <a href="/" style={{ display: 'inline-block', background: '#2563eb', color: '#fff', textDecoration: 'none', fontWeight: 700, padding: '10px 24px', borderRadius: 10, fontSize: 14 }}>
              Volver al inicio
            </a>
          </>
        )}
      </div>
    </div>
  )
}
