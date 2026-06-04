import { useEffect, useState } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'

interface FilterOption {
  label: string
  value: string
}

interface FilterConfig {
  id: string
  value: string
  onChange: (value: string) => void
  options?: FilterOption[]
  placeholder?: string
  type?: 'select' | 'date' | 'text'
  ariaLabel?: string
  inputMode?: InputHTMLAttributes<HTMLInputElement>['inputMode']
  disabled?: boolean
}

interface QuickChipConfig {
  id: string
  label: string
  active: boolean
  onClick: () => void
}

interface FilterBarProps {
  searchPlaceholder?: string
  searchValue?: string
  onSearchChange?: (value: string) => void
  searchAriaLabel?: string
  filters: FilterConfig[]
  sortControls?: FilterConfig[]
  quickChips: QuickChipConfig[]
  quickChipsPlacement?: 'bottom' | 'sort-row'
  actions?: ReactNode
  fieldsClassName?: string
}

export function FilterBar({
  searchPlaceholder,
  searchValue = '',
  onSearchChange,
  searchAriaLabel,
  filters,
  sortControls = [],
  quickChips,
  quickChipsPlacement = 'bottom',
  actions,
  fieldsClassName,
}: FilterBarProps) {
  const hasSearch = typeof onSearchChange === 'function'
  const [filtersOpen, setFiltersOpen] = useState(true)
  const renderControl = (filter: FilterConfig) => {
    if (filter.type === 'date' || filter.type === 'text') {
      return (
        <input
          key={filter.id}
          type={filter.type}
          value={filter.value}
          onChange={(event) => filter.onChange(event.target.value)}
          placeholder={filter.placeholder}
          aria-label={filter.ariaLabel ?? filter.placeholder ?? filter.id}
          inputMode={filter.inputMode}
          disabled={filter.disabled}
        />
      )
    }

    return (
      <select
        key={filter.id}
        value={filter.value}
        onChange={(event) => filter.onChange(event.target.value)}
        aria-label={filter.ariaLabel ?? filter.id}
        disabled={filter.disabled}
      >
        {filter.options?.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    )
  }

  useEffect(() => {
    const query = window.matchMedia('(max-width: 48rem)')
    const syncInitialState = () => setFiltersOpen(!query.matches)
    syncInitialState()
    query.addEventListener('change', syncInitialState)
    return () => query.removeEventListener('change', syncInitialState)
  }, [])

  return (
    <div className="filter-bar">
      {hasSearch && (
        <div className="filter-bar-top">
          <input
            type="search"
            value={searchValue}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder={searchPlaceholder}
            aria-label={searchAriaLabel ?? searchPlaceholder ?? 'Buscar'}
          />
          <div className="filter-bar-actions">
            <button
              type="button"
              className="inline-flex min-h-8 items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-xs font-semibold text-slate-700 transition hover:-translate-y-px hover:bg-slate-50"
              onClick={() => setFiltersOpen((current) => !current)}
              aria-expanded={filtersOpen}
            >
              <span className="btn-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="M4 6h16M7 12h10M10 18h4" />
                </svg>
              </span>
              Filtros
            </button>
          </div>
        </div>
      )}

      {filtersOpen && (
        <div className="filter-bar-panel">
          {filters.length > 0 || sortControls.length > 0 ? (
            <>
              {filters.length > 0 ? (
                <div className="filter-bar-section">
                  <p className="filter-bar-label">Filtros</p>
                  <div className={`filter-bar-fields ${fieldsClassName ?? ''}`.trim()}>
                    {filters.map(renderControl)}
                  </div>
                </div>
              ) : null}

              {(sortControls.length > 0 || quickChipsPlacement === 'sort-row' || actions) ? (
                <div className="filter-bar-section filter-bar-order-section">
                  <p className="filter-bar-label">Ordenar por</p>
                  <div className="filter-bar-order-row">
                    {sortControls.length > 0 ? (
                      <div className="filter-bar-sort-fields">
                        {sortControls.map(renderControl)}
                      </div>
                    ) : null}
                    {quickChipsPlacement === 'sort-row' && quickChips.length > 0 ? (
                      <div className="filter-bar-chips">
                        {quickChips.map((chip) => (
                          <button
                            key={chip.id}
                            type="button"
                            className={`filter-chip ${chip.active ? 'active' : ''}`}
                            onClick={chip.onClick}
                          >
                            {chip.label}
                          </button>
                        ))}
                      </div>
                    ) : null}
                    {actions ? <div className="filter-bar-panel-actions">{actions}</div> : null}
                  </div>
                </div>
              ) : null}
            </>
          ) : null}

          {quickChipsPlacement === 'bottom' && quickChips.length > 0 ? (
            <div className="filter-bar-panel-bottom">
              <div className="filter-bar-chips">
                {quickChips.map((chip) => (
                  <button
                    key={chip.id}
                    type="button"
                    className={`filter-chip ${chip.active ? 'active' : ''}`}
                    onClick={chip.onClick}
                  >
                    {chip.label}
                  </button>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      )}
    </div>
  )
}
