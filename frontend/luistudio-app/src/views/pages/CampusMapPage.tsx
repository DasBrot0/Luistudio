import { useCallback, useMemo, useState } from 'react'
import { useCampusMap } from '../../hooks/useCampusMap'
import type { CampusMapPavilion } from '../../models/campusMap'
import { api } from '../../services/api'
import { CampusMap } from '../components/map/CampusMap'
import { MapLegend } from '../components/map/MapLegend'
import { PavilionDetailsDrawer } from '../components/map/PavilionDetailsDrawer'
import { AppHeader } from '../components/layout/AppHeader'

interface Props { token: string; isDarkMode: boolean; isAdmin: boolean; onReserve: (id: number) => void }

export function CampusMapPage({ token, isDarkMode, isAdmin, onReserve }: Props) {
  const [campusName, setCampusName] = useState<string>()
  const { data, loading, error, refresh } = useCampusMap(token)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [open, setOpen] = useState(false)
  const [calibrate, setCalibrate] = useState(false)
  const [draft, setDraft] = useState<{ p: CampusMapPavilion; lat: number; lon: number } | null>(null)
  const [notice, setNotice] = useState('')
  const hasCenter = (value: { center: { latitude: number | null; longitude: number | null } }) =>
    Number.isFinite(value.center.latitude) && Number.isFinite(value.center.longitude)
  const campus = campusName
    ? data?.campuses.find((item) => item.name === campusName)
    : data?.campuses.find((item) => item.name.toLocaleLowerCase('es-PE').includes('monterrico') && hasCenter(item))
      ?? data?.campuses.find(hasCenter)
      ?? data?.campuses[0]
  const selected = useMemo(() => campus?.pavilions.find((item) => item.id === selectedId) ?? null, [campus, selectedId])
  const choose = useCallback((pavilion: CampusMapPavilion) => setSelectedId(pavilion.id), [])
  const show = useCallback((pavilion: CampusMapPavilion) => { setSelectedId(pavilion.id); setOpen(true) }, [])
  const moved = useCallback((pavilion: CampusMapPavilion, lat: number, lon: number) => {
    setDraft({ p: pavilion, lat, lon })
    const value = `${lat.toFixed(7)}, ${lon.toFixed(7)}`
    setNotice(`Nuevas coordenadas: ${value}`)
    void navigator.clipboard?.writeText(value)
  }, [])
  const save = async () => {
    if (!draft) return
    await api.updateBuildingLocation(token, draft.p.id, draft.lat, draft.lon)
    setNotice('Ubicación guardada')
    setDraft(null)
    await refresh()
  }

  return (
    <main className="page dashboard-page">
      <AppHeader title="Mapa" roleLabel={isAdmin ? 'Administrador' : 'Estudiante'} />
      <section className="dashboard-grid single-grid">
        <article className="card campus-map-card">
          <div className="card-head campus-map-head">
            <div>
              <h2>Disponibilidad por pabellón</h2>
              <p className="section-subtitle">
                {data ? `Actualizado ${new Date(data.generatedAt).toLocaleTimeString('es-PE')}` : 'Estado actual de salas'}
              </p>
            </div>
            <div className={`campus-map-controls ${isAdmin ? 'campus-map-controls-admin' : ''}`}>
              {data && data.campuses.length > 0 && (
                <label className="campus-map-campus-field" htmlFor="map-campus">
                  <span>Campus</span>
                  <select id="map-campus" value={campus?.name ?? ''} onChange={(event) => setCampusName(event.target.value)}>
                    {data.campuses.map((item) => <option key={item.code}>{item.name}</option>)}
                  </select>
                </label>
              )}
              {isAdmin && (
                <button type="button" className={calibrate ? 'soft-btn compact campus-map-calibrate active' : 'ghost-btn compact campus-map-calibrate'} onClick={() => setCalibrate((value) => !value)} aria-pressed={calibrate}>
                  Calibrar marcadores
                </button>
              )}
            </div>
          </div>

          {loading && !data && <div className="campus-map-loading" aria-label="Cargando mapa" />}
          {error && !data && <div className="dashboard-error">{error}<button className="link-btn" onClick={() => void refresh()}>Reintentar</button></div>}
          {data && data.campuses.length === 0 && (
            <div className="empty-state campus-map-empty">
              <h3>No hay pabellones habilitados</h3>
              <p>Verifica la configuración de edificios y campus.</p>
              <button className="ghost-btn compact" onClick={() => void refresh()}>Consultar nuevamente</button>
            </div>
          )}
          {campus && (
            <>
              <MapLegend />
              <div className="campus-map-layout">
                <CampusMap campus={campus} dark={isDarkMode} selected={selectedId} onSelect={choose} onOpen={show} calibrate={calibrate} onMoved={moved} />
                <PavilionDetailsDrawer pavilion={open ? selected : null} onClose={() => setOpen(false)} onReserve={onReserve} />
              </div>
            </>
          )}
        </article>
      </section>
      {calibrate && draft && <div className="campus-map-save-toast"><p>{notice}</p><button className="primary-btn" onClick={() => void save()}>Guardar ubicación</button></div>}
    </main>
  )
}
