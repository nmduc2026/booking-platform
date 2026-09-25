export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export function buildPageQuery(params: {
  page: number
  size?: number
  q?: string
  [key: string]: string | number | undefined
}) {
  const search = new URLSearchParams()
  search.set("page", String(params.page))
  search.set("size", String(params.size ?? 10))
  for (const [key, value] of Object.entries(params)) {
    if (key === "page" || key === "size") continue
    if (value === undefined || value === "") continue
    search.set(key, String(value))
  }
  return search.toString()
}
