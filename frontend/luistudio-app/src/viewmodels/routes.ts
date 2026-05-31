import type { AuthUser, RouteKey } from '../models/types'

export const routePaths: Record<RouteKey, string> = {
  login: '/',
  'reset-password': '/restablecer-contrasena',
  reservas: '/reservas',
  misreservas: '/misreservas',
  salas: '/salas',
  perfiles: '/perfiles',
  'admin-reservas': '/admin/reservas',
}

export function getRouteFromPath(pathname: string): RouteKey {
  const normalized = pathname.replace(/\/+$/, '') || '/'

  if (normalized === routePaths['reset-password']) return 'reset-password'
  if (normalized === routePaths.reservas) return 'reservas'
  if (normalized === routePaths.misreservas) return 'misreservas'
  if (normalized === routePaths.salas) return 'salas'
  if (normalized === routePaths.perfiles) return 'perfiles'
  if (normalized === routePaths['admin-reservas']) return 'admin-reservas'

  return 'login'
}

export function resolveRouteByAuth(route: RouteKey, user: AuthUser | null, preferredLanding: RouteKey | null = null): RouteKey {
  const studentLanding = (preferred: RouteKey | null) =>
    preferred === 'reservas' || preferred === 'misreservas' ? preferred : 'misreservas'

  const adminLanding = (preferred: RouteKey | null) =>
    preferred === 'admin-reservas' || preferred === 'salas' ? preferred : 'salas'

  if (route === 'reset-password') {
    return route
  }

  if (route === 'login' && user) {
    return user.role === 'student' ? studentLanding(preferredLanding) : adminLanding(preferredLanding)
  }

  if (route === 'login') {
    return route
  }

  if (!user) {
    return 'login'
  }

  if (user.role === 'student') {
    if (route === 'reservas' || route === 'misreservas') {
      return route
    }
    return studentLanding(preferredLanding)
  }

  if (route === 'salas' || route === 'perfiles' || route === 'admin-reservas') {
    return route
  }

  return adminLanding(preferredLanding)
}
