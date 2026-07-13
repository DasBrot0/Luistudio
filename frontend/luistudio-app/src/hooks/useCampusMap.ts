import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../services/api'
import type { CampusMapResponse } from '../models/campusMap'

export function useCampusMap(token: string, campus?: string) {
  const [data, setData] = useState<CampusMapResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const activeController = useRef<AbortController | null>(null)
  const requestId = useRef(0)
  const inFlight = useRef(false)
  const pendingAbort = useRef<number | null>(null)

  const refresh = useCallback(async () => {
    if (inFlight.current) return
    inFlight.current = true
    setLoading(true)
    const id = ++requestId.current
    const controller = new AbortController()
    activeController.current = controller

    try {
      const response = await api.getCampusMap(token, campus, controller.signal)
      if (id !== requestId.current) return
      setData(response)
      setError('')
    } catch (cause) {
      if (id !== requestId.current || controller.signal.aborted) return
      setError(cause instanceof Error ? cause.message : 'No se pudo actualizar el mapa')
    } finally {
      inFlight.current = false
      if (id === requestId.current && !controller.signal.aborted) setLoading(false)
    }
  }, [token, campus])

  useEffect(() => {
    if (pendingAbort.current !== null) {
      window.clearTimeout(pendingAbort.current)
      pendingAbort.current = null
    }
    const startTimer = window.setTimeout(() => void refresh(), 0)

    return () => {
      window.clearTimeout(startTimer)
      pendingAbort.current = window.setTimeout(() => {
        requestId.current += 1
        activeController.current?.abort()
        activeController.current = null
        inFlight.current = false
        pendingAbort.current = null
      }, 0)
    }
  }, [refresh])

  return { data, loading, error, refresh }
}
