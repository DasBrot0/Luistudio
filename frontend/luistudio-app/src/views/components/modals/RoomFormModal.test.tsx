import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { RoomDraft } from '../../../models/types'
import { RoomFormModal } from './RoomFormModal'

const initialDraft: RoomDraft = {
  name: 'Sala de prueba',
  campus: 'Monterrico',
  location: 'Pabellón F1',
  capacity: 8,
  minPeople: 1,
  minPeopleRequired: false,
  maxPeople: 8,
  status: 'Disponible',
  schedule: [],
  pabellonCode: 'F1',
  noiseLevel: 'MEDIO',
  supportsConcentration: false,
  roomType: 'GENERAL',
  equipment: [],
}

function Harness() {
  const [draft, setDraft] = useState(initialDraft)
  return (
    <RoomFormModal
      mode="edit"
      draft={draft}
      notice=""
      targetRoomId="F1-TEST"
      campusOptions={['Monterrico']}
      venueOptionsByCampus={new Map([['Monterrico', ['Pabellón F1']]])}
      onChange={setDraft}
      onCancel={() => undefined}
      onSubmit={(event) => event.preventDefault()}
    />
  )
}

describe('RoomFormModal intelligent search data', () => {
  it('allows the administrator to edit attributes and add or remove equipment', () => {
    render(<Harness />)

    fireEvent.change(screen.getByLabelText('Tipo de sala'), { target: { value: 'ESTUDIO_GRUPAL' } })
    fireEvent.change(screen.getByLabelText('Nivel de ruido'), { target: { value: 'BAJO' } })
    fireEvent.click(screen.getByLabelText('Apta para actividades que requieren concentración'))

    expect(screen.getByLabelText('Tipo de sala')).toHaveValue('ESTUDIO_GRUPAL')
    expect(screen.getByLabelText('Nivel de ruido')).toHaveValue('BAJO')
    expect(screen.getByLabelText('Apta para actividades que requieren concentración')).toBeChecked()

    fireEvent.change(screen.getByLabelText('Equipamiento'), { target: { value: 'Pizarra' } })
    fireEvent.click(screen.getByRole('button', { name: 'Agregar' }))
    expect(screen.getByText('pizarra')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Quitar pizarra' }))
    expect(screen.queryByText('pizarra')).not.toBeInTheDocument()
    expect(screen.getByText('Sin equipamiento registrado.')).toBeInTheDocument()
  })
})
