import type { AuthUser, RouteKey } from '../models/types'

export const routePaths: Record<RouteKey, string> = {
  login: '/',
  'reset-password': '/restablecer-contrasena',
  'confirm-change': '/confirmar-cambio',
  reservas: '/reservas',
  disponibilidad: '/disponibilidad',
  'busqueda-inteligente': '/busqueda-inteligente',
  misreservas: '/misreservas',
  mapa: '/mapa',
  dashboard: '/admin/dashboard',
  salas: '/salas',
  perfiles: '/perfiles',
  'admin-reservas': '/admin/reservas',
  asistencias: '/admin/asistencias',
  seguridad: '/admin/seguridad',
  comunicados: '/admin/comunicados',
  profile: '/perfil',
}

export function getRouteFromPath(pathname: string): RouteKey {
  const normalized = pathname.replace(/\/+$/, '') || '/'

  if (normalized === routePaths['reset-password']) return 'reset-password'
  if (normalized === routePaths['confirm-change']) return 'confirm-change'
  if (normalized === routePaths.reservas) return 'reservas'
  if (normalized === routePaths.disponibilidad) return 'disponibilidad'
  if (normalized === routePaths['busqueda-inteligente']) return 'busqueda-inteligente'
  if (normalized === routePaths.misreservas) return 'misreservas'
  if (normalized === routePaths.mapa) return 'mapa'
  if (normalized === routePaths.dashboard) return 'dashboard'
  if (normalized === routePaths.salas) return 'salas'
  if (normalized === routePaths.perfiles) return 'perfiles'
  if (normalized === routePaths['admin-reservas']) return 'admin-reservas'
  if (normalized === routePaths.asistencias) return 'asistencias'
  if (normalized === routePaths.seguridad) return 'seguridad'
  if (normalized === routePaths.comunicados) return 'comunicados'
  if (normalized === routePaths.profile) return 'profile'

  return 'login'
}

export function resolveRouteByAuth(route: RouteKey, user: AuthUser | null, preferredLanding: RouteKey | null = null): RouteKey {
  const studentLanding = (preferred: RouteKey | null) =>
    preferred === 'reservas' || preferred === 'misreservas' ? preferred : 'misreservas'

  const adminLanding = (preferred: RouteKey | null) =>
    preferred === 'admin-reservas' || preferred === 'salas' || preferred === 'perfiles' || preferred === 'dashboard'
      ? preferred
      : 'dashboard'

  if (route === 'reset-password' || route === 'confirm-change') {
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
    if (route === 'reservas' || route === 'disponibilidad' || route === 'busqueda-inteligente' || route === 'misreservas' || route === 'mapa' || route === 'profile') {
      return route
    }
    return studentLanding(preferredLanding)
  }

  if (route === 'dashboard' || route === 'salas' || route === 'perfiles' || route === 'admin-reservas' || route === 'asistencias' || route === 'seguridad' || route === 'comunicados' || route === 'mapa' || route === 'profile') {
    return route
  }

  return adminLanding(preferredLanding)
}
