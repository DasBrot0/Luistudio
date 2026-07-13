import { useState } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { ReservationForm } from '../../models/types'
import { api, type ApiIntelligentRoomSearchResponse } from '../../services/api'

interface SmartSearchPageProps {
  reservationForm: ReservationForm
  result: ApiIntelligentRoomSearchResponse | null
  loading: boolean
  error: string
  onReservationChange: (next: ReservationForm) => void
  onSearch: (query: string, date: string, start: string, end: string) => void
  onChooseRecommendation: (roomId: number) => void
}

function CheckIcon() {
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4.5 12.5 4.6 4.6L19.5 6.8" /></svg>
}

function SmartSearchContent({ reservationForm, result, loading, error, onReservationChange, onSearch, onChooseRecommendation }: SmartSearchPageProps) {
  const [query, setQuery] = useState('')

  return (
    <main className="page dashboard-page">
      <AppHeader title="Búsqueda inteligente" roleLabel="Estudiante" />

      <section className="card smart-search-page-card">
        <div className="card-head">
          <h2>Encuentra una sala</h2>
        </div>
        <p className="smart-search-description">Describe tu necesidad y ordenaremos las salas disponibles por compatibilidad.</p>

        <form className="smart-search-page-form" onSubmit={(event) => { event.preventDefault(); onSearch(query, reservationForm.date, reservationForm.start, reservationForm.end) }}>
          <div className="smart-search-input-panel">
            <div className="smart-search-fields">
              <label className="smart-search-query">Necesidad<textarea value={query} maxLength={500} rows={3} placeholder="Ej.: Necesito estudiar en silencio con otras 3 personas, con pizarra y proyector" onChange={(event) => setQuery(event.target.value)} /></label>
              <label>Fecha<input type="date" value={reservationForm.date} onChange={(event) => onReservationChange({ ...reservationForm, date: event.target.value })} /></label>
              <label>Inicio<input type="time" value={reservationForm.start} onChange={(event) => onReservationChange({ ...reservationForm, start: event.target.value })} /></label>
              <label>Fin<input type="time" value={reservationForm.end} onChange={(event) => onReservationChange({ ...reservationForm, end: event.target.value })} /></label>
            </div>
            <button className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={loading || !query.trim() || !reservationForm.date || !reservationForm.start || !reservationForm.end}>
              <span className="btn-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" /><path d="m16 16 4 4" /></svg></span>
              {loading ? 'Interpretando…' : 'Encontrar salas'}
            </button>
          </div>
        </form>

        {error && <p className="smart-search-error" role="alert">{error}</p>}
        {result && (
          <div className="smart-search-results">
            <div className="interpreted-intent"><span>Interpretamos:</span><b>{result.intent.minimumCapacity} personas</b><b>Ruido {result.intent.maximumNoise.toLowerCase()}</b>{result.intent.requiresConcentration && <b>Concentración</b>}{result.intent.requiredEquipment.map((item) => <b key={item}>{item}</b>)}</div>
            {result.recommendations.length === 0 ? <p className="smart-search-empty">No encontramos salas compatibles disponibles en ese horario. Prueba otra hora o describe requisitos más flexibles.</p> : (
              <div className="recommendation-grid">{result.recommendations.map((recommendation, index) => (
                <article className="recommendation-card" key={recommendation.room.id}>
                  <div className="recommendation-rank">#{index + 1}</div>
                  <div><span className="recommendation-score">{recommendation.score} puntos</span><h3>{recommendation.room.resourceLabel}</h3><p>{recommendation.room.campusLabel} · {recommendation.room.venueLabel} · Cap. {recommendation.room.capacity}</p></div>
                  <ul>{recommendation.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>
                  <button type="button" className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" onClick={() => onChooseRecommendation(recommendation.room.id)}><span className="btn-icon" aria-hidden="true"><CheckIcon /></span>Elegir esta sala</button>
                </article>
              ))}</div>
            )}
          </div>
        )}
      </section>
    </main>
  )
}

export function SmartSearchPage({ token, onChooseRecommendation }: { token: string; onChooseRecommendation: (roomId: number) => void }) {
  const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1)
  const [form, setForm] = useState<ReservationForm>({ campus: '', location: '', roomId: '', people: 1, date: tomorrow.toISOString().slice(0, 10), start: '09:00', end: '10:00' })
  const [result, setResult] = useState<ApiIntelligentRoomSearchResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const search = async (query: string, date: string, start: string, end: string) => { setLoading(true); setError(''); try { setResult(await api.intelligentRoomSearch(token, { query, date, start, end, limit: 3 })) } catch (cause) { setError(cause instanceof Error ? cause.message : 'No se pudo realizar la búsqueda') } finally { setLoading(false) } }
  return <SmartSearchContent reservationForm={form} result={result} loading={loading} error={error} onReservationChange={setForm} onSearch={(...args) => void search(...args)} onChooseRecommendation={onChooseRecommendation} />
}
