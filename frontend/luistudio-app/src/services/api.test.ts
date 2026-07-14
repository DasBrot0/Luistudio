import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'

const jsonResponse = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { 'Content-Type': 'application/json' },
})

describe('API client contracts', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('accepts a successful empty response when deleting a room', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.deleteRoom('session', 42)).resolves.toBeUndefined()

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toMatch(/\/api\/rooms\/42$/)
    expect(options.method).toBe('DELETE')
    expect(options.credentials).toBe('include')
  })

  it('receives the availability subscription returned by the backend', async () => {
    const subscription = {
      id: 9,
      roomId: 42,
      roomName: 'Sala grupal',
      targetDate: '2026-07-20',
      startTime: '10:00:00',
      endTime: '11:00:00',
      status: 'ACTIVA',
      createdAt: '2026-07-14T18:00:00-05:00',
    }
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(subscription))
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.subscribeToRoom('session', 42, '2026-07-20', '10:00', '11:00'))
      .resolves.toEqual(subscription)

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toMatch(/\/api\/rooms\/42\/availability-subscriptions$/)
    expect(options.method).toBe('POST')
    expect(JSON.parse(String(options.body))).toEqual({
      targetDate: '2026-07-20',
      startTime: '10:00',
      endTime: '11:00',
    })
  })

  it('downloads ICS using the authenticated cookie without a fake Bearer token', async () => {
    const calendar = new Blob(['BEGIN:VCALENDAR\r\nEND:VCALENDAR'], { type: 'text/calendar' })
    const fetchMock = vi.fn().mockResolvedValue(new Response(calendar, { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(api.downloadBookingIcs('session', 77)).resolves.toBeInstanceOf(Blob)

    const [url, options] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(url).toMatch(/\/api\/bookings\/77\/ics$/)
    expect(options.credentials).toBe('include')
    expect(new Headers(options.headers).has('Authorization')).toBe(false)
  })
})
