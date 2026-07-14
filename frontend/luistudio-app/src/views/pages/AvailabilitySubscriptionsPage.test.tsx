import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { AvailabilitySubscriptionsPage } from './AvailabilitySubscriptionsPage'

describe('AvailabilitySubscriptionsPage', () => {
  it('lists active notices and delegates cancellation by subscription', () => {
    const onCancel = vi.fn()
    render(
      <AvailabilitySubscriptionsPage
        subscriptions={[{
          id: 7,
          roomId: 12,
          roomName: 'Sala de estudio grupal',
          targetDate: '2026-07-20',
          startTime: '10:00',
          endTime: '11:00',
          status: 'ACTIVA',
        }]}
        onCancel={onCancel}
        onGoToReserve={() => undefined}
      />,
    )

    expect(screen.getByRole('heading', { name: 'Avisos de disponibilidad' })).toBeInTheDocument()
    expect(screen.getByText('Sala de estudio grupal')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar aviso' }))
    expect(onCancel).toHaveBeenCalledWith(7)
  })

  it('guides the student to Reservar when there are no active notices', () => {
    const onGoToReserve = vi.fn()
    render(
      <AvailabilitySubscriptionsPage
        subscriptions={[]}
        onCancel={() => undefined}
        onGoToReserve={onGoToReserve}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Ir a Reservar' }))
    expect(onGoToReserve).toHaveBeenCalledOnce()
  })
})
