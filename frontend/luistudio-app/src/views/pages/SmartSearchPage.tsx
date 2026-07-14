import { useState } from 'react'
import { AppHeader } from '../components/layout/AppHeader'
import type { ReservationForm } from '../../models/types'
import type { ApiIntelligentRoomSearchResponse } from '../../services/api'

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

const searchableStartTimes = Array.from({ length: 32 }, (_, index) => {
  const totalMinutes = 6 * 60 + index * 30
  return `${String(Math.floor(totalMinutes / 60)).padStart(2, '0')}:${String(totalMinutes % 60).padStart(2, '0')}`
})

function addMinutes(time: string, minutes: number) {
  if (!time) return ''
  const [hours, currentMinutes] = time.split(':').map(Number)
  const total = hours * 60 + currentMinutes + minutes
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

export function SmartSearchPage({ reservationForm, result, loading, error, onReservationChange, onSearch, onChooseRecommendation }: SmartSearchPageProps) {
  const [query, setQuery] = useState('')
  const [durationMinutes, setDurationMinutes] = useState<30 | 60>(60)
  const validStartTimes = searchableStartTimes.filter((time) => {
    const [, minutes] = time.split(':').map(Number)
    return (durationMinutes === 30 || minutes === 0) && addMinutes(time, durationMinutes) <= '22:00'
  })
  const calculatedEnd = addMinutes(reservationForm.start, durationMinutes)

  return (
    <main className="page dashboard-page">
      <AppHeader title="Búsqueda inteligente" roleLabel="Estudiante" />

      <section className="card smart-search-page-card">
        <div className="card-head">
          <h2>Encuentra una sala</h2>
        </div>
        <p className="smart-search-description">Describe tu necesidad y ordenaremos las salas disponibles por compatibilidad.</p>

        <form className="smart-search-page-form" onSubmit={(event) => { event.preventDefault(); onSearch(query, reservationForm.date, reservationForm.start, calculatedEnd) }}>
          <div className="smart-search-input-panel">
            <div className="smart-search-fields">
              <label className="smart-search-query">Necesidad<textarea value={query} maxLength={500} rows={3} placeholder="Ej.: Necesito estudiar cerca de F1, pero no quiero en el edificio M" onChange={(event) => setQuery(event.target.value)} /><small>Puedes indicar inclusiones, exclusiones, servicios cercanos y preferencias de cercanía o lejanía.</small></label>
              <label>Fecha<input type="date" value={reservationForm.date} onChange={(event) => onReservationChange({ ...reservationForm, date: event.target.value })} /></label>
              <label>Inicio<select value={validStartTimes.includes(reservationForm.start) ? reservationForm.start : ''} onChange={(event) => onReservationChange({ ...reservationForm, start: event.target.value, end: addMinutes(event.target.value, durationMinutes) })}><option value="">Selecciona una hora</option>{validStartTimes.map((time) => <option key={time} value={time}>{time}</option>)}</select></label>
              <label>Duración<select value={durationMinutes} onChange={(event) => { const duration = Number(event.target.value) as 30 | 60; const startRemainsValid = duration === 30 || reservationForm.start.endsWith(':00'); setDurationMinutes(duration); onReservationChange({ ...reservationForm, start: startRemainsValid ? reservationForm.start : '', end: startRemainsValid ? addMinutes(reservationForm.start, duration) : '' }) }}><option value={60}>1 hora</option><option value={30}>30 minutos</option></select></label>
              <label>Fin<input type="time" value={calculatedEnd} readOnly aria-readonly="true" /></label>
            </div>
            <button className="inline-flex min-h-10 items-center justify-center gap-2 rounded-full bg-primary px-4 text-sm font-semibold text-white transition hover:-translate-y-px hover:bg-primary-dark disabled:cursor-not-allowed disabled:opacity-60" type="submit" disabled={loading || !query.trim() || !reservationForm.date || !reservationForm.start || !calculatedEnd}>
              <span className="btn-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="6" /><path d="m16 16 4 4" /></svg></span>
              {loading ? 'Interpretando…' : 'Encontrar salas'}
            </button>
          </div>
        </form>

        {error && <p className="smart-search-error" role="alert">{error}</p>}
        {result && (
          <div className="smart-search-results">
            <div className="interpreted-intent"><span>Interpretamos:</span><b>{result.intent.minimumCapacity} personas</b><b>Ruido {result.intent.maximumNoise.toLowerCase()}</b>{result.intent.requiresConcentration && <b>Concentración</b>}{result.intent.requiredEquipment.map((item) => <b key={item}>{item}</b>)}</div>
            {result.recommendations.length === 0 ? <p className="smart-search-empty">{result.message ?? 'No encontramos salas compatibles disponibles en ese horario. Prueba otra hora o describe requisitos más flexibles.'}</p> : (
              <div className="recommendation-grid">{result.recommendations.map((recommendation, index) => (
                <article className="recommendation-card" key={recommendation.room.id}>
                  <div className="recommendation-rank">#{index + 1}</div>
                  <div><span className="recommendation-score">{recommendation.score} puntos</span><h3>{recommendation.room.resourceLabel}</h3><p>{recommendation.room.campusLabel} · {recommendation.room.venueLabel} · Cap. {recommendation.room.capacity}</p>{recommendation.room.description && <p>{recommendation.room.description}</p>}{recommendation.room.nearbyServices.length > 0 && <p>Servicios cercanos: {recommendation.room.nearbyServices.join(', ')}</p>}</div>
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
