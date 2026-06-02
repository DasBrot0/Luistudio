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
  quickChips: QuickChipConfig[]
  actions?: ReactNode
}

export function FilterBar({
  searchPlaceholder,
  searchValue = '',
  onSearchChange,
  searchAriaLabel,
  filters,
  quickChips,
  actions,
}: FilterBarProps) {
  const hasSearch = typeof onSearchChange === 'function'

  return (
    <div className="filter-bar">
      {(hasSearch || actions) && (
        <div className="filter-bar-top">
          {hasSearch ? (
            <input
              type="search"
              value={searchValue}
              onChange={(event) => onSearchChange(event.target.value)}
              placeholder={searchPlaceholder}
              aria-label={searchAriaLabel ?? searchPlaceholder ?? 'Buscar'}
            />
          ) : (
            <div />
          )}
          {actions ? <div className="filter-bar-actions">{actions}</div> : null}
        </div>
      )}

      {filters.length > 0 ? (
        <div className="filter-bar-fields">
          {filters.map((filter) => {
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
                />
              )
            }

            return (
              <select
                key={filter.id}
                value={filter.value}
                onChange={(event) => filter.onChange(event.target.value)}
                aria-label={filter.ariaLabel ?? filter.id}
              >
                {filter.options?.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            )
          })}
        </div>
      ) : null}

      {quickChips.length > 0 ? (
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
    </div>
  )
}
