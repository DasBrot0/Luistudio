import { describe, expect, it } from 'vitest'
import { buildPabellonCode, formatDate, minutesBetween, toMinutes } from './helpers'

describe('toMinutes', () => {
  it('converts an HH:mm string to total minutes', () => {
    expect(toMinutes('09:30')).toBe(570)
    expect(toMinutes('00:00')).toBe(0)
  })
})

describe('minutesBetween', () => {
  it('returns the difference in minutes between two times', () => {
    expect(minutesBetween('09:00', '10:30')).toBe(90)
  })
})

describe('formatDate', () => {
  it('converts an ISO date (yyyy-mm-dd) to dd-mm-yyyy', () => {
    expect(formatDate('2026-07-11')).toBe('11-07-2026')
  })
})

describe('buildPabellonCode', () => {
  it('builds initials from campus and venue words', () => {
    expect(buildPabellonCode('Campus Central', 'Pabellon A')).toBe('CCPA')
  })

  it('falls back to a sanitized slug when no alphanumeric words are found', () => {
    expect(buildPabellonCode('', '')).toBe('-')
  })
})
