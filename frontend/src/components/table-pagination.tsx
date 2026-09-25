import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination"
import { Button } from "@/components/ui/button"

type Props = {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function TablePagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: Props) {
  const safeTotalPages = Math.max(totalPages, 1)

  return (
    <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
      <p className="text-sm text-muted-foreground">
        {totalElements} item(s) · page {page + 1} / {safeTotalPages}
      </p>
      <Pagination className="mx-0 w-auto justify-end">
        <PaginationContent>
          <PaginationItem>
            <PaginationPrevious
              href="#"
              onClick={(e) => {
                e.preventDefault()
                if (page > 0) onPageChange(page - 1)
              }}
              className={page <= 0 ? "pointer-events-none opacity-50" : undefined}
            />
          </PaginationItem>
          <PaginationItem>
            <Button variant="outline" size="sm" disabled>
              {page + 1}
            </Button>
          </PaginationItem>
          <PaginationItem>
            <PaginationNext
              href="#"
              onClick={(e) => {
                e.preventDefault()
                if (page + 1 < safeTotalPages) onPageChange(page + 1)
              }}
              className={
                page + 1 >= safeTotalPages
                  ? "pointer-events-none opacity-50"
                  : undefined
              }
            />
          </PaginationItem>
        </PaginationContent>
      </Pagination>
    </div>
  )
}
