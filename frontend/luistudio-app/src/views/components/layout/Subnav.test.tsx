import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { Subnav } from './Subnav'

const items = [
  { key: 'inicio', label: 'Inicio' },
  { key: 'reservas', label: 'Reservas' },
]

describe('Subnav', () => {
  it('renders one button per item and marks the active item', () => {
    render(<Subnav activeKey="reservas" items={items} onNavigate={() => {}} />)

    expect(screen.getByRole('button', { name: 'Inicio' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reservas' })).toHaveClass('bg-primary')
  })

  it('calls onNavigate with the clicked item key', () => {
    const onNavigate = vi.fn()
    render(<Subnav activeKey="inicio" items={items} onNavigate={onNavigate} />)

    fireEvent.click(screen.getByRole('button', { name: 'Reservas' }))

    expect(onNavigate).toHaveBeenCalledWith('reservas')
  })
})
