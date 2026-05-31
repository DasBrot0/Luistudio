interface SubnavItem {
  key: string
  label: string
}

interface SubnavProps {
  activeKey: string
  items: SubnavItem[]
  onNavigate: (key: string) => void
}

export function Subnav({ activeKey, items, onNavigate }: SubnavProps) {
  return (
    <nav className="mx-auto mb-3 flex w-full max-w-7xl flex-wrap gap-2">
      {items.map((item) => (
        <button
          key={item.key}
          type="button"
          className={`min-h-9 rounded-full border px-4 text-sm font-semibold transition ${
            activeKey === item.key
              ? 'border-primary-dark bg-primary text-white'
              : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
          }`}
          onClick={() => onNavigate(item.key)}
        >
          {item.label}
        </button>
      ))}
    </nav>
  )
}
