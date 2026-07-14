import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { Room } from '../../models/types'
import { AdminAttendancePage } from './AdminAttendancePage'

const room = {
  backendId: 1,
  id: 'M-2-GRU-06',
  name: 'Sala grupal',
  resourceLabel: 'Sala grupal',
  campus: 'MON',
  campusLabel: 'Monterrico',
  venue: 'M',
  venueLabel: 'Biblioteca Antonio Pinilla',
  capacity: 6,
  location: '2do piso',
  minPeople: 1,
  minPeopleRequired: false,
  maxPeople: 6,
  slotMinutes: 60,
  schedule: [],
  active: true,
  status: 'Disponible',
  inventoryCount: 4,
} satisfies Room

const defaultProps = {
  rooms: [room],
  loading: false,
  query: '',
  campus: 'Todos',
  pavilion: 'Todos',
  status: 'Todos',
  from: '',
  to: '',
  sort: 'date:desc',
  page: 1,
  totalPages: 1,
  totalElements: 1,
  onQueryChange: () => undefined,
  onCampusChange: () => undefined,
  onPavilionChange: () => undefined,
  onStatusChange: () => undefined,
  onFromChange: () => undefined,
  onToChange: () => undefined,
  onSortChange: () => undefined,
  onClear: () => undefined,
  onPrev: () => undefined,
  onNext: () => undefined,
}

describe('AdminAttendancePage', () => {
  it('shows pavilion filters and lets the administrator mark attendance', () => {
    const onMark = vi.fn()
    render(
      <AdminAttendancePage
        {...defaultProps}
        items={[{
          bookingId: 30,
          userId: 10,
          studentCode: '20260001',
          studentName: 'Ana Torres',
          studentEmail: 'ana@aloe.ulima.edu.pe',
          roomId: 1,
          roomCode: 'M-2-GRU-06',
          roomName: 'Sala grupal',
          campus: 'Monterrico',
          pavilionCode: 'M',
          pavilionName: 'Biblioteca Antonio Pinilla',
          location: '2do piso',
          date: '2026-07-14',
          startTime: '10:00',
          endTime: '11:00',
          bookingStatus: 'ACTIVA',
          attendanceStatus: 'PENDIENTE',
        }]}
        onMark={onMark}
      />,
    )

    expect(screen.getByLabelText('attendance-pavilion-filter')).toHaveTextContent('Biblioteca Antonio Pinilla')
    fireEvent.click(screen.getAllByRole('button', { name: 'Marcar asistencia' })[0])
    expect(onMark).toHaveBeenCalledWith(30, 'ASISTIO')
  })
})
