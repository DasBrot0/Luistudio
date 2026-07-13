const items = [
  { className: 'free', label: 'Libre' },
  { className: 'occupied', label: 'Ocupada' },
  { className: 'maintenance', label: 'Mantenimiento' },
  { className: 'empty', label: 'Sin espacios' },
]

export function MapLegend() {
  return <div className="map-legend" aria-label="Estados de disponibilidad">{items.map((item) => <span key={item.label}><i className={item.className} />{item.label}</span>)}</div>
}
