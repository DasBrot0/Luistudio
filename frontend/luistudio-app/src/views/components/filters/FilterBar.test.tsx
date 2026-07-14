import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { FilterBar } from './FilterBar'

describe('FilterBar', () => {
  it('does not show an empty sort section when only filter actions are provided', () => {
    render(
      <FilterBar
        filters={[{ id: 'from', type: 'date', value: '', onChange: vi.fn(), ariaLabel: 'Desde' }]}
        quickChips={[]}
        actions={<button type="button">Reiniciar filtros</button>}
      />,
    )

    expect(screen.getByRole('button', { name: 'Reiniciar filtros' })).toBeInTheDocument()
    expect(screen.queryByText('Ordenar por')).not.toBeInTheDocument()
  })
})
