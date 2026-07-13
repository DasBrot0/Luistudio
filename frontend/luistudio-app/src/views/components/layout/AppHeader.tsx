interface AppHeaderProps {
  title: string
  roleLabel?: string
  subtitle?: string
}

export function AppHeader({ title, roleLabel, subtitle }: AppHeaderProps) {
  return (
    <header className="mx-auto mb-4 flex w-full max-w-7xl items-center justify-between gap-3 max-[896px]:flex-col max-[896px]:items-stretch">
      <div>
        {roleLabel && (
          <p className="m-0 text-[0.72rem] font-bold uppercase tracking-[0.12em] text-text-secondary">
            {roleLabel}
          </p>
        )}
        <h1 className="mt-0.5 text-2xl font-semibold text-text-primary">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-text-secondary">{subtitle}</p>}
      </div>
    </header>
  )
}
