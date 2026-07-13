import { useEffect, useRef } from 'react'
import maplibregl, { Marker } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import type { CampusMapCampus, CampusMapPavilion } from '../../../models/campusMap'

const styleUrl = (dark: boolean) => {
  const base = dark
    ? (import.meta.env.VITE_MAP_STYLE_DARK_URL || 'https://api.maptiler.com/maps/streets-v2-dark/style.json')
    : (import.meta.env.VITE_MAP_STYLE_LIGHT_URL || 'https://api.maptiler.com/maps/streets-v2/style.json')
  const key = import.meta.env.VITE_MAPTILER_API_KEY
  if (!base || !key) return ''
  const url = new URL(base); url.searchParams.set('key', key); return url.toString()
}

class RecenterControl {
  private container: HTMLDivElement | null = null
  private button: HTMLButtonElement | null = null
  private readonly onRecenter: () => void

  constructor(onRecenter: () => void, dark: boolean) {
    this.onRecenter = onRecenter
    this.setDark(dark)
  }

  onAdd() {
    this.container = document.createElement('div')
    this.container.className = 'maplibregl-ctrl maplibregl-ctrl-group'
    this.button = document.createElement('button')
    this.button.type = 'button'
    this.button.textContent = String.fromCodePoint(0x2316)
    this.button.title = 'Centrar mapa'
    this.button.setAttribute('aria-label', 'Centrar mapa')
    this.button.style.fontSize = '22px'
    this.button.style.fontWeight = '700'
    this.button.style.lineHeight = '1'
    this.button.onclick = this.onRecenter
    this.container.append(this.button)
    this.setDark(this.dark)
    return this.container
  }

  private dark = false

  setDark(dark: boolean) {
    this.dark = dark
    if (this.container) this.container.style.backgroundColor = dark ? '#0f172a' : '#ffffff'
    if (this.button) this.button.style.color = dark ? '#f8fafc' : '#1e293b'
  }

  onRemove() {
    this.container?.remove()
    this.container = null
    this.button = null
  }
}

interface Props { campus: CampusMapCampus; dark: boolean; selected: number|null; calibrate: boolean; onSelect:(p:CampusMapPavilion)=>void; onOpen:(p:CampusMapPavilion)=>void; onMoved:(p:CampusMapPavilion,lat:number,lon:number)=>void }
export function CampusMap({ campus, dark, selected, onSelect, onOpen, calibrate, onMoved }: Props) {
  const host = useRef<HTMLDivElement>(null), map = useRef<maplibregl.Map|null>(null), markers = useRef<Marker[]>([]), currentStyle = useRef('')
  const recenterControl = useRef<RecenterControl|null>(null)
  const previousCampusCode = useRef<string|null>(null)
  const campusView = useRef({ longitude: campus.center.longitude, latitude: campus.center.latitude, zoom: campus.defaultZoom })
  const failed = !styleUrl(dark) || !Number.isFinite(campus.center.longitude) || !Number.isFinite(campus.center.latitude)
  useEffect(() => {
    campusView.current = { longitude: campus.center.longitude, latitude: campus.center.latitude, zoom: campus.defaultZoom }
  }, [campus.center.longitude, campus.center.latitude, campus.defaultZoom])
  useEffect(() => {
    const style = styleUrl(dark)
    if (!host.current || !style || failed) return
    const center: [number,number] = [campus.center.longitude!, campus.center.latitude!]
    const isNewMap = !map.current
    const instance = map.current ?? new maplibregl.Map({ container: host.current, style, center, zoom: campus.defaultZoom })
    if (isNewMap) {
      recenterControl.current = new RecenterControl(() => {
      const view = campusView.current
      if (!Number.isFinite(view.longitude) || !Number.isFinite(view.latitude)) return
      map.current?.flyTo({ center: [view.longitude!, view.latitude!], zoom: view.zoom })
      }, dark)
      instance.addControl(recenterControl.current, 'bottom-right')
    }
    recenterControl.current?.setDark(dark)
    if (map.current && currentStyle.current !== style) instance.setStyle(style)
    currentStyle.current = style
    map.current = instance
    markers.current.forEach(marker => marker.remove())
    markers.current = campus.pavilions.filter(p => p.latitude != null && p.longitude != null).map(p => {
      const element = document.createElement('button')
      element.className = `h-11 min-w-11 rounded-full border-2 px-2 text-xs font-bold shadow ${selected===p.id?'border-violet-300 bg-violet-700 text-white':'border-white bg-primary text-white'}`
      element.textContent=p.code; element.title=`${p.name}: ${p.summary.free} libres`
      element.onclick=()=>onSelect(p)
      element.ondblclick=()=>{instance.flyTo({center:[p.longitude!,p.latitude!],zoom:20});onOpen(p)}
      element.onkeydown=e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();onOpen(p)}}
      const marker=new maplibregl.Marker({element,draggable:calibrate}).setLngLat([p.longitude!,p.latitude!]).addTo(instance)
      marker.on('dragend',()=>{const position=marker.getLngLat();onMoved(p,position.lat,position.lng)})
      return marker
    })
  }, [campus,dark,selected,calibrate,onSelect,onOpen,onMoved,failed])
  useEffect(() => {
    if (previousCampusCode.current === null) {
      previousCampusCode.current = campus.code
      return
    }
    if (previousCampusCode.current === campus.code) return
    previousCampusCode.current = campus.code
    if (!Number.isFinite(campus.center.longitude) || !Number.isFinite(campus.center.latitude)) return
    map.current?.flyTo({ center: [campus.center.longitude!, campus.center.latitude!], zoom: campus.defaultZoom })
  }, [campus.code, campus.center.latitude, campus.center.longitude, campus.defaultZoom])
  useEffect(()=>()=>{
    markers.current.forEach(marker => marker.remove())
    markers.current = []
    map.current?.remove()
    map.current = null
    recenterControl.current = null
    currentStyle.current = ''
  },[])
  return <div className="campus-map-canvas">{failed?<div className="campus-map-fallback"><b>Mapa base no disponible</b><span>La disponibilidad sigue accesible en la lista.</span></div>:<div ref={host} className="campus-map-canvas-host"/>}</div>
}
