import { useState } from 'react'
import type { FormEvent } from 'react'
import { AppHeader } from '../components/layout/AppHeader'

export interface AnnouncementItem {
  id: number
  title: string
  announcementType: string
  createdAt: string
  recipientCount: number
}

const ANNOUNCEMENT_TYPES = [
  { value: 'NUEVA_SALA', label: 'Nueva sala' },
  { value: 'CAMBIO_POLITICA', label: 'Cambio de política' },
  { value: 'MANTENIMIENTO', label: 'Mantenimiento' },
  { value: 'GENERAL', label: 'General' },
]

interface ComunicadosPageProps {
  published: AnnouncementItem[]
  sending: boolean
  onPublish: (title: string, content: string, announcementType: string) => Promise<void>
}

function typeLabel(type: string): string {
  return ANNOUNCEMENT_TYPES.find((t) => t.value === type)?.label ?? type
}

function formatDateTime(iso: string) {
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

function MegaphoneIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
      <path d="M3 11V13a2 2 0 0 0 2 2h1l2 4h2l-2-4h1a2 2 0 0 0 2-2v-2" />
      <path d="M11 11V7.5L21 5v14l-10-2.5V13" />
      <path d="M11 11H3" />
    </svg>
  )
}

export function ComunicadosPage({ published, sending, onPublish }: ComunicadosPageProps) {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [announcementType, setAnnouncementType] = useState('GENERAL')
  const [formError, setFormError] = useState('')

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setFormError('')
    if (!title.trim()) { setFormError('El título es obligatorio.'); return }
    if (!content.trim()) { setFormError('El contenido es obligatorio.'); return }
    try {
      await onPublish(title.trim(), content.trim(), announcementType)
      setTitle('')
      setContent('')
      setAnnouncementType('GENERAL')
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'No se pudo publicar el comunicado.')
    }
  }

  return (
    <main className="page dashboard-page">
      <AppHeader title="Comunicados" roleLabel="Administrador" />

      <section className="dashboard-grid" style={{ gridTemplateColumns: '1fr', gap: '1.5rem' }}>

        {/* Compose card */}
        <article className="card">
          <div className="card-head">
            <h2>Nuevo comunicado institucional</h2>
          </div>

          <form onSubmit={handleSubmit} noValidate style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <label htmlFor="ann-type" className="field-label">Tipo</label>
              <select
                id="ann-type"
                className="select-field"
                value={announcementType}
                onChange={(e) => setAnnouncementType(e.target.value)}
                disabled={sending}
              >
                {ANNOUNCEMENT_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <label htmlFor="ann-title" className="field-label">Título</label>
              <input
                id="ann-title"
                type="text"
                className="input-field"
                placeholder="Ej: Nueva sala disponible en Campus Norte"
                maxLength={160}
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                disabled={sending}
              />
              <span style={{ fontSize: 11, color: 'var(--color-text-secondary, #6b7280)', alignSelf: 'flex-end' }}>
                {title.length}/160
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <label htmlFor="ann-content" className="field-label">Contenido</label>
              <textarea
                id="ann-content"
                className="input-field"
                placeholder="Describe la comunicación institucional para los estudiantes..."
                rows={5}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                disabled={sending}
                style={{ resize: 'vertical', minHeight: 100 }}
              />
            </div>

            {formError && (
              <p role="alert" style={{ margin: 0, fontSize: 13, color: 'var(--color-error, #ef4444)' }}>
                {formError}
              </p>
            )}

            <div>
              <button
                type="submit"
                className="btn-primary"
                disabled={sending}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}
              >
                <MegaphoneIcon />
                {sending ? 'Enviando…' : 'Publicar y notificar a estudiantes'}
              </button>
            </div>
          </form>
        </article>

        {/* History card */}
        <article className="card">
          <div className="card-head">
            <h2>Historial de comunicados</h2>
          </div>

          {published.length === 0 && (
            <div className="empty-state">
              <p>No hay comunicados publicados aún.</p>
            </div>
          )}

          {published.length > 0 && (
            <div className="table-wrap desktop-table-only">
              <table>
                <thead>
                  <tr>
                    <th>Título</th>
                    <th>Tipo</th>
                    <th>Fecha</th>
                    <th>Destinatarios</th>
                  </tr>
                </thead>
                <tbody>
                  {published.map((item) => (
                    <tr key={item.id}>
                      <td data-label="Título">{item.title}</td>
                      <td data-label="Tipo"><span className="status-pill ok">{typeLabel(item.announcementType)}</span></td>
                      <td data-label="Fecha">{formatDateTime(item.createdAt)}</td>
                      <td data-label="Destinatarios">{item.recipientCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {published.length > 0 && (
            <div className="mobile-list-only">
              {published.map((item) => (
                <article key={`com-mob-${item.id}`} className="mobile-record-card">
                  <div className="mobile-record-grid">
                    <p><strong>Título:</strong> {item.title}</p>
                    <p><strong>Tipo:</strong> <span className="status-pill ok">{typeLabel(item.announcementType)}</span></p>
                    <p><strong>Fecha:</strong> {formatDateTime(item.createdAt)}</p>
                    <p><strong>Destinatarios:</strong> {item.recipientCount}</p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </article>
      </section>
    </main>
  )
}
