interface PaginationProps {
  page: number
  totalPages: number
  onPrev: () => void
  onNext: () => void
}

export function Pagination({ page, totalPages, onPrev, onNext }: PaginationProps) {
  const safeTotalPages = Math.max(1, totalPages)
  const safePage = Math.min(Math.max(1, page), safeTotalPages)

  return (
    <nav className="pagination" aria-label="Paginación de resultados">
      <button type="button" className="pagination-button" disabled={safePage === 1} onClick={onPrev}>
        Anterior
      </button>
      <p aria-live="polite">Página {safePage} de {safeTotalPages}</p>
      <button type="button" className="pagination-button" disabled={safePage === safeTotalPages} onClick={onNext}>
        Siguiente
      </button>
    </nav>
  )
}
