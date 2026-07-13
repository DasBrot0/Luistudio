import type { Role, AuthUser, RouteKey } from '../../../models/types'
import logoModoClaro from '../../../assets/logos/logo_modo_claro.png'
import logoModoOscuro from '../../../assets/logos/logo_modo_oscuro.png'
import logoHorizontalClaro from '../../../assets/logos/logo_horizontal_modo_claro.png'
import logoHorizontalOscuro from '../../../assets/logos/logo_horizontal_modo_oscuro.png'

interface NotificationItem {
  id: number
  message: string
  createdAt: string
}

interface NavItem {
  route: RouteKey
  label: string
  icon: 'dashboard' | 'calendar' | 'list' | 'rooms' | 'users' | 'bookings' | 'security' | 'megaphone' | 'map' | 'search'
}

interface GlobalTopbarProps {
  role: Role
  user: AuthUser | null
  activeRoute: RouteKey
  notifications: NotificationItem[]
  isSidebarCollapsed: boolean
  onNavigate: (route: RouteKey) => void
  onToggleSidebar: () => void
  onOpenNotifications: () => void
  onOpenSettings: () => void
}

const studentItems: NavItem[] = [
  { route: 'misreservas', label: 'Mis reservas', icon: 'list' },
  { route: 'reservas', label: 'Reservar', icon: 'calendar' },
  { route: 'busqueda-inteligente', label: 'Búsqueda inteligente', icon: 'search' },
  { route: 'mapa', label: 'Mapa', icon: 'map' },
]

const adminItems: NavItem[] = [
  { route: 'dashboard', label: 'Dashboard', icon: 'dashboard' },
  { route: 'salas', label: 'Salas', icon: 'rooms' },
  { route: 'perfiles', label: 'Perfiles', icon: 'users' },
  { route: 'admin-reservas', label: 'Reservas', icon: 'bookings' },
  { route: 'mapa', label: 'Mapa', icon: 'map' },
  { route: 'seguridad', label: 'Seguridad', icon: 'security' },
  { route: 'comunicados', label: 'Comunicados', icon: 'megaphone' },
]

function NavIcon({ type }: { type: NavItem['icon'] | 'bell' | 'collapse' | 'settings' | 'profile' }) {
  if (type === 'dashboard') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="4" rx="1" /><rect x="14" y="11" width="7" height="10" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /></svg>
  if (type === 'map') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 18l-6 3V6l6-3 6 3 6-3v15l-6 3-6-3zM9 3v15M15 6v15" /></svg>
  if (type === 'megaphone') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M3 11V13a2 2 0 0 0 2 2h1l2 4h2l-2-4h1a2 2 0 0 0 2-2v-2" /><path d="M11 11V7.5L21 5v14l-10-2.5V13" /><path d="M11 11H3" /></svg>
  if (type === 'security') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" /></svg>
  if (type === 'profile') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="8" r="4" /><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" /></svg>
  if (type === 'calendar') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></svg>
  if (type === 'search') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="6" /><path d="m16 16 4 4" /></svg>
  if (type === 'list') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" /></svg>
  if (type === 'rooms') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M3 21V8a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v13M3 21h18M8 11h2M8 15h2M16 3h3a2 2 0 0 1 2 2v16" /></svg>
  if (type === 'users') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="3.5" /><path d="M20 8v6M23 11h-6" /></svg>
  if (type === 'bookings') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M9 11l3 3L22 4" /><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" /></svg>
  if (type === 'bell') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" /><path d="M13.73 21a2 2 0 0 1-3.46 0" /></svg>
  if (type === 'settings') return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h.01a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h.01a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.01a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" /></svg>
  return <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth="2"><path d="M15 18l-6-6 6-6" /></svg>
}

