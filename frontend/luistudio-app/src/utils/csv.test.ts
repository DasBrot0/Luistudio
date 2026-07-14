import { describe, expect, it } from 'vitest'
import { buildExcelCompatibleCsv } from './csv'

describe('buildExcelCompatibleCsv', () => {
  it('incluye BOM UTF-8 y conserva caracteres del español', () => {
    const csv = buildExcelCompatibleCsv([
      ['Código', 'Estudiante'],
      ['DEMO014', 'Óscar Peña'],
    ])

    expect(csv.charCodeAt(0)).toBe(0xfeff)
    expect(csv).toContain('"Código","Estudiante"\r\n"DEMO014","Óscar Peña"')
  })

  it('escapa comillas y valores vacíos', () => {
    expect(buildExcelCompatibleCsv([['Sala "M"', null]])).toBe('\uFEFF"Sala ""M""",""')
  })
})