export function GlobalTopbar({
  role,
  user,
  activeRoute,
  notifications,
  isSidebarCollapsed,
  onNavigate,
  onToggleSidebar,
  onOpenNotifications,
  onOpenSettings,
}: GlobalTopbarProps) {
  const navItems = role === 'admin' ? adminItems : studentItems
  const roleLabel = role === 'admin' ? 'Administrador' : 'Estudiante'

  return (
    <>
      <aside className={`sidebar-shell hidden border-r border-primary-light bg-bg-sidebar text-text-on-primary md:fixed md:inset-y-0 md:left-0 md:z-40 md:flex md:flex-col md:transition-all ${isSidebarCollapsed ? 'md:w-24' : 'md:w-64'}`}>
        <button
          type="button"
          onClick={onToggleSidebar}
          className={`sidebar-edge-toggle ${isSidebarCollapsed ? 'collapsed' : ''}`}
          title={isSidebarCollapsed ? 'Expandir barra lateral' : 'Colapsar barra lateral'}
        >
          <div className={isSidebarCollapsed ? 'rotate-180' : ''}>
            <NavIcon type="collapse" />
          </div>
        </button>

        <div className={`flex items-center pb-4 pt-5 ${isSidebarCollapsed ? 'justify-center px-2' : 'justify-between px-4'}`}>
          <div className={`flex min-w-0 items-center ${isSidebarCollapsed ? 'justify-center' : 'gap-3'}`}>
            <span className={`sidebar-logo-frame ${isSidebarCollapsed ? 'compact' : ''}`}>
              <img src={logoModoClaro} alt="Logo de Luistudio" className="sidebar-logo logo-light" />
              <img src={logoModoOscuro} alt="Logo de Luistudio" className="sidebar-logo logo-dark" />
            </span>
            {!isSidebarCollapsed && <div className="min-w-0"><p className="m-0 truncate text-2xl font-extrabold tracking-tight">Luistudio</p><span className="role-chip mt-1">{roleLabel}</span></div>}
          </div>
        </div>

        <nav className="flex min-h-0 flex-1 flex-col gap-2 px-3">
          {navItems.map((item) => {
            const isActive = activeRoute === item.route
            return (
              <button
                key={item.route}
                type="button"
                className={`sidebar-nav-btn ${isActive ? 'active' : ''} flex items-center gap-3 rounded-lg border px-3 py-2 text-sm font-semibold transition ${isSidebarCollapsed ? 'justify-center' : 'justify-start'}`}
                onClick={() => onNavigate(item.route)}
                title={item.label}
              >
                <NavIcon type={item.icon} />
                {!isSidebarCollapsed && <span>{item.label}</span>}
              </button>
            )
          })}
        </nav>

        <div className="border-t border-primary-light px-3 pb-4 pt-3">
          <button
            type="button"
            className={`mb-2 flex w-full items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-semibold text-accent-muted transition hover:border-primary-light hover:bg-bg-active hover:text-accent ${activeRoute === 'profile' ? 'active border-primary-light bg-bg-active text-accent' : ''} ${isSidebarCollapsed ? 'justify-center' : 'justify-start'}`}
            onClick={() => onNavigate('profile')}
            title="Mi perfil"
          >
            <NavIcon type="profile" />
            {!isSidebarCollapsed && <span>{user ? `${user.firstName} ${user.lastName}` : 'Mi perfil'}</span>}
          </button>
          <button type="button" className={`mb-2 flex w-full items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-semibold text-accent-muted transition hover:border-primary-light hover:bg-bg-active hover:text-accent ${isSidebarCollapsed ? 'justify-center' : 'justify-start'}`} onClick={onOpenNotifications} title="Notificaciones"><NavIcon type="bell" />{!isSidebarCollapsed && <span>Notificaciones ({notifications.length})</span>}</button>
          <button type="button" className={`flex w-full items-center gap-3 rounded-lg border border-transparent px-3 py-2 text-sm font-semibold text-accent-muted transition hover:border-primary-light hover:bg-bg-active hover:text-accent ${isSidebarCollapsed ? 'justify-center' : 'justify-start'}`} onClick={onOpenSettings} title="Configuración"><NavIcon type="settings" />{!isSidebarCollapsed && <span>Configuración</span>}</button>
        </div>
      </aside>

      <header className="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-bg-card md:hidden">
        <div className="flex items-center justify-between gap-2 px-4 py-3">
          <div className="mobile-brand-wrap">
            <img src={logoHorizontalClaro} alt="Luistudio" className="mobile-brand-logo logo-light" />
            <img src={logoHorizontalOscuro} alt="Luistudio" className="mobile-brand-logo logo-dark" />
          </div>
          <div className="flex items-center gap-2">
            <button type="button" className="mobile-topbar-action" onClick={onOpenNotifications} aria-label={`Notificaciones${notifications.length ? ` (${notifications.length})` : ''}`} title="Notificaciones"><NavIcon type="bell" /></button>
            <button type="button" className={`mobile-topbar-action ${activeRoute === 'profile' ? 'active' : ''}`} onClick={() => onNavigate('profile')} aria-label="Mi perfil" title="Mi perfil" aria-current={activeRoute === 'profile' ? 'page' : undefined}><NavIcon type="profile" /></button>
            <button type="button" className="mobile-topbar-action" onClick={onOpenSettings} aria-label="Configuración" title="Configuración"><NavIcon type="settings" /></button>
          </div>
        </div>
      </header>

      <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-bg-card md:hidden">
        <div className="flex items-center gap-1 overflow-x-auto px-2 py-1.5">
          {navItems.map((item) => (
            <button key={item.route} type="button" onClick={() => onNavigate(item.route)} className={`mobile-nav-btn flex min-w-[4.8rem] flex-1 flex-col items-center gap-1 rounded-md px-2 py-2 text-[11px] font-semibold ${activeRoute === item.route ? 'active' : ''}`}>
              <NavIcon type={item.icon} />
              <span className="truncate">{item.label}</span>
            </button>
          ))}
        </div>
      </nav>
    </>
  )
}
